/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.parquet.columnreaders;

import com.google.common.base.Stopwatch;
import io.netty.buffer.DrillBuf;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.ExecConstants;
import org.apache.hadoop.io.compress.Decompressor;
import org.apache.hadoop.io.compress.DirectDecompressor;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.util.StopWatch;
import org.apache.parquet.hadoop.CodecFactory;
import org.apache.parquet.hadoop.codec.SnappyCodec;
import org.apache.parquet.hadoop.metadata.ColumnChunkMetaData;
import org.apache.drill.exec.util.filereader.DirectBufInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.column.page.DictionaryPage;
import org.apache.parquet.format.PageHeader;
import org.apache.parquet.format.PageType;
import org.apache.parquet.format.Util;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.velocity.runtime.directive.Stop;
import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.apache.parquet.column.Encoding.valueOf;
/**
 * The AyncPageReader reads one page of data at a time asynchronously from the provided InputStream. The
 * first request to the page reader creates a Future Task (AsyncPageReaderTask) and submits it to the
 * scan thread pool. The result of the Future task (a page) is put into a (blocking) queue and the scan
 * thread starts processing the data as soon as the Future task is complete.
 * This is a simple producer-consumer queue, the AsyncPageReaderTask is the producer and the ParquetScan is
 * the consumer.
 * The AsyncPageReaderTask submits another Future task for reading the next page as soon as it is done,
 * while the results queue is not full. Until the queue is full, therefore, the scan thread pool keeps the
 * disk as busy as possible.
 * In case the disk is slower than the processing, the queue is never filled up after the processing of the
 * pages begins. In this case, the next disk read begins immediately after the previous read is completed
 * and the disk is never idle. The query in this case is effectively bounded by the disk.
 * If, however, the processing is slower than the disk (can happen with SSDs, data being cached by the
 * FileSystem, or if the processing requires complex processing that is necessarily slow) the queue fills
 * up. Once the queue is full, the AsyncPageReaderTask does not submit any new Future tasks. The next Future
 * task is submitted by the *processing* thread as soon as it pulls a page out of the queue. (Note that the
 * invariant here is that there is space for at least one more page in the queue before the Future read task
 * is submitted to the pool). This sequence is important. Not doing so can lead to deadlocks - producer
 * threads may block on putting data into the queue which is full while the consumer threads might be
 * blocked trying to read from a queue that has no data.
 * The first request to the page reader can be either to load a dictionary page or a data page; this leads
 * to the rather odd looking code in the constructor since the parent PageReader calls
 * loadDictionaryIfExists in the constructor.
 * The Future tasks created are kept in a non blocking queue and the Future object is checked for any
 * exceptions that might have occurred during the execution. The queue of Futures is also used to cancel
 * any pending Futures at close (this may happen as a result of a cancel).
 *
 */
class AsyncPageReader extends PageReader {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AsyncPageReader.class);

  private ExecutorService threadPool;
  private long queueSize;
  private LinkedBlockingQueue<ReadStatus> pageQueue;
  private ConcurrentLinkedQueue<Future<Void>> asyncPageRead;
  private long totalPageValuesRead = 0;

  AsyncPageReader(ColumnReader<?> parentStatus, FileSystem fs, Path path,
      ColumnChunkMetaData columnChunkMetaData) throws ExecutionSetupException {
    super(parentStatus, fs, path, columnChunkMetaData);
    if (threadPool == null && asyncPageRead == null) {
      threadPool = parentColumnReader.parentReader.getOperatorContext().getScanExecutor();
      queueSize  = parentColumnReader.parentReader.readQueueSize;
      pageQueue = new LinkedBlockingQueue<>((int)queueSize);
      asyncPageRead = new ConcurrentLinkedQueue<>();
      asyncPageRead.offer(threadPool.submit(new AsyncPageReaderTask(debugName, pageQueue)));
    }
  }

  @Override
  protected void loadDictionaryIfExists(final ColumnReader<?> parentStatus,
      final ColumnChunkMetaData columnChunkMetaData, final DirectBufInputStream f) throws UserException {
    if (columnChunkMetaData.getDictionaryPageOffset() > 0) {
      try {
        assert(columnChunkMetaData.getDictionaryPageOffset() >= dataReader.getPos() );
        dataReader.skip(columnChunkMetaData.getDictionaryPageOffset() - dataReader.getPos());
      } catch (IOException e) {
        handleAndThrowException(e, "Error Reading dictionary page.");
      }
      // parent constructor may call this method before the thread pool is set.
      if (threadPool == null && asyncPageRead == null) {
        threadPool = parentColumnReader.parentReader.getOperatorContext().getScanExecutor();
        queueSize  = parentColumnReader.parentReader.getFragmentContext().getOptions()
            .getOption(ExecConstants.PARQUET_PAGEREADER_QUEUE_SIZE).num_val;
        pageQueue = new LinkedBlockingQueue<ReadStatus>((int)queueSize);
        asyncPageRead = new ConcurrentLinkedQueue<>();
        asyncPageRead.offer(threadPool.submit(new AsyncPageReaderTask(debugName, pageQueue)));
      }
    }
  }

  private DrillBuf getDecompressedPageData(ReadStatus readStatus) {
    DrillBuf data;
    boolean isDictionary = false;
    synchronized (this) {
      data = readStatus.getPageData();
      readStatus.setPageData(null);
      isDictionary = readStatus.isDictionaryPage;
    }
    if (parentColumnReader.columnChunkMetaData.getCodec() != CompressionCodecName.UNCOMPRESSED) {
      DrillBuf compressedData = data;
      data = decompress(readStatus.getPageHeader(), compressedData);
      synchronized (this) {
        readStatus.setPageData(null);
      }
      compressedData.release();
    } else {
      if (isDictionary) {
        stats.totalDictPageReadBytes.addAndGet(readStatus.bytesRead);
      } else {
        stats.totalDataPageReadBytes.addAndGet(readStatus.bytesRead);
      }
    }
    return data;
  }

  // Read and decode the dictionary and the header
  private void readDictionaryPage( final ColumnReader<?> parentStatus) throws UserException {
    try {
      Stopwatch timer = Stopwatch.createStarted();
      ReadStatus readStatus = null;
      synchronized(pageQueue) {
        boolean pageQueueFull = pageQueue.remainingCapacity() == 0;
        asyncPageRead.poll().get(); // get the result of execution
        readStatus = pageQueue.take(); // get the data if no exception has been thrown
        assert (readStatus.pageData != null);
        //if the queue was full before we took a page out, then there would
        // have been no new read tasks scheduled. In that case, schedule a new read.
        if (pageQueueFull) {
          asyncPageRead.offer(threadPool.submit(new AsyncPageReaderTask(debugName, pageQueue)));
        }
      }
      long timeBlocked = timer.elapsed(TimeUnit.NANOSECONDS);
      stats.timeDiskScanWait.addAndGet(timeBlocked);
      stats.timeDiskScan.addAndGet(readStatus.getDiskScanTime());
      stats.numDictPageLoads.incrementAndGet();
      stats.timeDictPageLoads.addAndGet(timeBlocked + readStatus.getDiskScanTime());
      readDictionaryPageData(readStatus, parentStatus);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      handleAndThrowException(e, "Error reading dictionary page.");
    }
  }

  // Read and decode the dictionary data
  private void readDictionaryPageData(final ReadStatus readStatus, final ColumnReader<?> parentStatus)
      throws UserException {
    try {
      pageHeader = readStatus.getPageHeader();
      int uncompressedSize = pageHeader.getUncompressed_page_size();
      final DrillBuf dictionaryData = getDecompressedPageData(readStatus);
      Stopwatch timer = Stopwatch.createStarted();
      allocatedDictionaryBuffers.add(dictionaryData);
      DictionaryPage page = new DictionaryPage(asBytesInput(dictionaryData, 0, uncompressedSize),
          pageHeader.uncompressed_page_size, pageHeader.dictionary_page_header.num_values,
          valueOf(pageHeader.dictionary_page_header.encoding.name()));
      this.dictionary = page.getEncoding().initDictionary(parentStatus.columnDescriptor, page);
      long timeToDecode = timer.elapsed(TimeUnit.NANOSECONDS);
      stats.timeDictPageDecode.addAndGet(timeToDecode);
    } catch (Exception e) {
      handleAndThrowException(e, "Error decoding dictionary page.");
    }
  }

  private void handleAndThrowException(Exception e, String msg) throws UserException {
    UserException ex = UserException.dataReadError(e).message(msg)
        .pushContext("Row Group Start: ", this.parentColumnReader.columnChunkMetaData.getStartingPos())
        .pushContext("Column: ", this.parentColumnReader.schemaElement.getName())
        .pushContext("File: ", this.fileName).build(logger);
    throw ex;
  }

  private DrillBuf decompress(PageHeader pageHeader, DrillBuf compressedData) {
    DrillBuf pageDataBuf = null;
    Stopwatch timer = Stopwatch.createUnstarted();
    long timeToRead;
    int compressedSize = pageHeader.getCompressed_page_size();
    int uncompressedSize = pageHeader.getUncompressed_page_size();
    pageDataBuf = allocateTemporaryBuffer(uncompressedSize);
    try {
      timer.start();
      CompressionCodecName codecName = parentColumnReader.columnChunkMetaData.getCodec();
      ByteBuffer input = compressedData.nioBuffer(0, compressedSize);
      ByteBuffer output = pageDataBuf.nioBuffer(0, uncompressedSize);
      DecompressionHelper decompressionHelper = new DecompressionHelper(codecName);
      decompressionHelper.decompress(input, compressedSize, output, uncompressedSize);
      pageDataBuf.writerIndex(uncompressedSize);
      timeToRead = timer.elapsed(TimeUnit.NANOSECONDS);
      this.updateStats(pageHeader, "Decompress", 0, timeToRead, compressedSize, uncompressedSize);
    } catch (IOException e) {
      handleAndThrowException(e, "Error decompressing data.");
    }
    return pageDataBuf;
  }

  @Override
  protected void nextInternal() throws IOException {
    ReadStatus readStatus = null;
    String name = parentColumnReader.columnChunkMetaData.toString();
    try {
      Stopwatch timer = Stopwatch.createStarted();
      parentColumnReader.parentReader.getOperatorContext().getStats().startWait();
      logger.trace("PERF - Operator [{}] Stop Processing.",
          this.parentColumnReader.parentReader.getFragmentContext().getFragIdString() + ":"
              + this.parentColumnReader.parentReader.getOperatorContext().getStats().getId());
      logger.trace("PERF - Operator [{}] Start Disk Wait.",
          this.parentColumnReader.parentReader.getFragmentContext().getFragIdString() + ":"
              + this.parentColumnReader.parentReader.getOperatorContext().getStats().getId());
      asyncPageRead.poll().get(); // get the result of execution
      synchronized(pageQueue) {
        boolean pageQueueFull = pageQueue.remainingCapacity() == 0;
        readStatus = pageQueue.take(); // get the data if no exception has been thrown
        if (readStatus.pageData == null || readStatus == ReadStatus.EMPTY) {
          throw new DrillRuntimeException("Unexpected end of data");
        }
        //if the queue was full before we took a page out, then there would
        // have been no new read tasks scheduled. In that case, schedule a new read.
        if (pageQueueFull) {
          asyncPageRead.offer(threadPool.submit(new AsyncPageReaderTask(debugName, pageQueue)));
        }
      }
      long timeBlocked = timer.elapsed(TimeUnit.NANOSECONDS);
      parentColumnReader.parentReader.getOperatorContext().getStats().stopWait();
      stats.timeDiskScanWait.addAndGet(timeBlocked);
      stats.timeDiskScan.addAndGet(readStatus.getDiskScanTime());
      logger.trace("PERF - Operator [{}] Stop Disk Wait. Waited {} ms.",
          this.parentColumnReader.parentReader.getFragmentContext().getFragIdString() + ":"
              + this.parentColumnReader.parentReader.getOperatorContext().getStats().getId(),
          ((double) timeBlocked) / 1000000);
      logger.trace("PERF - Operator [{}] Start Processing.",
          this.parentColumnReader.parentReader.getFragmentContext().getFragIdString() + ":"
              + this.parentColumnReader.parentReader.getOperatorContext().getStats().getId());
      if (readStatus.isDictionaryPage) {
        stats.numDictPageLoads.incrementAndGet();
        stats.timeDictPageLoads.addAndGet(timeBlocked + readStatus.getDiskScanTime());
      } else {
        stats.numDataPageLoads.incrementAndGet();
        stats.timeDataPageLoads.addAndGet(timeBlocked + readStatus.getDiskScanTime());
      }
      pageHeader = readStatus.getPageHeader();

    // TODO - figure out if we need multiple dictionary pages, I believe it may be limited to one
    // I think we are clobbering parts of the dictionary if there can be multiple pages of dictionary

      do {
        if (pageHeader.getType() == PageType.DICTIONARY_PAGE) {
          readDictionaryPageData(readStatus, parentColumnReader);
          asyncPageRead.poll().get(); // get the result of execution
          synchronized(pageQueue) {
            boolean pageQueueFull = pageQueue.remainingCapacity() == 0;
            readStatus = pageQueue.take(); // get the data if no exception has been thrown
            if (readStatus.pageData == null || readStatus == ReadStatus.EMPTY) {
              break;
            }
            //if the queue was full before we took a page out, then there would
            // have been no new read tasks scheduled. In that case, schedule a new read.
            if (pageQueueFull) {
              asyncPageRead.offer(threadPool.submit(new AsyncPageReaderTask(debugName, pageQueue)));
            }
          }
          assert (readStatus.pageData != null);
          pageHeader = readStatus.getPageHeader();
        }
      } while (pageHeader.getType() == PageType.DICTIONARY_PAGE);

    pageHeader = readStatus.getPageHeader();
    pageData = getDecompressedPageData(readStatus);
    assert(pageData != null);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Exception e){
      handleAndThrowException(e, "Error reading page data");
    }

  }

  @Override public void clear() {
    while (asyncPageRead != null && !asyncPageRead.isEmpty()) {
      try {
        Future<Void> f = asyncPageRead.poll();
        if(!f.isDone() && !f.isCancelled()){
          f.cancel(true);
        } else {
          f.get(1, TimeUnit.MILLISECONDS);
        }
      } catch (Exception e) {
        // Do nothing.
      }
    }

    //Empty the page queue
    String name = parentColumnReader.columnChunkMetaData.toString();
    ReadStatus r;
    while (!pageQueue.isEmpty()) {
      r = null;
      try {
        r = pageQueue.take();
        if (r == ReadStatus.EMPTY) {
          break;
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } finally {
        if (r != null && r.pageData != null) {
          r.pageData.release();
        }
      }
    }
    super.clear();
  }

  public static class ReadStatus {
    private PageHeader pageHeader;
    private DrillBuf pageData;
    private boolean isDictionaryPage = false;
    private long bytesRead = 0;
    private long valuesRead = 0;
    private long diskScanTime = 0;

    public static final ReadStatus EMPTY = new ReadStatus();

    public synchronized PageHeader getPageHeader() {
      return pageHeader;
    }

    public synchronized void setPageHeader(PageHeader pageHeader) {
      this.pageHeader = pageHeader;
    }

    public synchronized DrillBuf getPageData() {
      return pageData;
    }

    public synchronized void setPageData(DrillBuf pageData) {
      this.pageData = pageData;
    }

    public synchronized boolean isDictionaryPage() {
      return isDictionaryPage;
    }

    public synchronized void setIsDictionaryPage(boolean isDictionaryPage) {
      this.isDictionaryPage = isDictionaryPage;
    }

    public synchronized long getBytesRead() {
      return bytesRead;
    }

    public synchronized void setBytesRead(long bytesRead) {
      this.bytesRead = bytesRead;
    }

    public synchronized long getValuesRead() {
      return valuesRead;
    }

    public synchronized void setValuesRead(long valuesRead) {
      this.valuesRead = valuesRead;
    }

    public synchronized long getDiskScanTime() {
      return diskScanTime;
    }

    public synchronized void setDiskScanTime(long diskScanTime) {
      this.diskScanTime = diskScanTime;
    }

  }

  private class AsyncPageReaderTask implements Callable<Void> {

    private final AsyncPageReader parent = AsyncPageReader.this;
    private final LinkedBlockingQueue<ReadStatus> queue;
    private final String name;
    private final Stopwatch timer;
    private final ThreadPoolExecutor threadPool;
    private final long nPendingTasksAtCreation;
    private final long nActiveTasksAtCreation;
    private long nPendingTasksAtStart;
    private long nActiveTasksAtStart;

    public AsyncPageReaderTask(String name, LinkedBlockingQueue<ReadStatus> queue) {
      this.name = name;
      this.queue = queue;
      this.timer = Stopwatch.createStarted();
      this.threadPool = (ThreadPoolExecutor)parent.threadPool;
      BlockingQueue<Runnable> q = ((ThreadPoolExecutor) parent.threadPool).getQueue();
      this.nPendingTasksAtCreation = q.size();
      this.nActiveTasksAtCreation = threadPool.getActiveCount();
    }

    @Override
    public Void call() throws IOException {
      final long timeToStart = timer.elapsed(TimeUnit.MICROSECONDS);
      BlockingQueue<Runnable> q = ((ThreadPoolExecutor) parent.threadPool).getQueue();
      this.nPendingTasksAtStart = q.size();
      this.nActiveTasksAtStart = threadPool.getActiveCount();

      logger.trace(
          "PERF - Operator [{}] [{}] Disk Read Task : Time To Start = {} ms, Pending Tasks at Creation = {}, Active Tasks at Creation = {},  Pending Tasks at Start = {}, Active Tasks at Start {}",
          parent.parentColumnReader.parentReader.getFragmentContext().getFragIdString() + ":"
              + parent.parentColumnReader.parentReader.getOperatorContext().getStats().getId(), name,
          timeToStart / 1000, nPendingTasksAtCreation, nActiveTasksAtCreation, nPendingTasksAtStart,
          nActiveTasksAtStart);

      ReadStatus readStatus = new ReadStatus();

      long bytesRead = 0;
      long valuesRead = 0;
      final long totalValuesRead = parent.totalPageValuesRead;
      timer.reset();

      final long totalValuesCount = parent.parentColumnReader.columnChunkMetaData.getValueCount();

      // if we are done, just put a marker object in the queue and we are done.
      logger.trace("[{}]: Total Values COUNT {}  Total Values READ {} ", name, totalValuesCount, totalValuesRead);
      if (totalValuesRead >= totalValuesCount) {
        try {
          queue.put(ReadStatus.EMPTY);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          // Do nothing.
        }
        return null;
      }

      DrillBuf pageData = null;
      timer.reset();
      try {
        long s = parent.dataReader.getPos();
        logger.trace("PERF - Operator [{}] [{}] Disk read start (Page Header). Offset {}",
            parent.parentColumnReader.parentReader.getFragmentContext().getFragIdString() + ":"
                + parent.parentColumnReader.parentReader.getOperatorContext().getStats().getId(), name, s);
        PageHeader pageHeader = Util.readPageHeader(parent.dataReader);
        long e = parent.dataReader.getPos();
        //if (logger.isTraceEnabled()) {
        //  logger.trace("[{}]: Read Page Header : ReadPos = {} : Bytes Read = {} ", name, s, e - s);
        //}
        int compressedSize = pageHeader.getCompressed_page_size();
        s = parent.dataReader.getPos();
        logger.trace("PERF - Operator [{}] [{}] Disk read start. Offset {}, Length {} .",
            parent.parentColumnReader.parentReader.getFragmentContext().getFragIdString() + ":"
                + parent.parentColumnReader.parentReader.getOperatorContext().getStats().getId(), name, s,
            compressedSize);
        pageData = parent.dataReader.getNext(compressedSize);
        bytesRead = compressedSize;
        //e = parent.dataReader.getPos();
        //if (logger.isTraceEnabled()) {
        //  DrillBuf bufStart = pageData.slice(0, compressedSize>100?100:compressedSize);
        //  int endOffset = compressedSize>100?compressedSize-100:0;
        //  DrillBuf bufEnd = pageData.slice(endOffset, compressedSize-endOffset);
        //  logger
        //      .trace("[{}]: Read Page Data : ReadPos = {} : Bytes Read = {} : Buf Start = {} : Buf End = {} ",
        //          name, s, e - s, ByteBufUtil.hexDump(bufStart), ByteBufUtil.hexDump(bufEnd));
        //}

        synchronized (parent) {
          if (pageHeader.getType() == PageType.DICTIONARY_PAGE) {
            readStatus.setIsDictionaryPage(true);
            valuesRead += pageHeader.getDictionary_page_header().getNum_values();
          } else {
            valuesRead += pageHeader.getData_page_header().getNum_values();
            parent.totalPageValuesRead += valuesRead;
          }
          long timeToRead = timer.elapsed(TimeUnit.NANOSECONDS);
          readStatus.setPageHeader(pageHeader);
          readStatus.setPageData(pageData);
          readStatus.setBytesRead(bytesRead);
          readStatus.setValuesRead(valuesRead);
          readStatus.setDiskScanTime(timeToRead);
          logger.trace("PERF - Operator [{}] [{}] Disk read stop. Offset {}, Length {}, Time {} ms .",
              parent.parentColumnReader.parentReader.getFragmentContext().getFragIdString() + ":"
                  + parent.parentColumnReader.parentReader.getOperatorContext().getStats().getId(), name, s,
              compressedSize, ((double) timeToRead) / 1000000);
        }
        synchronized (queue) {
          queue.put(readStatus);
          // if the queue is not full, schedule another read task immediately. If it is then the consumer
          // will schedule a new read task as soon as it removes a page from the queue.
          if (queue.remainingCapacity() > 0) {
            asyncPageRead.offer(parent.threadPool.submit(new AsyncPageReaderTask(debugName, queue)));
          }
        }
        // Do nothing.
      } catch (InterruptedException e) {
        if (pageData != null) {
          pageData.release();
        }
        Thread.currentThread().interrupt();
      } catch (Exception e) {
        if (pageData != null) {
          pageData.release();
        }
        parent.handleAndThrowException(e, "Exception occurred while reading from disk.");
      } finally {
    }
      return null;
    }

  }

  private class DecompressionHelper {
    final CompressionCodecName codecName;

    public DecompressionHelper(CompressionCodecName codecName){
      this.codecName = codecName;
    }

    public void decompress (ByteBuffer input, int compressedSize, ByteBuffer output, int uncompressedSize)
        throws IOException {
      // GZip != thread_safe, so we go off and do our own thing.
      // The hadoop interface does not support ByteBuffer so we incur some
      // expensive copying.
      if (codecName == CompressionCodecName.GZIP) {
        GzipCodec codec = new GzipCodec();
        // DirectDecompressor: @see https://hadoop.apache.org/docs/r2.7.2/api/org/apache/hadoop/io/compress/DirectDecompressor.html
        DirectDecompressor directDecompressor = codec.createDirectDecompressor();
        if (directDecompressor != null) {
          logger.debug("Using GZIP direct decompressor.");
          directDecompressor.decompress(input, output);
        } else {
          logger.debug("Using GZIP (in)direct decompressor.");
          Decompressor decompressor = codec.createDecompressor();
          decompressor.reset();
          byte[] inputBytes = new byte[compressedSize];
          input.position(0);
          input.get(inputBytes);
          decompressor.setInput(inputBytes, 0, inputBytes.length);
          byte[] outputBytes = new byte[uncompressedSize];
          decompressor.decompress(outputBytes, 0, uncompressedSize);
          output.clear();
          output.put(outputBytes);
        }
      } else if (codecName == CompressionCodecName.SNAPPY) {
        // For Snappy, just call the Snappy decompressor directly instead
        // of going thru the DirectDecompressor class.
        // The Snappy codec is itself thread safe, while going thru the DirectDecompressor path
        // seems to have concurrency issues.
        output.clear();
        int size = Snappy.uncompress(input, output);
        output.limit(size);
      } else {
        CodecFactory.BytesDecompressor decompressor = codecFactory.getDecompressor(parentColumnReader.columnChunkMetaData.getCodec());
        decompressor.decompress(input, compressedSize, output, uncompressedSize);
      }
    }


  }

}

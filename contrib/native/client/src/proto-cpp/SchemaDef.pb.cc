// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: SchemaDef.proto

#define INTERNAL_SUPPRESS_PROTOBUF_FIELD_DEPRECATION
#include "SchemaDef.pb.h"

#include <algorithm>

#include <google/protobuf/stubs/common.h>
#include <google/protobuf/stubs/once.h>
#include <google/protobuf/io/coded_stream.h>
#include <google/protobuf/wire_format_lite_inl.h>
#include <google/protobuf/descriptor.h>
#include <google/protobuf/generated_message_reflection.h>
#include <google/protobuf/reflection_ops.h>
#include <google/protobuf/wire_format.h>
// @@protoc_insertion_point(includes)

namespace exec {

namespace {

const ::google::protobuf::Descriptor* NamePart_descriptor_ = NULL;
const ::google::protobuf::internal::GeneratedMessageReflection*
  NamePart_reflection_ = NULL;
const ::google::protobuf::EnumDescriptor* NamePart_Type_descriptor_ = NULL;
const ::google::protobuf::Descriptor* FieldDef_descriptor_ = NULL;
const ::google::protobuf::internal::GeneratedMessageReflection*
  FieldDef_reflection_ = NULL;
const ::google::protobuf::EnumDescriptor* ValueMode_descriptor_ = NULL;

}  // namespace


void protobuf_AssignDesc_SchemaDef_2eproto() {
  protobuf_AddDesc_SchemaDef_2eproto();
  const ::google::protobuf::FileDescriptor* file =
    ::google::protobuf::DescriptorPool::generated_pool()->FindFileByName(
      "SchemaDef.proto");
  GOOGLE_CHECK(file != NULL);
  NamePart_descriptor_ = file->message_type(0);
  static const int NamePart_offsets_[2] = {
    GOOGLE_PROTOBUF_GENERATED_MESSAGE_FIELD_OFFSET(NamePart, type_),
    GOOGLE_PROTOBUF_GENERATED_MESSAGE_FIELD_OFFSET(NamePart, name_),
  };
  NamePart_reflection_ =
    new ::google::protobuf::internal::GeneratedMessageReflection(
      NamePart_descriptor_,
      NamePart::default_instance_,
      NamePart_offsets_,
      GOOGLE_PROTOBUF_GENERATED_MESSAGE_FIELD_OFFSET(NamePart, _has_bits_[0]),
      GOOGLE_PROTOBUF_GENERATED_MESSAGE_FIELD_OFFSET(NamePart, _unknown_fields_),
      -1,
      ::google::protobuf::DescriptorPool::generated_pool(),
      ::google::protobuf::MessageFactory::generated_factory(),
      sizeof(NamePart));
  NamePart_Type_descriptor_ = NamePart_descriptor_->enum_type(0);
  FieldDef_descriptor_ = file->message_type(1);
  static const int FieldDef_offsets_[2] = {
    GOOGLE_PROTOBUF_GENERATED_MESSAGE_FIELD_OFFSET(FieldDef, name_),
    GOOGLE_PROTOBUF_GENERATED_MESSAGE_FIELD_OFFSET(FieldDef, major_type_),
  };
  FieldDef_reflection_ =
    new ::google::protobuf::internal::GeneratedMessageReflection(
      FieldDef_descriptor_,
      FieldDef::default_instance_,
      FieldDef_offsets_,
      GOOGLE_PROTOBUF_GENERATED_MESSAGE_FIELD_OFFSET(FieldDef, _has_bits_[0]),
      GOOGLE_PROTOBUF_GENERATED_MESSAGE_FIELD_OFFSET(FieldDef, _unknown_fields_),
      -1,
      ::google::protobuf::DescriptorPool::generated_pool(),
      ::google::protobuf::MessageFactory::generated_factory(),
      sizeof(FieldDef));
  ValueMode_descriptor_ = file->enum_type(0);
}

namespace {

GOOGLE_PROTOBUF_DECLARE_ONCE(protobuf_AssignDescriptors_once_);
inline void protobuf_AssignDescriptorsOnce() {
  ::google::protobuf::GoogleOnceInit(&protobuf_AssignDescriptors_once_,
                 &protobuf_AssignDesc_SchemaDef_2eproto);
}

void protobuf_RegisterTypes(const ::std::string&) {
  protobuf_AssignDescriptorsOnce();
  ::google::protobuf::MessageFactory::InternalRegisterGeneratedMessage(
    NamePart_descriptor_, &NamePart::default_instance());
  ::google::protobuf::MessageFactory::InternalRegisterGeneratedMessage(
    FieldDef_descriptor_, &FieldDef::default_instance());
}

}  // namespace

void protobuf_ShutdownFile_SchemaDef_2eproto() {
  delete NamePart::default_instance_;
  delete NamePart_reflection_;
  delete FieldDef::default_instance_;
  delete FieldDef_reflection_;
}

void protobuf_AddDesc_SchemaDef_2eproto() {
  static bool already_here = false;
  if (already_here) return;
  already_here = true;
  GOOGLE_PROTOBUF_VERIFY_VERSION;

  ::common::protobuf_AddDesc_Types_2eproto();
  ::google::protobuf::DescriptorPool::InternalAddGeneratedFile(
    "\n\017SchemaDef.proto\022\004exec\032\013Types.proto\"X\n\010"
    "NamePart\022!\n\004type\030\001 \001(\0162\023.exec.NamePart.T"
    "ype\022\014\n\004name\030\002 \001(\t\"\033\n\004Type\022\010\n\004NAME\020\000\022\t\n\005A"
    "RRAY\020\001\"O\n\010FieldDef\022\034\n\004name\030\001 \003(\0132\016.exec."
    "NamePart\022%\n\nmajor_type\030\002 \001(\0132\021.common.Ma"
    "jorType*0\n\tValueMode\022\020\n\014VALUE_VECTOR\020\000\022\007"
    "\n\003RLE\020\001\022\010\n\004DICT\020\002B0\n\033org.apache.drill.ex"
    "ec.protoB\017SchemaDefProtosH\001", 307);
  ::google::protobuf::MessageFactory::InternalRegisterGeneratedFile(
    "SchemaDef.proto", &protobuf_RegisterTypes);
  NamePart::default_instance_ = new NamePart();
  FieldDef::default_instance_ = new FieldDef();
  NamePart::default_instance_->InitAsDefaultInstance();
  FieldDef::default_instance_->InitAsDefaultInstance();
  ::google::protobuf::internal::OnShutdown(&protobuf_ShutdownFile_SchemaDef_2eproto);
}

// Force AddDescriptors() to be called at static initialization time.
struct StaticDescriptorInitializer_SchemaDef_2eproto {
  StaticDescriptorInitializer_SchemaDef_2eproto() {
    protobuf_AddDesc_SchemaDef_2eproto();
  }
} static_descriptor_initializer_SchemaDef_2eproto_;
const ::google::protobuf::EnumDescriptor* ValueMode_descriptor() {
  protobuf_AssignDescriptorsOnce();
  return ValueMode_descriptor_;
}
bool ValueMode_IsValid(int value) {
  switch(value) {
    case 0:
    case 1:
    case 2:
      return true;
    default:
      return false;
  }
}


// ===================================================================

const ::google::protobuf::EnumDescriptor* NamePart_Type_descriptor() {
  protobuf_AssignDescriptorsOnce();
  return NamePart_Type_descriptor_;
}
bool NamePart_Type_IsValid(int value) {
  switch(value) {
    case 0:
    case 1:
      return true;
    default:
      return false;
  }
}

#ifndef _MSC_VER
const NamePart_Type NamePart::NAME;
const NamePart_Type NamePart::ARRAY;
const NamePart_Type NamePart::Type_MIN;
const NamePart_Type NamePart::Type_MAX;
const int NamePart::Type_ARRAYSIZE;
#endif  // _MSC_VER
#ifndef _MSC_VER
const int NamePart::kTypeFieldNumber;
const int NamePart::kNameFieldNumber;
#endif  // !_MSC_VER

NamePart::NamePart()
  : ::google::protobuf::Message() {
  SharedCtor();
}

void NamePart::InitAsDefaultInstance() {
}

NamePart::NamePart(const NamePart& from)
  : ::google::protobuf::Message() {
  SharedCtor();
  MergeFrom(from);
}

void NamePart::SharedCtor() {
  _cached_size_ = 0;
  type_ = 0;
  name_ = const_cast< ::std::string*>(&::google::protobuf::internal::kEmptyString);
  ::memset(_has_bits_, 0, sizeof(_has_bits_));
}

NamePart::~NamePart() {
  SharedDtor();
}

void NamePart::SharedDtor() {
  if (name_ != &::google::protobuf::internal::kEmptyString) {
    delete name_;
  }
  if (this != default_instance_) {
  }
}

void NamePart::SetCachedSize(int size) const {
  GOOGLE_SAFE_CONCURRENT_WRITES_BEGIN();
  _cached_size_ = size;
  GOOGLE_SAFE_CONCURRENT_WRITES_END();
}
const ::google::protobuf::Descriptor* NamePart::descriptor() {
  protobuf_AssignDescriptorsOnce();
  return NamePart_descriptor_;
}

const NamePart& NamePart::default_instance() {
  if (default_instance_ == NULL) protobuf_AddDesc_SchemaDef_2eproto();
  return *default_instance_;
}

NamePart* NamePart::default_instance_ = NULL;

NamePart* NamePart::New() const {
  return new NamePart;
}

void NamePart::Clear() {
  if (_has_bits_[0 / 32] & (0xffu << (0 % 32))) {
    type_ = 0;
    if (has_name()) {
      if (name_ != &::google::protobuf::internal::kEmptyString) {
        name_->clear();
      }
    }
  }
  ::memset(_has_bits_, 0, sizeof(_has_bits_));
  mutable_unknown_fields()->Clear();
}

bool NamePart::MergePartialFromCodedStream(
    ::google::protobuf::io::CodedInputStream* input) {
#define DO_(EXPRESSION) if (!(EXPRESSION)) return false
  ::google::protobuf::uint32 tag;
  while ((tag = input->ReadTag()) != 0) {
    switch (::google::protobuf::internal::WireFormatLite::GetTagFieldNumber(tag)) {
      // optional .exec.NamePart.Type type = 1;
      case 1: {
        if (::google::protobuf::internal::WireFormatLite::GetTagWireType(tag) ==
            ::google::protobuf::internal::WireFormatLite::WIRETYPE_VARINT) {
          int value;
          DO_((::google::protobuf::internal::WireFormatLite::ReadPrimitive<
                   int, ::google::protobuf::internal::WireFormatLite::TYPE_ENUM>(
                 input, &value)));
          if (::exec::NamePart_Type_IsValid(value)) {
            set_type(static_cast< ::exec::NamePart_Type >(value));
          } else {
            mutable_unknown_fields()->AddVarint(1, value);
          }
        } else {
          goto handle_uninterpreted;
        }
        if (input->ExpectTag(18)) goto parse_name;
        break;
      }

      // optional string name = 2;
      case 2: {
        if (::google::protobuf::internal::WireFormatLite::GetTagWireType(tag) ==
            ::google::protobuf::internal::WireFormatLite::WIRETYPE_LENGTH_DELIMITED) {
         parse_name:
          DO_(::google::protobuf::internal::WireFormatLite::ReadString(
                input, this->mutable_name()));
          ::google::protobuf::internal::WireFormat::VerifyUTF8String(
            this->name().data(), this->name().length(),
            ::google::protobuf::internal::WireFormat::PARSE);
        } else {
          goto handle_uninterpreted;
        }
        if (input->ExpectAtEnd()) return true;
        break;
      }

      default: {
      handle_uninterpreted:
        if (::google::protobuf::internal::WireFormatLite::GetTagWireType(tag) ==
            ::google::protobuf::internal::WireFormatLite::WIRETYPE_END_GROUP) {
          return true;
        }
        DO_(::google::protobuf::internal::WireFormat::SkipField(
              input, tag, mutable_unknown_fields()));
        break;
      }
    }
  }
  return true;
#undef DO_
}

void NamePart::SerializeWithCachedSizes(
    ::google::protobuf::io::CodedOutputStream* output) const {
  // optional .exec.NamePart.Type type = 1;
  if (has_type()) {
    ::google::protobuf::internal::WireFormatLite::WriteEnum(
      1, this->type(), output);
  }

  // optional string name = 2;
  if (has_name()) {
    ::google::protobuf::internal::WireFormat::VerifyUTF8String(
      this->name().data(), this->name().length(),
      ::google::protobuf::internal::WireFormat::SERIALIZE);
    ::google::protobuf::internal::WireFormatLite::WriteString(
      2, this->name(), output);
  }

  if (!unknown_fields().empty()) {
    ::google::protobuf::internal::WireFormat::SerializeUnknownFields(
        unknown_fields(), output);
  }
}

::google::protobuf::uint8* NamePart::SerializeWithCachedSizesToArray(
    ::google::protobuf::uint8* target) const {
  // optional .exec.NamePart.Type type = 1;
  if (has_type()) {
    target = ::google::protobuf::internal::WireFormatLite::WriteEnumToArray(
      1, this->type(), target);
  }

  // optional string name = 2;
  if (has_name()) {
    ::google::protobuf::internal::WireFormat::VerifyUTF8String(
      this->name().data(), this->name().length(),
      ::google::protobuf::internal::WireFormat::SERIALIZE);
    target =
      ::google::protobuf::internal::WireFormatLite::WriteStringToArray(
        2, this->name(), target);
  }

  if (!unknown_fields().empty()) {
    target = ::google::protobuf::internal::WireFormat::SerializeUnknownFieldsToArray(
        unknown_fields(), target);
  }
  return target;
}

int NamePart::ByteSize() const {
  int total_size = 0;

  if (_has_bits_[0 / 32] & (0xffu << (0 % 32))) {
    // optional .exec.NamePart.Type type = 1;
    if (has_type()) {
      total_size += 1 +
        ::google::protobuf::internal::WireFormatLite::EnumSize(this->type());
    }

    // optional string name = 2;
    if (has_name()) {
      total_size += 1 +
        ::google::protobuf::internal::WireFormatLite::StringSize(
          this->name());
    }

  }
  if (!unknown_fields().empty()) {
    total_size +=
      ::google::protobuf::internal::WireFormat::ComputeUnknownFieldsSize(
        unknown_fields());
  }
  GOOGLE_SAFE_CONCURRENT_WRITES_BEGIN();
  _cached_size_ = total_size;
  GOOGLE_SAFE_CONCURRENT_WRITES_END();
  return total_size;
}

void NamePart::MergeFrom(const ::google::protobuf::Message& from) {
  GOOGLE_CHECK_NE(&from, this);
  const NamePart* source =
    ::google::protobuf::internal::dynamic_cast_if_available<const NamePart*>(
      &from);
  if (source == NULL) {
    ::google::protobuf::internal::ReflectionOps::Merge(from, this);
  } else {
    MergeFrom(*source);
  }
}

void NamePart::MergeFrom(const NamePart& from) {
  GOOGLE_CHECK_NE(&from, this);
  if (from._has_bits_[0 / 32] & (0xffu << (0 % 32))) {
    if (from.has_type()) {
      set_type(from.type());
    }
    if (from.has_name()) {
      set_name(from.name());
    }
  }
  mutable_unknown_fields()->MergeFrom(from.unknown_fields());
}

void NamePart::CopyFrom(const ::google::protobuf::Message& from) {
  if (&from == this) return;
  Clear();
  MergeFrom(from);
}

void NamePart::CopyFrom(const NamePart& from) {
  if (&from == this) return;
  Clear();
  MergeFrom(from);
}

bool NamePart::IsInitialized() const {

  return true;
}

void NamePart::Swap(NamePart* other) {
  if (other != this) {
    std::swap(type_, other->type_);
    std::swap(name_, other->name_);
    std::swap(_has_bits_[0], other->_has_bits_[0]);
    _unknown_fields_.Swap(&other->_unknown_fields_);
    std::swap(_cached_size_, other->_cached_size_);
  }
}

::google::protobuf::Metadata NamePart::GetMetadata() const {
  protobuf_AssignDescriptorsOnce();
  ::google::protobuf::Metadata metadata;
  metadata.descriptor = NamePart_descriptor_;
  metadata.reflection = NamePart_reflection_;
  return metadata;
}


// ===================================================================

#ifndef _MSC_VER
const int FieldDef::kNameFieldNumber;
const int FieldDef::kMajorTypeFieldNumber;
#endif  // !_MSC_VER

FieldDef::FieldDef()
  : ::google::protobuf::Message() {
  SharedCtor();
}

void FieldDef::InitAsDefaultInstance() {
  major_type_ = const_cast< ::common::MajorType*>(&::common::MajorType::default_instance());
}

FieldDef::FieldDef(const FieldDef& from)
  : ::google::protobuf::Message() {
  SharedCtor();
  MergeFrom(from);
}

void FieldDef::SharedCtor() {
  _cached_size_ = 0;
  major_type_ = NULL;
  ::memset(_has_bits_, 0, sizeof(_has_bits_));
}

FieldDef::~FieldDef() {
  SharedDtor();
}

void FieldDef::SharedDtor() {
  if (this != default_instance_) {
    delete major_type_;
  }
}

void FieldDef::SetCachedSize(int size) const {
  GOOGLE_SAFE_CONCURRENT_WRITES_BEGIN();
  _cached_size_ = size;
  GOOGLE_SAFE_CONCURRENT_WRITES_END();
}
const ::google::protobuf::Descriptor* FieldDef::descriptor() {
  protobuf_AssignDescriptorsOnce();
  return FieldDef_descriptor_;
}

const FieldDef& FieldDef::default_instance() {
  if (default_instance_ == NULL) protobuf_AddDesc_SchemaDef_2eproto();
  return *default_instance_;
}

FieldDef* FieldDef::default_instance_ = NULL;

FieldDef* FieldDef::New() const {
  return new FieldDef;
}

void FieldDef::Clear() {
  if (_has_bits_[1 / 32] & (0xffu << (1 % 32))) {
    if (has_major_type()) {
      if (major_type_ != NULL) major_type_->::common::MajorType::Clear();
    }
  }
  name_.Clear();
  ::memset(_has_bits_, 0, sizeof(_has_bits_));
  mutable_unknown_fields()->Clear();
}

bool FieldDef::MergePartialFromCodedStream(
    ::google::protobuf::io::CodedInputStream* input) {
#define DO_(EXPRESSION) if (!(EXPRESSION)) return false
  ::google::protobuf::uint32 tag;
  while ((tag = input->ReadTag()) != 0) {
    switch (::google::protobuf::internal::WireFormatLite::GetTagFieldNumber(tag)) {
      // repeated .exec.NamePart name = 1;
      case 1: {
        if (::google::protobuf::internal::WireFormatLite::GetTagWireType(tag) ==
            ::google::protobuf::internal::WireFormatLite::WIRETYPE_LENGTH_DELIMITED) {
         parse_name:
          DO_(::google::protobuf::internal::WireFormatLite::ReadMessageNoVirtual(
                input, add_name()));
        } else {
          goto handle_uninterpreted;
        }
        if (input->ExpectTag(10)) goto parse_name;
        if (input->ExpectTag(18)) goto parse_major_type;
        break;
      }

      // optional .common.MajorType major_type = 2;
      case 2: {
        if (::google::protobuf::internal::WireFormatLite::GetTagWireType(tag) ==
            ::google::protobuf::internal::WireFormatLite::WIRETYPE_LENGTH_DELIMITED) {
         parse_major_type:
          DO_(::google::protobuf::internal::WireFormatLite::ReadMessageNoVirtual(
               input, mutable_major_type()));
        } else {
          goto handle_uninterpreted;
        }
        if (input->ExpectAtEnd()) return true;
        break;
      }

      default: {
      handle_uninterpreted:
        if (::google::protobuf::internal::WireFormatLite::GetTagWireType(tag) ==
            ::google::protobuf::internal::WireFormatLite::WIRETYPE_END_GROUP) {
          return true;
        }
        DO_(::google::protobuf::internal::WireFormat::SkipField(
              input, tag, mutable_unknown_fields()));
        break;
      }
    }
  }
  return true;
#undef DO_
}

void FieldDef::SerializeWithCachedSizes(
    ::google::protobuf::io::CodedOutputStream* output) const {
  // repeated .exec.NamePart name = 1;
  for (int i = 0; i < this->name_size(); i++) {
    ::google::protobuf::internal::WireFormatLite::WriteMessageMaybeToArray(
      1, this->name(i), output);
  }

  // optional .common.MajorType major_type = 2;
  if (has_major_type()) {
    ::google::protobuf::internal::WireFormatLite::WriteMessageMaybeToArray(
      2, this->major_type(), output);
  }

  if (!unknown_fields().empty()) {
    ::google::protobuf::internal::WireFormat::SerializeUnknownFields(
        unknown_fields(), output);
  }
}

::google::protobuf::uint8* FieldDef::SerializeWithCachedSizesToArray(
    ::google::protobuf::uint8* target) const {
  // repeated .exec.NamePart name = 1;
  for (int i = 0; i < this->name_size(); i++) {
    target = ::google::protobuf::internal::WireFormatLite::
      WriteMessageNoVirtualToArray(
        1, this->name(i), target);
  }

  // optional .common.MajorType major_type = 2;
  if (has_major_type()) {
    target = ::google::protobuf::internal::WireFormatLite::
      WriteMessageNoVirtualToArray(
        2, this->major_type(), target);
  }

  if (!unknown_fields().empty()) {
    target = ::google::protobuf::internal::WireFormat::SerializeUnknownFieldsToArray(
        unknown_fields(), target);
  }
  return target;
}

int FieldDef::ByteSize() const {
  int total_size = 0;

  if (_has_bits_[1 / 32] & (0xffu << (1 % 32))) {
    // optional .common.MajorType major_type = 2;
    if (has_major_type()) {
      total_size += 1 +
        ::google::protobuf::internal::WireFormatLite::MessageSizeNoVirtual(
          this->major_type());
    }

  }
  // repeated .exec.NamePart name = 1;
  total_size += 1 * this->name_size();
  for (int i = 0; i < this->name_size(); i++) {
    total_size +=
      ::google::protobuf::internal::WireFormatLite::MessageSizeNoVirtual(
        this->name(i));
  }

  if (!unknown_fields().empty()) {
    total_size +=
      ::google::protobuf::internal::WireFormat::ComputeUnknownFieldsSize(
        unknown_fields());
  }
  GOOGLE_SAFE_CONCURRENT_WRITES_BEGIN();
  _cached_size_ = total_size;
  GOOGLE_SAFE_CONCURRENT_WRITES_END();
  return total_size;
}

void FieldDef::MergeFrom(const ::google::protobuf::Message& from) {
  GOOGLE_CHECK_NE(&from, this);
  const FieldDef* source =
    ::google::protobuf::internal::dynamic_cast_if_available<const FieldDef*>(
      &from);
  if (source == NULL) {
    ::google::protobuf::internal::ReflectionOps::Merge(from, this);
  } else {
    MergeFrom(*source);
  }
}

void FieldDef::MergeFrom(const FieldDef& from) {
  GOOGLE_CHECK_NE(&from, this);
  name_.MergeFrom(from.name_);
  if (from._has_bits_[1 / 32] & (0xffu << (1 % 32))) {
    if (from.has_major_type()) {
      mutable_major_type()->::common::MajorType::MergeFrom(from.major_type());
    }
  }
  mutable_unknown_fields()->MergeFrom(from.unknown_fields());
}

void FieldDef::CopyFrom(const ::google::protobuf::Message& from) {
  if (&from == this) return;
  Clear();
  MergeFrom(from);
}

void FieldDef::CopyFrom(const FieldDef& from) {
  if (&from == this) return;
  Clear();
  MergeFrom(from);
}

bool FieldDef::IsInitialized() const {

  return true;
}

void FieldDef::Swap(FieldDef* other) {
  if (other != this) {
    name_.Swap(&other->name_);
    std::swap(major_type_, other->major_type_);
    std::swap(_has_bits_[0], other->_has_bits_[0]);
    _unknown_fields_.Swap(&other->_unknown_fields_);
    std::swap(_cached_size_, other->_cached_size_);
  }
}

::google::protobuf::Metadata FieldDef::GetMetadata() const {
  protobuf_AssignDescriptorsOnce();
  ::google::protobuf::Metadata metadata;
  metadata.descriptor = FieldDef_descriptor_;
  metadata.reflection = FieldDef_reflection_;
  return metadata;
}


// @@protoc_insertion_point(namespace_scope)

}  // namespace exec

// @@protoc_insertion_point(global_scope)

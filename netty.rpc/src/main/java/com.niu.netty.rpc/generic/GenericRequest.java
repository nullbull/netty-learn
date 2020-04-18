/**
 * Autogenerated by Thrift Compiler (0.8.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package com.niu.netty.rpc.generic;

import org.apache.thrift.protocol.TTupleProtocol;
import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;
import org.apache.thrift.scheme.TupleScheme;

import java.util.*;

/**
 */
public class GenericRequest implements org.apache.thrift.TBase<GenericRequest, GenericRequest._Fields>, java.io.Serializable, Cloneable {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("GenericRequest");

  private static final org.apache.thrift.protocol.TField METHOD_NAME_FIELD_DESC = new org.apache.thrift.protocol.TField("methodName", org.apache.thrift.protocol.TType.STRING, (short)1);
  private static final org.apache.thrift.protocol.TField CLASS_TYPE_FIELD_DESC = new org.apache.thrift.protocol.TField("classType", org.apache.thrift.protocol.TType.LIST, (short)2);
  private static final org.apache.thrift.protocol.TField REQUEST_OBJ_FIELD_DESC = new org.apache.thrift.protocol.TField("requestObj", org.apache.thrift.protocol.TType.LIST, (short)3);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new GenericRequestStandardSchemeFactory());
    schemes.put(TupleScheme.class, new GenericRequestTupleSchemeFactory());
  }

  public String methodName; // required
  public List<String> classType; // required
  public List<String> requestObj; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    METHOD_NAME((short)1, "methodName"),
    CLASS_TYPE((short)2, "classType"),
    REQUEST_OBJ((short)3, "requestObj");

    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // METHOD_NAME
          return METHOD_NAME;
        case 2: // CLASS_TYPE
          return CLASS_TYPE;
        case 3: // REQUEST_OBJ
          return REQUEST_OBJ;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.METHOD_NAME, new org.apache.thrift.meta_data.FieldMetaData("methodName", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.CLASS_TYPE, new org.apache.thrift.meta_data.FieldMetaData("classType", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING))));
    tmpMap.put(_Fields.REQUEST_OBJ, new org.apache.thrift.meta_data.FieldMetaData("requestObj", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING))));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(GenericRequest.class, metaDataMap);
  }

  public GenericRequest() {
  }

  public GenericRequest(
    String methodName,
    List<String> classType,
    List<String> requestObj)
  {
    this();
    this.methodName = methodName;
    this.classType = classType;
    this.requestObj = requestObj;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public GenericRequest(GenericRequest other) {
    if (other.isSetMethodName()) {
      this.methodName = other.methodName;
    }
    if (other.isSetClassType()) {
      List<String> __this__classType = new ArrayList<String>();
      for (String other_element : other.classType) {
        __this__classType.add(other_element);
      }
      this.classType = __this__classType;
    }
    if (other.isSetRequestObj()) {
      List<String> __this__requestObj = new ArrayList<String>();
      for (String other_element : other.requestObj) {
        __this__requestObj.add(other_element);
      }
      this.requestObj = __this__requestObj;
    }
  }

  public GenericRequest deepCopy() {
    return new GenericRequest(this);
  }

  @Override
  public void clear() {
    this.methodName = null;
    this.classType = null;
    this.requestObj = null;
  }

  public String getMethodName() {
    return this.methodName;
  }

  public GenericRequest setMethodName(String methodName) {
    this.methodName = methodName;
    return this;
  }

  public void unsetMethodName() {
    this.methodName = null;
  }

  /** Returns true if field methodName is set (has been assigned a value) and false otherwise */
  public boolean isSetMethodName() {
    return this.methodName != null;
  }

  public void setMethodNameIsSet(boolean value) {
    if (!value) {
      this.methodName = null;
    }
  }

  public int getClassTypeSize() {
    return (this.classType == null) ? 0 : this.classType.size();
  }

  public java.util.Iterator<String> getClassTypeIterator() {
    return (this.classType == null) ? null : this.classType.iterator();
  }

  public void addToClassType(String elem) {
    if (this.classType == null) {
      this.classType = new ArrayList<String>();
    }
    this.classType.add(elem);
  }

  public List<String> getClassType() {
    return this.classType;
  }

  public GenericRequest setClassType(List<String> classType) {
    this.classType = classType;
    return this;
  }

  public void unsetClassType() {
    this.classType = null;
  }

  /** Returns true if field classType is set (has been assigned a value) and false otherwise */
  public boolean isSetClassType() {
    return this.classType != null;
  }

  public void setClassTypeIsSet(boolean value) {
    if (!value) {
      this.classType = null;
    }
  }

  public int getRequestObjSize() {
    return (this.requestObj == null) ? 0 : this.requestObj.size();
  }

  public java.util.Iterator<String> getRequestObjIterator() {
    return (this.requestObj == null) ? null : this.requestObj.iterator();
  }

  public void addToRequestObj(String elem) {
    if (this.requestObj == null) {
      this.requestObj = new ArrayList<String>();
    }
    this.requestObj.add(elem);
  }

  public List<String> getRequestObj() {
    return this.requestObj;
  }

  public GenericRequest setRequestObj(List<String> requestObj) {
    this.requestObj = requestObj;
    return this;
  }

  public void unsetRequestObj() {
    this.requestObj = null;
  }

  /** Returns true if field requestObj is set (has been assigned a value) and false otherwise */
  public boolean isSetRequestObj() {
    return this.requestObj != null;
  }

  public void setRequestObjIsSet(boolean value) {
    if (!value) {
      this.requestObj = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case METHOD_NAME:
      if (value == null) {
        unsetMethodName();
      } else {
        setMethodName((String)value);
      }
      break;

    case CLASS_TYPE:
      if (value == null) {
        unsetClassType();
      } else {
        setClassType((List<String>)value);
      }
      break;

    case REQUEST_OBJ:
      if (value == null) {
        unsetRequestObj();
      } else {
        setRequestObj((List<String>)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case METHOD_NAME:
      return getMethodName();

    case CLASS_TYPE:
      return getClassType();

    case REQUEST_OBJ:
      return getRequestObj();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case METHOD_NAME:
      return isSetMethodName();
    case CLASS_TYPE:
      return isSetClassType();
    case REQUEST_OBJ:
      return isSetRequestObj();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof GenericRequest)
      return this.equals((GenericRequest)that);
    return false;
  }

  public boolean equals(GenericRequest that) {
    if (that == null)
      return false;

    boolean this_present_methodName = true && this.isSetMethodName();
    boolean that_present_methodName = true && that.isSetMethodName();
    if (this_present_methodName || that_present_methodName) {
      if (!(this_present_methodName && that_present_methodName))
        return false;
      if (!this.methodName.equals(that.methodName))
        return false;
    }

    boolean this_present_classType = true && this.isSetClassType();
    boolean that_present_classType = true && that.isSetClassType();
    if (this_present_classType || that_present_classType) {
      if (!(this_present_classType && that_present_classType))
        return false;
      if (!this.classType.equals(that.classType))
        return false;
    }

    boolean this_present_requestObj = true && this.isSetRequestObj();
    boolean that_present_requestObj = true && that.isSetRequestObj();
    if (this_present_requestObj || that_present_requestObj) {
      if (!(this_present_requestObj && that_present_requestObj))
        return false;
      if (!this.requestObj.equals(that.requestObj))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  public int compareTo(GenericRequest other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;
    GenericRequest typedOther = (GenericRequest)other;

    lastComparison = Boolean.valueOf(isSetMethodName()).compareTo(typedOther.isSetMethodName());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetMethodName()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.methodName, typedOther.methodName);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetClassType()).compareTo(typedOther.isSetClassType());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetClassType()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.classType, typedOther.classType);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetRequestObj()).compareTo(typedOther.isSetRequestObj());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetRequestObj()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.requestObj, typedOther.requestObj);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    schemes.get(iprot.getScheme()).getScheme().read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    schemes.get(oprot.getScheme()).getScheme().write(oprot, this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("GenericRequest(");
    boolean first = true;

    sb.append("methodName:");
    if (this.methodName == null) {
      sb.append("null");
    } else {
      sb.append(this.methodName);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("classType:");
    if (this.classType == null) {
      sb.append("null");
    } else {
      sb.append(this.classType);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("requestObj:");
    if (this.requestObj == null) {
      sb.append("null");
    } else {
      sb.append(this.requestObj);
    }
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    try {
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class GenericRequestStandardSchemeFactory implements SchemeFactory {
    public GenericRequestStandardScheme getScheme() {
      return new GenericRequestStandardScheme();
    }
  }

  private static class GenericRequestStandardScheme extends StandardScheme<GenericRequest> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, GenericRequest struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // METHOD_NAME
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.methodName = iprot.readString();
              struct.setMethodNameIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // CLASS_TYPE
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list0 = iprot.readListBegin();
                struct.classType = new ArrayList<String>(_list0.size);
                for (int _i1 = 0; _i1 < _list0.size; ++_i1)
                {
                  String _elem2; // required
                  _elem2 = iprot.readString();
                  struct.classType.add(_elem2);
                }
                iprot.readListEnd();
              }
              struct.setClassTypeIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // REQUEST_OBJ
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list3 = iprot.readListBegin();
                struct.requestObj = new ArrayList<String>(_list3.size);
                for (int _i4 = 0; _i4 < _list3.size; ++_i4)
                {
                  String _elem5; // required
                  _elem5 = iprot.readString();
                  struct.requestObj.add(_elem5);
                }
                iprot.readListEnd();
              }
              struct.setRequestObjIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();

      // check for required fields of primitive type, which can't be checked in the validate method
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, GenericRequest struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.methodName != null) {
        oprot.writeFieldBegin(METHOD_NAME_FIELD_DESC);
        oprot.writeString(struct.methodName);
        oprot.writeFieldEnd();
      }
      if (struct.classType != null) {
        oprot.writeFieldBegin(CLASS_TYPE_FIELD_DESC);
        {
          oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, struct.classType.size()));
          for (String _iter6 : struct.classType)
          {
            oprot.writeString(_iter6);
          }
          oprot.writeListEnd();
        }
        oprot.writeFieldEnd();
      }
      if (struct.requestObj != null) {
        oprot.writeFieldBegin(REQUEST_OBJ_FIELD_DESC);
        {
          oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, struct.requestObj.size()));
          for (String _iter7 : struct.requestObj)
          {
            oprot.writeString(_iter7);
          }
          oprot.writeListEnd();
        }
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class GenericRequestTupleSchemeFactory implements SchemeFactory {
    public GenericRequestTupleScheme getScheme() {
      return new GenericRequestTupleScheme();
    }
  }

  private static class GenericRequestTupleScheme extends TupleScheme<GenericRequest> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, GenericRequest struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetMethodName()) {
        optionals.set(0);
      }
      if (struct.isSetClassType()) {
        optionals.set(1);
      }
      if (struct.isSetRequestObj()) {
        optionals.set(2);
      }
      oprot.writeBitSet(optionals, 3);
      if (struct.isSetMethodName()) {
        oprot.writeString(struct.methodName);
      }
      if (struct.isSetClassType()) {
        {
          oprot.writeI32(struct.classType.size());
          for (String _iter8 : struct.classType)
          {
            oprot.writeString(_iter8);
          }
        }
      }
      if (struct.isSetRequestObj()) {
        {
          oprot.writeI32(struct.requestObj.size());
          for (String _iter9 : struct.requestObj)
          {
            oprot.writeString(_iter9);
          }
        }
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, GenericRequest struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(3);
      if (incoming.get(0)) {
        struct.methodName = iprot.readString();
        struct.setMethodNameIsSet(true);
      }
      if (incoming.get(1)) {
        {
          org.apache.thrift.protocol.TList _list10 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
          struct.classType = new ArrayList<String>(_list10.size);
          for (int _i11 = 0; _i11 < _list10.size; ++_i11)
          {
            String _elem12; // required
            _elem12 = iprot.readString();
            struct.classType.add(_elem12);
          }
        }
        struct.setClassTypeIsSet(true);
      }
      if (incoming.get(2)) {
        {
          org.apache.thrift.protocol.TList _list13 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
          struct.requestObj = new ArrayList<String>(_list13.size);
          for (int _i14 = 0; _i14 < _list13.size; ++_i14)
          {
            String _elem15; // required
            _elem15 = iprot.readString();
            struct.requestObj.add(_elem15);
          }
        }
        struct.setRequestObjIsSet(true);
      }
    }
  }

}


// File generated by hadoop record compiler. Do not edit.
package com.yahoo.zookeeper.proto;

import com.yahoo.jute.*;
public class CreateRequest implements Record {
  private String path;
  private byte[] data;
  private java.util.ArrayList acl;
  private int flags;
  public CreateRequest() {
  }
  public CreateRequest(
        String path,
        byte[] data,
        java.util.ArrayList acl,
        int flags) {
    this.path=path;
    this.data=data;
    this.acl=acl;
    this.flags=flags;
  }
  public String getPath() {
    return path;
  }
  public void setPath(String m_) {
    path=m_;
  }
  public byte[] getData() {
    return data;
  }
  public void setData(byte[] m_) {
    data=m_;
  }
  public java.util.ArrayList getAcl() {
    return acl;
  }
  public void setAcl(java.util.ArrayList m_) {
    acl=m_;
  }
  public int getFlags() {
    return flags;
  }
  public void setFlags(int m_) {
    flags=m_;
  }
  public void serialize(OutputArchive a_, String tag) throws java.io.IOException {
    a_.startRecord(this,tag);
    a_.writeString(path,"path");
    a_.writeBuffer(data,"data");
    {
      a_.startVector(acl,"acl");
      if (acl!= null) {          int len1 = acl.size();
          for(int vidx1 = 0; vidx1<len1; vidx1++) {
            com.yahoo.zookeeper.data.ACL e1 = (com.yahoo.zookeeper.data.ACL) acl.get(vidx1);
    a_.writeRecord(e1,"e1");
          }
      }
      a_.endVector(acl,"acl");
    }
    a_.writeInt(flags,"flags");
    a_.endRecord(this,tag);
  }
  public void deserialize(InputArchive a_, String tag) throws java.io.IOException {
    a_.startRecord(tag);
    path=a_.readString("path");
    data=a_.readBuffer("data");
    {
      Index vidx1 = a_.startVector("acl");
      if (vidx1!= null) {          acl=new java.util.ArrayList();
          for (; !vidx1.done(); vidx1.incr()) {
    com.yahoo.zookeeper.data.ACL e1;
    e1= new com.yahoo.zookeeper.data.ACL();
    a_.readRecord(e1,"e1");
            acl.add(e1);
          }
      }
    a_.endVector("acl");
    }
    flags=a_.readInt("flags");
    a_.endRecord(tag);
}
  public String toString() {
    try {
      java.io.ByteArrayOutputStream s =
        new java.io.ByteArrayOutputStream();
      CsvOutputArchive a_ = 
        new CsvOutputArchive(s);
      a_.startRecord(this,"");
    a_.writeString(path,"path");
    a_.writeBuffer(data,"data");
    {
      a_.startVector(acl,"acl");
      if (acl!= null) {          int len1 = acl.size();
          for(int vidx1 = 0; vidx1<len1; vidx1++) {
            com.yahoo.zookeeper.data.ACL e1 = (com.yahoo.zookeeper.data.ACL) acl.get(vidx1);
    a_.writeRecord(e1,"e1");
          }
      }
      a_.endVector(acl,"acl");
    }
    a_.writeInt(flags,"flags");
      a_.endRecord(this,"");
      return new String(s.toByteArray(), "UTF-8");
    } catch (Throwable ex) {
      ex.printStackTrace();
    }
    return "ERROR";
  }
  public void write(java.io.DataOutput out) throws java.io.IOException {
    BinaryOutputArchive archive = new BinaryOutputArchive(out);
    serialize(archive, "");
  }
  public void readFields(java.io.DataInput in) throws java.io.IOException {
    BinaryInputArchive archive = new BinaryInputArchive(in);
    deserialize(archive, "");
  }
  public int compareTo (Object peer_) throws ClassCastException {
    if (!(peer_ instanceof CreateRequest)) {
      throw new ClassCastException("Comparing different types of records.");
    }
    CreateRequest peer = (CreateRequest) peer_;
    int ret = 0;
    ret = path.compareTo(peer.path);
    if (ret != 0) return ret;
    {
      byte[] my = data;
      byte[] ur = peer.data;
      ret = Utils.compareBytes(my,0,my.length,ur,0,ur.length);
    }
    if (ret != 0) return ret;
    if (ret != 0) return ret;
    ret = (flags == peer.flags)? 0 :((flags<peer.flags)?-1:1);
    if (ret != 0) return ret;
     return ret;
  }
  public boolean equals(Object peer_) {
    if (!(peer_ instanceof CreateRequest)) {
      return false;
    }
    if (peer_ == this) {
      return true;
    }
    CreateRequest peer = (CreateRequest) peer_;
    boolean ret = false;
    ret = path.equals(peer.path);
    if (!ret) return ret;
    ret = Utils.bufEquals(data,peer.data);
    if (!ret) return ret;
    ret = acl.equals(peer.acl);
    if (!ret) return ret;
    ret = (flags==peer.flags);
    if (!ret) return ret;
     return ret;
  }
  public int hashCode() {
    int result = 17;
    int ret;
    ret = path.hashCode();
    result = 37*result + ret;
    ret = data.toString().hashCode();
    result = 37*result + ret;
    ret = acl.hashCode();
    result = 37*result + ret;
    ret = (int)flags;
    result = 37*result + ret;
    return result;
  }
  public static String signature() {
    return "LCreateRequest(sB[LACL(iLId(ss))]i)";
  }
}

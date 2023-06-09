// File generated by hadoop record compiler. Do not edit.
package com.yahoo.zookeeper.proto;

import com.yahoo.jute.*;
public class SetACLRequest implements Record {
  private String path;
  private java.util.ArrayList acl;
  private int version;
  public SetACLRequest() {
  }
  public SetACLRequest(
        String path,
        java.util.ArrayList acl,
        int version) {
    this.path=path;
    this.acl=acl;
    this.version=version;
  }
  public String getPath() {
    return path;
  }
  public void setPath(String m_) {
    path=m_;
  }
  public java.util.ArrayList getAcl() {
    return acl;
  }
  public void setAcl(java.util.ArrayList m_) {
    acl=m_;
  }
  public int getVersion() {
    return version;
  }
  public void setVersion(int m_) {
    version=m_;
  }
  public void serialize(OutputArchive a_, String tag) throws java.io.IOException {
    a_.startRecord(this,tag);
    a_.writeString(path,"path");
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
    a_.writeInt(version,"version");
    a_.endRecord(this,tag);
  }
  public void deserialize(InputArchive a_, String tag) throws java.io.IOException {
    a_.startRecord(tag);
    path=a_.readString("path");
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
    version=a_.readInt("version");
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
    a_.writeInt(version,"version");
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
    if (!(peer_ instanceof SetACLRequest)) {
      throw new ClassCastException("Comparing different types of records.");
    }
    SetACLRequest peer = (SetACLRequest) peer_;
    int ret = 0;
    ret = path.compareTo(peer.path);
    if (ret != 0) return ret;
    if (ret != 0) return ret;
    ret = (version == peer.version)? 0 :((version<peer.version)?-1:1);
    if (ret != 0) return ret;
     return ret;
  }
  public boolean equals(Object peer_) {
    if (!(peer_ instanceof SetACLRequest)) {
      return false;
    }
    if (peer_ == this) {
      return true;
    }
    SetACLRequest peer = (SetACLRequest) peer_;
    boolean ret = false;
    ret = path.equals(peer.path);
    if (!ret) return ret;
    ret = acl.equals(peer.acl);
    if (!ret) return ret;
    ret = (version==peer.version);
    if (!ret) return ret;
     return ret;
  }
  public int hashCode() {
    int result = 17;
    int ret;
    ret = path.hashCode();
    result = 37*result + ret;
    ret = acl.hashCode();
    result = 37*result + ret;
    ret = (int)version;
    result = 37*result + ret;
    return result;
  }
  public static String signature() {
    return "LSetACLRequest(s[LACL(iLId(ss))]i)";
  }
}

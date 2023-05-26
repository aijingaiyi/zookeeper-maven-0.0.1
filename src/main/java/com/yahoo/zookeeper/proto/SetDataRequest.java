// File generated by hadoop record compiler. Do not edit.
package com.yahoo.zookeeper.proto;

import com.yahoo.jute.*;
public class SetDataRequest implements Record {
  private String path;
  private byte[] data;
  private int version;
  public SetDataRequest() {
  }
  public SetDataRequest(
        String path,
        byte[] data,
        int version) {
    this.path=path;
    this.data=data;
    this.version=version;
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
  public int getVersion() {
    return version;
  }
  public void setVersion(int m_) {
    version=m_;
  }
  public void serialize(OutputArchive a_, String tag) throws java.io.IOException {
    a_.startRecord(this,tag);
    a_.writeString(path,"path");
    a_.writeBuffer(data,"data");
    a_.writeInt(version,"version");
    a_.endRecord(this,tag);
  }
  public void deserialize(InputArchive a_, String tag) throws java.io.IOException {
    a_.startRecord(tag);
    path=a_.readString("path");
    data=a_.readBuffer("data");
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
    a_.writeBuffer(data,"data");
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
    if (!(peer_ instanceof SetDataRequest)) {
      throw new ClassCastException("Comparing different types of records.");
    }
    SetDataRequest peer = (SetDataRequest) peer_;
    int ret = 0;
    ret = path.compareTo(peer.path);
    if (ret != 0) return ret;
    {
      byte[] my = data;
      byte[] ur = peer.data;
      ret = Utils.compareBytes(my,0,my.length,ur,0,ur.length);
    }
    if (ret != 0) return ret;
    ret = (version == peer.version)? 0 :((version<peer.version)?-1:1);
    if (ret != 0) return ret;
     return ret;
  }
  public boolean equals(Object peer_) {
    if (!(peer_ instanceof SetDataRequest)) {
      return false;
    }
    if (peer_ == this) {
      return true;
    }
    SetDataRequest peer = (SetDataRequest) peer_;
    boolean ret = false;
    ret = path.equals(peer.path);
    if (!ret) return ret;
    ret = Utils.bufEquals(data,peer.data);
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
    ret = data.toString().hashCode();
    result = 37*result + ret;
    ret = (int)version;
    result = 37*result + ret;
    return result;
  }
  public static String signature() {
    return "LSetDataRequest(sBi)";
  }
}

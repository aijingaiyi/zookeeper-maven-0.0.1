// File generated by hadoop record compiler. Do not edit.
package com.yahoo.zookeeper.proto;

import com.yahoo.jute.*;
public class op_result_t implements Record {
  private int rc;
  private int op;
  private byte[] response;
  public op_result_t() {
  }
  public op_result_t(
        int rc,
        int op,
        byte[] response) {
    this.rc=rc;
    this.op=op;
    this.response=response;
  }
  public int getRc() {
    return rc;
  }
  public void setRc(int m_) {
    rc=m_;
  }
  public int getOp() {
    return op;
  }
  public void setOp(int m_) {
    op=m_;
  }
  public byte[] getResponse() {
    return response;
  }
  public void setResponse(byte[] m_) {
    response=m_;
  }
  public void serialize(OutputArchive a_, String tag) throws java.io.IOException {
    a_.startRecord(this,tag);
    a_.writeInt(rc,"rc");
    a_.writeInt(op,"op");
    a_.writeBuffer(response,"response");
    a_.endRecord(this,tag);
  }
  public void deserialize(InputArchive a_, String tag) throws java.io.IOException {
    a_.startRecord(tag);
    rc=a_.readInt("rc");
    op=a_.readInt("op");
    response=a_.readBuffer("response");
    a_.endRecord(tag);
}
  public String toString() {
    try {
      java.io.ByteArrayOutputStream s =
        new java.io.ByteArrayOutputStream();
      CsvOutputArchive a_ = 
        new CsvOutputArchive(s);
      a_.startRecord(this,"");
    a_.writeInt(rc,"rc");
    a_.writeInt(op,"op");
    a_.writeBuffer(response,"response");
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
    if (!(peer_ instanceof op_result_t)) {
      throw new ClassCastException("Comparing different types of records.");
    }
    op_result_t peer = (op_result_t) peer_;
    int ret = 0;
    ret = (rc == peer.rc)? 0 :((rc<peer.rc)?-1:1);
    if (ret != 0) return ret;
    ret = (op == peer.op)? 0 :((op<peer.op)?-1:1);
    if (ret != 0) return ret;
    {
      byte[] my = response;
      byte[] ur = peer.response;
      ret = Utils.compareBytes(my,0,my.length,ur,0,ur.length);
    }
    if (ret != 0) return ret;
     return ret;
  }
  public boolean equals(Object peer_) {
    if (!(peer_ instanceof op_result_t)) {
      return false;
    }
    if (peer_ == this) {
      return true;
    }
    op_result_t peer = (op_result_t) peer_;
    boolean ret = false;
    ret = (rc==peer.rc);
    if (!ret) return ret;
    ret = (op==peer.op);
    if (!ret) return ret;
    ret = Utils.bufEquals(response,peer.response);
    if (!ret) return ret;
     return ret;
  }
  public int hashCode() {
    int result = 17;
    int ret;
    ret = (int)rc;
    result = 37*result + ret;
    ret = (int)op;
    result = 37*result + ret;
    ret = response.toString().hashCode();
    result = 37*result + ret;
    return result;
  }
  public static String signature() {
    return "Lop_result_t(iiB)";
  }
}

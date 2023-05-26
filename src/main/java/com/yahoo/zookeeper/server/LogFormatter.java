/*
 * Copyright 2008, Yahoo! Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yahoo.zookeeper.server;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.text.DateFormat;
import java.util.Date;

import com.yahoo.jute.BinaryInputArchive;
import com.yahoo.jute.InputArchive;
import com.yahoo.zookeeper.txn.TxnHeader;

public class LogFormatter {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("USAGE: LogFormatter log_file");
            System.exit(2);
        }
        FileInputStream fis = new FileInputStream(args[0]);
        BinaryInputArchive logStream = BinaryInputArchive.getArchive(fis);
        while (true) {
            byte[] bytes = logStream.readBuffer("txnEntry");
            if (bytes.length == 0) {
                // Since we preallocate, we define EOF to be an
                // empty transaction
                throw new EOFException();
            }
            InputArchive ia = BinaryInputArchive
                    .getArchive(new ByteArrayInputStream(bytes));
            TxnHeader hdr = new TxnHeader();
            hdr.deserialize(ia, "hdr");
            System.out.println(DateFormat.getDateTimeInstance(DateFormat.SHORT,
                    DateFormat.LONG).format(new Date(hdr.getTime()))
                    + " "
                    + Long.toHexString(hdr.getClientId())
                    + ":"
                    + hdr.getCxid()
                    + " "
                    + Long.toHexString(hdr.getZxid())
                    + " " + TraceFormatter.op2String(hdr.getType()));
            if (logStream.readByte("EOR") != 'B') {
                ZooLog.logError("Last transaction was partial.");
                throw new EOFException();
            }
        }
    }
}

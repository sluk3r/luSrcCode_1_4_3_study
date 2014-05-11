package org.apache.lucene.index;

/**
 * Copyright 2004 The Apache Software Foundation
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

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.InputStream;
import org.apache.lucene.store.OutputStream;

import java.io.IOException;
import java.util.Vector;

final class SegmentInfos extends Vector {           //wangxc 直接让业务类继承自一Util里的类， 很少见。

    /**
     * The file format version, a negative number.
     */
    /* Works since counter, the old 1st entry, is always >= 0 */
    public static final int FORMAT = -1;

    public int counter = 0;    // used to name new segments
    private long version = 0; //counts how often the index has been changed by adding or deleting docs

    public final SegmentInfo info(int i) {
        return (SegmentInfo) elementAt(i);
    }

    public final void read(Directory directory) throws IOException {

        InputStream input = directory.openFile("segments");
        try {
            int format = input.readInt();
            if (format < 0) {     // file contains explicit format info
                // check that it is a format we can understand
                if (format < FORMAT) //wangxc 这FORMAT是什么意思？
                    throw new IOException("Unknown format version: " + format);
                version = input.readLong(); // read version
                counter = input.readInt(); // read counter //wangxc 这个counter记得什么的个数？
            } else {     // file is in old format without explicit format info
                counter = format; //wangxc 把format直接当counter？
            }

            for (int i = input.readInt(); i > 0; i--) { // read segmentInfos
                SegmentInfo si =
                        new SegmentInfo(input.readString(), input.readInt(), directory); //wangxc 如果自己实现， 底层这些数据格式是个大问题。  可以自己先定义一个简单的格式，以后再朝这个目标改进。
                addElement(si);
            }

            if (format >= 0) {    // in old format the version number may be at the end of the file
                if (input.getFilePointer() >= input.length())   //wangxc 能不能再独立一层出来？缺点是会有性能方面的不足。可以这样， 把这些数据写到数据库， 自己看着也方便。
                    version = 0; // old file format without version number
                else
                    version = input.readLong(); // read version
            }
        } finally {
            input.close();
        }
    }

    public final void write(Directory directory) throws IOException {
        OutputStream output = directory.createFile("segments.new");
        try {
            output.writeInt(FORMAT); // write FORMAT
            output.writeLong(++version); // every write changes the index
            output.writeInt(counter); // write counter
            output.writeInt(size()); // write infos
            for (int i = 0; i < size(); i++) {
                SegmentInfo si = info(i);
                output.writeString(si.name);
                output.writeInt(si.docCount);
            }
        } finally {
            output.close();
        }

        // install new segment info
        directory.renameFile("segments.new", "segments");
    }

    /**
     * version number when this SegmentInfos was generated.
     */
    public long getVersion() {
        return version;
    }

    /**
     * Current version number from segments file.
     */
    public static long readCurrentVersion(Directory directory)
            throws IOException {

        InputStream input = directory.openFile("segments");
        int format = 0;
        long version = 0;
        try {
            format = input.readInt();
            if (format < 0) {
                if (format < FORMAT)
                    throw new IOException("Unknown format version: " + format);
                version = input.readLong(); // read version
            }
        } finally {
            input.close();
        }

        if (format < 0)
            return version;

        // We cannot be sure about the format of the file.
        // Therefore we have to read the whole file and cannot simply seek to the version entry.

        SegmentInfos sis = new SegmentInfos();
        sis.read(directory);
        return sis.getVersion();
    }
}

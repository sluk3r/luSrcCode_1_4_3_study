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

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.InputStream;

import java.io.IOException;

/**
 * Class responsible for access to stored document fields.
 * <p/>
 * It uses &lt;segment&gt;.fdt and &lt;segment&gt;.fdx; files.
 *
 * @version $Id: FieldsReader.java,v 1.7 2004/03/29 22:48:02 cutting Exp $
 */
final class FieldsReader {
    private FieldInfos fieldInfos;
    private InputStream fieldsStream;
    private InputStream indexStream;
    private int size;

    FieldsReader(Directory d, String segment, FieldInfos fn) throws IOException {
        fieldInfos = fn;//wangxc 这个fn是从哪传来的？

        fieldsStream = d.openFile(segment + ".fdt");
        indexStream = d.openFile(segment + ".fdx");

        size = (int) (indexStream.length() / 8);
    }

    final void close() throws IOException {
        fieldsStream.close();
        indexStream.close();
    }

    final int size() {
        return size;
    }

    final Document doc(int n) throws IOException {
        indexStream.seek(n * 8L);
        long position = indexStream.readLong();
        fieldsStream.seek(position);  //wangxc 看来这些Postion信息是在加载时定位用的。   先定位到

        Document doc = new Document();
        int numFields = fieldsStream.readVInt();
        for (int i = 0; i < numFields; i++) {
            int fieldNumber = fieldsStream.readVInt();
            FieldInfo fi = fieldInfos.fieldInfo(fieldNumber);

            byte bits = fieldsStream.readByte(); //wangxc 这个值是个啥？

            doc.add(new Field(fi.name,          // name
                    fieldsStream.readString(), // read value   //wangxc， 如果不Store时， 这个值会是什么样子？ 还能读出来原始值？
                    true,              // stored
                    fi.isIndexed,          // indexed
                    (bits & 1) != 0, fi.storeTermVector)); // vector   //wangxc     这是要直接还原成建索引时的情况。
        }

        return doc;
    }
}
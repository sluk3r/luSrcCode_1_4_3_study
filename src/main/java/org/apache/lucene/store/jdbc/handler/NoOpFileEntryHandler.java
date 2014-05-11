/*
 * Copyright 2004-2006 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.lucene.store.jdbc.handler;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.store.InputStream;
import org.apache.lucene.store.OutputStream;
import org.apache.lucene.store.jdbc.JdbcDirectory;

/**
 * A No Operation file entry handler. Performs no actual dirty operations,
 * and returns empty data for read operations.
 *
 * @author kimchy
 */
public class NoOpFileEntryHandler implements FileEntryHandler {

    private static class NoOpIndexInput extends InputStream {
        @Override
        protected void readInternal(byte[] b, int offset, int length) throws IOException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void close() throws IOException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        protected void seekInternal(long pos) throws IOException {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    }

    private static class NoOpIndexOutput extends OutputStream {
        @Override
        protected void flushBuffer(byte[] b, int len) throws IOException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public long length() throws IOException {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }

    private static InputStream indexInput = new NoOpIndexInput();

    private static OutputStream  indexOutput = new NoOpIndexOutput();

    public void configure(JdbcDirectory jdbcDirectory) {
    }

    public boolean fileExists(final String name) throws IOException {
        return false;
    }

    public long fileModified(final String name) throws IOException {
        return 0;
    }

    public void touchFile(final String name) throws IOException {
    }

    public void deleteFile(final String name) throws IOException {
    }

    public List deleteFiles(List names) throws IOException {
        return null;
    }

    public void renameFile(final String from, final String to) throws IOException {
    }

    public long fileLength(final String name) throws IOException {
        return 0;
    }

    public InputStream openInput(String name) throws IOException {
        return indexInput;
    }

    public OutputStream createOutput(String name) throws IOException {
        return indexOutput;
    }

    public void close() throws IOException {
        // do notihng
    }
}

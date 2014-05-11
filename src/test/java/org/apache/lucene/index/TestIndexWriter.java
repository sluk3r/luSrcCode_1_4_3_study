package org.apache.lucene.index;

import junit.framework.TestCase;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import java.io.IOException;


/**
 * @author goller
 * @version $Id: TestIndexWriter.java,v 1.3 2003/10/13 14:31:38 otis Exp $
 */
public class TestIndexWriter extends TestCase {
    public void testDocCount() {
        Directory dir = new RAMDirectory();

        IndexWriter writer = null;
        IndexReader reader = null;
        int i;

        try {
            writer = new IndexWriter(dir, new WhitespaceAnalyzer(), true);

            // add 100 documents
            for (i = 0; i < 100; i++) {
                addDoc(writer);
            }
            assertEquals(100, writer.docCount());
            writer.close();

            // delete 40 documents
            reader = IndexReader.open(dir);
            for (i = 0; i < 40; i++) {
                reader.delete(i);
            }
            reader.close();

            // test doc count before segments are merged/index is optimized
            writer = new IndexWriter(dir, new WhitespaceAnalyzer(), false);
            assertEquals(100, writer.docCount());
            writer.close();

            reader = IndexReader.open(dir);
            assertEquals(100, reader.maxDoc());
            assertEquals(60, reader.numDocs());
            reader.close();

            // optimize the index and check that the new doc count is correct
            writer = new IndexWriter(dir, new WhitespaceAnalyzer(), false);
            writer.optimize();
            assertEquals(60, writer.docCount());
            writer.close();

            // check that the index reader gives the same numbers.
            reader = IndexReader.open(dir);
            assertEquals(60, reader.maxDoc());
            assertEquals(60, reader.numDocs());
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addDoc(IndexWriter writer) {
        Document doc = new Document();
        doc.add(Field.UnStored("content", "aaa"));

        try {
            writer.addDocument(doc);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
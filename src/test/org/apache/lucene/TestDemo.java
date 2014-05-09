package org.apache.lucene;

import junit.framework.TestCase;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: wangxc
 * Date: 14-5-9
 * Time: 下午10:20
 * To change this template use File | Settings | File Templates.
 */
public class TestDemo extends TestCase {

    public void testDemo() throws IOException {
        doTest("import org.apache.lucene.store.RAMDirectory;", "import", true);
        doTest("import org.apache.lucene.store.RAMDirectory;", "lucene", false);    //wangxc， 搜import没问题， 搜lucene失败。分词的作用？

        doTest("A very simple demo used in the API documentation (src/java/overview.html)", "simple", true);
    }

    public void doTest(String content, String queryValue, boolean assertTrue) throws IOException {
        Directory directory = new RAMDirectory();
        Analyzer analyzer = new StandardAnalyzer();

        IndexWriter indexWriter = new IndexWriter(directory, analyzer, true);
        for (int i = 0; i < 100; i++) {
            Document d = new Document();
            d.add(Field.Text("content", content));

            indexWriter.addDocument(d);
        }
        indexWriter.close();

        IndexReader indexReader = IndexReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);

        Term term = new Term("content", queryValue);
        Query termQuery = new TermQuery(term);
        ScoreDoc[] hits = indexSearcher.search(termQuery, null, 1000).scoreDocs;

        if (assertTrue) {
            assertTrue(hits.length > 0);
        } else {
            assertFalse(hits.length > 0);
        }

        for (int i = 0; i < hits.length; i++) {
            Document hitDoc = indexSearcher.doc(hits[i].doc);
            assertTrue(content.equals(hitDoc.get("content")));
        }

        indexSearcher.close();
    }
}

package org.apache.lucene.store.jdbc;

import junit.framework.TestCase;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.JdbcDirectoryStore;

import javax.sql.DataSource;
import java.io.IOException;

/**
* Created by IntelliJ IDEA.
* User: wangxc
* Date: 14-5-11
* Time: 上午11:32
* To change this template use File | Settings | File Templates.
*/
public class TestJDBCDirectory extends TestCase {

    public void testJDBC() throws IOException {
        String content = "A very simple demo used in the API documentation (src/java/overview.html)";
        String queryValue = "simple";

//        DataSource dataSource =  new DriverManagerDataSource();
//        String tableName = "jdbc_test_table_name";
        Directory directory = new JdbcDirectoryStore();
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

        for (int i = 0; i < hits.length; i++) {
            Document hitDoc = indexSearcher.doc(hits[i].doc);
            assertTrue(content.equals(hitDoc.get("content")));
        }

        indexSearcher.close();
    }
}

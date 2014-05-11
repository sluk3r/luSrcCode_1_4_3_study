package org.apache.lucene.store.jdbc.exception;

/**
 * Created by IntelliJ IDEA.
 * User: wangxc
 * Date: 14-5-11
 * Time: 上午11:20
 * To change this template use File | Settings | File Templates.
 */
public class JDBCException extends RuntimeException {
    public JDBCException(String msg, Throwable root) {
        super(msg, root);
    }

    public JDBCException(String s) {
        super(s);
    }
}

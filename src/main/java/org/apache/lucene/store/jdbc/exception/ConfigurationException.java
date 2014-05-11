package org.apache.lucene.store.jdbc.exception;

/**
 * Created by IntelliJ IDEA.
 * User: wangxc
 * Date: 14-5-11
 * Time: 上午10:55
 * To change this template use File | Settings | File Templates.
 */
public class ConfigurationException extends JDBCException{

    public ConfigurationException(String msg, Throwable root) {
        super(msg, root);
    }

    public ConfigurationException(String s) {
        super(s);
    }
}

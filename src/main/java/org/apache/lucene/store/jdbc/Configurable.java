package org.apache.lucene.store.jdbc;

import org.apache.lucene.store.jdbc.exception.JDBCException;

/**
 * Created by IntelliJ IDEA.
 * User: wangxc
 * Date: 14-5-11
 * Time: 上午10:56
 * To change this template use File | Settings | File Templates.
 */
public interface Configurable {
    void configure(Settings settings) throws JDBCException;
}

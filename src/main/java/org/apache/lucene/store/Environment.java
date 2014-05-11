package org.apache.lucene.store;

/**
 * Created by IntelliJ IDEA.
 * User: wangxc
 * Date: 14-5-11
 * Time: 上午11:23
 * To change this template use File | Settings | File Templates.
 */
public class Environment {

    /**
     * Determines the connection string (like file path)
     */
    public static final String CONNECTION = "compass.engine.connection";

    /**
     * Expert. The sub context of the connection.
     */
    public static final String CONNECTION_SUB_CONTEXT = "compass.engine.connection.subContext";

    /**
     * The name of the compass instance. If Jndi is enabled, will also be the name
     * under which compass will register.
     */
    public static final String NAME = "compass.name";

    public static final String MAPPING_PREFIX = "compass.mapping";
}

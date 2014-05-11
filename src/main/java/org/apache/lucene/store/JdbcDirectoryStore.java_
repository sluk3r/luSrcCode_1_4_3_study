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

package org.apache.lucene.store;

import java.io.IOException;
import java.sql.Connection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;

import org.apache.lucene.store.jdbc.*;
import org.apache.lucene.store.jdbc.datasource.DataSourceUtils;
import org.apache.lucene.store.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.apache.lucene.store.jdbc.dialect.Dialect;
import org.apache.lucene.store.jdbc.dialect.DialectResolver;
import org.apache.lucene.store.jdbc.exception.ConfigurationException;
import org.apache.lucene.store.jdbc.exception.JDBCException;
import org.apache.lucene.store.jdbc.index.FetchPerTransactionJdbcIndexInput;
import org.apache.lucene.store.jdbc.support.JdbcTable;
import org.apache.lucene.util.ClassUtils;

/**
 * @author kimchy
 */
public class JdbcDirectoryStore extends Directory{
    public static final String PROTOCOL = "jdbc://";

    private JdbcDirectorySettings jdbcSettings;

    private DataSource dataSource;

    private DataSourceProvider dataSourceProvider;

    private Dialect dialect;

    private boolean managed;

    private boolean disableSchemaOperation;

    private Map<String, JdbcTable> cachedJdbcTables = new ConcurrentHashMap<String, JdbcTable>();

    public void configure(Settings settings) throws JDBCException {
        String connection = settings.getSetting(Environment.CONNECTION);
        String url = connection.substring(PROTOCOL.length());

        dataSourceProvider = (DataSourceProvider) settings.getSettingAsInstance(LuceneEnvironment.JdbcStore.DataSourceProvider.CLASS,
                DriverManagerDataSourceProvider.class.getName());
        dataSourceProvider.configure(url, settings);
        this.dataSource = dataSourceProvider.getDataSource();

        String dialectClassName = settings.getSetting(LuceneEnvironment.JdbcStore.DIALECT, null);
        if (dialectClassName == null) {
            try {
                dialect = new DialectResolver().getDialect(dataSource);
            } catch (JdbcStoreException e) {
                throw new ConfigurationException("Failed to auto detect dialect", e);
            }
        } else {
            try {
                dialect = (Dialect) ClassUtils.forName(dialectClassName, settings.getClassLoader()).newInstance();
            } catch (Exception e) {
                throw new ConfigurationException("Failed to configure dialect [" + dialectClassName + "]");
            }
        }


        managed = settings.getSettingAsBoolean(LuceneEnvironment.JdbcStore.MANAGED, false);
        if (!managed) {
            this.dataSource = new TransactionAwareDataSourceProxy(this.dataSource);
        }

        disableSchemaOperation = settings.getSettingAsBoolean(LuceneEnvironment.JdbcStore.DISABLE_SCHEMA_OPERATIONS, false);

        jdbcSettings = new JdbcDirectorySettings();
        jdbcSettings.setNameColumnName(settings.getSetting(LuceneEnvironment.JdbcStore.DDL.NAME_NAME, jdbcSettings.getNameColumnName()));
        jdbcSettings.setValueColumnName(settings.getSetting(LuceneEnvironment.JdbcStore.DDL.VALUE_NAME, jdbcSettings.getValueColumnName()));
        jdbcSettings.setSizeColumnName(settings.getSetting(LuceneEnvironment.JdbcStore.DDL.SIZE_NAME, jdbcSettings.getSizeColumnName()));
        jdbcSettings.setLastModifiedColumnName(settings.getSetting(LuceneEnvironment.JdbcStore.DDL.LAST_MODIFIED_NAME, jdbcSettings.getLastModifiedColumnName()));
        jdbcSettings.setDeletedColumnName(settings.getSetting(LuceneEnvironment.JdbcStore.DDL.DELETED_NAME, jdbcSettings.getDeletedColumnName()));

        jdbcSettings.setNameColumnLength(settings.getSettingAsInt(LuceneEnvironment.JdbcStore.DDL.NAME_LENGTH, jdbcSettings.getNameColumnLength()));
        jdbcSettings.setValueColumnLengthInK(settings.getSettingAsInt(LuceneEnvironment.JdbcStore.DDL.VALUE_LENGTH, jdbcSettings.getValueColumnLengthInK()));

        jdbcSettings.setDeleteMarkDeletedDelta(settings.getSettingAsLong(LuceneEnvironment.JdbcStore.DELETE_MARK_DELETED_DELTA, jdbcSettings.getDeleteMarkDeletedDelta()));
        jdbcSettings.setQueryTimeout(settings.getSettingAsInt(LuceneEnvironment.Transaction.LOCK_TIMEOUT, jdbcSettings.getQueryTimeout()));

        try {
            jdbcSettings.setLockClass(settings.getSettingAsClass(LuceneEnvironment.JdbcStore.LOCK_TYPE, jdbcSettings.getLockClass()));
        } catch (ClassNotFoundException e) {
            throw new JDBCException("Failed to create jdbc lock class [" + settings.getSetting(LuceneEnvironment.JdbcStore.LOCK_TYPE) + "]");
        }
//        if (log.isDebugEnabled()) {
//            log.debug("Using lock strategy [" + jdbcSettings.getLockClass().getName() + "]");
//        }

        if (dialect.supportTransactionalScopedBlobs() &&
                !"true".equalsIgnoreCase(settings.getSetting(LuceneEnvironment.JdbcStore.Connection.AUTO_COMMIT, "false"))) {
            // Use FetchPerTransaction is dialect supports it
            jdbcSettings.getDefaultFileEntrySettings().setClassSetting(JdbcFileEntrySettings.INDEX_INPUT_TYPE_SETTING,
                    FetchPerTransactionJdbcIndexInput.class);
//            if (log.isDebugEnabled()) {
//                log.debug("Using transactional blobs (dialect supports it)");
//            }
        } else {
//            if (log.isDebugEnabled()) {
//                log.debug("Using non transactional blobs (dialect does not supports it)");
//            }
        }

        Map fileEntries = settings.getSettingGroups(LuceneEnvironment.JdbcStore.FileEntry.PREFIX);
        for (Iterator it = fileEntries.keySet().iterator(); it.hasNext();) {
            String fileEntryName = (String) it.next();
            Settings compassFeSettings = (Settings) fileEntries.get(fileEntryName);
            JdbcFileEntrySettings jdbcFileEntrySettings = jdbcSettings.getFileEntrySettingsWithoutDefault(fileEntryName);
            if (jdbcFileEntrySettings == null) {
                jdbcFileEntrySettings = new JdbcFileEntrySettings();
            }
            // iterate over all the settings and copy them to the jdbc settings
            for (Iterator feIt = compassFeSettings.keySet().iterator(); feIt.hasNext();) {
                String feSetting = (String) feIt.next();
                jdbcFileEntrySettings.setSetting(feSetting, compassFeSettings.getSetting(feSetting));
            }
            jdbcSettings.registerFileEntrySettings(fileEntryName, jdbcFileEntrySettings);
        }
    }

    public Directory open(String subContext, String subIndex) throws JDBCException {
        String totalPath = subContext + "_" + subIndex;
        JdbcTable jdbcTable = cachedJdbcTables.get(totalPath);
        if (jdbcTable == null) {
            jdbcTable = new JdbcTable(jdbcSettings, dialect, totalPath);
            cachedJdbcTables.put(totalPath, jdbcTable);
        }
        JdbcDirectory dir = new JdbcDirectory(dataSource, jdbcTable);
        if (!disableSchemaOperation) {
            try {
                dir.create();
            } catch (IOException e) {
                throw new JDBCException("Failed to create dir [" + totalPath + "]", e);
            }
        }
        return dir;
    }

    public Boolean indexExists(Directory dir) throws JDBCException {
        try {
            // for databases that fail if there is no table (like postgres)
            if (dialect.supportsTableExists()) {
                boolean tableExists = ((JdbcDirectory) dir).tableExists();
                if (!tableExists) {
                    return Boolean.FALSE;
                }
            }
        } catch (IOException e) {
//            log.warn("Failed to check if index exists", e);
        } catch (UnsupportedOperationException e) {
            // do nothing, let the base class check for it
        }
        return null;
    }

    public void deleteIndex(Directory dir, String subContext, String subIndex) throws JDBCException {
        try {
            if (disableSchemaOperation) {
                ((JdbcDirectory) dir).deleteContent();
            } else {
                ((JdbcDirectory) dir).delete();
            }
        } catch (IOException e) {
            throw new JDBCException("Failed to delete index [" + subIndex + "]", e);
        }
    }

    public void cleanIndex(Directory dir, String subContext, String subIndex) throws JDBCException {
        JdbcDirectory jdbcDirectory = (JdbcDirectory) dir;
        try {
            jdbcDirectory.deleteContent();
        } catch (IOException e) {
            throw new JDBCException("Failed to delete content of [" + subIndex + "]", e);
        }
    }

    public void performScheduledTasks(Directory dir, String subContext, String subIndex) throws JDBCException {
        try {
            ((JdbcDirectory) dir).deleteMarkDeleted();
        } catch (IOException e) {
            throw new JDBCException("Failed to delete mark deleted with jdbc for [" + subIndex + "]", e);
        }
    }

//    public CopyFromHolder beforeCopyFrom(String subContext, String subIndex, Directory dir) throws JDBCException {
//        try {
//            ((JdbcDirectory) dir).deleteContent();
//        } catch (IOException e) {
//            throw new JDBCException("Failed to delete index content");
//        }
//        return new CopyFromHolder();
//    }

//    public void registerEventListeners(SearchEngine searchEngine, SearchEngineEventManager eventManager) {
//        if (managed) {
//            eventManager.registerLifecycleListener(new ManagedEventListeners());
//        } else {
//            eventManager.registerLifecycleListener(new NoneManagedEventListeners());
//        }
//    }

    @Override
    public String[] list() throws IOException {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean fileExists(String name) throws IOException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long fileModified(String name) throws IOException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void touchFile(String name) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void deleteFile(String name) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void renameFile(String from, String to) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long fileLength(String name) throws IOException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public OutputStream createFile(String name) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public InputStream openFile(String name) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Lock makeLock(String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void close() {
        this.dataSourceProvider.closeDataSource();
    }

    /**
     * The Jdbc store does require transactional context when executing async operations.
     */
    public boolean requiresAsyncTransactionalContext() {
        return true;
    }

    private class ManagedEventListeners implements SearchEngineLifecycleEventListener {

        public void beforeBeginTransaction() throws JDBCException {

        }

        public void afterBeginTransaction() throws JDBCException {

        }

        public void afterPrepare() throws JDBCException {

        }

        public void afterCommit(boolean onePhase) throws JDBCException {
            Connection conn;
            try {
                conn = DataSourceUtils.getConnection(dataSource);
            } catch (JdbcStoreException e) {
                throw new JDBCException("Failed to get connection", e);
            }
            FetchPerTransactionJdbcIndexInput.releaseBlobs(conn);
            DataSourceUtils.releaseConnection(conn);
        }

        public void afterRollback() throws JDBCException {
            Connection conn;
            try {
                conn = DataSourceUtils.getConnection(dataSource);
            } catch (JdbcStoreException e) {
                throw new JDBCException("Failed to get connection", e);
            }
            FetchPerTransactionJdbcIndexInput.releaseBlobs(conn);
            DataSourceUtils.releaseConnection(conn);
        }

        public void close() throws JDBCException {

        }
    }

    private class NoneManagedEventListeners implements SearchEngineLifecycleEventListener {

        private Connection connection;

        public void beforeBeginTransaction() throws JDBCException {
            try {
                connection = DataSourceUtils.getConnection(dataSource);
            } catch (JdbcStoreException e) {
                throw new JDBCException("Failed to open db connection", e);
            }
        }

        public void afterBeginTransaction() throws JDBCException {
        }

        public void afterPrepare() throws JDBCException {
        }

        public void afterCommit(boolean onePhase) throws JDBCException {
            try {
                DataSourceUtils.commitConnectionIfPossible(connection);
            } catch (JdbcStoreException e) {
                throw new JDBCException("Failed to commit database transcation", e);
            } finally {
                DataSourceUtils.releaseConnection(connection);
                this.connection = null;
            }
        }

        public void afterRollback() throws JDBCException {
            try {
                DataSourceUtils.rollbackConnectionIfPossible(connection);
            } catch (JdbcStoreException e) {
                throw new JDBCException("Failed to rollback database transcation", e);
            } finally {
                DataSourceUtils.releaseConnection(connection);
                this.connection = null;
            }
        }

        public void close() throws JDBCException {
        }
    }
}

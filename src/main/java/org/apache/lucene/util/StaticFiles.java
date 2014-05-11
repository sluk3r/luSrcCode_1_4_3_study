package org.apache.lucene.util;

import java.util.HashSet;
import java.util.Set;

/**
 * @author kimchy
 */
public class StaticFiles {

    private static final Set<String> staticFiles;

    static {
        staticFiles = new HashSet<String>();
        staticFiles.add("segments.gen");
//        staticFiles.add(DefaultLuceneSearchEngineIndexManager.CLEAR_CACHE_NAME);
//        staticFiles.add(DefaultLuceneSpellCheckManager.SPELL_CHECK_VERSION_FILENAME);
    }

    public static boolean isStaticFile(String name) {
        return staticFiles.contains(name);
    }
}

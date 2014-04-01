/*
 * Copyright (c) 2008-2013, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.client;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.nearcache.ClientNearCache;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.config.NearCacheConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.monitor.NearCacheStats;
import com.hazelcast.test.AssertTask;
import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.HazelcastTestSupport;
import com.hazelcast.test.annotation.ProblematicTest;
import com.hazelcast.test.annotation.QuickTest;
import java.util.HashSet;
import java.util.concurrent.Future;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static com.hazelcast.test.HazelcastTestSupport.*;
import static org.junit.Assert.*;

@RunWith(HazelcastParallelClassRunner.class)
@Category(QuickTest.class)
public class ClientNearCacheTest {

    private static final int MAX_CACHE_SIZE = 100;
    private static final int MAX_TTL_SECONDS = 3;
    private static final int MAX_IDLE_SECONDS = 1;

    private static final String NEAR_CACHE_WITH_DEFAULT_CONFIG = "NEAR_CACHE_WITH_DEFAULT_CONFIG";
    private static final String NEAR_CACHE_WITH_NO_INVALIDATION = "NEAR_CACHE_WITH_NO_INVALIDATION";
    private static final String NEAR_CACHE_WITH_MAX_SIZE = "NEAR_CACHE_WITH_MAX_SIZE";
    private static final String NEAR_CACHE_WITH_TTL = "NEAR_CACHE_WITH_TTL";
    private static final String NEAR_CACHE_WITH_IDLE = "NEAR_CACHE_WITH_IDLE";
    private static final String NEAR_CACHE_WITH_INVALIDATION = "NEAR_CACHE_WITH_INVALIDATION";

    private static HazelcastInstance h1;
    private static HazelcastInstance h2;
    private static HazelcastInstance client;

    @BeforeClass
    public static void setup() throws Exception {
        h1 = Hazelcast.newHazelcastInstance();
        h2 = Hazelcast.newHazelcastInstance();

        ClientConfig clientConfig = new ClientConfig();

        NearCacheConfig basicConfig = new NearCacheConfig();
        basicConfig.setInMemoryFormat(InMemoryFormat.OBJECT);
        clientConfig.addNearCacheConfig(NEAR_CACHE_WITH_DEFAULT_CONFIG + "*", basicConfig);

        NearCacheConfig basicConfigNoInvalidation = new NearCacheConfig();
        basicConfig.setInMemoryFormat(InMemoryFormat.OBJECT);
        basicConfigNoInvalidation.setInvalidateOnChange(false);
        clientConfig.addNearCacheConfig(NEAR_CACHE_WITH_NO_INVALIDATION +"*", basicConfigNoInvalidation);

        NearCacheConfig maxSizeConfig = new NearCacheConfig();
        maxSizeConfig.setMaxSize(MAX_CACHE_SIZE);
        clientConfig.addNearCacheConfig(NEAR_CACHE_WITH_MAX_SIZE + "*", maxSizeConfig);

        NearCacheConfig ttlConfig = new NearCacheConfig();
        ttlConfig.setTimeToLiveSeconds(MAX_TTL_SECONDS);
        clientConfig.addNearCacheConfig(NEAR_CACHE_WITH_TTL + "*", ttlConfig);

        NearCacheConfig idleConfig = new NearCacheConfig();
        idleConfig.setMaxIdleSeconds(MAX_IDLE_SECONDS);
        clientConfig.addNearCacheConfig(NEAR_CACHE_WITH_IDLE + "*", idleConfig);

        NearCacheConfig invalidateConfig = new NearCacheConfig();
        invalidateConfig.setInvalidateOnChange(true);
        clientConfig.addNearCacheConfig(NEAR_CACHE_WITH_INVALIDATION + "*", invalidateConfig);

        client = HazelcastClient.newHazelcastClient(clientConfig);
    }

    @AfterClass
    public static void cleanup() throws Exception {
        HazelcastClient.shutdownAll();
        Hazelcast.shutdownAll();
    }

    @Test
    public void testNearCacheFasterThanGoingToTheCluster() {
        final IMap map = client.getMap(randomMapName(NEAR_CACHE_WITH_DEFAULT_CONFIG));

        final int size = 2007;
        for (int i = 0; i < size; i++) {
            map.put(i, i);
        }

        long begin = System.currentTimeMillis();
        for (int i = 0; i < size; i++) {
            map.get(i);
        }
        long readFromClusterTime = System.currentTimeMillis() - begin;

        begin = System.currentTimeMillis();
        for (int i = 0; i < size; i++) {
            map.get(i);
        }
        long readFromCacheTime = System.currentTimeMillis() - begin;

        assertTrue("readFromCacheTime > readFromClusterTime", readFromCacheTime < readFromClusterTime);
    }

    @Test
    public void testGetAllChecksNearCacheFirst() throws Exception {
        final IMap map = client.getMap(randomMapName(NEAR_CACHE_WITH_DEFAULT_CONFIG));
        final HashSet keys = new HashSet();

        final int size = 1003;
        for (int i = 0; i < size; i++) {
            map.put(i, i);
            keys.add(i);
        }
        //populate near cache
        for (int i = 0; i < size; i++) {
            map.get(i);
        }
        //getAll generates the near cache hits
        map.getAll(keys);

        NearCacheStats stats = map.getLocalMapStats().getNearCacheStats();
        assertEquals(size, stats.getOwnedEntryCount());
        assertEquals(size, stats.getHits());
    }

    @Test
    public void testGetAllPopulatesNearCache() throws Exception {
        final IMap map = client.getMap(randomMapName(NEAR_CACHE_WITH_DEFAULT_CONFIG));
        final HashSet keys = new HashSet();

        final int size = 1214;
        for (int i = 0; i < size; i++) {
            map.put(i, i);
            keys.add(i);
        }
        //getAll populates near cache
        map.getAll(keys);

        NearCacheStats stats = map.getLocalMapStats().getNearCacheStats();
        assertEquals(size, stats.getOwnedEntryCount());
    }

    @Test
    public void testGetAsync() throws Exception {
        final IMap map = client.getMap(randomMapName(NEAR_CACHE_WITH_DEFAULT_CONFIG));

        int size = 1009;
        for (int i = 0; i < size; i++) {
            map.put(i, i);
        }
        //populate near cache
        for (int i = 0; i < size; i++) {
            map.get(i);
        }
        //generate near cache hits with async call
        for (int i = 0; i < size; i++) {
            Future async = map.getAsync(i);
            async.get();
        }
        NearCacheStats stats = map.getLocalMapStats().getNearCacheStats();
        assertEquals(size, stats.getOwnedEntryCount());
        assertEquals(size, stats.getHits());
    }

    @Test
    @Category(ProblematicTest.class)
    public void testGetAsyncPopulatesNearCache() throws Exception {
        final IMap map = client.getMap(randomMapName(NEAR_CACHE_WITH_DEFAULT_CONFIG));

        int size = 1239;
        for (int i = 0; i < size; i++) {
            map.put(i, i);
        }
        //populate near cache
        for (int i = 0; i < size; i++) {
            Future async = map.getAsync(i);
            async.get();
        }
        //generate near cache hits with async call
        for (int i = 0; i < size; i++) {
            map.get(i);
        }
        NearCacheStats stats = map.getLocalMapStats().getNearCacheStats();
        assertEquals(size, stats.getOwnedEntryCount());
        assertEquals(size, stats.getHits());
    }

    @Test
    @Category(ProblematicTest.class)
    public void testRemovedKeyValueNotInNearCache() throws Exception {
        final IMap map = client.getMap(randomMapName(NEAR_CACHE_WITH_DEFAULT_CONFIG));

        int size = 1247;
        for (int i = 0; i < size; i++) {
            map.put(i, i);
        }
        //populate near cache
        for (int i = 0; i < size; i++) {
            map.get(i);
        }

        for (int i = 0; i < size; i++) {
            map.remove(i);
            assertNull(map.get(i));
        }
    }

    @Test
    public void testNearCachePopulatedAndHitsGenerated() throws Exception {
        final IMap map = client.getMap(randomMapName(NEAR_CACHE_WITH_NO_INVALIDATION));

        final int size = 1278;
        for (int i = 0; i < size; i++) {
            map.put(i, i);
            map.get(i);  //populate near cache
            map.get(i);  //generate near cache hits
        }

        NearCacheStats stats = map.getLocalMapStats().getNearCacheStats();
        System.out.println("stats = " + stats);
        assertEquals(size, stats.getOwnedEntryCount());
        assertEquals(size, stats.getHits());
    }

    @Test
    public void testNearCachePopulatedAndHitsGenerated2() throws Exception {
        final IMap map = client.getMap(randomMapName(NEAR_CACHE_WITH_NO_INVALIDATION));

        final int size = 1278;
        for (int i = 0; i < size; i++) {
            map.put(i, i);
        }
        for (int i = 0; i < size; i++) {
            map.get(i);  //populate near cache
        }
        for (int i = 0; i < size; i++) {
            map.get(i);  //generate near cache hits
        }

        NearCacheStats stats = map.getLocalMapStats().getNearCacheStats();
        System.out.println("stats = " + stats);
        assertEquals(size, stats.getOwnedEntryCount());
        assertEquals(size, stats.getHits());
    }

    @Test
    public void testIssue2009() throws Exception {
        final IMap map = client.getMap(randomMapName(NEAR_CACHE_WITH_DEFAULT_CONFIG));
        NearCacheStats stats = map.getLocalMapStats().getNearCacheStats();
        assertNotNull(stats);
    }

    @Test
    public void testGetNearCacheStatsBeforePopulation() {
        final IMap map = client.getMap(randomMapName(NEAR_CACHE_WITH_NO_INVALIDATION));
        final int size = 101;
        for (int i = 0; i < size; i++) {
            map.put(i, i);
        }
        final NearCacheStats stats = map.getLocalMapStats().getNearCacheStats();
        assertNotNull(stats);
    }

    @Test
    public void testNearCacheMisses() {
        final IMap map = client.getMap(randomMapName(NEAR_CACHE_WITH_NO_INVALIDATION));

        final int size = 1321;
        for (int i = 0; i < size; i++) {
            map.get("NotThere" + i);
        }
        NearCacheStats stats = map.getLocalMapStats().getNearCacheStats();
        assertEquals(size, stats.getMisses());
        assertEquals(size, stats.getOwnedEntryCount());
    }

    @Test
    public void testNearCacheMisses_whenRepeatedOnSameKey() {
        final IMap map = client.getMap(randomMapName(NEAR_CACHE_WITH_DEFAULT_CONFIG));

        final int size = 17;
        for (int i = 0; i < size; i++) {
            map.get("NOT_THERE");
        }

        NearCacheStats stats = map.getLocalMapStats().getNearCacheStats();
        assertEquals(1, stats.getOwnedEntryCount());
        assertEquals(size, stats.getMisses());
    }

    // possible cause : https://github.com/hazelcast/hazelcast/issues/2065
    @Test
    @Category(ProblematicTest.class)
    public void testMapRemove_WithNearCache() {
        final IMap map = client.getMap(randomMapName(NEAR_CACHE_WITH_DEFAULT_CONFIG));

        final int size = 1113;
        for (int i = 0; i < size; i++) {
            map.put(i, i);
        }
        for (int i = 0; i < size; i++) {
            map.get(i);
        }
        for (int i = 0; i < size; i++) {
            map.remove(i);
        }

        NearCacheStats stats = map.getLocalMapStats().getNearCacheStats();
        assertEquals(size, stats.getMisses());
        assertEquals(0, stats.getOwnedEntryCount());
    }

    @Test
    public void testNearCacheMaxSize() {
        final IMap map = client.getMap(randomMapName(NEAR_CACHE_WITH_MAX_SIZE));

        for (int i = 0; i < MAX_CACHE_SIZE + 1; i++) {
            map.put(i, i);
        }
        //populate near cache
        for (int i = 0; i < MAX_CACHE_SIZE + 1; i++) {
            map.get(i);
        }

        final int evictionSize = (int) (MAX_CACHE_SIZE * (ClientNearCache.EVICTION_PERCENTAGE / 100.0));
        final int remainingSize = MAX_CACHE_SIZE - evictionSize;

        HazelcastTestSupport.assertTrueEventually(new AssertTask() {
            @Override
            public void run() throws Exception {
                final NearCacheStats stats = map.getLocalMapStats().getNearCacheStats();
                assertEquals(remainingSize, stats.getOwnedEntryCount());
            }
        });
    }

    @Test
    public void testNearCacheTTLCleanup() {
        final IMap map = client.getMap(randomMapName(NEAR_CACHE_WITH_TTL));

        final int size = 133;
        for (int i = 0; i < size; i++) {
            map.put(i, i);
        }
        //populate near cache
        for (int i = 0; i < size; i++) {
            map.get(i);
        }

        sleepSeconds(ClientNearCache.TTL_CLEANUP_INTERVAL_MILLS / 1000);
        map.get(0);

        final int expectedSize = 1;
        HazelcastTestSupport.assertTrueEventually(new AssertTask() {
            public void run() throws Exception {
                final NearCacheStats stats = map.getLocalMapStats().getNearCacheStats();
                assertEquals(expectedSize, stats.getOwnedEntryCount());
            }
        });
    }

    @Test
    public void testNearCacheIdleRecordsEvicted() {
        final IMap map = client.getMap(randomMapName(NEAR_CACHE_WITH_IDLE));

        final int size = 147;
        for (int i = 0; i < size; i++) {
            map.put(i, i);
        }
        //populate near cache
        for (int i = 0; i < size; i++) {
            map.get(i);
        }
        //generate near cache hits
        for (int i = 0; i < size; i++) {
            map.get(i);
        }

        NearCacheStats stats = map.getLocalMapStats().getNearCacheStats();
        long hitsBeforeIdleExpire = stats.getHits();

        sleepSeconds(MAX_IDLE_SECONDS + 1);

        for (int i = 0; i < size; i++) {
            map.get(i);
        }
        stats = map.getLocalMapStats().getNearCacheStats();

        assertEquals("as the hits are not equal, the entries were not cleared from near cash after MaxIdleSeconds", hitsBeforeIdleExpire, stats.getHits(), size);
    }

    @Test
    public void testNearCacheInvalidateOnChange() {
        final String mapName = randomMapName(NEAR_CACHE_WITH_INVALIDATION);
        final IMap nodeMap = h1.getMap(mapName);
        final IMap clientMap = client.getMap(mapName);

        final int size = 118;
        for (int i = 0; i < size; i++) {
            nodeMap.put(i, i);
        }
        //populate near cache
        for (int i = 0; i < size; i++) {
            clientMap.get(i);
        }

        NearCacheStats stats = clientMap.getLocalMapStats().getNearCacheStats();
        long OwnedEntryCountBeforeInvalidate = stats.getOwnedEntryCount();

        //invalidate near cache from cluster
        for (int i = 0; i < size; i++) {
            nodeMap.put(i, i);
        }

        assertEquals(size, OwnedEntryCountBeforeInvalidate);

        assertTrueEventually(new AssertTask() {
            public void run() throws Exception {
                NearCacheStats stats = clientMap.getLocalMapStats().getNearCacheStats();
                assertEquals(0, stats.getOwnedEntryCount());
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void testNearCacheContainsNullKey() {
        final IMap map = client.getMap(randomMapName(NEAR_CACHE_WITH_DEFAULT_CONFIG));
        map.containsKey(null);
    }

    @Test
    public void testNearCacheContainsKey() {
        final IMap map = client.getMap(randomMapName(NEAR_CACHE_WITH_DEFAULT_CONFIG));
        final Object key = "key";

        map.put(key, "value");
        map.get(key);

        assertTrue(map.containsKey(key));
    }

    @Test
    public void testNearCacheContainsKey_whenKeyAbsent() {
        final IMap map = client.getMap(randomMapName(NEAR_CACHE_WITH_DEFAULT_CONFIG));

        assertFalse(map.containsKey("NOT_THERE"));
    }

    @Test
    @Category(ProblematicTest.class) // one can see stale value on the near cache after map.remove
    public void testNearCacheContainsKey_afterRemove() {
        final IMap map = client.getMap(randomMapName(NEAR_CACHE_WITH_DEFAULT_CONFIG));
        final Object key = "key";

        map.put(key, "value");
        map.get(key);
        map.remove(key);

        assertFalse(map.containsKey(key));
    }
}
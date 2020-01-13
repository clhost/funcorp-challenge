package com.clhost.memes.app.cache;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import com.hazelcast.query.EntryObject;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.PredicateBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CacheInteract {
    private static final String HAZELCAST_LOCK = "hazelcast_lock";
    private static final String HAZELCAST_MEMES_MAP = "memes_map";
    private static final int ONE_MONTH = 2592000;

    private final HazelcastInstance hazelcast;
    private final ILock lock;

    @Autowired
    public CacheInteract() {
        this.hazelcast = HazelcastClient.newHazelcastClient();
        this.lock = hazelcast.getLock(HAZELCAST_LOCK);
        createDistributedMapIfNotExists();
    }

    public List<CacheBucket> acquireAndRanged() {
        lock.lock();

        int now = toSeconds(System.currentTimeMillis());
        EntryObject object = new PredicateBuilder().getEntryObject();
        Predicate predicate = object.get("date").between(now, now - ONE_MONTH);

        IMap<String, CacheBucket> map = hazelcast.getMap(HAZELCAST_MEMES_MAP);
        return new ArrayList<>(map.values(predicate));
    }

    public void releaseLock() {
        lock.unlock();
    }

    public void addBucket(String key, CacheBucket bucket) {
        hazelcast.getMap(HAZELCAST_MEMES_MAP).put(key, bucket);
    }

    public boolean atLeastOneBucketExistsBySource(String source) {
        EntryObject object = new PredicateBuilder().getEntryObject();
        Predicate predicate = object.get("source").equal(source);
        return !hazelcast.getMap(HAZELCAST_MEMES_MAP).values(predicate).isEmpty();
    }

    private void createDistributedMapIfNotExists() {
        /*Collection<DistributedObject> objects = hazelcast.getDistributedObjects();
        for (DistributedObject object : objects) {
            if (object instanceof IMap) {
                IMap map = (IMap) object;
                if (HAZELCAST_MEMES_MAP.equals(map.getName())) return;
            }
        }
        hazelcast.getMap(HAZELCAST_MEMES_MAP).addIndex("date", true);
        hazelcast.getMap(HAZELCAST_MEMES_MAP).addIndex("source", true);*/
    }

    private int toSeconds(long millis) {
        return (int) (millis / 1000);
    }
}

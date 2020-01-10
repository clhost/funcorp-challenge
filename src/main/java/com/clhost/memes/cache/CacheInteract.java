package com.clhost.memes.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RedisInteract {

    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public RedisInteract(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public List<RedisBucket> ranged() {
        return null;
    }

    public List<RedisBucket> ranged(int count) {
        return null;
    }

    public boolean addBucket(String key, RedisBucket bucket) {
        return false;
    }

    public boolean atLeastOneBucketExistsBySource(String source) {
        return false;
    }
}

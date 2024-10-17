package com.lwq.demo02;

import com.lwq.demo02.RedisLock02;
import redis.clients.jedis.Jedis;

import java.util.UUID;

public class RedisLockTest2 {

    private int count = 0;
    private String lockKey = "lock";

    private void call(Jedis jedis) {
        // 加锁
        String requestId = UUID.randomUUID().toString();
        boolean locked = RedisLock02.tryLock(jedis, lockKey,
                requestId, 60);
        try {
            if (locked) {
                for (int i =0; i < 500; i++) {
                    count ++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            RedisLock02.unlock(jedis, lockKey, requestId);
        }
    }

}
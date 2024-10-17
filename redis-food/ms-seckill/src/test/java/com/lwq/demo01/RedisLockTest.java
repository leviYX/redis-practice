package com.lwq.demo01;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.UUID;

public class RedisLockTest {

    private  int count = 0;
    private String lockKey = "lock";

    private void call(Jedis jedis) {
        // 加锁
        boolean locked = RedisLock.tryLock(jedis, lockKey,
                UUID.randomUUID().toString(), 60);
        try {
            if (locked) {
                for (int i = 0; i < 500; i++) {
                    //第二个线程进来，这里读取count要从主存去拿，因为他以前没拿过,要是没锁，就会拿自己的临时存储空间
                    System.out.println(Thread.currentThread().getName() + count);
                    count++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            RedisLock.unlock(jedis, lockKey);
        }
    }

    public static void main(String[] args) throws Exception {
        RedisLockTest redisLockTest = new RedisLockTest();
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMinIdle(1);
        jedisPoolConfig.setMaxTotal(5);
        JedisPool jedisPool = new JedisPool(jedisPoolConfig,
                "127.0.0.1", 6379, 1000, null);

        Thread t1 = new Thread(() -> redisLockTest.call(jedisPool.getResource()));
        Thread t2 = new Thread(() -> redisLockTest.call(jedisPool.getResource()));
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println(redisLockTest.count);
    }
}
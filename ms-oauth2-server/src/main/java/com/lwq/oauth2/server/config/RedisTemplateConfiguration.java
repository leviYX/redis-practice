package com.lwq.oauth2.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

//@Configuration
public class RedisTemplateConfiguration {
    //@Bean
    public RedisConnectionFactory lettuceConnectionFactory(){
        RedisSentinelConfiguration sentinelConfiguration = new RedisSentinelConfiguration()
                //这个就是你在启动哨兵时候的主节点用的名字，你自己起的
                .master("mymaster")
                .sentinel("39.106.106.199",26379)
                .sentinel("39.106.106.199",26380)
                .sentinel("39.106.106.199",26381);
        //指定数据库
        sentinelConfiguration.setDatabase(1);
        //指定密码，启动服务保持一样，这里直接就配一个
        sentinelConfiguration.setPassword("123456");
        //return new JedisConnectionFactory(sentinelConfiguration);//jedis工厂方式
        return new LettuceConnectionFactory(sentinelConfiguration);//Lettuce工厂方式，这个高效
    }
}
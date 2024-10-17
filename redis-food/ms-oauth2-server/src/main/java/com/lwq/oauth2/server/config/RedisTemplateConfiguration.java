package com.lwq.oauth2.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.util.ArrayList;
import java.util.List;

// @Configuration
public class RedisTemplateConfiguration {

    /**
     * Lettuce 配置 Cluster
     */
    // @Bean
    public RedisConnectionFactory lettuceConnectionFactory() {
        RedisClusterConfiguration clusterConfiguration = new RedisClusterConfiguration();
        // 节点
        List<RedisNode> redisNodeList = new ArrayList<>();
        redisNodeList.add(new RedisNode("192.168.10.101", 6371));
        redisNodeList.add(new RedisNode("192.168.10.101", 6372));
        redisNodeList.add(new RedisNode("192.168.10.102", 6373));
        redisNodeList.add(new RedisNode("192.168.10.102", 6374));
        redisNodeList.add(new RedisNode("192.168.10.103", 6375));
        redisNodeList.add(new RedisNode("192.168.10.104", 6376));
        clusterConfiguration.setClusterNodes(redisNodeList);
        // Redis 命令执行时最多转发次数
        clusterConfiguration.setMaxRedirects(5);
        // 密码
        clusterConfiguration.setPassword("123456");
        return new LettuceConnectionFactory(clusterConfiguration);
    }

}
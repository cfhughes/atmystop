package com.chughes.atmystop.abq_data;

import com.chughes.atmystop.common.model.repository.BusUpdateDataRepository;
import io.redisearch.client.Client;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = "com.chughes.atmystop")
@EnableRedisRepositories("com.chughes.atmystop.common.model.repository")
public class AbqDataApplication {

    @Bean
    JedisConnectionFactory jedisConnectionFactory() {
        return new JedisConnectionFactory();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(jedisConnectionFactory());
        return template;
    }

    @Bean
    public Client redisearchClient() {
        return new Client("atmystop", "localhost", 6379);
    }

    public static void main(String[] args) {
        SpringApplication.run(AbqDataApplication.class, args);
    }

}

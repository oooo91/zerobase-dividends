package com.devideds.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@RequiredArgsConstructor
public class CacheConfig {

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private int port;

    //캐시 매니저
    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration conf = RedisCacheConfiguration.defaultCacheConfig()
                //직렬화: 데이터, 오브젝트를 바이트 형태로 변환 (자바가 아닌 환경에도 기능, 호환성) 레디스는 자바 외부의 캐시 서버이기 때문에 직렬화가 필요
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
        return RedisCacheManager.RedisCacheManagerBuilder
                .fromConnectionFactory(redisConnectionFactory)
                .cacheDefaults(conf)
                .build();
    }

    //초기화
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        //싱글 인스턴스
        RedisStandaloneConfiguration conf = new RedisStandaloneConfiguration();
        conf.setHostName(this.host);
        conf.setPort(this.port);
        return new LettuceConnectionFactory(conf); //레디스 커넥션 팩토리 -> 캐시 매니저에 등록
    }
}

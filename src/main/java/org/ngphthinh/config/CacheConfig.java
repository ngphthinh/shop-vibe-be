package org.ngphthinh.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
// Thay vì dùng RedisSerializer.json(), ta dùng JdkSerializationSerializer
        // Nó sẽ biến Object Java thành chuỗi byte nhị phân để lưu vào Redis, cực kỳ an toàn và không sợ lỗi ép kiểu JSON
        RedisSerializer<Object> jdkSerializer = new JdkSerializationRedisSerializer();

        // Thiết lập cấu hình Cache mặc định
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // TTL mặc định cho các tủ là 10 phút
                .disableCachingNullValues()       // Không lưu giá trị null tránh tốn bộ nhớ
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jdkSerializer));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // categories thường thay đổi ít hơn -> Cho sống lâu hơn, ví dụ 30 phút
        cacheConfigurations.put("categories", defaultCacheConfig.entryTtl(Duration.ofMinutes(30)));

        // products có thể thay đổi thường xuyên hơn -> TTL ngắn hơn, ví dụ 5 phút
        cacheConfigurations.put("products", defaultCacheConfig.entryTtl(Duration.ofMinutes(5)));
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
package org.ngphthinh.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class UserClockerService {
    private static final String USER_CLOCKER_PREFIX = "login:failed:";
    private final RedisTemplate<String, String> redis;

    private static final int TIMEOUT_MINUTES = 15;;

    public UserClockerService(@Qualifier("redisTemplate") RedisTemplate<String, String> redis) {
        this.redis = redis;
    }


    public int incrementFailedAttempts(String email) {

        String key = getKey(email);

        Long count = redis.opsForValue().increment(key) ;

        if (count != null && count == 1) {
            redis.expire(key, TIMEOUT_MINUTES, TimeUnit.MINUTES);
        }

        return count != null ? count.intValue() : 0;
    }

    public String getKey(String email) {
        return USER_CLOCKER_PREFIX + email;
    }

    public void resetFailedAttempts(String email) {
        redis.delete(getKey(email));
    }




}

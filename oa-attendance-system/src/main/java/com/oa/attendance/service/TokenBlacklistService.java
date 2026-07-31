package com.oa.attendance.service;

import com.oa.attendance.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * JWT退出登录黑名单
 */
@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);
    private static final String BLACKLIST_PREFIX = "auth:token:blacklist:";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    public void blacklist(String token) {
        if (!StringUtils.hasText(token) || !jwtUtil.validateToken(token)) {
            return;
        }

        long remainingSeconds = jwtUtil.getRemainingSeconds(token);
        if (remainingSeconds <= 0) {
            return;
        }

        try {
            stringRedisTemplate.opsForValue().set(buildKey(token), "1", remainingSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("写入Token黑名单失败: {}", e.getMessage());
        }
    }

    public boolean isBlacklisted(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }

        try {
            Boolean exists = stringRedisTemplate.hasKey(buildKey(token));
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn("读取Token黑名单失败: {}", e.getMessage());
            return false;
        }
    }

    private String buildKey(String token) {
        return BLACKLIST_PREFIX + token;
    }
}

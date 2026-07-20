package com.oa.attendance.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT工具类
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire}")
    private Long expire;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        // HS512 requires at least 64 bytes, pad/truncate as needed
        byte[] key = new byte[64];
        System.arraycopy(keyBytes, 0, key, 0, Math.min(keyBytes.length, 64));
        return Keys.hmacShaKeyFor(key);
    }

    /**
     * 生成JWT Token
     *
     * @param userId 用户ID
     * @return JWT Token
     */
    public String generateToken(Long userId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expire * 1000);

        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 从Token中获取用户ID
     *
     * @param token JWT Token
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return Long.valueOf(claims.getSubject());
    }

    /**
     * 获取Token剩余有效秒数
     *
     * @param token JWT Token
     * @return 剩余秒数
     */
    public long getRemainingSeconds(String token) {
        Claims claims = getClaimsFromToken(token);
        long remainingMillis = claims.getExpiration().getTime() - System.currentTimeMillis();
        return Math.max(remainingMillis / 1000, 0);
    }

    /**
     * 从Token中获取Claims
     *
     * @param token JWT Token
     * @return Claims
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 验证Token是否有效
     *
     * @param token JWT Token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}

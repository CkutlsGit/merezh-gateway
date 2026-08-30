package ru.merezh.gateway.security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.merezh.gateway.security.dto.JwtUserDto;

import java.security.Key;
import java.util.Date;

@Service
@Slf4j
public class JwtValidateService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    public boolean checkValidateAccessToken(String accessToken) {
        try {
            Claims claims = getAllClaims(accessToken);

            return !isTokenExpired(claims) &&
                    "access".equals(claims.get("type"));
        }
        catch (Exception e) {
            log.error("Ошибка при валидации токена - {} - {}", e.getClass(), e.getMessage());

            return false;
        }
    }

    public JwtUserDto getUserDataFromToken(String accessToken) {
        return new JwtUserDto(
                getUserIdFromToken(accessToken),
                getUserRoleFromToken(accessToken)
        );
    }

    private Long getUserIdFromToken(String accessToken) {
        String userIdString = Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(accessToken)
                .getBody()
                .getSubject();

        return Long.valueOf(userIdString);
    }

    private String getUserRoleFromToken(String accessToken) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(accessToken)
                .getBody()
                .get("role").toString();
    }

    private boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    private Claims getAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

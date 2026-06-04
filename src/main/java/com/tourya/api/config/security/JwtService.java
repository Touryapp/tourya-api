package com.tourya.api.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    @Value("${application.security.jwt.secret-key}")
    private String secretKey;
    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails
    ) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        var authorities = userDetails.getAuthorities()
                .stream().
                map(GrantedAuthority::getAuthority)
                .toList();
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .claim("authorities", authorities)
                .signWith(getSignInKey())
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * Roles embebidos en el JWT al login ({@code claim authorities}). El filtro los fusiona con los de BD.
     */
    public List<GrantedAuthority> extractAuthoritiesFromToken(String token) {
        Object raw = extractAllClaims(token).get("authorities");
        if (raw == null) {
            return List.of();
        }
        List<GrantedAuthority> result = new ArrayList<>();
        if (raw instanceof Collection<?> collection) {
            for (Object item : collection) {
                addGrantedAuthority(result, item);
            }
            return result;
        }
        if (raw.getClass().isArray()) {
            for (Object item : (Object[]) raw) {
                addGrantedAuthority(result, item);
            }
            return result;
        }
        addGrantedAuthority(result, raw);
        return result;
    }

    private static void addGrantedAuthority(List<GrantedAuthority> result, Object item) {
        if (item != null && !item.toString().isBlank()) {
            result.add(new SimpleGrantedAuthority(item.toString().trim()));
        }
    }

    public Collection<? extends GrantedAuthority> mergeAuthorities(
            Collection<? extends GrantedAuthority> fromUser,
            Collection<? extends GrantedAuthority> fromToken) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        if (fromUser != null) {
            fromUser.forEach(a -> names.add(a.getAuthority()));
        }
        if (fromToken != null) {
            fromToken.forEach(a -> names.add(a.getAuthority()));
        }
        return names.stream().map(SimpleGrantedAuthority::new).toList();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

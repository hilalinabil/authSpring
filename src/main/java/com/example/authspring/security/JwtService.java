package com.example.authspring.security;


import com.example.authspring.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;

@Service
public class JwtService {

    @Value("${jwt.SECRET_KEY}")
    private String secretKey;

    public String generateToken(User user){
        return Jwts.builder().setSubject(user.getEmail()).
                signWith(
                        getSigningKey(), SignatureAlgorithm.HS256).
                compact();
    }

    private Key getSigningKey(){
        return Keys.hmacShaKeyFor(secretKey.getBytes(
                StandardCharsets.UTF_8
        ));
    }

}

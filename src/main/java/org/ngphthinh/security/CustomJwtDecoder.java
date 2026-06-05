package org.ngphthinh.security;

import lombok.RequiredArgsConstructor;
import org.ngphthinh.dto.request.user.IntrospectRequest;
import org.ngphthinh.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.util.Objects;

@RequiredArgsConstructor
@Component
public class CustomJwtDecoder implements JwtDecoder {

    private final AuthenticationService authenticationService;

    @Value("${jwt.secret-key}")
    private String SECRET_KEY;

    private NimbusJwtDecoder nimbusJwtDecoder = null;


    /*
     * This method will first call the introspect endpoint to check if the token is active.
     * If the token is active, it will decode the token using NimbusJwtDecoder.
     * If the token is not active, it will throw a JwtException.
     */
    @Override
    public Jwt decode(String token) throws JwtException {
        IntrospectRequest introspectRequest = IntrospectRequest.builder().accessToken(token).build();
        var response = authenticationService.introspect(introspectRequest);

        if (!response.isActive()) throw new JwtException("Invalid token");

        if (Objects.isNull(nimbusJwtDecoder)) {
            SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET_KEY.getBytes(), "HS512");
            nimbusJwtDecoder = NimbusJwtDecoder.withSecretKey(secretKeySpec)
                    .macAlgorithm(MacAlgorithm.HS512)
                    .build();
        }

        return nimbusJwtDecoder.decode(token);
    }
}

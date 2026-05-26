package org.example.authservice;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
public class AuthController {

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    public AuthController(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody LoginRequest loginRequest) {

        var userDetails = userDetailsService.loadUserByUsername(loginRequest.username());
        boolean validPassword = passwordEncoder.matches(loginRequest.password(), userDetails.getPassword());

        if (!validPassword) {
            throw new BadCredentialsException("Invalid username or password.");
        }

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("http://127.0.0.1:9000")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .subject(userDetails.getUsername())
                .claim("scope", "user.read user.write")
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        return new TokenResponse(token);
    }
}

record LoginRequest(String username, String password) {
}
record TokenResponse(String accessToken) {
}

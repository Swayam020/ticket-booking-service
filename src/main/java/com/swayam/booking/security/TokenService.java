package com.swayam.booking.security;

import com.swayam.booking.account.AppUser;
import com.swayam.booking.api.Dtos.TokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class TokenService {

	private final JwtEncoder encoder;
	private final Duration ttl;

	public TokenService(JwtEncoder encoder, @Value("${app.jwt.ttl}") Duration ttl) {
		this.encoder = encoder;
		this.ttl = ttl;
	}

	public TokenResponse issue(AppUser user) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(user.getId().toString())
				.claim(SecurityConfig.ROLE_CLAIM, user.getRole().name())
				.issuedAt(now)
				.expiresAt(now.plus(ttl))
				.build();
		String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
				.getTokenValue();
		return new TokenResponse(token, "Bearer", ttl.toSeconds());
	}

}

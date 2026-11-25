package com.swayam.booking.account;

import com.swayam.booking.UnauthorizedException;
import com.swayam.booking.api.Dtos.LoginRequest;
import com.swayam.booking.api.Dtos.RegisterRequest;
import com.swayam.booking.api.Dtos.TokenResponse;
import com.swayam.booking.api.Dtos.UserResponse;
import com.swayam.booking.security.TokenService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AccountService {

	private final AppUserRepository users;
	private final PasswordEncoder encoder;
	private final TokenService tokens;
	// compared against when the email is unknown, so that case costs one BCrypt check like a wrong password does
	// and response time doesn't reveal which emails have accounts
	private final String dummyHash;

	public AccountService(AppUserRepository users, PasswordEncoder encoder, TokenService tokens) {
		this.users = users;
		this.encoder = encoder;
		this.tokens = tokens;
		this.dummyHash = encoder.encode("timing-equaliser-not-a-real-password");
	}

	@Transactional
	public UserResponse register(RegisterRequest request) {
		AppUser user = new AppUser(Emails.normalise(request.email()), encoder.encode(request.password()), Role.USER);
		try {
			users.saveAndFlush(user);
		}
		catch (DataIntegrityViolationException e) {
			throw new EmailAlreadyRegisteredException();
		}
		return new UserResponse(user.getId(), user.getEmail(), user.getRole());
	}

	@Transactional(readOnly = true)
	public TokenResponse login(LoginRequest request) {
		Optional<AppUser> user = users.findByEmail(Emails.normalise(request.email()));
		boolean passwordMatches = encoder.matches(request.password(),
				user.map(AppUser::getPasswordHash).orElse(dummyHash));
		if (user.isEmpty() || !passwordMatches) {
			throw new UnauthorizedException("invalid email or password");
		}
		return tokens.issue(user.get());
	}

}

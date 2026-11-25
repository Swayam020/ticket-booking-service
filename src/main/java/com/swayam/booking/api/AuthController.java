package com.swayam.booking.api;

import com.swayam.booking.account.AccountService;
import com.swayam.booking.api.Dtos.LoginRequest;
import com.swayam.booking.api.Dtos.RegisterRequest;
import com.swayam.booking.api.Dtos.TokenResponse;
import com.swayam.booking.api.Dtos.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class AuthController {

	private final AccountService accounts;

	AuthController(AccountService accounts) {
		this.accounts = accounts;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	UserResponse register(@Valid @RequestBody RegisterRequest request) {
		return accounts.register(request);
	}

	@PostMapping("/login")
	TokenResponse login(@Valid @RequestBody LoginRequest request) {
		return accounts.login(request);
	}

}

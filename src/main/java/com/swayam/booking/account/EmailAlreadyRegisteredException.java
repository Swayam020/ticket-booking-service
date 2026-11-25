package com.swayam.booking.account;

public class EmailAlreadyRegisteredException extends RuntimeException {

	public EmailAlreadyRegisteredException() {
		super("email already registered");
	}

}

package com.swayam.booking.reservation;

public class SeatAlreadyBookedException extends RuntimeException {

	public SeatAlreadyBookedException() {
		super("seat already booked");
	}

}

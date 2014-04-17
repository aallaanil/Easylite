package com.example.easylite.services;

public class EasyServiceException extends Exception {
	private static final long serialVersionUID = 1L;

	public EasyServiceException(String message) {
        super(message);
    }
}
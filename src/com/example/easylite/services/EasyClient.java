package com.example.easylite.services;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;

import com.example.easylite.services.model.Easy;

public interface EasyClient {
	public Easy.LoginResult login(String associateId, String password)
			throws ClientProtocolException, IOException;

	public TimeSheetClient openTimeSheetModule() throws ClientProtocolException, IOException;
}

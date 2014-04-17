package com.example.easylite.services;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;

public class BasicClientImpl {
	protected String lastNavigatedUrl = "";
	protected HttpClient httpClient = null;

	public BasicClientImpl(HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	public BasicClientImpl(HttpClient httpClient, String lastNavigatedUrl) {
		this.httpClient = httpClient;
		this.lastNavigatedUrl = lastNavigatedUrl;
	}
	
	protected HttpResponse executeHttpRequest(HttpRequestBase httpRequest)
			throws ClientProtocolException, IOException {
		HttpResponse resp = httpClient.execute(httpRequest);
		lastNavigatedUrl = httpRequest.getURI().toString();
		return resp;
	}

}

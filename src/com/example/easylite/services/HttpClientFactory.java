package com.example.easylite.services;

import org.apache.http.client.HttpClient;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;

public class HttpClientFactory {
	private static DefaultHttpClient httpClient = null;

	public static HttpClient newHttpClient() {
		if (httpClient == null) {
			httpClient = new DefaultHttpClient();
			httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY,
					CookiePolicy.BROWSER_COMPATIBILITY);
			HttpClientParams.setRedirecting(httpClient.getParams(), true);
		}
		return httpClient;
	}
}

package com.example.easylite.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.example.easylite.services.model.Easy;

public class EasyClientImpl extends BasicClientImpl implements EasyClient {

	public EasyClientImpl(HttpClient httpClient) {
		super(httpClient);
	}

	@Override
	public Easy.LoginResult login(String associateId, String password)
			throws ClientProtocolException, IOException {
		// Get the Login Page.
		String loginPageHtml = getLoginpage();

		// Do the actual login.
		HttpResponse loginResponse = doLogin(loginPageHtml, associateId,
				password);
		EntityUtils.toString(loginResponse.getEntity());

		return null;
	}

	private HttpResponse doLogin(String loginPageHtml, String associateId,
			String password) throws ClientProtocolException, IOException {
		// Read the form data of the login page which needs to be sent back to
		// the server for login.
		Document doc = Jsoup.parse(loginPageHtml);
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("__VIEWSTATE", doc.getElementById(
				"__VIEWSTATE").attr("value")));
		nvps.add(new BasicNameValuePair("__LASTFOCUS", doc.getElementById(
				"__LASTFOCUS").attr("value")));
		nvps.add(new BasicNameValuePair("__EVENTTARGET", doc.getElementById(
				"__EVENTTARGET").attr("value")));
		nvps.add(new BasicNameValuePair("__EVENTARGUMENT", doc.getElementById(
				"__EVENTARGUMENT").attr("value")));
		nvps.add(new BasicNameValuePair("__EVENTVALIDATION", doc
				.getElementById("__EVENTVALIDATION").attr("value")));
		nvps.add(new BasicNameValuePair("txtLanId", associateId));
		nvps.add(new BasicNameValuePair("txtPassword", password));
		nvps.add(new BasicNameValuePair("imgLogin.x", "-872"));
		nvps.add(new BasicNameValuePair("imgLogin.y", "-369"));

		// Do the actual login.
		HttpPost httpPost = new HttpPost(Constants.URL_EASY_LOGIN);
		httpPost.addHeader("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
		httpPost.addHeader("Accept-Encoding", "gzip,deflate,sdch");
		httpPost.addHeader("Accept-Language", "en-US,en;q=0.8");
		httpPost.addHeader("Cache-Control", "max-age=0");
		httpPost.addHeader("Connection", "keep-alive");
		httpPost.addHeader("Host", Constants.EASY_HOST_NAME);
		httpPost.addHeader("User-Agent", Constants.USER_AGENT);
		httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
		httpPost.addHeader("Referer", Constants.URL_EASY_LOGIN);
		httpPost.addHeader("Origin", Constants.URL_EASY);
		httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

		return executeHttpRequest(httpPost);
	}

	private String getLoginpage() throws ClientProtocolException, IOException {
		HttpGet httpGet = new HttpGet(Constants.URL_EASY_LOGIN);
		httpGet.addHeader("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
		httpGet.addHeader("Accept-Encoding", "gzip,deflate,sdch");
		httpGet.addHeader("Accept-Language", "en-US,en;q=0.8");
		httpGet.addHeader("Cache-Control", "max-age=0");
		httpGet.addHeader("Connection", "keep-alive");
		httpGet.addHeader("Host", Constants.EASY_HOST_NAME);
		httpGet.addHeader("User-Agent", Constants.USER_AGENT);

		HttpResponse resp = executeHttpRequest(httpGet);
		return EntityUtils.toString(resp.getEntity());
	}

	private String getTimeSheetModuleUrl() throws ClientProtocolException,
			IOException {
		HttpGet httpGet = new HttpGet(Constants.URL_EASY_TIMESHEET_LINK);
		httpGet.addHeader("Accept", "*/*");
		httpGet.addHeader("Accept-Encoding", "gzip,deflate,sdch");
		httpGet.addHeader("Accept-Language", "en-US,en;q=0.8");
		httpGet.addHeader("DNT", "1");
		httpGet.addHeader("Connection", "keep-alive");
		httpGet.addHeader("Host", Constants.EASY_HOST_NAME);
		httpGet.addHeader("Referer", lastNavigatedUrl); // Constants.EASY_HOST_NAME
		httpGet.addHeader("User-Agent", Constants.USER_AGENT);

		HttpResponse resp = executeHttpRequest(httpGet);
		return EntityUtils.toString(resp.getEntity());
	}

	@Override
	public TimeSheetClient openTimeSheetModule() throws ClientProtocolException, IOException {
		String timesheetModuleUrl = getTimeSheetModuleUrl();
		HttpGet httpGet = new HttpGet(Constants.URL_EASY_TIMESHEET_LINK);
		httpGet.addHeader("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
		httpGet.addHeader("Accept-Encoding", "gzip,deflate,sdch");
		httpGet.addHeader("Accept-Language", "en-US,en;q=0.8");
		httpGet.addHeader("DNT", "1");
		httpGet.addHeader("Connection", "keep-alive");
		httpGet.addHeader("Host", Constants.URL_TECHM_HR_BS);
		httpGet.addHeader("User-Agent", Constants.USER_AGENT);
		HttpResponse resp = executeHttpRequest(httpGet);
		resp.getEntity().consumeContent();
		
		return  new TimeSheetClientImpl(httpClient, timesheetModuleUrl);
	}

}

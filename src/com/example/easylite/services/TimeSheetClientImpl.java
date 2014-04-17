package com.example.easylite.services;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import com.example.easylite.services.model.Result.Status;
import com.example.easylite.services.model.TimeSheet;
import com.example.easylite.services.model.TimeSheet.Data;
import com.example.easylite.services.model.TimeSheet.SubmitResult;
import com.example.easylite.services.model.TimeSheet.TimesheetHourStatus;

public class TimeSheetClientImpl extends BasicClientImpl implements TimeSheetClient {
	private String timeSheetModuleUrl = "";
	private String referer = "";
	private TimeSheetResponse lastRequestTimeSheetResponse = null;
	private int icStateNum;
	private enum Page {
		TIMESHEET,
		TIMESHEET_SUBMIT_RESULT,
	};
	private Page currentPage = Page.TIMESHEET;

	protected TimeSheetClientImpl(HttpClient httpClient, String timeSheetModuleUrl)
			throws ClientProtocolException, IOException {
		super(httpClient, timeSheetModuleUrl);
		this.timeSheetModuleUrl = timeSheetModuleUrl;
	}

	@Override
	public TimeSheet.Data getTimeSheetDataFor(Date requestDate) throws TimeSheetServiceException {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DATE, calendar.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY);
		Date currentWeekStartDate = calendar.getTime();
		calendar.add(Calendar.DATE, 6);
		Date currentWeekEndDate = calendar.getTime();
		if (requestDate.compareTo(currentWeekEndDate) > 0) {
			throw (new TimeSheetServiceException("Request date cannot be a future date."));
		}
		
		if (currentPage == Page.TIMESHEET_SUBMIT_RESULT) {
			goBackToTimeSheetPage();
		}
		
		Date prevousRequestStartDate = currentWeekStartDate;
		Date prevousRequestEndDate = currentWeekEndDate;
		if (lastRequestTimeSheetResponse == null) {
			// If no previous requests, first navigate to the current week timesheet page.
			lastRequestTimeSheetResponse = navigateToCurrentWeekTimeSheetPage();
			prevousRequestStartDate = lastRequestTimeSheetResponse
					.getTimesheetData().getWeekStartDate();
			calendar.setTime(prevousRequestStartDate);
			calendar.add(Calendar.DATE, 6);
			prevousRequestEndDate = calendar.getTime();
		}

		// If the requested date is within the previous request week (Mon-Sun)
		if (requestDate.compareTo(prevousRequestStartDate) >= 0
				&& requestDate.compareTo(prevousRequestEndDate) <= 0) {
			// Requested date is something other than the previous requested date.
			// So post the date change request.
			lastRequestTimeSheetResponse = changeDate(requestDate);
		}
		icStateNum = lastRequestTimeSheetResponse.getIcStateNum();
		return lastRequestTimeSheetResponse.getTimesheetData();
	}

	private TimeSheetResponse parseTimeSheetResponse(String responseHtml) throws ParseException {
		lastRequestTimeSheetResponse = new TimeSheetResponse().parse(responseHtml);
		icStateNum = lastRequestTimeSheetResponse.getIcStateNum();
		return lastRequestTimeSheetResponse;
	}
	
	private TimeSheetResponse changeDate(Date newDate) throws TimeSheetServiceException {
		try {
			int icStateNum = lastRequestTimeSheetResponse.getIcStateNum();

			// Try changing the date.
			String response = tryChangeDate(newDate,
					String.format("%d", icStateNum), "TL_LINK_WRK_REFRESH_ICN");

			// If the response contains, "You have unsaved Data on this page.&nbsp;
			// Click OK to go back and save, or Cancel to continue."
			// Retry changing the date by dismissing the dialog with Cancel button.
			if (response.contains("You have unsaved Data on this page.")) {
				response = tryChangeDate(newDate,
						String.format("%d", ++icStateNum), "#ICCancel");
			}

			return parseTimeSheetResponse(response);
		} catch (java.text.ParseException e) {
			throw (new TimeSheetServiceException(
					"Error while parsing the change date http response. Addition info: "
							+ e.getMessage()));
		} catch (Exception e) {
			throw (new TimeSheetServiceException(
					"Error while changing the date. Addition info: "
							+ e.getMessage()));
		}
	}

	private String tryChangeDate(Date newDate, String icStateNum,
			String icAction) throws ClientProtocolException, IOException {
		HttpPost httpPost = new HttpPost(Constants.URL_TIMESHEET_MODULE);
		httpPost.addHeader("Accept", "*/*");
		httpPost.addHeader("Accept-Encoding", "gzip,deflate,sdch");
		httpPost.addHeader("Accept-Language", "en-US,en;q=0.8");
		httpPost.addHeader("Connection", "keep-alive");
		httpPost.addHeader("Host", Constants.TECHM_HR_BS_HOST);
		httpPost.addHeader("User-Agent", Constants.USER_AGENT);
		httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
		httpPost.addHeader("Referer", referer);
		httpPost.addHeader("Origin", Constants.URL_TECHM_HR_BS);
		Data tsData = lastRequestTimeSheetResponse.getTimesheetData();
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("ICAJAX", "1"));
		nvps.add(new BasicNameValuePair("ICNAVTYPEDROPDOWN", "1"));
		nvps.add(new BasicNameValuePair("ICType", "Panel"));
		nvps.add(new BasicNameValuePair("ICElementNum", "0"));
		nvps.add(new BasicNameValuePair("ICStateNum", icStateNum));
		nvps.add(new BasicNameValuePair("ICAction", icAction));
		nvps.add(new BasicNameValuePair("ICXPos", "0"));
		nvps.add(new BasicNameValuePair("ICYPos", "0"));
		nvps.add(new BasicNameValuePair("ResponsetoDiffFrame", "-1"));
		nvps.add(new BasicNameValuePair("TargetFrameName", "None"));
		nvps.add(new BasicNameValuePair("ICFocus", ""));
		nvps.add(new BasicNameValuePair("ICSaveWarningFilter", "0"));
		nvps.add(new BasicNameValuePair("ICChanged", "-1"));
		nvps.add(new BasicNameValuePair("ICResubmit", "0"));
		nvps.add(new BasicNameValuePair("ICSID", lastRequestTimeSheetResponse
				.getIcsId()));
		nvps.add(new BasicNameValuePair("ICModalWidget", "0"));
		nvps.add(new BasicNameValuePair("ICZoomGrid", "0"));
		nvps.add(new BasicNameValuePair("ICZoomGridRt", "0"));
		nvps.add(new BasicNameValuePair("ICModalLongClosed", ""));
		nvps.add(new BasicNameValuePair("ICActionPrompt", "false"));
		nvps.add(new BasicNameValuePair("ICTypeAheadID", ""));
		nvps.add(new BasicNameValuePair("ICFind", ""));
		nvps.add(new BasicNameValuePair("ICAddCount", ""));
		nvps.add(new BasicNameValuePair("DATE_DAY1", new SimpleDateFormat(
				"dd/MM/yyyy").format(tsData.getWeekStartDate())));
		nvps.add(new BasicNameValuePair("TL_TR_WEEK_WRK_USER_FIELD_1$0", "PA"));
		nvps.add(new BasicNameValuePair("PROJECT$0", tsData
				.getSelectedProjectId()));
		nvps.add(new BasicNameValuePair("USER_FIELD_3$0", tsData.getTaskId()));
		addTimeSheetHours(tsData.getTimeSheetHoursForWeek(),
				tsData.getTimeSheetHourStatesForWeek(), nvps);
		httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		HttpResponse httpResp = executeHttpRequest(httpPost);
		return EntityUtils.toString(httpResp.getEntity());
	}

	private void addTimeSheetHours(String[] hours, TimesheetHourStatus[] hourStates,
			List<NameValuePair> nvps) {
		int i = 0;
		for (TimesheetHourStatus hourState : hourStates) {
			if (hourState != TimesheetHourStatus.DISABLED) {
				nvps.add(new BasicNameValuePair(String.format("QTY_DAY%d$0", i + 1), hours[i]));
			}
			i = i + 1;
		}
	}

	@Override
	public TimeSheet.SubmitResult submitTimeSheet(Date date, String[] timesheetHours)
			throws TimeSheetServiceException {
		if (timesheetHours == null) {
			throw(new TimeSheetServiceException("Timesheet hour values not set."));
		}
		if (currentPage == Page.TIMESHEET_SUBMIT_RESULT) {
			goBackToTimeSheetPage();
		}
		HttpPost httpPost = new HttpPost(Constants.URL_TIMESHEET_MODULE);
		httpPost.addHeader("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
		httpPost.addHeader("Accept-Encoding", "gzip,deflate,sdch");
		httpPost.addHeader("Accept-Language", "en-US,en;q=0.8");
		httpPost.addHeader("Connection", "keep-alive");
		httpPost.addHeader("Cache-Control", "max-age=0");
		httpPost.addHeader("Host", Constants.TECHM_HR_BS_HOST);
		httpPost.addHeader("User-Agent", Constants.USER_AGENT);
		httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
		httpPost.addHeader("Referer", Constants.URL_TIMESHEET_MODULE);
		httpPost.addHeader("Origin", Constants.URL_TECHM_HR_BS);
		
		Data tsData = lastRequestTimeSheetResponse.getTimesheetData();
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("ICAJAX", "1"));
		nvps.add(new BasicNameValuePair("ICNAVTYPEDROPDOWN", "1"));
		nvps.add(new BasicNameValuePair("ICType", "Panel"));
		nvps.add(new BasicNameValuePair("ICElementNum", "0"));
		nvps.add(new BasicNameValuePair("ICStateNum", String.format("%d", icStateNum)));
		nvps.add(new BasicNameValuePair("ICAction", "TL_SAVE_PB"));
		nvps.add(new BasicNameValuePair("ICXPos", "0"));
		nvps.add(new BasicNameValuePair("ICYPos", "0"));
		nvps.add(new BasicNameValuePair("ResponsetoDiffFrame", "-1"));
		nvps.add(new BasicNameValuePair("TargetFrameName", "None"));
		nvps.add(new BasicNameValuePair("ICFocus", ""));
		nvps.add(new BasicNameValuePair("ICSaveWarningFilter", "0"));
		nvps.add(new BasicNameValuePair("ICChanged", "-1"));
		nvps.add(new BasicNameValuePair("ICResubmit", "0"));
		nvps.add(new BasicNameValuePair("ICSID", lastRequestTimeSheetResponse.getIcsId()));
		nvps.add(new BasicNameValuePair("ICModalWidget", "0"));
		nvps.add(new BasicNameValuePair("ICZoomGrid", "0"));
		nvps.add(new BasicNameValuePair("ICZoomGridRt", "0"));
		nvps.add(new BasicNameValuePair("ICModalLongClosed", ""));
		nvps.add(new BasicNameValuePair("ICActionPrompt", "false"));
		nvps.add(new BasicNameValuePair("ICTypeAheadID", ""));
		nvps.add(new BasicNameValuePair("ICFind", ""));
		nvps.add(new BasicNameValuePair("ICAddCount", ""));
		nvps.add(new BasicNameValuePair("DATE_DAY1", new SimpleDateFormat(
				"dd/MM/yyyy").format(tsData.getWeekStartDate())));
		nvps.add(new BasicNameValuePair("TL_TR_WEEK_WRK_USER_FIELD_1$0", "PA"));
		nvps.add(new BasicNameValuePair("PROJECT$0", tsData.getSelectedProjectId()));
		nvps.add(new BasicNameValuePair("USER_FIELD_3$0", tsData.getTaskId()));
		addTimeSheetHours(timesheetHours,
				lastRequestTimeSheetResponse.getTimesheetData().getTimeSheetHourStatesForWeek(),
				nvps);
		HttpResponse httpResp;
		try {
			httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
			httpResp = executeHttpRequest(httpPost);
		} catch (Exception e) {
			throw(new TimeSheetServiceException("Error while executing Submit-Http request. "
					+ "Addition Info: " + e.getMessage()));
		}
		String result;
		try {
			result = EntityUtils.toString(httpResp.getEntity());
		} catch (Exception e) {
			throw(new TimeSheetServiceException("Error while parsing Submit-Http response. "
					+ "Addition Info: " + e.getMessage()));
		}
		TimeSheet.SubmitResult.Builder submitResultBuilder = TimeSheet.SubmitResult.newBuilder();
		if (result.contains(Constants.TIMESHEET_SUBMIT_SUCCESS)) {
			// Submit is successful.
			submitResultBuilder.status(Status.OK);
			currentPage = Page.TIMESHEET_SUBMIT_RESULT;
		} else {
			// Extract the failure message.
			submitResultBuilder.status(Status.FAIL);
			Pattern p = Pattern.compile(
					"(.*)<div id=alertmsg(.*?)<span class='popupText'(.*?)>(.*?)</span></div>(.*)",
					Pattern.CASE_INSENSITIVE);
			Matcher matcher = p.matcher(result.replace("\n", ""));
			if (matcher != null && matcher.groupCount() > 4) {
				submitResultBuilder.failureReason(matcher.group(4));
			}
		}

		return (SubmitResult) submitResultBuilder.build();
	}

	private TimeSheetResponse navigateToCurrentWeekTimeSheetPage()
			throws TimeSheetServiceException {
		try {
			String key = timeSheetModuleUrl.substring(timeSheetModuleUrl
					.lastIndexOf("&ID=") + 4);
			String url = Constants.URL_TIMESHEET_MODULE;
			url += "?Page=TL_RPTD_ELP";
			url += "&Action=U";
			url += "&ID=" + key;
			url += "&PortalActualURL=" + url;
			url += "&PortalRegistryName=EMPLOYEE";
			url += "&PortalServletURI=https://tmbshr.techmahindra.com/psp/TMBSHR/";
			url += "&PortalURI=https://tmbshr.techmahindra.com/psc/TMBSHR/";
			url += "&PortalHostNode=HRMS&NoCrumbs=yes&PortalKeyStruct=yes";
			referer = url;
			HttpGet httpGet = new HttpGet(url);
			httpGet.addHeader("Accept",
					"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
			httpGet.addHeader("Accept-Encoding", "gzip,deflate,sdch");
			httpGet.addHeader("Accept-Language", "en-US,en;q=0.8");
			httpGet.addHeader("Connection", "keep-alive");
			httpGet.addHeader("Host", Constants.TECHM_HR_BS_HOST);
			httpGet.addHeader("Referer", timeSheetModuleUrl);
			httpGet.addHeader("User-Agent", Constants.USER_AGENT);

			HttpResponse resp = executeHttpRequest(httpGet);
			return parseTimeSheetResponse((EntityUtils.toString(resp.getEntity())));
		} catch (Exception e) {
			throw (new TimeSheetServiceException(
					"Error while parsing the Http response of currwent week timesheet request."
					+ " Addition Info: " + e.getMessage()));
		}
	}

	@Override
	public Map<String, String> searchTasks(String projectId, String searchString)
			throws TimeSheetServiceException {
		if (currentPage == Page.TIMESHEET_SUBMIT_RESULT) {
			goBackToTimeSheetPage();
		}
		// Open the search dialog.
		HttpPost httpPost = new HttpPost(Constants.URL_TIMESHEET_MODULE);
		httpPost.addHeader("Accept", "*/*");
		httpPost.addHeader("Accept-Encoding", "gzip,deflate,sdch");
		httpPost.addHeader("Accept-Language", "en-US,en;q=0.8");
		httpPost.addHeader("Connection", "keep-alive");
		httpPost.addHeader("Cache-Control", "max-age=0");
		httpPost.addHeader("Host", Constants.TECHM_HR_BS_HOST);
		httpPost.addHeader("User-Agent", Constants.USER_AGENT);
		httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
		httpPost.addHeader("Referer", referer);
		httpPost.addHeader("Origin", Constants.URL_TECHM_HR_BS);
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("ICAJAX", "1"));
		nvps.add(new BasicNameValuePair("ICNAVTYPEDROPDOWN", "1"));
		nvps.add(new BasicNameValuePair("ICSPROMPT", "1"));
		nvps.add(new BasicNameValuePair("ICType", "Panel"));
		nvps.add(new BasicNameValuePair("ICElementNum", ""));
		nvps.add(new BasicNameValuePair("ICStateNum", String.format("%d", icStateNum)));
		nvps.add(new BasicNameValuePair("ICAction", "USER_FIELD_3$prompt$0"));
		nvps.add(new BasicNameValuePair("ICXPos", "0"));
		nvps.add(new BasicNameValuePair("ICYPos", "256"));
		nvps.add(new BasicNameValuePair("ResponsetoDiffFrame", "-1"));
		nvps.add(new BasicNameValuePair("TargetFrameName", "None"));
		nvps.add(new BasicNameValuePair("ICFocus", ""));
		nvps.add(new BasicNameValuePair("ICSaveWarningFilter", "0"));
		nvps.add(new BasicNameValuePair("ICChanged", "-1"));
		nvps.add(new BasicNameValuePair("ICResubmit", "0"));
		nvps.add(new BasicNameValuePair("ICSID", lastRequestTimeSheetResponse.getIcsId()));
		nvps.add(new BasicNameValuePair("ICModalWidget", "0"));
		nvps.add(new BasicNameValuePair("ICZoomGrid", "0"));
		nvps.add(new BasicNameValuePair("ICZoomGridRt", "0"));
		nvps.add(new BasicNameValuePair("ICModalLongClosed", ""));
		nvps.add(new BasicNameValuePair("ICActionPrompt", "false"));
		nvps.add(new BasicNameValuePair("ICTypeAheadID", ""));
		nvps.add(new BasicNameValuePair("ICFind", ""));
		nvps.add(new BasicNameValuePair("ICAddCount", ""));
		TimeSheet.Data tsData = lastRequestTimeSheetResponse.getTimesheetData();
		nvps.add(new BasicNameValuePair("DATE_DAY1", new SimpleDateFormat(
				"dd/MM/yyyy").format(tsData.getWeekStartDate())));
		nvps.add(new BasicNameValuePair("TL_TR_WEEK_WRK_USER_FIELD_1$0", "PA"));
		nvps.add(new BasicNameValuePair("PROJECT$0", tsData.getSelectedProjectId()));
		nvps.add(new BasicNameValuePair("USER_FIELD_3$0", tsData.getTaskId()));
		addTimeSheetHours(tsData.getTimeSheetHoursForWeek(), tsData.getTimeSheetHourStatesForWeek(),
				nvps);
		String result = "";
		try {
			httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
			HttpResponse httpResp = executeHttpRequest(httpPost);
			EntityUtils.toString(httpResp.getEntity());
		} catch (Exception e) {
			throw new TimeSheetServiceException("Error while opening Timesheet task search dialog."
					+ " Additional Info: " + e.getMessage());
		}

		// Select the advanced search option.
		httpPost.addHeader("Accept", "*/*");
		httpPost.addHeader("Accept-Encoding", "gzip,deflate,sdch");
		httpPost.addHeader("Accept-Language", "en-US,en;q=0.8");
		httpPost.addHeader("Connection", "keep-alive");
		httpPost.addHeader("Host", Constants.TECHM_HR_BS_HOST);
		httpPost.addHeader("User-Agent", Constants.USER_AGENT);
		httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
		httpPost.addHeader("Referer", referer);
		httpPost.addHeader("Origin", Constants.URL_TECHM_HR_BS);
		nvps.clear();
		nvps.add(new BasicNameValuePair("ICAJAX", "1"));
		nvps.add(new BasicNameValuePair("ICNAVTYPEDROPDOWN", "0"));
		nvps.add(new BasicNameValuePair("ICType", "Panel"));
		nvps.add(new BasicNameValuePair("ICElementNum", "0"));
		nvps.add(new BasicNameValuePair("ICStateNum", getNextIcStateNum()));
		nvps.add(new BasicNameValuePair("ICAction", "#ICAdvsearch"));
		nvps.add(new BasicNameValuePair("ICXPos", "0"));
		nvps.add(new BasicNameValuePair("ICYPos", "0"));
		nvps.add(new BasicNameValuePair("ResponsetoDiffFrame", "-1"));
		nvps.add(new BasicNameValuePair("TargetFrameName", "None"));
		nvps.add(new BasicNameValuePair("ICFocus", ""));
		nvps.add(new BasicNameValuePair("ICSaveWarningFilter", "0"));
		nvps.add(new BasicNameValuePair("ICChanged", "-1"));
		nvps.add(new BasicNameValuePair("ICResubmit", "0"));
		nvps.add(new BasicNameValuePair("ICSID", lastRequestTimeSheetResponse.getIcsId()));
		nvps.add(new BasicNameValuePair("ICModalWidget", "0"));
		nvps.add(new BasicNameValuePair("ICZoomGrid", "0"));
		nvps.add(new BasicNameValuePair("ICZoomGridRt", "0"));
		nvps.add(new BasicNameValuePair("ICModalLongClosed", ""));
		nvps.add(new BasicNameValuePair("ICActionPrompt", "false"));
		nvps.add(new BasicNameValuePair("ICTypeAheadID", ""));
		nvps.add(new BasicNameValuePair("ICFind", ""));
		nvps.add(new BasicNameValuePair("ICAddCount", ""));
		nvps.add(new BasicNameValuePair("#ICKeyselect", "0"));
		nvps.add(new BasicNameValuePair("TM_TL_TASK_DVW_TM_TL_TASK_ID$op", "1"));
		nvps.add(new BasicNameValuePair("TM_TL_TASK_DVW_TM_TL_TASK_ID", ""));
		try {
			httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
			HttpResponse httpResp = executeHttpRequest(httpPost);
			EntityUtils.toString(httpResp.getEntity());
		} catch (Exception e) {
			throw new TimeSheetServiceException("Error while changing the search type to Advanced."
					+ " Additional Info: " + e.getMessage());
		}
		
		// Now do the search with task hierarchy - contains options.
		httpPost.addHeader("Accept", "*/*");
		httpPost.addHeader("Accept-Encoding", "gzip,deflate,sdch");
		httpPost.addHeader("Accept-Language", "en-US,en;q=0.8");
		httpPost.addHeader("Connection", "keep-alive");
		httpPost.addHeader("Host", Constants.TECHM_HR_BS_HOST);
		httpPost.addHeader("User-Agent", Constants.USER_AGENT);
		httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
		httpPost.addHeader("Referer", referer);
		httpPost.addHeader("Origin", Constants.URL_TECHM_HR_BS);
		nvps.clear();
		nvps.add(new BasicNameValuePair("ICAJAX", "1"));
		nvps.add(new BasicNameValuePair("ICNAVTYPEDROPDOWN", "0"));
		nvps.add(new BasicNameValuePair("ICType", "Panel"));
		nvps.add(new BasicNameValuePair("ICElementNum", "0"));
		nvps.add(new BasicNameValuePair("ICStateNum", getNextIcStateNum()));
		nvps.add(new BasicNameValuePair("ICAction", "#ICSearch"));
		nvps.add(new BasicNameValuePair("ICXPos", "0"));
		nvps.add(new BasicNameValuePair("ICYPos", "48"));
		nvps.add(new BasicNameValuePair("ResponsetoDiffFrame", "-1"));
		nvps.add(new BasicNameValuePair("TargetFrameName", "None"));
		nvps.add(new BasicNameValuePair("ICFocus", ""));
		nvps.add(new BasicNameValuePair("ICSaveWarningFilter", "0"));
		nvps.add(new BasicNameValuePair("ICChanged", "-1"));
		nvps.add(new BasicNameValuePair("ICResubmit", "0"));
		nvps.add(new BasicNameValuePair("ICSID", lastRequestTimeSheetResponse.getIcsId()));
		nvps.add(new BasicNameValuePair("ICModalWidget", "0"));
		nvps.add(new BasicNameValuePair("ICZoomGrid", "0"));
		nvps.add(new BasicNameValuePair("ICZoomGridRt", "0"));
		nvps.add(new BasicNameValuePair("ICModalLongClosed", ""));
		nvps.add(new BasicNameValuePair("ICActionPrompt", "true"));
		nvps.add(new BasicNameValuePair("ICTypeAheadID", ""));
		nvps.add(new BasicNameValuePair("ICFind", ""));
		nvps.add(new BasicNameValuePair("ICAddCount", ""));
		nvps.add(new BasicNameValuePair("#ICKeyselect", "0"));
		nvps.add(new BasicNameValuePair("TM_TL_TASK_DVW_TM_TL_TASK_ID$op", "1"));
		nvps.add(new BasicNameValuePair("TM_TL_TASK_DVW_TM_TL_TASK_ID", ""));
		nvps.add(new BasicNameValuePair("TM_TL_TASK_DVW_TM_TL_TASK_DESCR$op", "1"));
		nvps.add(new BasicNameValuePair("TM_TL_TASK_DVW_TM_TL_TASK_DESCR", ""));
		nvps.add(new BasicNameValuePair("TM_TL_TASK_DVW_TM_TL_TASK_DESCR254$op", "8"));
		nvps.add(new BasicNameValuePair("TM_TL_TASK_DVW_TM_TL_TASK_DESCR254", searchString));
		try {
			httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
			HttpResponse httpResp = executeHttpRequest(httpPost);
			result = EntityUtils.toString(httpResp.getEntity());
		} catch (Exception e) {
			throw new TimeSheetServiceException("Error while changing the search type to Advanced."
					+ " Additional Info: " + e.getMessage());
		}

		Map<String, String> tasks = null;
		// Parse the XML response.
		try {
			tasks = TimeSheetTaskSearchResponseParser.parse(result);
		} catch (Exception e) {
			throw new TimeSheetServiceException("Error while parsing the task search response."
					+ " Additional Info: " + e.getMessage());
		}

		// Close the task search dialog.
		httpPost.addHeader("Accept", "*/*");
		httpPost.addHeader("Accept-Encoding", "gzip,deflate,sdch");
		httpPost.addHeader("Accept-Language", "en-US,en;q=0.8");
		httpPost.addHeader("Connection", "keep-alive");
		httpPost.addHeader("Host", Constants.TECHM_HR_BS_HOST);
		httpPost.addHeader("User-Agent", Constants.USER_AGENT);
		httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
		httpPost.addHeader("Referer", referer);
		httpPost.addHeader("Origin", Constants.URL_TECHM_HR_BS);
		nvps.clear();
		nvps.add(new BasicNameValuePair("ICAJAX", "1"));
		nvps.add(new BasicNameValuePair("ICNAVTYPEDROPDOWN", "1"));
		nvps.add(new BasicNameValuePair("ICType", "Panel"));
		nvps.add(new BasicNameValuePair("ICElementNum", "0"));
		nvps.add(new BasicNameValuePair("ICStateNum", getNextIcStateNum()));
		nvps.add(new BasicNameValuePair("ICAction", "#ICCancel"));
		nvps.add(new BasicNameValuePair("ICXPos", "0"));
		nvps.add(new BasicNameValuePair("ICYPos", "48"));
		nvps.add(new BasicNameValuePair("ResponsetoDiffFrame", "-1"));
		nvps.add(new BasicNameValuePair("TargetFrameName", "None"));
		nvps.add(new BasicNameValuePair("ICFocus", ""));
		nvps.add(new BasicNameValuePair("ICSaveWarningFilter", "0"));
		nvps.add(new BasicNameValuePair("ICChanged", "-1"));
		nvps.add(new BasicNameValuePair("ICResubmit", "0"));
		nvps.add(new BasicNameValuePair("ICSID", lastRequestTimeSheetResponse.getIcsId()));
		nvps.add(new BasicNameValuePair("ICModalWidget", "0"));
		nvps.add(new BasicNameValuePair("ICZoomGrid", "0"));
		nvps.add(new BasicNameValuePair("ICZoomGridRt", "0"));
		nvps.add(new BasicNameValuePair("ICModalLongClosed", ""));
		nvps.add(new BasicNameValuePair("ICActionPrompt", "true"));
		nvps.add(new BasicNameValuePair("ICTypeAheadID", ""));
		nvps.add(new BasicNameValuePair("ICFind", ""));
		nvps.add(new BasicNameValuePair("ICAddCount", ""));
		nvps.add(new BasicNameValuePair("DATE_DAY1", new SimpleDateFormat(
				"dd/MM/yyyy").format(tsData.getWeekStartDate())));
		nvps.add(new BasicNameValuePair("TL_TR_WEEK_WRK_USER_FIELD_1$0", "PA"));
		nvps.add(new BasicNameValuePair("PROJECT$0", tsData.getSelectedProjectId()));
		nvps.add(new BasicNameValuePair("USER_FIELD_3$0", tsData.getTaskId()));
		addTimeSheetHours(tsData.getTimeSheetHoursForWeek(), tsData.getTimeSheetHourStatesForWeek(),
				nvps);
		try {
			httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
			HttpResponse httpResp = executeHttpRequest(httpPost);
			EntityUtils.toString(httpResp.getEntity());
			getNextIcStateNum();
		} catch (Exception e) {
			throw new TimeSheetServiceException("Error while opening Timesheet task search dialog."
					+ " Additional Info: " + e.getMessage());
		}
		return tasks;
	}
	
	private String getNextIcStateNum() {
		return String.format("%d", ++icStateNum);
	}
	
	private void goBackToTimeSheetPage() throws TimeSheetServiceException {
		HttpPost httpPost = new HttpPost(Constants.URL_TIMESHEET_MODULE);
		httpPost.addHeader("Accept", "*/*");
		httpPost.addHeader("Accept-Encoding", "gzip,deflate,sdch");
		httpPost.addHeader("Accept-Language", "en-US,en;q=0.8");
		httpPost.addHeader("Connection", "keep-alive");
		httpPost.addHeader("Host", Constants.TECHM_HR_BS_HOST);
		httpPost.addHeader("User-Agent", Constants.USER_AGENT);
		httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
		httpPost.addHeader("Referer", referer);
		httpPost.addHeader("Origin", Constants.URL_TECHM_HR_BS);
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("ICAJAX", "1"));
		nvps.add(new BasicNameValuePair("ICNAVTYPEDROPDOWN", "1"));
		nvps.add(new BasicNameValuePair("ICType", "Panel"));
		nvps.add(new BasicNameValuePair("ICElementNum", "0"));
		nvps.add(new BasicNameValuePair("ICStateNum", String.format("%d", icStateNum)));
		nvps.add(new BasicNameValuePair("ICAction", "DERIVED_ETEO_SAVE_PB"));
		nvps.add(new BasicNameValuePair("ICXPos", "0"));
		nvps.add(new BasicNameValuePair("ICYPos", "0"));
		nvps.add(new BasicNameValuePair("ResponsetoDiffFrame", "-1"));
		nvps.add(new BasicNameValuePair("TargetFrameName", "None"));
		nvps.add(new BasicNameValuePair("ICFocus", ""));
		nvps.add(new BasicNameValuePair("ICSaveWarningFilter", "0"));
		nvps.add(new BasicNameValuePair("ICChanged", "-1"));
		nvps.add(new BasicNameValuePair("ICResubmit", "0"));
		nvps.add(new BasicNameValuePair("ICSID", lastRequestTimeSheetResponse.getIcsId()));
		nvps.add(new BasicNameValuePair("ICModalWidget", "0"));
		nvps.add(new BasicNameValuePair("ICZoomGrid", "0"));
		nvps.add(new BasicNameValuePair("ICZoomGridRt", "0"));
		nvps.add(new BasicNameValuePair("ICModalLongClosed", ""));
		nvps.add(new BasicNameValuePair("ICActionPrompt", "false"));
		nvps.add(new BasicNameValuePair("ICTypeAheadID", ""));
		nvps.add(new BasicNameValuePair("ICFind", ""));
		nvps.add(new BasicNameValuePair("ICAddCount", ""));
		try {
			httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
			HttpResponse httpResp = executeHttpRequest(httpPost);
			String html = EntityUtils.toString(httpResp.getEntity());
			parseTimeSheetResponse(html);
			currentPage = Page.TIMESHEET;
		} catch (Exception e) {
			throw new TimeSheetServiceException("Error while opening Timesheet task search dialog."
					+ " Additional Info: " + e.getMessage());
		}
	}
}

package com.example.easylite.services;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.example.easylite.services.model.TimeSheet;
import com.example.easylite.services.model.TimeSheet.Data.Builder;
import com.example.easylite.services.model.TimeSheet.TimesheetHourStatus;

public class TimeSheetResponse {

	private int icStateNum;
	private String icsId;
	private TimeSheet.Data timesheetData;
	private static final String TS_WINDOW_DISABLED = "Timesheet Window is closed for this Period";

	public TimeSheetResponse() {
		this.timesheetData = null;
	}

	public TimeSheetResponse parse(final String html) throws ParseException {
		Document doc = Jsoup.parse(html);
		icStateNum = Integer.parseInt(doc.getElementById("ICStateNum").attr(
				"value"));
		icsId = doc.getElementById("ICSID").attr("value");

		Builder timeSheetDataBuilder = TimeSheet.Data.newBuilder();
		SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
		timeSheetDataBuilder.setWeekStartDate(format.parse(doc.getElementById(
				"DATE_DAY1").attr("value")));
		timeSheetDataBuilder.setTaskId(doc.getElementById("USER_FIELD_3$0")
				.attr("value")); // Task ID
		timeSheetDataBuilder.setTaskName(doc.getElementById(
				"TM_TL_DD_WRK_COMMENTS$0").attr("value")); // Task Hierarchy

		if (html.toLowerCase().contains(TS_WINDOW_DISABLED.toLowerCase())) {
			timeSheetDataBuilder.setIsTimeSheetWindowDisabled(true);
		}

		// Parse Projects.
		parseProjects(doc, timeSheetDataBuilder);

		// Parse Timesheet hour fields data.
		parseTimeSheetHours(doc, timeSheetDataBuilder);

		timesheetData = timeSheetDataBuilder.build();

		return this;
	}

	private void parseTimeSheetHours(Document doc, Builder builder) {
		String[] timeSheetHours = new String[7];
		TimesheetHourStatus[] timeSheetHoursStates = new TimesheetHourStatus[7];

		for (int i=0; i<=6; ++i) {
			String key = String.format("QTY_DAY%d$0", i+1);
			Attributes attributes = doc.getElementById(key).attributes();
			timeSheetHours[i] = attributes.get("value");
			boolean isDisabled = false;
			if (attributes.hasKey("disabled") && attributes.get("disabled") == "disabled") {
				isDisabled = true;
			}
			String cssClass = attributes.get("class");
			if (cssClass == "PSEDITBOX") {
				timeSheetHoursStates[i] = TimesheetHourStatus.NORMAL;
			} else if (cssClass == "PSEDITBOX_DISABLED" || isDisabled) {
				timeSheetHoursStates[i] = TimesheetHourStatus.DISABLED;
			} else if (cssClass == "TM_TL_WEEKEND") {
				timeSheetHoursStates[i] = TimesheetHourStatus.WEEKEND;
			} else if (cssClass == "TM_TL_LEAVE") {
				timeSheetHoursStates[i] = TimesheetHourStatus.LEAVE;
			} else if (cssClass == "TM_TL_HOLIDAY") {
				timeSheetHoursStates[i] = TimesheetHourStatus.HOLIDAY;
			}
		}
		builder.setTimeSheetHoursForWeek(timeSheetHours);
		builder.setTimeSheetHourStatesForWeek(timeSheetHoursStates);
	}

	private void parseProjects(Document doc, Builder builder) {
		Map<String, String> projects = new HashMap<String, String>();
		String selectedProjectId = "";
		for (Element e : doc.getElementById("PROJECT$0").children()) {
			String projName = e.text().trim();
			String projId = e.attr("value").trim();
			if(!projId.isEmpty()) {
				projects.put(projId, projName);
				if (e.hasAttr("selected") && e.attr("selected").equals("selected")) {
					selectedProjectId = projId;
				}
			}
		}
		builder.setProjects(projects);
		builder.setSelectedProjectId(selectedProjectId);
	}

	public int getIcStateNum() {
		return icStateNum;
	}

	public String getIcsId() {
		return icsId;
	}

	public TimeSheet.Data getTimesheetData() {
		return timesheetData;
	}
}

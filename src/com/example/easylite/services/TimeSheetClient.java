package com.example.easylite.services;

import java.util.Date;
import java.util.Map;

import com.example.easylite.services.model.TimeSheet;

public interface TimeSheetClient {

	TimeSheet.SubmitResult submitTimeSheet(Date date, String[] timesheetHours)
			throws TimeSheetServiceException;

	TimeSheet.Data getTimeSheetDataFor(Date date) throws TimeSheetServiceException;
	
	Map<String, String> searchTasks(String projectId, String searchString)
			throws TimeSheetServiceException;
}

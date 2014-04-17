package com.example.easylite.services;

public class Constants {
	public static final String EASY_HOST_NAME = "easy.techmahindra.com";
	public static final String URL_EASY = "https://" + EASY_HOST_NAME;
	public static final String URL_EASY_HOME = URL_EASY + "/EasyHome.aspx";
	public static final String URL_EASY_LOGIN = URL_EASY + "/easylogin.aspx";
	public static final String URL_EASY_TIMESHEET_LINK = URL_EASY + 
			"/frameAjax.aspx?tag=TimesheetEntryLink";
	public static final String TECHM_HR_BS_HOST = "tmbshr.techmahindra.com";
	public static final String URL_TECHM_HR_BS = "https://tmbshr.techmahindra.com";
	public static final String URL_TIMESHEET_MODULE = URL_TECHM_HR_BS + 
			"/psc/TMBSHR/EMPLOYEE/PSFT_HR/c/ROLE_EMPLOYEE.TL_MSS_EE_SRCH_PRD.GBL";
	
	public static final String USER_AGENT = 
			"Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebkit/537.36(KHTML, like Gecko) Chrome/31.0";
	public static final String TIMESHEET_SUBMIT_SUCCESS = "The submit was successful";
}

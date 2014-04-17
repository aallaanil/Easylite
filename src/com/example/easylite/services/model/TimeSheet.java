package com.example.easylite.services.model;

import java.util.Date;
import java.util.Map;

public final class TimeSheet {
	public static enum TimesheetHourStatus {
		NORMAL,
		DISABLED,
		LEAVE,
		HOLIDAY,
		WEEKEND
	};
	
	public final static class Data {
		private final Map<String, String> projects; //Map of Project ID -> Project Name.
		private final String selectedProjectId;
		private final String taskId;
		private final String taskName;
		private final Date weekStartDate;
		private final String[] timeSheetHoursForWeek;
		private final TimesheetHourStatus[] timeSheetHourStatesForWeek;
		private final boolean isTimeSheetWindowDisabled;
		
		public static Builder newBuilder() {
			return new Builder();
		}
		
		private Data(Builder builder) {
			this.projects = builder.projects;
			this.selectedProjectId = builder.selectedProjectId;
			this.taskId = builder.taskId;
			this.taskName = builder.taskName;
			this.weekStartDate = builder.weekStartDate;
			this.timeSheetHoursForWeek = builder.timeSheetHoursForWeek;
			this.timeSheetHourStatesForWeek = builder.timeSheetHourStatesForWeek;
			this.isTimeSheetWindowDisabled = builder.isTimeSheetWindowDisabled;
		}
		
		
		public Map<String, String> getProjects() {
			return projects;
		}

		public String getSelectedProjectId() {
			return selectedProjectId;
		}

		public String getTaskId() {
			return taskId;
		}

		public String getTaskName() {
			return taskName;
		}

		public Date getWeekStartDate() {
			return weekStartDate;
		}

		public String[] getTimeSheetHoursForWeek() {
			return timeSheetHoursForWeek;
		}

		public TimesheetHourStatus[] getTimeSheetHourStatesForWeek() {
			return timeSheetHourStatesForWeek;
		}

		public boolean isTimeSheetWindowDisabled() {
			return isTimeSheetWindowDisabled;
		}

		public static final class Builder {
			public boolean isTimeSheetWindowDisabled;
			private Map<String, String> projects;
			private String selectedProjectId;
			private String taskId;
			private String taskName;
			private Date weekStartDate;
			private TimesheetHourStatus[] timeSheetHourStatesForWeek;
			private String[] timeSheetHoursForWeek;
			
			public Builder() {
				this.isTimeSheetWindowDisabled = false;
			}
			
			public Builder setProjects(Map<String, String> projects) {
				this.projects = projects;
				return this;
			}

			public Builder setSelectedProjectId(String selectedProjectId) {
				this.selectedProjectId = selectedProjectId;
				return this;
			}

			public Builder setTaskId(String taskId) {
				this.taskId = taskId;
				return this;
			}

			public Builder setTaskName(String taskName) {
				this.taskName = taskName;
				return this;
			}

			public Builder setWeekStartDate(Date weekStartDate) {
				this.weekStartDate = weekStartDate;
				return this;
			}

			public Builder setTimeSheetHoursForWeek(String[] timeSheetHoursForWeek) {
				this.timeSheetHoursForWeek = timeSheetHoursForWeek;
				return this;
			}

			public Builder setTimeSheetHourStatesForWeek(
					TimesheetHourStatus[] timeSheetHoursStates) {
				this.timeSheetHourStatesForWeek = timeSheetHoursStates;
				return this;
			}
			
			public Builder setIsTimeSheetWindowDisabled(boolean disabled) {
				this.isTimeSheetWindowDisabled = disabled;
				return this;
			}
			
			public Data build() {
				return new Data(this);
			}
			
		}
	}

	public final static class SubmitResult extends Result {
		
	}

	public final static class TaskSearchresult extends Result {
		private final Map<String, String> tasks;
		
		private TaskSearchresult(Builder builder) {
			super(builder);
			this.tasks = builder.tasks;
		}

		public Map<String, String> getTasks() {
			return tasks;
		}

		public static Builder newBuilder() {
			return new Builder();
		}
		
		public static class Builder extends Result.Builder {
			private Map<String, String> tasks;

			public Builder() {
			}

			public Builder tasks(Map<String, String> tasks) {
				this.tasks = tasks;
				return this;
			}

			public TaskSearchresult build() {
				return new TaskSearchresult(this);
			}
		}
	}
}

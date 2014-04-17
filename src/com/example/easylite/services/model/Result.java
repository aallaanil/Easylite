package com.example.easylite.services.model;

public class Result {
	public static enum Status {
		OK, FAIL, NOT_SET
	};

	protected final Status status;
	protected final String failureReason;

	protected Result() {
		this.status = Status.NOT_SET;
		this.failureReason = "";
	}
	
	protected Result(Builder builder) {
		this.status = builder.status;
		this.failureReason = builder.failureReason;
	}

	public Status getStatus() {
		return status;
	}

	public String getFailureReason() {
		return failureReason;
	}

	public static Builder newBuilder() {
		return new Builder();
	}
	
	public static class Builder {
		protected Status status = Status.NOT_SET;
		protected String failureReason = "";

		public Builder() {
		}

		public Builder status(Status status) {
			this.status = status;
			return this;
		}

		public Builder failureReason(String reason) {
			this.failureReason = reason;
			return this;
		}

		public Result build() {
			return new Result(this);
		}
	}
}
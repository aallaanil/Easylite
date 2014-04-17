package com.example.easylite.services.model;

public class Easy {
	public final static class LoginResult extends Result {
		private final Associate associate;

		private LoginResult(Builder builder) {
			super(builder);
			this.associate = builder.associate;
		}

		public Associate getAssociate() {
			return associate;
		}

		public static Builder newBuilder() {
			return new Builder();
		}
		
		public static class Builder extends Result.Builder {
			private Associate associate;

			public Builder() {
			}

			public Builder associate(Associate associate) {
				this.associate = associate;
				return this;
			}

			public LoginResult build() {
				return new LoginResult(this);
			}
		}
	}

	public final static class Associate {
		private final String associateId;
		private final String associateName;

		private Associate(Builder builder) {
			this.associateId = builder.associateId;
			this.associateName = builder.associateName;
		}

		public String getAssociateId() {
			return associateId;
		}

		public String getAssociateName() {
			return associateName;
		}

		public Builder newBuilder() {
			return new Builder();
		}
		
		public static final class Builder {
			private String associateId;
			private String associateName;

			Builder() {
			}
			
			public Builder(String associateId, String associateName) {
				this.associateId = associateId;
				this.associateName = associateName;
			}

			public Builder associateId(String associateId) {
				this.associateId = associateId;
				return this;
			}

			public Builder associateName(String associateName) {
				this.associateName = associateName;
				return this;
			}
			
			public Associate build() {
				return new Associate(this);
			}
		}
	}
}

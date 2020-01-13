package com.automation.config.telstra;

public class TargetCredential {
	private String userName;
	private String password;
	private String endPoint;
	private String grantService;
	private String clientId;
	private String clientSecret;
	
	public TargetCredential(){}
	
	public TargetCredential(String userName,String password, String endPoint,String grantService,String clientId,
			String clientSecret){
		this.userName=userName;
		this.password=password;
		this.endPoint=endPoint;
		this.grantService=grantService;
		this.clientId=clientId;
		this.clientSecret=clientSecret;
	}
	
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getEndPoint() {
		return endPoint;
	}
	public void setEndPoint(String endPoint) {
		this.endPoint = endPoint;
	}
	public String getGrantService() {
		return grantService;
	}
	public void setGrantService(String grantService) {
		this.grantService = grantService;
	}
	public String getClientId() {
		return clientId;
	}
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	public String getClientSecret() {
		return clientSecret;
	}
	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

}

package com.automation.config.telstra;

public class TargetConfig {

	private String entity;
	private String outputSuccess;
	private String outputError;
	private TargetCredential trgCredential;
	
	public TargetConfig() {}
	
	public TargetConfig(TargetCredential trgCredential, String entity, String outputSuccess, String outputError) {
		this.trgCredential=trgCredential;
		this.entity=entity;
		this.outputSuccess=outputSuccess;
		this.outputError=outputError;
	}
	
	
	public TargetCredential getTrgCredential() {
		return trgCredential;
	}
	
	public void setTrgCredential(TargetCredential trgCredential) {
		this.trgCredential = trgCredential;
	}

	public String getOutputSuccess() {
		return outputSuccess;
	}
	public void setOutputSuccess(String outputSuccess) {
		this.outputSuccess = outputSuccess;
	}
	public String getOutputError() {
		return outputError;
	}
	public void setOutputError(String outputError) {
		this.outputError = outputError;
	}
	public String getEntity() {
		return entity;
	}
	public void setEntity(String entity) {
		this.entity = entity;
	}
	
}

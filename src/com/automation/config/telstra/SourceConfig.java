package com.automation.config.telstra;

public class SourceConfig {
	
	private String extractionSOQL;
	private String fileName;
	private String saveFile;
	private String outputSuccess;
	private String outputError;
	private SourceCredential srcCred;
	
	public SourceConfig() {}
	
	public SourceConfig(SourceCredential srcCred, String extractionSOQL, String fileName, String saveFile, String outputSuccess,
			String outputError) {
		this.srcCred=srcCred;
		this.extractionSOQL=extractionSOQL;
		this.fileName=fileName;
		this.saveFile=saveFile;
		this.outputSuccess=outputSuccess;
		this.outputError=outputError;
		
	}
	

	public SourceCredential getSrcCred() {
		return srcCred;
	}
	public void setSrcCred(SourceCredential srcCred) {
		this.srcCred = srcCred;
	}
	public String getExtractionSOQL() {
		return extractionSOQL;
	}
	public void setExtractionSOQL(String extractionSOQL) {
		this.extractionSOQL = extractionSOQL;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getSaveFile() {
		return saveFile;
	}
	public void setSaveFile(String saveFile) {
		this.saveFile = saveFile;
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
	
	
	


}

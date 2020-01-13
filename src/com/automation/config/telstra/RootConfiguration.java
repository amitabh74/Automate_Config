package com.automation.config.telstra;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Configurations")
public class RootConfiguration {

	List<UserConfiguration> userConfiguration;

	@XmlElement(name = "source-cred")
	private SourceCredential sourceCred;
	
	@XmlElement(name = "target-cred")
	private TargetCredential targetCreds;
	
	public List<UserConfiguration> getUserConfiguration(){
		return userConfiguration;
	}
	
	@XmlElement(name = "UserConf")
    public void setUserConfiguration(List<UserConfiguration> userConfiguration) {
        this.userConfiguration = userConfiguration;
    }

	/**
	 * @return the sourceCred
	 */
	public SourceCredential getSourceCred() {
		return this.sourceCred;
	}

	/**
	 * @return the targetCreds
	 */
	public TargetCredential getTargetCreds() {
		return this.targetCreds;
	}

}

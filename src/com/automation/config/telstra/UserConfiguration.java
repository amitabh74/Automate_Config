package com.automation.config.telstra;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "UserConf")
@XmlAccessorType(XmlAccessType.FIELD)
public class UserConfiguration implements Serializable{
	
	 /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@XmlAttribute(name = "id")
		 private String configId;
		 
		 @XmlAttribute(name = "name")
		 private String name;
		 
		 @XmlElement(name = "sourceconfig")
		 private SourceConfig sourceConfig;
		 
		 @XmlElement(name = "targetconfig")
		 private TargetConfig targetConfig;
		 
		 public UserConfiguration() {
			 
		 }
		 
		 public UserConfiguration(SourceConfig sourceConfig, TargetConfig targetConfig) {
			 this.sourceConfig=sourceConfig;
			 this.targetConfig=targetConfig;
		 }
		 
		 
		 public String getConfigId() {
				return configId;
			}

			public void setConfigId(String configId) {
				this.configId = configId;
			}

			public String getName() {
				return name;
			}

			public void setName(String name) {
				this.name = name;
			}

			public SourceConfig getSourceConfig() {
				return sourceConfig;
			}

			public void setSourceConfig(SourceConfig sourceConfig) {
				this.sourceConfig = sourceConfig;
			}

			public TargetConfig getTargetConfig() {
				return targetConfig;
			}

			public void setTargetConfig(TargetConfig targetConfig) {
				this.targetConfig = targetConfig;
			}


}

package com.automation.cs.telstra;

import com.automation.config.telstra.SourceConfig;
import com.automation.config.telstra.TargetConfig;

public interface IMigrateData {

    String extract(SourceConfig srcConfig);
	void insert(String json, TargetConfig trgConfig);
}

package com.automation.cs.telstra;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.automation.config.telstra.SourceConfig;
import com.automation.config.telstra.TargetConfig;

public class MigrateProductAttributeMetadata extends MigrateData implements IMigrateData{
    final static Logger logger = Logger.getLogger(MigrateProductAttributeMetadata.class);
    
    public MigrateProductAttributeMetadata(){}
    
    /**
	 * Extracts the data from Product_Attribute_Metadata__c object  from source org
	 * @param SourceConfig
	 * @return String json
	 */
    @Override
	public String extract(SourceConfig srcConfig){
    	
    	String sJson = null;
    	HttpResponse response = super.initSourceOrgConnection(srcConfig);
    	
    	if(response != null){
    		// Process the result
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
             
            	String response_string;
				try {
					response_string = EntityUtils.toString(response.getEntity());
	                JSONObject json = new JSONObject(response_string);
	                JSONArray j = json.getJSONArray("records");
	            	
	                int refId = 1;
                    for (int i = 0; i < j.length(); i++, refId++){
                    	//Update referenceId in attribute
                    	JSONObject obj = json.getJSONArray("records").getJSONObject(i).getJSONObject("attributes");
                    	obj.put("referenceId", "ref" + refId);
                    	
                    }
	                sJson = createJSON(json);
	                
				} catch (ParseException | IOException ex) {
					logger.error(ex.getMessage(), ex);
				}
                    
            } else {
            	logger.debug("Query was unsuccessful. Status code returned is " + statusCode);
            	logger.debug("An error has occured. Http status: " + response.getStatusLine().getStatusCode());
              
            	createErrorProcessLog(response);
            }
        } 
    	if(conn != null){
    		 conn.ReleaseConnection(httpPost);
    	}
		return sJson;
    }
    
    /**
	 * Insert Json to the Product_Attribute_Metadata__c object in target Org
	 * @param String json
	 */
    @Override
   	public void insert(String json, TargetConfig trgConfig) {
    	super.insert(json, trgConfig);
   	}

}

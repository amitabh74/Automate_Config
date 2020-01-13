package com.automation.cs.telstra;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.automation.config.telstra.SourceConfig;
import com.automation.config.telstra.TargetConfig;
import com.automation.util.telstra.Utility;

public class MigrateProductConfigurationAssociation extends MigrateData implements IMigrateData{
	
	private final String REST_ENDPOINT = "/services/data" ;
	private final String API_VERSION = "/v46.0" ;
    private String baseUri;
    private final Header prettyPrintHeader = new BasicHeader("X-PrettyPrint", "1");
    final static Logger logger = Logger.getLogger(MigrateProductConfigurationAssociation.class);

    public MigrateProductConfigurationAssociation(){}
    
    /**
	 * Performs SOQL to fetch association between cscfga__Product_Category__c object 
	 * and csbb__Configuration_Association__c
	 * @param srcConfig
	 */
    @Override
	public String extract(SourceConfig srcConfig){
    	
    	String sJson = null;
    	HttpResponse response = super.initSourceOrgConnection(srcConfig);
    	
    	if(response != null){
    		// Process the result
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                try {
                	 String response_string = EntityUtils.toString(response.getEntity());
                	JSONObject json = new JSONObject(response_string);
                    //JSONArray j = json.getJSONArray("records");
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
    
    @Override
   	public void insert(String sJson, TargetConfig trgConfig) {
    	SFConnection conn = null;
		Map<String, List<String>>prodCatg_ConfigAssoc = new HashMap<String, List<String>>();
		List<String> configAssoc_ExtId = null;
		
		
		JSONObject json = new JSONObject(sJson);
        JSONArray j = json.getJSONArray("records");
        String key = null;
        for (int i = 0; i < j.length(); i++){
           //Update referenceId in attribute
            JSONObject obj = json.getJSONArray("records").getJSONObject(i);
            	
            if(obj.opt("csexpimp1__guid__c") != JSONObject.NULL){
            	configAssoc_ExtId = new ArrayList<String>();
            	key = obj.getString("csexpimp1__guid__c");
            		
	            //Get Child Console Configuration Associations
	            if(json.getJSONArray("records").getJSONObject(i).get("csbb__Configuration_Associations__r") != JSONObject.NULL){
	            	JSONObject childObj = json.getJSONArray("records")
	            				.getJSONObject(i).getJSONObject("csbb__Configuration_Associations__r");
	            		
	            	JSONArray childArray = childObj.getJSONArray("records");
	                for(int cnt = 0; cnt < childArray.length(); cnt++){
	                	String extId = childArray.getJSONObject(cnt).getString("CCA_External_ID__c");
	                	//logger.debug("---" + key + ": " + extId);
	                	configAssoc_ExtId.add(extId);
	                }
	                prodCatg_ConfigAssoc.put(key, configAssoc_ExtId);
	            }
            }
            	
           }
			
			//Verify the content of map.
			/*for(Map.Entry<String, List<String>> entry : prodCatg_ConfigAssoc.entrySet()){
				logger.debug("--Key:" + entry.getKey() + " values: " + entry.getValue());
			}*/
			Map<String, String> prodCatg = getAssociatedProductCategory(trgConfig, prodCatg_ConfigAssoc);
			Map<String, String> consoleConfigAssoc = getConfigAssociationMapping(trgConfig, prodCatg_ConfigAssoc);
			
		try{
			conn = new SFConnection(trgConfig.getTrgCredential().getUserName(), 
   					trgConfig.getTrgCredential().getPassword(), 
   					trgConfig.getTrgCredential().getEndPoint(), 
   					trgConfig.getTrgCredential().getGrantService(), 
   					trgConfig.getTrgCredential().getClientId(), 
   					trgConfig.getTrgCredential().getClientSecret(),
   					"target");
			
			logger.debug("----Creation connection with target Salesforce");
			HttpPost httpPost = conn.createConnection();
			if (httpPost == null) return;
			
			baseUri = conn.getLoginInstanceUrl() + REST_ENDPOINT + API_VERSION;
		 
            //Create the JSON for update.
            JSONArray arConsoleConfig_ProdCateg_Assoc = new JSONArray();
    		String uri = baseUri + "/composite/sobjects";
    		
            for(Map.Entry<String, List<String>> entry: prodCatg_ConfigAssoc.entrySet()){
				String prodCatgId = prodCatg.get(entry.getKey());
				List<String> configExtIds = entry.getValue();
				
				for(String configExtId : configExtIds){
					if(consoleConfigAssoc.get(configExtId) != null){
						String configAssoc_Id = consoleConfigAssoc.get(configExtId);
						//logger.debug("--" + entry.getKey() + ":" + prodCatgId + " ----> " + configExtId + ":" + configAssoc_Id);
						
						JSONObject jsonConsoleConfigAssoc = new JSONObject();
		    			JSONObject attrib = new JSONObject();
		    			jsonConsoleConfigAssoc.put("attributes", attrib.put("type", "csbb__Configuration_Association__c"));
		    			jsonConsoleConfigAssoc.put("id", configAssoc_Id);
		    			jsonConsoleConfigAssoc.put("csbb__Product_Category__c", prodCatgId);
	
	
		    			arConsoleConfig_ProdCateg_Assoc.put(jsonConsoleConfigAssoc);
					}
				}
				//logger.debug("-------------------------------------------------------------------------------------------");
			}
            
            StringBuilder sb = new StringBuilder("{\"allOrNone\":" + false + ",\"records\":");
	    	sb.append(arConsoleConfig_ProdCateg_Assoc.toString(1)).append("}");
	    	
	    	logger.debug("JSON for update of csbb__Configuration_Association__c record:\n" + sb.toString());
            
            //Set up the HTTP objects needed to make the request.
	    	 HttpClient httpClient = HttpClientBuilder.create().build();
	         HttpPatch httpPatch = new HttpPatch(uri);
	         Header oauthHeader = new BasicHeader("Authorization", "OAuth " + conn.getLoginAccessToken()) ;
	         httpPatch.addHeader(oauthHeader);
	         httpPatch.addHeader(prettyPrintHeader);
            
            //Json to be posted
	         StringEntity body = new StringEntity(sb.toString());
	         body.setContentType("application/json");
	         httpPatch.setEntity(body);
            
            //Post the request
            HttpResponse response = httpClient.execute(httpPatch);
          
            //logger.debug("--Http Post Response: " + response);
            //Process the response
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                System.out.println("Updated the csbb__Configuration_Association__c successfully.");
                String response_string = EntityUtils.toString(response.getEntity());
                
                if(trgConfig.getOutputSuccess() != null || ! trgConfig.getOutputSuccess().trim().isEmpty()){
                	//Verify if output error file already exists then remove
                	Utility.cleanLogfile(new File(trgConfig.getOutputSuccess()));
                	
                	//Save the success output
                	logger.debug("--Saving the success output");
                	Utility.saveFile(response_string, new File(trgConfig.getOutputSuccess()));
                }
            } else {
                System.out.println("csbb__Configuration_Association__c update NOT successfully. Status code is " + statusCode);
                //Verify if output error file already exists then remove
            	Utility.cleanLogfile(new File(trgConfig.getOutputError()));
            	
            	//Save the error output
            	Utility.saveFile(EntityUtils.toString(response.getEntity()), new File(trgConfig.getOutputError()));
            }
            conn.ReleaseConnection(httpPost);
         
	     } catch (UnsupportedEncodingException use){
	    	// logger.error(use.getMessage(), use);
		 } catch (NullPointerException npe) {
			 logger.error(npe.getMessage(), npe);
	    } catch (IOException ioe) {
			//logger.error(ioe.getMessage(), ioe);
		 }
    }
    
    /**
     * Get Ids for the associated product category from the target sandbox
     * @return prodCatg
     */
    private Map<String, String> getAssociatedProductCategory(TargetConfig trgConfig, 
    									Map<String, List<String>>prodCatg_ConfigAssoc){
    	
    	Map <String, String> prodCatg = new HashMap<String, String>();
        StringBuilder sb = new StringBuilder();
        int i=0;
        
        for(Map.Entry<String, List<String>> entry : prodCatg_ConfigAssoc.entrySet()){
        	if(i==0){
        		sb.append('\'' + entry.getKey() + '\'').append("+");
        	}else{
        		sb.append(",+").append('\'' + entry.getKey() + '\'');
        	}
        	i++;
         }
            
        String soqlQuery = "Select+Id+,+Name+,+csexpimp1__guid__c+From+cscfga__Product_Category__c+"
        		+ "Where+csexpimp1__guid__c+in+(+"+ sb.toString()+"+)";
        
        HttpResponse response = initTargetOrgConnection(trgConfig, soqlQuery);

        if(response != null){
            // Process the result
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
	              String response_string = null;
	              try {
	            	  response_string = EntityUtils.toString(response.getEntity());
	            	  JSONObject json = new JSONObject(response_string);
	                  logger.debug("JSON result of Query:\n" + json.toString(1));
	                  JSONArray j = json.getJSONArray("records");
	                  
	                  //Create the externalId and Id mapping
	                  for (int cnt = 0; cnt < j.length(); cnt++){
	                	  JSONObject obj = json.getJSONArray("records").getJSONObject(cnt);
	                	  prodCatg.put(obj.getString("csexpimp1__guid__c"), obj.getString("Id"));
	                  }
	                  
	                  //Verify the mapping
	                  /*logger.debug("---Verify cscfga__Product_Category__c mapping");
	                  for(Map.Entry<String, String> map : prodCatg.entrySet()){
	                	  logger.debug("Key: " + map.getKey() + " Value: " + map.getValue());
	                  }*/
	
	              }catch (ParseException | IOException ex) {
						logger.error(ex.getMessage(), ex);
	              }
	          } else {
	          	logger.debug("Query was unsuccessful. Status code returned is " + statusCode);
	          	logger.debug("An error has occured. Http status: " + response.getStatusLine().getStatusCode());
	            
	          }
        }
        if(conn != null){
        	conn.ReleaseConnection(httpPost);
        }
	  	return  prodCatg;
    }
    /**
     * Get Ids for the Console Configuration Association from the target sandbox
     * @param trgConfig
     * @param prodCatg_ConfigAssoc
     * @return consoleConfigAssoc
     */
    private Map<String, String> getConfigAssociationMapping(TargetConfig trgConfig, Map<String, List<String>>prodCatg_ConfigAssoc){
    	
    	JSONObject json = null;
    	Map <String, String> consoleConfigAssoc = new HashMap<String, String>();
    	StringBuilder sb = new StringBuilder();
        //List<String> configAssoc_ExtId = new ArrayList<String>();
        
        for(Map.Entry<String, List<String>> entry : prodCatg_ConfigAssoc.entrySet()){
        	List<String> l = entry.getValue();
        	for(int cnt=0; cnt<l.size(); cnt++){
            	if(sb.length() == 0){
            		sb.append('\'' + l.get(cnt) + '\'').append("+");
            	}else{
            		sb.append(",+").append('\'' + l.get(cnt) + '\'').append("+");
            	}
        	}
        }
        
        String soqlQuery = "Select+Id+,+Name+,+CCA_External_ID__c+From+csbb__Configuration_Association__c+where+CCA_External_ID__c+in+(+" + sb.toString() + ")";
        HttpResponse response = initTargetOrgConnection(trgConfig, soqlQuery);
        
        if(response != null){
            // Process the result
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
            	String response_string = null;
                try {
                	response_string = EntityUtils.toString(response.getEntity());
                    json = new JSONObject(response_string);
                    logger.debug("JSON result of Query:\n" + json.toString(1));
                    JSONArray j = json.getJSONArray("records");
                    
                    //Create the externalId and Id mapping
                    for (int cnt = 0; cnt < j.length(); cnt++){
                  	  JSONObject obj = json.getJSONArray("records").getJSONObject(cnt);
                  	  consoleConfigAssoc.put(obj.getString("CCA_External_ID__c"), obj.getString("Id"));
                    }
                    
                    //Verify the mapping
                   /* logger.debug("---Verify csbb__Configuration_Association__c mapping");
                    for(Map.Entry<String, String> map : consoleConfigAssoc.entrySet()){
                  	  logger.debug("Key: " + map.getKey() + " Value: " + map.getValue());
                    }*/

                }catch (ParseException | IOException ex) {
					logger.error(ex.getMessage(), ex);
              }
            } else {
            	logger.debug("Query was unsuccessful. Status code returned is " + statusCode);
            	logger.debug("An error has occured. Http status: " + response.getStatusLine().getStatusCode());
              
            }
        }
        if(conn != null){
        	conn.ReleaseConnection(httpPost);
        }
        return  consoleConfigAssoc; 
    }
}

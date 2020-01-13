package com.automation.cs.telstra;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
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

public class MigrateSpecificationMetadata extends MigrateData implements IMigrateData{
	private final String REST_ENDPOINT = "/services/data" ;
	private final String API_VERSION = "/v46.0" ;
    private String baseUri;
    private final Header prettyPrintHeader = new BasicHeader("X-PrettyPrint", "1");
    
    final static Logger logger = Logger.getLogger(MigrateSpecificationMetadata.class);
    
    /**
   	 * Extracts the data from csedm__Specification__c and child object Specification_Metadata__c from source org
   	 * @param SourceConfig
   	 * @return String json
   	 */
	@Override
	public String extract(SourceConfig srcConfig) {
		
		String sJson = null;
    	HttpResponse response = super.initSourceOrgConnection(srcConfig);
    	
    	if(response != null){
    		// Process the result
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
            	try {
	               	String response_string = EntityUtils.toString(response.getEntity());
	               	JSONObject json = new JSONObject(response_string);
	                   logger.debug("JSON result of Query:\n" + json.toString(1));
	                   JSONArray j = json.getJSONArray("records");
	                   
	                   int refId = 0;
	                    for (int i = 0; i < j.length(); i++){
	                    	//Update referenceId on Parent
	                    	refId++;
	                    	JSONObject obj = json.getJSONArray("records").getJSONObject(i).getJSONObject("attributes");
	                    	obj.put("referenceId", "ref" + refId);
	                    }
	                   sJson = createJSON(json);

            	} catch (ParseException | IOException ex) {
					logger.error(ex.getMessage(), ex);
				}
            	
            }else {
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
	 * Get all specifications  from target sandbox
	 * @param conn
	 * @param trgConfig
	 * @return mapSpecs
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public Map<String, String> getSpecifications(TargetConfig trgConfig) throws ClientProtocolException, IOException{
		Map<String, String> mapSpecs = new HashMap<String, String>();
		
		String soqlQuery = "Select+ID+,+Name+From+csedm__Specification__c";
		//String sJson = null;
		
        HttpResponse response = initTargetOrgConnection(trgConfig, soqlQuery);
        
        if(response != null){
        	// Process the result
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
	              String response_string = null;
	              try {
	            	  response_string = EntityUtils.toString(response.getEntity());
	            	  JSONObject json = new JSONObject(response_string);
	                  //logger.debug("JSON result of Query:\n" + json.toString(1));
	                  JSONArray j = json.getJSONArray("records");
	                  
	                  for (int i = 0; i < j.length(); i++){
	                  	JSONObject obj = json.getJSONArray("records").getJSONObject(i);
	                  	mapSpecs.put(obj.getString("Name"), obj.getString("Id"));
	                  }
	                  
	                  //Verify the values.
	                 /* for(Map.Entry<String, String> m : mapSpecs.entrySet()){
	                  	logger.debug("----Name: " + m.getKey() + " Value: " + m.getValue());
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
        return mapSpecs;
	}
	
	
	/**
	 * Insert Json to the Specification_Metadata__c object in target Org
	 * @param String json
	 */
	@Override
	public void insert(String sJson, TargetConfig trgConfig) {
		SFConnection conn = null;
		StringBuilder jsonBuilder = null;
		//Map<Integer, String> mapSpec_ProdDefName = new HashMap<Integer, String>();
		
		try {
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
			
			Map<String, String> mapSpec = getSpecifications(trgConfig);
			
			baseUri = conn.getLoginInstanceUrl() + REST_ENDPOINT + API_VERSION;
			String uri = baseUri + "/composite/tree/" + trgConfig.getEntity();
			logger.debug("-- Uri: " + uri);
			
			 //Set up the HTTP objects needed to make the request.
            HttpClient httpClient = HttpClientBuilder.create().build();
            httpPost = new HttpPost(uri);
            
            Header oauthHeader = new BasicHeader("Authorization", "OAuth " + conn.getLoginAccessToken()) ;
            httpPost.addHeader(oauthHeader);
            httpPost.addHeader(prettyPrintHeader);
            
            //Parse the Json to fetch csedm__Product_Definition__r.Name and then remove the object
            JSONObject json = new JSONObject(sJson);
            JSONArray jRecords = json.getJSONArray("records");
            JSONArray arrRecords = new JSONArray();
            int index = 0;
            
            for (int i = 0; i < jRecords.length(); i++){
            
            	JSONObject obj = jRecords.getJSONObject(i);
            	
            	if(jRecords.getJSONObject(i).get("Specification_Metadata__r") != JSONObject.NULL){
            		 JSONObject childObj = jRecords.getJSONObject(i).getJSONObject("Specification_Metadata__r");
            		 logger.debug("Specification Name: " + obj.getString("Name"));
            		 
            		 if(mapSpec.get(obj.getString("Name")) != null){ //Check if Specification Name exists at current record index
            			 String Id = mapSpec.get(obj.getString("Name"));
            			 JSONArray specRecords = childObj.getJSONArray("records");
            			 //logger.debug("Product Def Name: " + obj.getString("Name") + "  Id: " + Id);
            			 
            			 for(int k=0; k<specRecords.length(); k++){
            				 index++;
            				 specRecords.getJSONObject(k).put("Specification__c", Id);
            				 //Update the attribute
            				 JSONObject attrib = specRecords.getJSONObject(k).getJSONObject("attributes");
            				 attrib.put("referenceId", "ref" + index);
            				 attrib.remove("url");
            				 arrRecords.put(specRecords.getJSONObject(k));
            			 }
            		 } 
            	}
            }
           
            //logger.debug("---Printing: " + index + "  " + arrRecords);
            if(arrRecords.length() > 0){
	            jsonBuilder = new StringBuilder("{\"totalSize\":" + arrRecords.length() + ",\"records\":");
	            jsonBuilder.append(arrRecords).append(", \"done\": true }");
	            
	            StringEntity body = new StringEntity(jsonBuilder.toString());
	            body.setContentType("application/json");
	            httpPost.setEntity(body);
	            
	            //Post the request
	            HttpResponse response = httpClient.execute(httpPost);
	          
	            logger.debug("--Http Post Response: " + response);
	            //Process the results
	            int statusCode = response.getStatusLine().getStatusCode();
	            if (statusCode == 201) {
	                String response_string = EntityUtils.toString(response.getEntity());
	                
	                if(trgConfig.getOutputSuccess() != null || ! trgConfig.getOutputSuccess().trim().isEmpty()){
	                	//Verify if output error file already exists then remove
	                	Utility.cleanLogfile(new File(trgConfig.getOutputSuccess()));
	                	
	                	//Save the success output
	                	logger.debug("--Saving the success output");
	                	Utility.saveFile(response_string, new File(trgConfig.getOutputSuccess()));
	                }
	                
	            } else {
	                logger.debug("Insertion unsuccessful. Status code returned is " + statusCode);
	                if(trgConfig.getOutputError() != null || ! trgConfig.getOutputError().trim().isEmpty()){
	                	//Verify if output error file already exists then remove
	                	Utility.cleanLogfile(new File(trgConfig.getOutputError()));
	                	
	                	//Save the error output
	                	Utility.saveFile(EntityUtils.toString(response.getEntity()), new File(trgConfig.getOutputError()));
	                }
	            }
            }
          
            conn.ReleaseConnection(httpPost);
	     } catch (UnsupportedEncodingException use){
	    	 logger.error(use.getMessage(), use);
		 } catch (NullPointerException npe) {
			 logger.error(npe.getMessage(), npe);
		 } catch (IOException e) {
			 logger.error(e.getMessage(), e);
		 }
	}
}

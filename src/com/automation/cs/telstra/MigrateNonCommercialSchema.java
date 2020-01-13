package com.automation.cs.telstra;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

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

public class MigrateNonCommercialSchema extends MigrateData implements IMigrateData{

	private final String REST_ENDPOINT = "/services/data" ;
	private final String API_VERSION = "/v46.0" ;
    private String baseUri;
    private final Header prettyPrintHeader = new BasicHeader("X-PrettyPrint", "1");
    
    final static Logger logger = Logger.getLogger(MigrateNonCommercialSchema.class);   

    /**
   	 * Extracts the data from csoe__Non_Commercial_Schema__c from source org
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
	 * Insert Json to the csoe__Non_Commercial_Schema__c object in target Org
	 * @param String json
	 */
	@Override
	public void insert(String sJson, TargetConfig trgConfig) {
		
		SFConnection conn = null;
		StringBuilder jsonBuilder = null;
		final int batchSize = 200;
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
			
			baseUri = conn.getLoginInstanceUrl() + REST_ENDPOINT + API_VERSION;
			String uri = baseUri + "/composite/tree/" + trgConfig.getEntity(); 
			logger.debug("-- Uri: " + uri);
			
			 //Set up the HTTP objects needed to make the request.
            HttpClient httpClient = HttpClientBuilder.create().build();
            httpPost = new HttpPost(uri);
            
            Header oauthHeader = new BasicHeader("Authorization", "OAuth " + conn.getLoginAccessToken()) ;
            httpPost.addHeader(oauthHeader);
            httpPost.addHeader(prettyPrintHeader);
            
            JSONObject json = new JSONObject(sJson);
            JSONArray jRecords = json.getJSONArray("records");
            JSONArray arrRecords = new JSONArray();
            List<JSONArray> lstJsonArray = new ArrayList<JSONArray>();
            int index = 0;
            
            for (int i = 0; i < jRecords.length(); i++){
            	JSONObject obj = jRecords.getJSONObject(i);
				 
            	//Update the attribute
				JSONObject attrib = jRecords.getJSONObject(i).getJSONObject("attributes");
				attrib.put("referenceId", "ref" + index++);
				attrib.remove("url");
				arrRecords.put(obj);
				
				if(index % batchSize == 0){
					lstJsonArray.add(arrRecords);
					arrRecords = new JSONArray(); //Create new Array once old array with 200 records are added to list
				}
            }
            
            //Take care of the remaining records which are not within multiple of 200
            if(arrRecords.length() > 0){
            	lstJsonArray.add(arrRecords);
            }
            
            /*logger.debug("--Size of Json arraylist: " + lstJsonArray.size());
            for(JSONArray arrJson : lstJsonArray){
            	logger.debug("---Size of Json array: " + arrJson.length());
            }*/
            
            int tmpCnt = 0;
            for(int cnt = 0; cnt < lstJsonArray.size(); cnt++){
            	tmpCnt++;
            	logger.debug("--Loop Count: " + tmpCnt);
            	JSONArray arrJson = lstJsonArray.get(cnt);
            	if(arrJson.length() > 0){
            		jsonBuilder = new StringBuilder("{\"totalSize\":" + arrJson.length() + ",\"records\":");
		            jsonBuilder.append(arrJson).append(", \"done\": true }");
		            
		            //Json to be posted
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
            }
            conn.ReleaseConnection(httpPost);
	     } catch (UnsupportedEncodingException use){
	    	 logger.error(use.getMessage(), use);
		 } catch (NullPointerException npe) {
			 logger.error(npe.getMessage(), npe);
	     } catch (ClientProtocolException cpe) {
	    	 logger.error(cpe.getMessage(), cpe);
		 } catch (IOException e) {
			 logger.error(e.getMessage(), e);
		 }
	}
}

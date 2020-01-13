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

public class MigrateBillOfMaterialID extends MigrateData implements IMigrateData{
    final static Logger logger = Logger.getLogger(MigrateBillOfMaterialID.class);
    
    public MigrateBillOfMaterialID(){}
    
    /**
	 * Extracts the data from BillOfMaterialID__c object  from source org
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
	 * Insert Json to the BillOfMaterialID__c object in target Org
	 * @param String json
	 */
    @Override
   	public void insert(String json, TargetConfig trgConfig) {
   		/*SFConnection conn = null;
   		StringBuilder jsonBuilder = null;
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
               
               //Json to be posted
               StringEntity body = new StringEntity(json.toString());
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
               
               conn.ReleaseConnection(httpPost);
   	     } catch (UnsupportedEncodingException use){
   	    	 logger.error(use.getMessage(), use);
   		 } catch (NullPointerException npe) {
   			 logger.error(npe.getMessage(), npe);
   	     } catch (ClientProtocolException cpe) {
   	    	 logger.error(cpe.getMessage(), cpe);
   		 } catch (IOException e) {
   			 logger.error(e.getMessage(), e);
   		 }*/
    	super.insert(json, trgConfig);
   	}

}

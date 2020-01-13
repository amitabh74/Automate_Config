package com.automation.cs.telstra;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.automation.config.telstra.SourceConfig;
import com.automation.config.telstra.TargetConfig;
import com.automation.util.telstra.Utility;

public class MigrateData {

	private final String REST_ENDPOINT = "/services/data" ;
	private final String API_VERSION = "/v46.0" ;
    private String baseUri;
    private SourceConfig srcConfig;
    private TargetConfig trgConfig;
    private final Header prettyPrintHeader = new BasicHeader("X-PrettyPrint", "1");
    protected SFConnection conn;
    protected HttpPost httpPost;
    protected Header oauthHeader;
    
    final static Logger logger = Logger.getLogger(MigrateData.class);
    
    /**
     * Create connection with source-org to perform SOQL and get records
     * @param srcConfig
     * @return response
     */
	protected HttpResponse initSourceOrgConnection(SourceConfig srcConfig) {
		this.srcConfig = srcConfig;
		conn = new SFConnection(srcConfig.getSrcCred().getUserName(), 
				srcConfig.getSrcCred().getPassword(), 
				srcConfig.getSrcCred().getEndPoint(), 
				srcConfig.getSrcCred().getGrantService(), 
				srcConfig.getSrcCred().getClientId(), 
				srcConfig.getSrcCred().getClientSecret(),
				"source");
		
		logger.debug("----Creation connection with source Salesforce");
		httpPost = conn.createConnection();
		if (httpPost == null) return null;
		
		baseUri = conn.getLoginInstanceUrl() + REST_ENDPOINT + API_VERSION;
		logger.debug("--Base Uri: " + baseUri);
		 //String uri = baseUri;
		
        //Set up the HTTP objects needed to make the request.
        HttpClient httpClient = HttpClientBuilder.create().build();
        String soqlQuery = srcConfig.getExtractionSOQL();
        String uri = baseUri + "/query?q=" + soqlQuery;
        logger.debug("Query URL: " + uri);
       
        HttpGet httpGet = new HttpGet(uri);
        
        oauthHeader = new BasicHeader("Authorization", "OAuth " + conn.getLoginAccessToken()) ;
        //System.out.println("oauthHeader2: " + oauthHeader);
        httpGet.addHeader(oauthHeader);
        httpGet.addHeader(prettyPrintHeader);

        // Make the request.
        HttpResponse response = null;
		try {
			response = httpClient.execute(httpGet);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage(), e);
		}
        return response;
	}
	
	/**
	 * Create connection with target-org to perform SOQL and get records
	 * @param trgConfig
	 * @param soqlQuery
	 * @return response
	 */
	protected HttpResponse initTargetOrgConnection(TargetConfig trgConfig, String soqlQuery) {
		this.trgConfig = trgConfig;
		conn = new SFConnection(trgConfig.getTrgCredential().getUserName(), 
				trgConfig.getTrgCredential().getPassword(), 
				trgConfig.getTrgCredential().getEndPoint(), 
				trgConfig.getTrgCredential().getGrantService(), 
				trgConfig.getTrgCredential().getClientId(), 
				trgConfig.getTrgCredential().getClientSecret(),
				"target");
		
		logger.debug("----Creation connection with target Salesforce");
		HttpPost httpPost = conn.createConnection();
		if (httpPost == null) return null;
		
		baseUri = conn.getLoginInstanceUrl() + REST_ENDPOINT + API_VERSION;
		logger.debug("--Base Uri: " + baseUri);
		
	    //Set up the HTTP objects needed to make the request.
	    HttpClient httpClient = HttpClientBuilder.create().build();
	    
	    logger.debug("Printing SOQL: " + soqlQuery);
	    
	    
	    String uri = baseUri + "/query?q=" + soqlQuery;
	    logger.debug("Query URL: " + uri);
	     
	    HttpGet httpGet = new HttpGet(uri);
	      
	    oauthHeader = new BasicHeader("Authorization", "OAuth " + conn.getLoginAccessToken()) ;
	            httpGet.addHeader(oauthHeader);
	            httpGet.addHeader(prettyPrintHeader);
	   
	            // Make the request.
	    HttpResponse response = null;
		try {
			response = httpClient.execute(httpGet);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage(), e);
		}
	    return response;
            
	}
	
	/**
	 * Create JSON file for extracted records
	 * @param json
	 */
	protected String createJSON(JSONObject json){
		StringBuilder jsonBuilder = null;
		
		JSONArray myJson = json.getJSONArray("records");
        jsonBuilder = new StringBuilder("{\"totalSize\":" + json.getInt("totalSize") + ",\"records\":");
        jsonBuilder.append(myJson).append(", \"done\": true }");
        
       // System.out.println("Print the updated JSON: " + jsonBuilder.toString());
        if(srcConfig.getSaveFile().equalsIgnoreCase("Yes")){
        	try{
	        	//Save the Generated JSON with defined file name
	        	Utility.saveFile(jsonBuilder.toString(), new File(srcConfig.getFileName()));
        	}catch (JSONException je) {
            	logger.error(je.getMessage(), je);
            }catch(IOException ioe){
            	logger.error(ioe.getMessage(), ioe);
            }
        }
        return jsonBuilder.toString();
	}
	
	/**
	 * Create ErrorProcess Logs
	 * @param response
	 */
	protected void createErrorProcessLog(HttpResponse response){
		 if(srcConfig.getOutputError() != null || ! srcConfig.getOutputError().trim().isEmpty()){
         	//Verify if output error file already exists then remove
         	Utility.cleanLogfile(new File(srcConfig.getOutputError()));
         	
         	try{
         		//Save the error output
         		Utility.saveFile(EntityUtils.toString(response.getEntity()), new File(srcConfig.getOutputError()));
         	}catch(IOException ioe){
         		logger.error(ioe.getMessage(), ioe);
         	}
         }
	}

	/**
	 * Inserts the Json to target org object
	 * @param json
	 * @param trgConfig
	 */
	protected void insert(String json, TargetConfig trgConfig) {
		SFConnection conn = null;
		//StringBuilder jsonBuilder = null;
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
			String uri = baseUri + "/composite/tree/" + trgConfig.getEntity();  //csbb__Configuration__c";
			//System.out.println("-- Uri: " + uri);
			
			 //Set up the HTTP objects needed to make the request.
            HttpClient httpClient = HttpClientBuilder.create().build();
            httpPost = new HttpPost(uri);
            
            oauthHeader = new BasicHeader("Authorization", "OAuth " + conn.getLoginAccessToken()) ;
            httpPost.addHeader(oauthHeader);
            httpPost.addHeader(prettyPrintHeader);
            //System.out.println("---OAUTH HEADER: " + oauthHeader);
            
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
	     } catch (IOException ioe) {
			logger.error(ioe.getMessage(), ioe);
		 }

	}

}

package com.automation.cs.telstra;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

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

public class MigrateProductAttributeLOV implements IMigrateData{
	private final String REST_ENDPOINT = "/services/data" ;
	private final String API_VERSION = "/v46.0" ;
    private String baseUri;
    private final Header prettyPrintHeader = new BasicHeader("X-PrettyPrint", "1");
    
    final static Logger logger = Logger.getLogger(MigrateProductAttributeLOV.class);   
    
    /**
  	 * Extracts the data from Product_Attribute_LOV__c object from source org
  	 * @param SourceConfig
  	 * @return String json
  	 */
	@Override
	public String extract(SourceConfig srcConfig) {
		SFConnection conn = null;
		StringBuilder jsonBuilder = null;
		JSONArray arrRecords = new JSONArray();
         
		final int queryBatch = 1000;
		int itr = 1;
		int refId = 0;
		try {
			conn = new SFConnection(srcConfig.getSrcCred().getUserName(), 
					srcConfig.getSrcCred().getPassword(), 
					srcConfig.getSrcCred().getEndPoint(), 
					srcConfig.getSrcCred().getGrantService(), 
					srcConfig.getSrcCred().getClientId(), 
					srcConfig.getSrcCred().getClientSecret(),
					"source");
			
			logger.debug("----Creation connection with source Salesforce");
			HttpPost httpPost = conn.createConnection();
			if (httpPost == null) return null;
			
			baseUri = conn.getLoginInstanceUrl() + REST_ENDPOINT + API_VERSION;
			logger.debug("--Base Uri: " + baseUri);
			
			int recCnt = getRecordCount(conn, baseUri);
			logger.debug("Total Record Count: " + recCnt);
			
			//Gets the number of query iteration to be performed if only 1000 records can be fetched in single call
			if(recCnt > 1000){
				double n = (double) recCnt/queryBatch;
				logger.debug("---" + n);
				double x = n - (int) n;
				logger.debug("--- decimal value: " + x);
				if(x > 0){
					itr = (int) n;
					itr++;
					
				}
			}
			logger.debug("--- Total iterations: " + itr);
			
           //Set up the HTTP objects needed to make the request.
           HttpClient httpClient = HttpClientBuilder.create().build();
           
           int offsetVal = 0;
           for(int i=0; i<itr; i++){
        	   logger.debug("---- queryBatch: " + queryBatch + "  OFFSET" + offsetVal);
        	   String soql = srcConfig.getExtractionSOQL()+"+LIMIT+"+queryBatch+"+OFFSET+"+offsetVal;
        	   offsetVal = offsetVal + queryBatch;
        	   recCnt = recCnt - queryBatch;
        	   //String uri = baseUri + "/query?q=" + srcConfig.getExtractionSOQL();
           
        	   String uri = baseUri + "/query?q=" + soql;
        	   logger.debug("Query URL: " + uri);
          
        	   HttpGet httpGet = new HttpGet(uri);
           
              Header oauthHeader = new BasicHeader("Authorization", "OAuth " + conn.getLoginAccessToken()) ;
              httpGet.addHeader(oauthHeader);
              httpGet.addHeader(prettyPrintHeader);

	           // Make the request.
	           HttpResponse response = httpClient.execute(httpGet);

	           // Process the result
	           int statusCode = response.getStatusLine().getStatusCode();
	           if (statusCode == 200) {
	               String response_string = EntityUtils.toString(response.getEntity());
	               try {
	                   JSONObject json = new JSONObject(response_string);
	                   logger.debug("JSON result of Query:\n" + json.toString(1));
	                   JSONArray j = json.getJSONArray("records");
	                   
	                   int totalSize = json.getInt("totalSize");
	                   logger.debug("---Total Size: " + totalSize);
	               	
	                  
	                   for (int k = 0; k < j.length(); k++){
	                   	//Update referenceId on Parent
	                   	refId++;
	                   	JSONObject obj = json.getJSONArray("records").getJSONObject(k);
	                   	obj.getJSONObject("attributes").put("referenceId", "ref" + refId);
	                   	//obj.put("referenceId", "ref" + refId);
	                   	arrRecords.put(obj);
	                   }
	                   
	               } catch (JSONException je) {
	               	logger.error(je.getMessage(), je);
	               }
	           } else {
	           	logger.debug("Query was unsuccessful. Status code returned is " + statusCode);
	           	logger.debug("An error has occured. Http status: " + response.getStatusLine().getStatusCode());
	               
	               if(srcConfig.getOutputError() != null || ! srcConfig.getOutputError().trim().isEmpty()){
	               	//Verify if output error file already exists then remove
	               	Utility.cleanLogfile(new File(srcConfig.getOutputError()));
	               	
	               	//Save the error output
	               	Utility.saveFile(EntityUtils.toString(response.getEntity()), new File(srcConfig.getOutputError()));
	               }
	           }
          }
           
          if(arrRecords.length() > 0){
        	  jsonBuilder = new StringBuilder("{\"totalSize\":" + arrRecords.length() + ",\"records\":");
              jsonBuilder.append(arrRecords).append(", \"done\": true }");
              
              //System.out.println("Print the updated JSON: " + jsonBuilder.toString());
              if(srcConfig.getSaveFile().equalsIgnoreCase("Yes")){
              	//Save the Generated JSON with defined file name
              	Utility.saveFile(jsonBuilder.toString(), new File(srcConfig.getFileName()));
              }
          }
           
          conn.ReleaseConnection(httpPost);
       } catch (IOException ioe) {
       	logger.error(ioe.getMessage(), ioe);
       } catch (NullPointerException npe) {
           logger.error(npe.getMessage(), npe);
       }
		return jsonBuilder.toString();
	}
	
	
	/**
	 * Gets the total record count for object Product_Attribute_LOV__c object
	 * @param conn
	 * @param baseUri
	 * @return totalSize
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	private int getRecordCount(SFConnection conn, String baseUri) throws ClientProtocolException, IOException{
		int totalSize = 0;
		
		//Set up the HTTP objects needed to make the request.
        HttpClient httpClient = HttpClientBuilder.create().build();
  
        String uri = baseUri + "/query?q=Select+count(+Id+)+from+Product_Attribute_LOV__c";
        logger.debug("Query URL: " + uri);
       
        HttpGet httpGet = new HttpGet(uri);
        
        Header oauthHeader = new BasicHeader("Authorization", "OAuth " + conn.getLoginAccessToken()) ;
        httpGet.addHeader(oauthHeader);
        httpGet.addHeader(prettyPrintHeader);

        // Make the request.
        HttpResponse response = httpClient.execute(httpGet);

        // Process the result
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 200) {
            String response_string = EntityUtils.toString(response.getEntity());
            try {
                JSONObject json = new JSONObject(response_string);
                JSONArray j = json.getJSONArray("records");
                
                if(j.length() > 0){
                	totalSize = j.getJSONObject(0).getInt("expr0");
                }
                
            } catch (JSONException je) {
            	logger.error(je.getMessage(), je);
            }
        } else {
        	logger.debug("Query was unsuccessful. Status code returned is " + statusCode);
        	logger.debug("An error has occured. Http status: " + response.getStatusLine().getStatusCode());
            
        }

		return totalSize;
	}

	/**
	 * Insert Json to the Product_Attribute_LOV__c object in target Org
	 * @param String json
	 */
	@Override
	public void insert(String sJson, TargetConfig trgConfig) {
		SFConnection conn = null;
		final int batchSize = 200;
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
            
            logger.debug("---Total Length of records array: " + jRecords.length());
            for (int i = 0; i < jRecords.length(); i++){
                logger.debug("---Loop Count preparing for insert : " + i);
            	JSONObject obj = jRecords.getJSONObject(i);
		
				//Update the attribute
				JSONObject attrib = obj.getJSONObject("attributes");
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

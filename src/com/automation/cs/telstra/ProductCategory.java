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
import org.apache.http.client.methods.HttpPatch;
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

public class ProductCategory extends MigrateData implements IMigrateData{
	private final String REST_ENDPOINT = "/services/data" ;
	private final String API_VERSION = "/v46.0" ;
    private String baseUri;
    private final Header prettyPrintHeader = new BasicHeader("X-PrettyPrint", "1");
    
    final static Logger logger = Logger.getLogger(ProductCategory.class);
    private Map<String, String> id_guid = new HashMap<String, String>();
    private Map<String, String> guid_parentGuid_Map = new HashMap<String, String>();
    
    public ProductCategory(){}
    
    /**
  	 * Extracts the data from cscfga__Product_Category__c object  from source org
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
	                logger.debug("JSON result of Query:\n" + json.toString(1));
	                JSONArray j = json.getJSONArray("records");
	               
	                int refId = 1;
                    for (int i = 0; i < j.length(); i++, refId++){
                    	
                  	  String guid=null;
                  	  JSONObject objRec = json.getJSONArray("records").getJSONObject(i);
                  	  JSONObject obj = objRec.getJSONObject("attributes");
                  	  String id = obj.getString("url").substring(obj.getString("url").lastIndexOf("/") + 1);
            
                  	  if(objRec.get("csexpimp1__guid__c") instanceof String){
                  		  guid = objRec.getString("csexpimp1__guid__c");
                  	  }
                  	  logger.debug("--- Id: " + id);
                  	  logger.debug("--- guid:" + guid);
                  	  //Create a map of record id and guid (Unique External Id)
                  	  id_guid.put(id, guid);
                  	  
                  	  //Update referenceId in attribute
                  	  obj.put("referenceId", "ref" + refId);
                    }
                    
                    //Re-loop to create map of self-join association
                    logger.debug("-----Re-looping to set self-join association----");
                    for (int i = 0; i < j.length(); i++){
                  	  JSONObject objRec = json.getJSONArray("records").getJSONObject(i);
                  	  if(objRec.get("cscfga__Parent_Category__c") instanceof String){
                  		  String parentGuid = id_guid.get(objRec.getString("cscfga__Parent_Category__c"));
                  		  logger.debug("---Parent Category: " +  objRec.getString("cscfga__Parent_Category__c")
                  		  + " : parent guid " + parentGuid);
                  		  
                  		  guid_parentGuid_Map.put(objRec.getString("csexpimp1__guid__c"), parentGuid);
                  		  
                  		  objRec.remove("cscfga__Parent_Category__c");
                  		  objRec.put("cscfga__Parent_Category__c", JSONObject.NULL);
                  	  }
                    }
                    
                    //Loop to verify the self-join association created.
                   /* for (Map.Entry<String,String> entry : guid_parentGuid_Map.entrySet()){
                  	  logger.debug("---- key: " + entry.getKey() + " value: " + entry.getValue());  
                    }*/
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
  	 * Insert Json to the cscfga__Product_Category__c object in target Org
  	 * @param String json
  	 */
      @Override
  	public void insert(String json, TargetConfig trgConfig) {
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
  			//String uri = baseUri + "/sobjects/" + trgConfig.getEntity();  //csbb__Callout_Template__c";
  			String uri = baseUri + "/composite/tree/" + trgConfig.getEntity();
  			
  			logger.debug("-- Uri: " + uri);
  			
  			 //Set up the HTTP objects needed to make the request.
              HttpClient httpClient = HttpClientBuilder.create().build();
              httpPost = new HttpPost(uri);
              
              Header oauthHeader = new BasicHeader("Authorization", "OAuth " + conn.getLoginAccessToken()) ;
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
                  //JSONObject jsonObj = new JSONObject(response_string);
                  //System.out.println("--Http Post Response Success: " + response_string);
                  
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

              //Update the cscfga__Parent_Category__c field with the Parent Id
              getProductCategory(trgConfig);
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
    
    /**
     * Retrieves the Product Category record from Target sandbox
     * @param conn
     */
    private void getProductCategory(TargetConfig trgConfig){
    	 //Create criteria
        StringBuilder sb = new StringBuilder();
        Map<String, String> trgMap = new HashMap<String, String>();
        
        for (Map.Entry<String,String> entry : guid_parentGuid_Map.entrySet()){
        	logger.debug("---- key: " + entry.getKey() + " value: " + entry.getValue());  
        	 sb.append("csexpimp1__guid__c='").append(entry.getKey()).append("'+or+")
        	 					.append("csexpimp1__guid__c='").append(entry.getValue()).append("'+or+");
        }
        String criteria = sb.toString();
        criteria = criteria.substring(0, criteria.length() - 4);
        
        String soqlQuery = "Select+Id+,+Name+,+csexpimp1__guid__c+From+cscfga__Product_Category__c+where+" + criteria;
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
	                  
	                //Loop to fetch the guid and id from the json and store it in key-value pair
	                  for (int i = 0; i < j.length(); i++){
	                	 JSONObject rec = json.getJSONArray("records").getJSONObject(i);
	                	 trgMap.put(rec.getString("csexpimp1__guid__c"), rec.getString("Id"));
	                  }
	                  
	                  //Finally update the self-association for Product Record
	                  updateProductRecords(trgMap, trgConfig, oauthHeader);
	
	              }catch (ParseException | IOException ex) {
						logger.error(ex.getMessage(), ex);
	              }
            }else {
              	logger.debug("Query was unsuccessful. Status code returned is " + statusCode);
              	logger.debug("An error has occured. Http status: " + response.getStatusLine().getStatusCode());
            }
        }
        if(conn != null){
      		 conn.ReleaseConnection(httpPost);
        }
    	
    }
    /**
     * Updates the cscfga__Parent_Category__c field with parent Id
     * @param Map<String, String>
     * @param TargetConfig
     * @param Header
     */
    private void updateProductRecords(Map<String, String>trgMap, TargetConfig trgConfig, Header oauthHeader){
    	//logger.debug("----Printing target guid & id-----");
    	/*for(Map.Entry<String,String> recEntry : trgMap.entrySet()){
      	  logger.debug("---- key: " + recEntry.getKey() + " value: " + recEntry.getValue()); 
        }
    	
    	for(Map.Entry<String, String> guid : guid_parentGuid_Map.entrySet()){
    		logger.debug("---- key: " + guid.getKey() + " value: " + guid.getValue()); 
    	}*/
    	
    	try{
    		JSONArray arProductCatg = new JSONArray();
    		String uri = baseUri + "/composite/sobjects";
	    	for(Map.Entry<String,String> recEntry : trgMap.entrySet()){
	    		logger.debug("---Printing the value based on stored keys: " + guid_parentGuid_Map.get(recEntry.getKey()));
	        	  if(guid_parentGuid_Map.get(recEntry.getKey()) != null){
	        		  //String uri = baseUri + "/sobjects/cscfga__Product_Category__c/" + recEntry.getValue();
	        		 
	    			  JSONObject product_catg = new JSONObject();
	    			  JSONObject attrib = new JSONObject();
	    			  product_catg.put("attributes", attrib.put("type", "cscfga__Product_Category__c"));
	    			  product_catg.put("id", recEntry.getValue());
	    			  product_catg.put("csexpimp1__guid__c", recEntry.getKey());
	    			  /*logger.debug("--Printing the association Id: " + 
	    					  	trgMap.get(guid_parentGuid_Map.get(recEntry.getKey())));*/
	    			  
	    			  product_catg.put("cscfga__Parent_Category__c", 
	    					  trgMap.get(guid_parentGuid_Map.get(recEntry.getKey())));
	    			  
	    			  arProductCatg.put(product_catg);
	        	  }
	         }
	    	StringBuilder sb = new StringBuilder("{\"allOrNone\":" + false + ",\"records\":");
	    	sb.append(arProductCatg.toString(1)).append("}");
	    	
	    	logger.debug("JSON for update of cscfga__Product_Category__c record:\n" + sb.toString());
	    	
	    	 //Set up the objects necessary to make the request.
            HttpClient httpClient = HttpClientBuilder.create().build();
 
            HttpPatch httpPatch = new HttpPatch(uri);
            httpPatch.addHeader(oauthHeader);
            httpPatch.addHeader(prettyPrintHeader);
            
            //Json to be posted
            StringEntity body = new StringEntity(sb.toString());
            body.setContentType("application/json");
            httpPatch.setEntity(body);

            //Make the request
            HttpResponse response = httpClient.execute(httpPatch);
            
            //Process the response
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                System.out.println("Updated the cscfga__Product_Category__c successfully.");
            } else {
                System.out.println("cscfga__Product_Category__c update NOT successfully. Status code is " + statusCode);
            }
	    					
    	}catch (JSONException e) {
           logger.error(e.getMessage(), e);
    	}catch (IOException ioe) {
    		logger.error(ioe.getMessage(), ioe);
        } catch (NullPointerException npe) {
            logger.error(npe.getMessage(), npe);
        }
    	
    }
}

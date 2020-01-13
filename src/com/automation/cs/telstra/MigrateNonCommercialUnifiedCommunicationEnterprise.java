package com.automation.cs.telstra;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

public class MigrateNonCommercialUnifiedCommunicationEnterprise extends MigrateData implements IMigrateData{
	private final String REST_ENDPOINT = "/services/data" ;
	private final String API_VERSION = "/v46.0" ;
    private String baseUri;
    private final Header prettyPrintHeader = new BasicHeader("X-PrettyPrint", "1");
    
    final static Logger logger = Logger.getLogger(MigrateNonCommercialUnifiedCommunicationEnterprise.class);   

    /**
   	 * Create Sample data as mentioned in CSConfiguration the data from csoe__Non_Commercial_Schema__c from source org
   	 * @param SourceConfig
   	 * @return String json
   	 */
	@Override
	public String extract(SourceConfig srcConfig) {
		try {
          
            if(srcConfig.getSaveFile().equalsIgnoreCase("Yes")){
            	//Save the Generated JSON with defined file name
            	Utility.saveFile(NonCommercialUnifiedCommunicationEnterprise(), new File(srcConfig.getFileName()));
            }
                    
         } catch (IOException je) {
               logger.error(je.getMessage(), je);
         }
              
		return NonCommercialUnifiedCommunicationEnterprise();
	}
	
	/**
	 * Create the hard-coded JSON in code as per requirement mentioned in "CS Configuration SetUp_v1.3" document
	 * @return sJson
	 */
	private String NonCommercialUnifiedCommunicationEnterprise(){
		String sJson = "{\"totalSize\":2,\"done\":true,\"records\":[{\"attributes\":{\"type\":\"csoe__Non_Commercial_Product_Association__c\"},\"csoe__Commercial_Product_Definition__c\":\"Unified Communication Enterprise\",\"csoe__Max__c\":1,\"csoe__Min__c\":1,\"csoe__Tab_Order__c\":0,\"csoe__Non_Commercial_Schema__c\":null,\"Name\":\"Order Primary Contact\",\"csoe__Non_Commercial_Product_Definition__c\":\"Order primary Contact\"},{\"attributes\":{\"type\":\"csoe__Non_Commercial_Product_Association__c\"},\"csoe__Commercial_Product_Definition__c\":\"Unified Communication Enterprise\",\"csoe__Max__c\":1,\"csoe__Min__c\":1,\"csoe__Non_Commercial_Schema__c\":\"NumberManagementv1\",\"csoe__Tab_Order__c\":1,\"Name\":\"UC Numbers\",\"csoe__Non_Commercial_Product_Definition__c\":null}]}";
		//String sJson = "{\"totalSize\":2,\"done\":true,\"records\":[{\"attributes\":{\"type\":\"csoe__Non_Commercial_Product_Association__c\"},\"csoe__Commercial_Product_Definition__c\":\"Unified Communication Enterprise\",\"csoe__Max__c\":1,\"csoe__Min__c\":1,\"csoe__Tab_Order__c\":0,\"csoe__Non_Commercial_Schema__c\":null,\"Name\":\"Order Primary Contact\",\"csoe__Non_Commercial_Product_Definition__c\":\"Order primary Contact\"},{\"attributes\":{\"type\":\"csoe__Non_Commercial_Product_Association__c\"},\"csoe__Commercial_Product_Definition__c\":\"Unified Communication Enterprise\",\"csoe__Max__c\":1,\"csoe__Min__c\":1,\"csoe__Non_Commercial_Schema__c\":\"NumberManagementv1\",\"csoe__Tab_Order__c\":1,\"Name\":\"UC Numbers\",\"csoe__Non_Commercial_Product_Definition__c\":\"Unified Communication Enterprise\"}]}";
		return sJson;
	}
	
	/**
	 * Get Product Definition ID for passed Product Definition Name
	 * @param conn
	 * @param trgConfig
	 * @return mapProdDef
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public Map<String, String> getProductDefintionName(TargetConfig trgConfig, 
								String prodDefName) throws ClientProtocolException, IOException{
		
		Map<String, String> mapProdDef = new HashMap<String, String>();

        prodDefName = Utility.replaceWhiteSpace(prodDefName);
        logger.debug("--Printing string w/o whitspace: " + prodDefName);
        
        String soqlQuery = "Select+ID+,+Name+From+cscfga__Product_Definition__c+Where+Name='"+prodDefName+"'";
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
	                  JSONArray j = json.getJSONArray("records");
	                  
	                  for (int i = 0; i < j.length(); i++){
	                  	JSONObject obj = json.getJSONArray("records").getJSONObject(i);
	                  	mapProdDef.put(obj.getString("Name"), obj.getString("Id"));
	                  }
	                  
	                  //Verify the values.
	                  /*for(Map.Entry<String, String> m : mapProdDef.entrySet()){
	                  	logger.debug("----Prod Def Name: " + m.getKey() + " Value: " + m.getValue());
	                  }*/
	
	              }catch (ParseException | IOException ex) {
						logger.error(ex.getMessage(), ex);
	              }
            }else {
              	logger.debug("Query was unsuccessful. Status code returned is " + statusCode);
              	logger.debug("An error has occured. Http status: " + response.getStatusLine().getStatusCode());
                
             }
        }
        return mapProdDef;
	}
	
	/**
	 * Get Product Definition Names for the list of product definitions
	 * @param conn
	 * @param trgConfig
	 * @param lstNonCommercialProdDefNames
	 * @return mapNonCommProdDef
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public Map<String, String> getNonCommercialProductDefintionNames(TargetConfig trgConfig, 
			List<String> lstNonCommercialProdDefNames) throws ClientProtocolException, IOException{

		Map<String, String> mapNonCommProdDef = new HashMap<String, String>();

		List<String> lstNames = Utility.replaceWhiteSpace(lstNonCommercialProdDefNames);
        String sNames = "";
        
        for(int x=0; x<lstNames.size(); x++){
        	if(x==0){
        		sNames = "'" + lstNames.get(x) + "'";
			}else{
				sNames = sNames+","+ "'" + lstNames.get(x) + "'";
			}
        }
        
        String soqlQuery = "Select+ID+,+Name+From+cscfga__Product_Definition__c+Where+Name+in+("+ sNames + ")";
        HttpResponse response = initTargetOrgConnection(trgConfig, soqlQuery);
        
        if(response != null){
        	// Process the result
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
	              String response_string = null;
	              try {
	            	  response_string = EntityUtils.toString(response.getEntity());
	            	  JSONObject json = new JSONObject(response_string);
	                  JSONArray j = json.getJSONArray("records");
	                  
	                  for (int i = 0; i < j.length(); i++){
	  					JSONObject obj = json.getJSONArray("records").getJSONObject(i);
	  					mapNonCommProdDef.put(obj.getString("Name"), obj.getString("Id"));
	  				  }
	  				
		  				//Verify the values.
		  			/*for(Map.Entry<String, String> m : mapNonCommProdDef.entrySet()){
		  					logger.debug("----Non Commercial Prod Def Name: " + m.getKey() + " Value: " + m.getValue());
		  			}*/
	
	              }catch (ParseException | IOException ex) {
						logger.error(ex.getMessage(), ex);
	              }
            }else {
              	logger.debug("Query was unsuccessful. Status code returned is " + statusCode);
              	logger.debug("An error has occured. Http status: " + response.getStatusLine().getStatusCode());
                
             }
        }
        return mapNonCommProdDef;
	}
	
	/**
	 * Get the Non Commercial Schema Names from the list of Non Commercial
	 * @param conn
	 * @param trgConfig
	 * @param lstNonCommercial
	 * @return mapNonCommSchema
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	private Map<String, String> getNonCommercialSchemaNames(TargetConfig trgConfig, 
			List<String> lstNonCommercial) throws ClientProtocolException, IOException{
		
		Map<String, String> mapNonCommSchema = new HashMap<String, String>();

        List<String> lstNames = Utility.replaceWhiteSpace(lstNonCommercial);
        String sNames = "";
        
        for(int x=0; x<lstNames.size(); x++){
        	if(x==0){
        		sNames = "'" + lstNames.get(x) + "'";
			}else{
				sNames = sNames+","+ "'" + lstNames.get(x) + "'";
			}
        }
        
        String soqlQuery = "Select+ID+,+Name+From+csoe__Non_Commercial_Schema__c+Where+Name+in+("+ sNames + ")";
        //String uri = baseUri + "/query?q=" + "Select+ID+,+Name+From+cscfga__Product_Definition__c+Where+Name='IP+Site'";
        
        HttpResponse response = initTargetOrgConnection(trgConfig, soqlQuery);
        if(response != null){
        	// Process the result
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
	              String response_string = null;
	              try {
	            	  response_string = EntityUtils.toString(response.getEntity());
	            	  JSONObject json = new JSONObject(response_string);
	                  JSONArray j = json.getJSONArray("records");
	                  
	                  for (int i = 0; i < j.length(); i++){
	                  	JSONObject obj = json.getJSONArray("records").getJSONObject(i);
	                  	mapNonCommSchema.put(obj.getString("Name"), obj.getString("Id"));
	                  }
	                  
	                  //Verify the values.
	                  /*for(Map.Entry<String, String> m : mapNonCommSchema.entrySet()){
	                  	logger.debug("----Non-Ccommercial Schema Name: " + m.getKey() + " Value: " + m.getValue());
	                  }*/
	
	              }catch (ParseException | IOException ex) {
						logger.error(ex.getMessage(), ex);
	              }
            }else {
              	logger.debug("Query was unsuccessful. Status code returned is " + statusCode);
              	logger.debug("An error has occured. Http status: " + response.getStatusLine().getStatusCode());
                
             }
        }
        return mapNonCommSchema;
	}
	
	/**
	 * Insert Json to the csoe__Non_Commercial_Schema__c object in target Org
	 * @param String json
	 */
	@Override
	public void insert(String sJson, TargetConfig trgConfig) {
		SFConnection conn = null;
		String profDefName = null;
		List<String> lstNonCommercial = new ArrayList<String>();
		List<String> lstNonCommercialProdDef = new ArrayList<String>();
		
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
			
			//Parse the Json to fetch Product Definition Name and Non-Commercial Schema Name
            JSONObject json = new JSONObject(sJson);
            JSONArray jRecords = json.getJSONArray("records");
            
            for (int i = 0; i < jRecords.length(); i++){
            	JSONObject obj = jRecords.getJSONObject(i);
            	if(i==0){
            		profDefName = obj.getString("csoe__Commercial_Product_Definition__c");
            	}
            	if(obj.get("csoe__Non_Commercial_Schema__c") != JSONObject.NULL){
            		lstNonCommercial.add(obj.getString("csoe__Non_Commercial_Schema__c"));
            	}
            	if(obj.get("csoe__Non_Commercial_Product_Definition__c") != JSONObject.NULL){
            		lstNonCommercialProdDef.add(obj.getString("csoe__Non_Commercial_Product_Definition__c"));
            	}
            }
			//verify the extracted Product Definition Name and Non-Commercial Schema Name
            logger.debug("--Product Definition Name: " + profDefName);
            for(String s : lstNonCommercial){
            	logger.debug("--Non-commercial-schema: " + s);
            }
            for(String s : lstNonCommercialProdDef){
            	logger.debug("--Non-commercial-Prod Def: " + s);
            }
            
			Map<String, String> mapProdDef = getProductDefintionName(trgConfig, profDefName);
			Map<String, String> mapNonCommSchema = getNonCommercialSchemaNames(trgConfig, lstNonCommercial);
			Map<String, String> mapNonCommProdDef = getNonCommercialProductDefintionNames(trgConfig, lstNonCommercialProdDef);
			
			baseUri = conn.getLoginInstanceUrl() + REST_ENDPOINT + API_VERSION;
			String uri = baseUri + "/composite/tree/" + trgConfig.getEntity();
			
			 //Set up the HTTP objects needed to make the request.
            HttpClient httpClient = HttpClientBuilder.create().build();
            httpPost = new HttpPost(uri);
            
            Header oauthHeader = new BasicHeader("Authorization", "OAuth " + conn.getLoginAccessToken()) ;
            httpPost.addHeader(oauthHeader);
            httpPost.addHeader(prettyPrintHeader);
            
            for (int i = 0; i < jRecords.length(); i++){
                
            	JSONObject obj = jRecords.getJSONObject(i);
            	JSONObject attribute = obj.getJSONObject("attributes");
            	
            	attribute.put("referenceId", "ref" + (i+1));
            	
            	logger.debug("--- " + obj.getString("Name"));
            	//Check Product Definition
            	if(obj.get("csoe__Commercial_Product_Definition__c") != JSONObject.NULL){
            		obj.put("csoe__Commercial_Product_Definition__c", 
            				mapProdDef.get(obj.get("csoe__Commercial_Product_Definition__c")));
            	}else{
            		obj.put("csoe__Commercial_Product_Definition__c", JSONObject.NULL);
            	}
            	
            	//Check for Non-Commercial Schema
            	if(obj.get("csoe__Non_Commercial_Schema__c") != JSONObject.NULL){
            		obj.put("csoe__Non_Commercial_Schema__c", 
            				mapNonCommSchema.get(obj.get("csoe__Non_Commercial_Schema__c")));
            	}else{
            		obj.put("csoe__Non_Commercial_Schema__c", JSONObject.NULL);
            	}
            	
            	//Check for Non-Commercial Product Definition
            	logger.debug("--- " + obj.get("csoe__Non_Commercial_Product_Definition__c"));
            	
            	if(obj.get("csoe__Non_Commercial_Product_Definition__c") != JSONObject.NULL){
            		logger.debug("--Inside the soe__Non_Commercial_Product_Definition__c condition");
            		logger.debug("Printing Id: " + mapNonCommProdDef.get(obj.get("csoe__Non_Commercial_Product_Definition__c")));
            		
            		obj.put("csoe__Non_Commercial_Product_Definition__c", 
            				mapNonCommProdDef.get(obj.get("csoe__Non_Commercial_Product_Definition__c")));
            	}else{
            		obj.put("csoe__Non_Commercial_Product_Definition__c", JSONObject.NULL);
            	}
            	
            }
 
            logger.debug("--Json Value: " + json);
            
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
			
		 } catch (NullPointerException npe) {
			 logger.error(npe.getMessage(), npe);
		 } catch (Exception e) {
			 logger.error(e.getMessage(), e);
		 }
	}

}

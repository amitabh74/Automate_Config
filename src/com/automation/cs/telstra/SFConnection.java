package com.automation.cs.telstra;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class SFConnection {
	private String USERNAME;    
    private String PASSWORD;    
    private String LOGINURL;     
    private String GRANTSERVICE; 
    private String CLIENTID;     
    private String CLIENTSECRET; 
    
    private String connectionType;
    //private HttpPost httpPost;
    private String loginInstanceUrl;
    private String loginAccessToken;
    
    final static Logger logger = Logger.getLogger(SFConnection.class);
    
    /**
     * //The constructor should initialize connection (Source or Target Org) based on configuration params
     * @param userName
     * @param pwd
     * @param loginURL
     * @param grantService
     * @param clientId
     * @param clientSecret
     */
    public SFConnection(String userName, String password, String loginURL, String grantService, 
    		String clientId, String clientSecret, String type){
    	
    	this.setUserName(userName);
    	this.setPassword(password);
    	this.setLoginURL(loginURL);
    	this.setGrantService(grantService);
    	this.setClientId(clientId);
    	this.setClientSecret(clientSecret);
    	setConnectionType(type); 
    }
    
    /**
     * Sets the Grant Service
	 * @param grantService
	 */
	private void setGrantService(String grantService) {
		this.GRANTSERVICE = grantService;
	}

	/**
	 * Sets the client Id
	 * @param clientId
	 */
	private void setClientId(String clientId) {
		this.CLIENTID = clientId;
	}

	/**
	 * Sets the client secret
	 * @param clientSecret
	 */
	private void setClientSecret(String clientSecret) {
		this.CLIENTSECRET = clientSecret;
	}
	
	/**
	 * Sets the user name
	 * @param userName
	 */
	private void setUserName(String userName) {
		this.USERNAME = userName;
	}

	/**
	 * Sets the password
	 * @param password
	 */
	private void setPassword(String password) {
		this.PASSWORD = password;
	}

	/**
	 * Sets the login url
	 * @param loginURL
	 */
	private void setLoginURL(String loginURL) {
		this.LOGINURL = loginURL;
	}

	/**
     * Gets the connection type source/target
     * @return connectionType
     */
    public String getConnectionType() {
		return connectionType;
	}

    /**
     * Sets the connection type
     * @param connectionType
     */
	private void setConnectionType(String connectionType) {
		this.connectionType = connectionType;
	}

	/**
     * Gets the logged-in salesforce instance url
     * @return loginInstanceUrl
     */
	public String getLoginInstanceUrl() {
		return loginInstanceUrl;
	}
	
	/**
	 * Gets the login access token
	 * @return loginAccessToken
	 */
	public String getLoginAccessToken() {
		return loginAccessToken;
	}

	/**
	 * Creates OAuth connection with Salesforce org
	 * 
	 */
	public HttpPost createConnection(){
		HttpClient httpclient = HttpClientBuilder.create().build();
		
		 // Assemble the login request URL
        String loginURL = LOGINURL +
                          GRANTSERVICE +
                          "&client_id=" + CLIENTID +
                          "&client_secret=" + CLIENTSECRET +
                          "&username=" + USERNAME +
                          "&password=" + PASSWORD;

        // Login requests must be POSTs
        HttpPost httpPost = new HttpPost(loginURL);
        HttpResponse response = null;

        try {
            // Execute the login POST request
            response = httpclient.execute(httpPost);
        } catch (ClientProtocolException cpException) {
            logger.error(cpException.getMessage(), cpException);
        } catch (IOException ioException) {
            logger.error(ioException.getMessage(), ioException);
        }

        // verify response is HTTP OK
        final int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != HttpStatus.SC_OK) {
            logger.debug("Error authenticating to Force.com: "+statusCode);
            // Error is in EntityUtils.toString(response.getEntity())
            return null;
        }

        String getResult = null;
        try {
            getResult = EntityUtils.toString(response.getEntity());
            //System.out.println("Printing: " + getResult);
        } catch (IOException ioException) {
            logger.error(ioException.getMessage(), ioException);
        }
        
        JSONObject jsonObject = null;
        try {
            jsonObject = (JSONObject) new JSONTokener(getResult).nextValue();
            loginAccessToken = jsonObject.getString("access_token");
            loginInstanceUrl = jsonObject.getString("instance_url");
        } catch (JSONException jsonException) {
            logger.error(jsonException.getMessage(), jsonException);
        }
        logger.debug(response.getStatusLine());
        logger.debug("Successful login");
        logger.debug("  instance URL: "+loginInstanceUrl);
        logger.debug("  access token/session ID: "+loginAccessToken);

        
        return httpPost;

    }
	
	/**
	 * Releases http connection
	 */
	public void ReleaseConnection(HttpPost httpPost){
		 httpPost.releaseConnection();
	}

}

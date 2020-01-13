package com.automation.cs.telstra;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.automation.config.telstra.RootConfiguration;
import com.automation.config.telstra.SourceConfig;
import com.automation.config.telstra.TargetConfig;
import com.automation.config.telstra.UserConfiguration;
import com.automation.util.telstra.Utility;


public class Automate {
	final static Logger logger = Logger.getLogger(Automate.class);
 /**
  * Initiates the automation process
  * @param args
  * @throws IllegalAccessException
  * @throws IllegalArgumentException
  * @throws InvocationTargetException
  * @throws ClassNotFoundException
  * @throws InstantiationException
  * @throws NoSuchMethodException
  */
    public static void main(String[] args) throws IllegalAccessException, 
    				IllegalArgumentException, InvocationTargetException,
    				ClassNotFoundException, InstantiationException, NoSuchMethodException{
        	
    	
    	//Initialize Log4J framework
    	initializeLogging();
    	RootConfiguration rootConfiguration;
    	Class[] cArg;
    	Method m;
    	
    	
    	//Read the config file from APP_PATH and iterate through elements
    	try {
    		File file = new File("./config/conf.xml");
    		//File file = new File("config/conf.xml");
    		JAXBContext jaxbContext = JAXBContext.newInstance(RootConfiguration.class);
	        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
	        rootConfiguration = (RootConfiguration) unmarshaller.unmarshal(file);
	     
	        for(UserConfiguration userConfig:rootConfiguration.getUserConfiguration()) {
	        	
	        	logger.debug("--class name: " + userConfig.getName());
	        	Object obj = Class.forName(userConfig.getName()).newInstance();
	        	Object json_extract = null;
	        	
	        	if(fileExists(userConfig.getSourceConfig())){
	        		try{
	        			json_extract = Utility.readFile(new File(userConfig.getSourceConfig().getFileName()));
	        			
	        		}catch(IOException ioe){
	        			//Uses java reflection to invoke extract method
		        		cArg = new Class[1];
		        		cArg[0] = SourceConfig.class;
		        		m = obj.getClass().getMethod("extract", cArg);
		        		userConfig.getSourceConfig().setSrcCred(rootConfiguration.getSourceCred());
		        		json_extract = m.invoke(obj, userConfig.getSourceConfig());
	        		}
	        	}else{
	        		
	        		//Uses java reflection to invoke extract method
	        		cArg = new Class[1];
	        		cArg[0] = SourceConfig.class;
	        		m = obj.getClass().getMethod("extract", cArg);
	        		userConfig.getSourceConfig().setSrcCred(rootConfiguration.getSourceCred());
	        		json_extract = m.invoke(obj, userConfig.getSourceConfig());
	        	}
	        	
	        	//Use java reflection to invoke insert method
	        	cArg = new Class[2];
	            cArg[0] = String.class;
	            cArg[1] = TargetConfig.class;
	            
	        	m = obj.getClass().getMethod("insert", cArg);
	        	userConfig.getTargetConfig().setTrgCredential(rootConfiguration.getTargetCreds());
	        	m.invoke(obj, json_extract, userConfig.getTargetConfig());
	        	
	        }
    	}catch(Exception e){
    		//e.printStackTrace();
    		logger.error(e.getMessage(), e);
    	}
	}
   
    /**
     * Initialize Log4J framework
     */
    private static void initializeLogging(){
    	String log4jConfigFile = "./log4j.properties";
    	//String log4jConfigFile = "log4j.properties";
        PropertyConfigurator.configure(log4jConfigFile);
        
    }
    
    /**
     * Verify if data json file exists
     * @param srcConfig
     * @return boolean
     */
    private static boolean fileExists(SourceConfig srcConfig){
    	
    	//Verify if file containing corresponding json already exists
    	String fileName = srcConfig.getFileName();
    	try{
    		File f = new File(fileName);
    		if(f.exists()){
    			return true;
    		}else{
    			return false;
    		}
    	}catch(NullPointerException npe){
    		return false;
    	}
    }
	

}

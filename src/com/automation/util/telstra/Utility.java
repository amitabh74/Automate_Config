package com.automation.util.telstra;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Utility {
	/**
	 * Saves the Json string to the specified file path
	 * @param Json
	 * @param file
	 * @throws IOException
	 */
	public static void saveFile(String Json, File file)throws IOException{
		
		FileWriter fileWriter = new FileWriter(file);
		fileWriter.write(Json);
		fileWriter.close();
	}

	/**
	 * Reads the content of text file and returns the string
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static String readFile(File file)throws IOException
	{
	    StringBuilder contentBuilder = new StringBuilder();
	   
	    BufferedReader br = new BufferedReader(new FileReader(file));
	 
	    String sCurrentLine;
	    while ((sCurrentLine = br.readLine()) != null) {
	            contentBuilder.append(sCurrentLine).append("\n");
	    }
	    br.close();
	    
	    return contentBuilder.toString();
	}
	
	/**
	 * Checks and delete the file from the filepath
	 * @param f
	 * @throws SecurityException
	 */
	public static void cleanLogfile(File f) throws SecurityException{
		if(f.exists()){
			f.delete();
		}
	}
	
	/**
	 * removes the white space from string
	 * @param s
	 * @return str
	 */
	public static String replaceWhiteSpace(String s){
		String[] arrSplit = s.split(" ");
		String str = "";
		if(arrSplit.length > 0){
			for(int i=0; i<arrSplit.length; i++){
				if(i==0){
					str = arrSplit[i];
				}else{
					str = str+"+"+arrSplit[i];
				}
			}
		}else{
			str = s;
		}
		
		return str;
	}
	
	/**
	 * Iterates through list of string and removes the white space
	 * @param lstNames
	 * @return lst
	 */
	public static List<String> replaceWhiteSpace(List<String> lstNames){
		List<String> lst = new ArrayList<String>();
		
		for(String s : lstNames){
			lst.add(replaceWhiteSpace(s));
		}
		
		return lst;
	}
}

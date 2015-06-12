/**
 * 
 */
package com.ibm.iotf.client.app;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;

import com.google.gson.JsonObject;

/**
 * @author jeetesh
 *
 */
public class FileEvent {
	
	private BufferedReader br;
	private String filePath;
	private int currentRecord;
	
//	public FileEvent(String filePath) throws FileNotFoundException
//	{
//		this.filePath = filePath;
//		fis = new FileInputStream(filePath);
//		Files.newBufferedReader(FileSystems.getDefault()., Charset.forName("UTF-8"));
//	}
//	
//	public FileEvent()
//	{
//		fis = (FileInputStream) ClassLoader.getSystemResourceAsStream("mockdata.csv");
//	}
	
	JsonObject getData()
	{
		JsonObject jo = new JsonObject();
		jo.addProperty("t", Math.round((Math.random()+1)*30));
		return jo;
	}
	
	void close() throws IOException
	{
//		if(fis != null) {
//			fis.close();
//		}
//		currentRecord = 0;
	}

}

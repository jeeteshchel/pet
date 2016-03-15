/**
 * 
 */
package com.ibm.iotf.client.app;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * @author jeetesh
 *
 */
public class FileEvent {
	
	private static final Logger LOG = Logger.getLogger(FileEvent.class.getName());
	private BufferedReader br;
	private String filePath;
	private int currentRecord;
	
	public FileEvent(String filePath) throws FileNotFoundException
	{
		this.filePath = filePath;
		this.br = getFileStream(filePath);
	}
	
	public boolean isClosed() {
		return (br == null);
	}
	
	public BufferedReader getFileStream (String filePath) {
		BufferedReader br = null;
		try{
			
			 br = Files.newBufferedReader(Paths.get(this.filePath), Charset.forName("utf-8"));
		}catch(Exception io) {
			LOG.log(Level.SEVERE, io.getMessage(), io);
		}
		return br;
	}
//	
//	public FileEvent()
//	{
//		fis = (FileInputStream) ClassLoader.getSystemResourceAsStream("mockdata.csv");
//	}
	
	JsonObject getData()
	{
		String line = "";
		try{
		if(br == null || (line = br.readLine()) == null) {
			if(br != null)br.close();
			this.br = getFileStream (this.filePath);
			line = br.readLine();
			
		}
//		line = br.readLine();
		}catch(Exception e){
			LOG.log(Level.SEVERE, e.getMessage(), e);
		}
		JsonObject jo = new JsonParser().parse(line).getAsJsonObject();
		return jo;
	}
	
	void close() throws IOException
	{
		try{
			if(br != null) br.close();
			br = null;
		}catch (Exception io){
			LOG.log(Level.SEVERE, io.getMessage(), io);
		}
	}

}

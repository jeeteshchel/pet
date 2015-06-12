package com.ibm.iotf.client.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.util.Debug;

import com.ibm.iotf.client.device.DeviceClient;

public class FileEventListener implements Runnable {
	
	private boolean quit = false;
	private FileEvent fileEvent = null;
	private Properties options = new Properties();
	protected DeviceClient client;
	private static final Logger LOG = Logger.getLogger(FileEventListener.class.getName());
	
	public FileEventListener(String configFilePath) throws Exception {
		this.fileEvent = new FileEvent();
		this.options = DeviceClient.parsePropertiesFile(new File(configFilePath));
		this.client = new DeviceClient(this.options);
	}

	public FileEventListener() throws Exception {
		this.options.put("org", "gtwunk");
		this.options.put("type", "LocalMQTTFx");
		this.options.put("id", "jeetlocmqttfx");
		this.options.put("auth-method", "token");
		this.options.put("auth-token", "@2)qm5RoDK)BDQA3Sy");
		this.client = new DeviceClient(this.options);
		fileEvent = new FileEvent();
	}
	
	public void quit() {
		this.quit = true;
	}
	
	public void run() {
		try {
			client.connect();
			// Send a dataset every 1 second, until we are told to quit
			while (!quit) {
				
				client.publishEvent("status", fileEvent.getData().toString());
//				Debug d = client.getMqttClient().getDebug();
//				d.dumpClientDebug();
				Thread.sleep(10000);
			}
			fileEvent.close();
			// Once told to stop, cleanly disconnect from the service
			client.disconnect();
		} catch (InterruptedException e) {
			try {
				fileEvent.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				LOG.log(Level.SEVERE, "Exception thrown ", e1);
				e1.printStackTrace();
			}
			LOG.log(Level.SEVERE, "Exception thrown ", e);
			e.printStackTrace();
		} catch (IOException ioe) {
			LOG.log(Level.SEVERE, "Exception thrown ", ioe);
			ioe.printStackTrace();
		}
		
	}
	
	public static class LauncherOptions {
//		@Option(name="-c", aliases={"--config"}, usage="The path to a device configuration file")
//		public String configFilePath = null;
//		
//		public LauncherOptions() {} 
	}
	
	public static void main(String[] args) throws Exception {
		// Load custom logging properties file
		try {
			FileInputStream fis = new FileInputStream("logging.properties");
			LogManager.getLogManager().readConfiguration(fis);
		} catch (SecurityException e) {
		} catch (IOException e) {
		}

		// Start the device thread
		FileEventListener d = new FileEventListener();
		Thread t1 = new Thread(d);
		t1.start();

		System.out.println("(Press <enter> to disconnect)");
		// Wait for <enter>
		Scanner sc = new Scanner(System.in);
		sc.nextLine();
		sc.close();

		System.out.println("Closing connection to the IBM Internet of Things Cloud service");
		// Let the device thread know it can terminate
		d.quit();
	}
	
}
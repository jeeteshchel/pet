package com.ibm.iotf.client.device;

import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.iotf.client.AbstractClient;


/**
 * A client, used by device, that handles connections with the IBM Internet of Things Foundation. <br>
 * 
 * This is a derived class from AbstractClient and can be used by embedded devices to handle connections with IBM Internet of Things Foundation.
 */
public class DeviceClient extends AbstractClient implements MqttCallback, CommandCallback{
	
	private static final String CLASS_NAME = DeviceClient.class.getName();
	private static final Logger LOG = Logger.getLogger(CLASS_NAME);
	
	private static final Pattern COMMAND_PATTERN = Pattern.compile("iot-2/cmd/(.+)/fmt/(.+)");
	
	private CommandCallback commandCallback = null;
	
	String override;
	
	protected final static JsonParser JSON_PARSER = new JsonParser();
	private static final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");
	
	/**
	 * Create a device client for the IBM Internet of Things Foundation. <br>
	 * 
	 * Connecting to a specific account on the IoTF.
	 * @throws Exception 
	 */
	public DeviceClient(Properties options) throws Exception {
		super(options);
		LOG.fine(options.toString());
		this.clientId = "d" + CLIENT_ID_DELIMITER + getOrgId() + CLIENT_ID_DELIMITER + getDeviceType() + CLIENT_ID_DELIMITER + getDeviceId();
		
		if (getAuthMethod() == null) {
			this.clientUsername = null;
			this.clientPassword = null;
		}
		else if (!getAuthMethod().equals("token")) {
			throw new Exception("Unsupported Authentication Method: " + getAuthMethod());
		}
		else {
			// use-token-auth is the only authentication method currently supported
			this.clientUsername = "use-token-auth";
			this.clientPassword = getAuthToken();
		}
		createClient(this);
		this.commandCallback = this;
	}
	
	public String getOrgId() {
		return options.getProperty("org");
	}

	public String getDeviceId() {
		return options.getProperty("id");
	}

	public String getDeviceType() {
		return options.getProperty("type");
	}

	public String getAuthMethod() {
		return options.getProperty("auth-method");
	}

	public String getAuthToken() {
		return options.getProperty("auth-token");
	}


	public String getFormat() {
		String format = options.getProperty("format");
		if(format != null && ! format.equals(""))
			return format;
		else
			return "json";
		
	}
	
	/**
	 * Connect to the IBM Internet of Things Foundation
	 * 
	 */	
	@Override
	public void connect() {
		super.connect();
		if (!getOrgId().equals("quickstart")) {
			subscribeToCommands();
		}
	}
	
	private void subscribeToCommands() {
		try {
			mqttClient.subscribe("iot-2/cmd/+/fmt/" + getFormat(), 2);
			LOG.info("Subscribed to "+"iot-2/cmd/+/fmt/" + getFormat());
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Publish data to the IBM Internet of Things Foundation.<br>
	 * Note that data is published
	 * at Quality of Service (QoS) 0, which means that a successful send does not guarantee
	 * receipt even if the publish has been successful.
	 * 
	 * @param event
	 *            Name of the dataset under which to publish the data
	 * @param data
	 *            Object to be added to the payload as the dataset
	 * @return Whether the send was successful.
	 */
	public boolean publishEvent(String event, Object data) {
		return publishEvent(event, data, 0);
	}

	/**
	 * Publish data to the IBM Internet of Things Foundation.<br>
	 * 
	 * This method allows QoS to be passed as an argument
	 * 
	 * @param event
	 *            Name of the dataset under which to publish the data
	 * @param data
	 *            Object to be added to the payload as the dataset
	 * @param qos
	 *            Quality of Service - should be 0, 1 or 2
	 * @return Whether the send was successful.
	 */	
	public boolean publishEvent(String event, Object data, int qos) {
		if (!isConnected()) {
			return false;
		}
		if(override != null){
			data = override;
//			override = null;
		}
		JsonObject payload = new JsonObject();
		String runDateTime = sdf.format(new Date());
//		String timestamp = ISO8601_DATE_FORMAT.format(new Date());
//		payload.addProperty("ts", timestamp);
		
		JsonElement dataElement = gson.toJsonTree(data);
		dataElement.getAsJsonObject().addProperty("RUN_DATE_TIME", runDateTime);
		payload.add("d", dataElement);
		
		String topic = "iot-2/evt/" + event + "/fmt/json";
		
		LOG.fine("Topic   = " + topic);
		LOG.fine("Payload = " + payload.toString());
		
		MqttMessage msg = new MqttMessage(payload.toString().getBytes(Charset.forName("UTF-8")));
		msg.setQos(qos);
		msg.setRetained(false);
		
		try {
			mqttClient.publish(topic, msg);
		} catch (MqttPersistenceException e) {
			e.printStackTrace();
			return false;
		} catch (MqttException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	
	/**
	 * If we lose connection trigger the connect logic to attempt to
	 * reconnect to the IBM Internet of Things Foundation.
	 * 
	 * @param exception
	 *            Throwable which caused the connection to get lost
	 */
	public void connectionLost(Throwable exception) {
		LOG.info("Connection lost: " + exception.getMessage());
		connect();
	}
	
	/**
	 * A completed deliver does not guarantee that the message is received by the service
	 * because devices send messages with Quality of Service (QoS) 0. <br>
	 * 
	 * The message count
	 * represents the number of messages that were sent by the device without an error on
	 * from the perspective of the device.
	 * @param token
	 *            MQTT delivery token
	 */
	public void deliveryComplete(IMqttDeliveryToken token) {
		LOG.fine("Delivery Complete!");
		messageCount++;
	}
	
	/**
	 * The Device client does not currently support subscriptions.
	 */
	public void messageArrived(String topic, MqttMessage msg) throws Exception {
		if (commandCallback != null) {
			/* Only check whether the message is a command if a callback 
			 * has been defined, otherwise it is a waste of time
			 * as without a callback there is nothing to process the generated
			 * command.
			 */
			LOG.info("Received message "+msg+" on topic "+topic);
			Matcher matcher = COMMAND_PATTERN.matcher(topic);
			if (matcher.matches()) {
				String command = matcher.group(1);
				String format = matcher.group(2);
				Command cmd = new Command(command, format, msg);
				LOG.fine("Event received: " + cmd.toString());
				commandCallback.processCommand(cmd);
		    }
		}
	}
	
	public void setCommandCallback(CommandCallback callback) {
		this.commandCallback  = callback;
	}
	
	public static void main(String[] args) {
		
	}

	@Override
	public void processCommand(Command cmd) {
		LOG.info("Command received "+cmd.toString());
		JsonObject payloadJson = JSON_PARSER.parse(cmd.getPayload().replaceAll("\\", "")).getAsJsonObject();
		if (payloadJson.has("d")) {
			this.override = payloadJson.get("d").getAsJsonObject().toString();

		} else {
			this.override = payloadJson.toString();
		}

		LOG.info("Sending event "+cmd.toString());		
		publishEvent("status", override);
	}
	
}

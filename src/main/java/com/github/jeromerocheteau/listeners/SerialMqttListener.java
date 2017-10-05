package com.github.jeromerocheteau.listeners;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import purejavacomm.CommPortIdentifier;
import purejavacomm.NoSuchPortException;
import purejavacomm.PortInUseException;
import purejavacomm.SerialPort;
import purejavacomm.UnsupportedCommOperationException;

public abstract class SerialMqttListener implements Runnable, ServletContextListener {

	private SerialPort port;
	
	private CommPortIdentifier identifier;
	
    private InputStream stream;
    
    private InputStreamReader reader;
    
    private MqttClient client;
	
	public void contextInitialized(ServletContextEvent sce) {
		try {
			this.setMqttClient(sce.getServletContext());
			this.setSerialReader(sce.getServletContext());
			new Thread(this).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void contextDestroyed(ServletContextEvent sce) {
		try {
			Thread.currentThread().interrupt();
			if (reader != null) reader.close();
			if (stream != null) stream.close();
			if (port != null) port.close();
			if (client != null && client.isConnected()) client.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setMqttClient(ServletContext context) throws MqttException, MqttSecurityException {
		String broker = context.getInitParameter("mqtt-broker");
		String id = context.getInitParameter("mqtt-client-id");
		id = id == null ? UUID.randomUUID().toString() : id;
		this.setMqttClient(broker, id);
	}

	private void setMqttClient(String broker, String id) throws MqttException, MqttSecurityException {
		MemoryPersistence persistence = new MemoryPersistence();
		this.client = new MqttClient(broker, id, persistence);
		MqttConnectOptions options = new MqttConnectOptions();
		options.setCleanSession(true);
		this.client.connect(options);
	}

	private void setSerialReader(ServletContext context) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, InterruptedException, IOException {
		String port = context.getInitParameter("serial-port");
		String id = context.getInitParameter("serial-client-id");
		id = id == null ? UUID.randomUUID().toString() : id;
		String to = context.getInitParameter("timeout");
		int timeout = to == null ? 2000 : Integer.valueOf(to).intValue();
		Integer rate = Integer.valueOf(context.getInitParameter("baud-rate"));
		int databits = this.getDataBits(context.getInitParameter("data-bits"));
		int stopbits = this.getStopBits(context.getInitParameter("stop-bits"));
		int parity = this.getParity(context.getInitParameter("parity"));
		int mode = this.getFlowControlMode(context.getInitParameter("flow-control-mode"));
		this.setSerialReader(port, id, timeout, rate, databits, stopbits, parity, mode);
	}

	private void setSerialReader(String port, String id, int timeout, Integer rate, int databits, int stopbits, int parity, int mode) 
			throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, InterruptedException, IOException {
		this.identifier = CommPortIdentifier.getPortIdentifier(port);
		this.port = (SerialPort) identifier.open(id, timeout);
		this.port.setSerialPortParams(rate, databits, stopbits, parity);
		this.port.setFlowControlMode(mode);
		Thread.sleep(100);
		this.stream = this.port.getInputStream();
		this.reader = new InputStreamReader(stream);
	}

	private int getDataBits(String value) throws NumberFormatException {
		int out = SerialPort.DATABITS_8; 
		if (value == null || value.isEmpty()) {
			
		} else {
			int in = Integer.valueOf(value).intValue();
			switch (in) {
				case 5: out = SerialPort.DATABITS_5; break;
				case 6: out = SerialPort.DATABITS_6; break;
				case 7: out = SerialPort.DATABITS_7; break;
				default: break;
			}			
		}
		return out;
	}

	private int getStopBits(String value) throws NumberFormatException {
		int out = SerialPort.STOPBITS_1; 
		if (value == null || value.isEmpty()) {
			
		} else {
			float in = Float.valueOf(value).floatValue();
			if (in == 1.5) {
				out = SerialPort.STOPBITS_1_5;
			} else if (in == 2.0) {
				out = SerialPort.STOPBITS_2;
			} else {
				
			}
		}
		return out;
	}

	private int getParity(String value) {
		int out = SerialPort.PARITY_NONE;
		if (value == null || value.isEmpty()) {
			
		} else if (value.equalsIgnoreCase("even")) {
			out = SerialPort.PARITY_EVEN;
		} else if (value.equalsIgnoreCase("odd")) {
			out = SerialPort.PARITY_ODD;
		} else if (value.equalsIgnoreCase("mark")) {
			out = SerialPort.PARITY_MARK;
		} else if (value.equalsIgnoreCase("space")) {
			out = SerialPort.PARITY_SPACE;
		} else {
			
		}
		return out;
	}

	private int getFlowControlMode(String value) {
		int out = SerialPort.FLOWCONTROL_NONE;
		if (value == null || value.isEmpty()) {
			
		} else if (value.equalsIgnoreCase("rtscts-in")) {
			out = SerialPort.FLOWCONTROL_RTSCTS_IN;
		} else if (value.equalsIgnoreCase("rtscts-out")) {
			out = SerialPort.FLOWCONTROL_RTSCTS_OUT;
		} else if (value.equalsIgnoreCase("xonxoff-in")) {
			out = SerialPort.FLOWCONTROL_XONXOFF_IN;
		} else if (value.equalsIgnoreCase("xonxoff-out")) {
			out = SerialPort.FLOWCONTROL_XONXOFF_OUT;
		} else {
			
		}
		return out;
	}

	protected int read() throws IOException {
		return this.reader.read();
	}
	
	protected void publish(String topic, String measurement) throws MqttPersistenceException, MqttException {
		MqttMessage message =  new MqttMessage();
		message.setPayload(measurement.getBytes());
		client.publish(topic, message);
	}
   
}
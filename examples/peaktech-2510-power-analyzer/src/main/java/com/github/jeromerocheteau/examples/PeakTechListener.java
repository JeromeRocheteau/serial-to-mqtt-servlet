package com.github.jeromerocheteau.examples;

import javax.servlet.ServletContextListener;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

import com.github.jeromerocheteau.listeners.SerialMqttListener;

public class PeakTechListener extends SerialMqttListener implements Runnable, ServletContextListener {

	private static final String PEAKTECH = "peaktech";
	
    public void run() {
       	int intch = -1;
       	int index = 0;
       	int[] values = new int[13];
    	do {
    		try {
    			intch = super.read();
       			if (intch == '\r') {
       				if (index == 15) {
       					this.process(values);
       				}
       				index = 0;
       			} else if (index == 15) {
       			} else {
       				index++;
       				if (index > 2) {
       					values[index - 3] = intch;
       				}
       			}
    		} catch (Exception e) { }
    	} while (intch != -1);
    }

    private void process(int[] t) throws Exception {
    	switch (t[1]) {
    	case '4': this.power(t); break;
    	case '5': this.other(t); break;
    	default: break;
    	}
	}

	private void other(int[] t) throws Exception {
    	switch (t[2]) {
    	case '0': this.voltage(t); break;
    	case '2': this.intensity(t); break;
    	case '4': this.factor(t); break;
    	default: break;
    	}		
	}

	private void voltage(int[] t) throws Exception {
		double value = this.value(t);
		this.pub("voltage", value);
	}

	private void intensity(int[] t) throws Exception {
		double value = this.value(t);
		this.pub("intensity", value);
	}

	private void factor(int[] t) throws Exception {
		double value = this.value(t);
		this.pub("factor", value);
	}

	private void power(int[] t) throws Exception {
    	switch (t[2]) {
    	case '8': this.power(t, true); break;
    	case '7': this.power(t, false); break;
    	default: break;
    	}		
	}

	private void power(int[] t, boolean k) throws Exception {
		double value = this.value(t) * (k ? 1000 : 1);
		this.pub("power", value);
	}

	private double value(int[] t) {
		double v = 0;
		for (int i = 5; i < 13; i++) {
			int j = Character.getNumericValue(t[i]);
			v = v + (j * Math.pow(10, 12 - i));
		}
		v = v / Math.pow(10, dec(t));
		return (t[3] == '0') ? v : -v;
	}

	private double dec(int[] t) {
		double d = 0;
		switch (t[4]) {
    		case '0': d = 0; break;
    		case '1': d = 1; break;
    		case '2': d = 2; break;
    		case '3': d = 3; break;
    		default: break;
    	}
		return d;
	}

	private void pub(String topic, double value) throws MqttPersistenceException, MqttException {
		String measurement = Double.valueOf(value).toString();
		this.publish(PEAKTECH + "/" + topic, measurement);
	}

}
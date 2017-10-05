package com.github.jeromerocheteau.examples;

import javax.servlet.ServletContextListener;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

import com.github.jeromerocheteau.listeners.SerialMqttListener;

public class ArdgettiListener extends SerialMqttListener implements Runnable, ServletContextListener {

	private static final String ARGETTI = "ardgetti";
	
    public void run() {
       	int intch = -1;
       	int index = 0;
       	StringBuffer buffer = new StringBuffer(16);
    	do {
    		try {
    			intch = this.read();
    			if (intch == '\n') {
        			this.process(index, buffer.toString());
    				buffer.delete(0, buffer.length());
    				index = 0;
    			} else if (intch == '\t') {
        			this.process(index, buffer.toString());
    				buffer.delete(0, buffer.length());
    				index++;
    			} else {
    				buffer.append((char) intch);
    			}
    		} catch (Exception e) { }
    	} while (intch != -1);
    }

    private void process(int index, String value) throws MqttPersistenceException, MqttException {
    	switch (index) {
    	case 0:  break;
    	case 1: this.pub("tension", value); break;
    	case 2: this.pub("1/power", value); break;
    	case 3: this.pub("1/intensity", value); break;
    	case 4: this.pub("1/factor", value); break;
    	case 5: this.pub("2/power", value); break;
    	case 6: this.pub("2/intensity", value); break;
    	case 7: this.pub("2/factor", value); break;
    	case 8: this.pub("3/power", value); break;
    	case 9: this.pub("3/intensity", value); break;
    	case 10: this.pub("3/factor", value); break;
    	default: ; break;
    	}
	}
    
    private void pub(String topic, String measurement) throws MqttPersistenceException, MqttException {
    	this.publish(ARGETTI + "/" + topic, measurement);
    }
  
}
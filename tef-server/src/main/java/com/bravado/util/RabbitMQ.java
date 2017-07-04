package com.bravado.util;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
/*
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
*/
public class RabbitMQ {
	private final static String QUEUE_NAME = "TransitionMQ";
	//private static Connection connection;
	//private static Channel channel;
	private static boolean connected = false;

	public static void Connect() throws IOException, TimeoutException {
		
		if (!connected) {			
			/*
			ConnectionFactory factory = new ConnectionFactory();
			factory.setHost("192.168.25.158");
			factory.setUsername("listoServerQueue");
			factory.setPassword("listoqueue");
			
			connection = factory.newConnection();
			*/
			connected = true;
		}
		
	}
	
	public static void Disconnect() throws IOException, TimeoutException {
		
		if (connected) {
			//connection.close();			
			connected = false;
		}
		
	}

	public static void Send(String message) throws IOException, TimeoutException {
		/*
		try {
			if ((connected) && (connection != null)) {
				channel = connection.createChannel();
				channel.queueDeclare(QUEUE_NAME, false, false, false, null);
				channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
				//System.out.println("----------\n" + message + "\n---------------");
			}
		} catch (Exception e) {
			// TODO: handle exception
			if (connection.isOpen())
				connection.close();			
			connected = false;
		}
		*/
	}
}

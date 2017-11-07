package com.bravado.util;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import br.listofacil.AcquirerSettings;

public class RabbitMQ {
	private final static String QUEUE_NAME = AcquirerSettings.getFila();
	private static Connection connection;
	private static Channel channel;
	private static boolean connected = false;

	public static void Connect() throws IOException, TimeoutException {

		if (!connected) {

			ConnectionFactory factory = new ConnectionFactory();
			factory.setHost(AcquirerSettings.getIp());

			if (!AcquirerSettings.getPort().isEmpty() || AcquirerSettings.getPort() != null) {
				factory.setPort(Integer.parseInt(AcquirerSettings.getPort()));
			}

			connection = factory.newConnection();

			connected = true;
		}

	}

	public static void Disconnect() throws IOException, TimeoutException {

		if (connected) {
			connection.close();
			connected = false;
		}

	}

	public static void Send(String message) throws IOException, TimeoutException {

		try {
			if ((!connected) && (connection == null)) {
				Connect();
			}

			if (connected) {
				channel = connection.createChannel();
				channel.queueDeclare(QUEUE_NAME, false, false, false, null);
				channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
			}

		} catch (Exception e) {
			if (!connection.isOpen() || connection == null) {
				Connect();
			}
		}

	}
}

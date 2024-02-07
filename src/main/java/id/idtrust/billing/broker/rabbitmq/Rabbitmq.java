//package id.idtrust.billing.broker.rabbitmq;
//
//import java.io.UnsupportedEncodingException;
//import java.text.DateFormat;
//import java.text.SimpleDateFormat;
//
//import com.pengrad.telegrambot.TelegramBot;
//import com.pengrad.telegrambot.request.SendMessage;
//
//import id.idtrust.billing.util.LogSystem;
//import com.rabbitmq.client.AMQP;
//import com.rabbitmq.client.Channel;
//import com.rabbitmq.client.ConnectionFactory;
//import com.rabbitmq.client.Connection;
//
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.concurrent.TimeoutException;
//
//import static id.idtrust.billing.util.Description.*;
//
//public class Rabbitmq
//{
//
//	private static final String EXCHANGE_NAME = "payment_void";
//	private static final String QUEUE_NAME = "billtrust";
//	private static final String ROUTING_KEY = "billtrust-key";
//
//	public Boolean send(String message, String uuid, String created_date, String expired_at) throws Exception
//	{
//		try {
//				DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSSSSS");
//				LogSystem.info("Send to rabbitmq");
//				//Send to message broker
//				ConnectionFactory factory = new ConnectionFactory();
//		        factory.setHost(RABBITMQ_URL);
//		        factory.setUsername(RABBITMQ_USER);
//		        factory.setPassword(RABBITMQ_PASSWORD);
//		        factory.setPort(Integer.parseInt(RABBITMQ_PORT));
//
//		        LogSystem.info("URL " + factory.getHost() + " Exchange Name " + EXCHANGE_NAME + " Message " + message);
//
//		        Map<String, Object> args = new HashMap<String, Object>();
//		        args.put("x-delayed-type", "direct");
//
//		        try (Connection connection = factory.newConnection();
//		             Channel channel = connection.createChannel()) {
//		            channel.exchangeDeclare(EXCHANGE_NAME, "x-delayed-message", true, false, args);
//		            channel.queueDeclare(QUEUE_NAME, true, false, false, args);
//		            channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, ROUTING_KEY, args);
//
//		            Map<String, Object> headers2 = new HashMap<String, Object>();
//		            headers2.put("x-delay", 3600000);
////		            headers2.put("x-delay", 5000);
//		            AMQP.BasicProperties.Builder props = new AMQP.BasicProperties.Builder().deliveryMode(2).headers(headers2);
//
//		            channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY, props.build(), message.getBytes("UTF-8"));
//		            LogSystem.info("RabbitMQ [x] Sent '" + message + " at " + created_date + " will execute at " + expired_at +"'");
//
//		        } catch (UnsupportedEncodingException e) {
//		            throw new RuntimeException(e);
//		        } catch (IOException | TimeoutException e) {
//		            throw new RuntimeException(e);
//		        }
//		        LogSystem.info("Success rabbitmq");
//				return true;
//		}catch(Exception e)
//		{
//			LogSystem.info("Failed process to RabbitMQ, notify admin");
//			try
//			{
//				TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
//				bot.execute(new SendMessage(213382980,  "Failed process to RabbitMQ  " + "\n Message : " + e));
//			}catch(Exception t)
//			{
//				LogSystem.error("Failed send message telegram");
//				LogSystem.error(t.toString());
//			}
//			e.printStackTrace();
//			throw new Exception(e.toString());
//		}
//	}
//
//}

package id.idtrust.billing.broker.kafka;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import id.idtrust.billing.util.LogSystem;
import io.confluent.kafka.serializers.KafkaJsonSerializer;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.concurrent.Future;

import static id.idtrust.billing.util.Description.KAFKA_SCHEMA_URL;
import static id.idtrust.billing.util.Description.KAFKA_URL;

@Component
public class Send {

//    private static final Logger logger = LogManager.getLogger();

//	public Boolean sendPayment(Payment message, String topic, String uuid) throws Exception
//    {
//        //as JSON
//        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
//        String json = ow.writeValueAsString(message);
//
//        logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO ] : " + json);
//
//        Subsegment subSegment=null;
//
//        subSegment = AWSXRay.beginSubsegment("## Kafka " + topic);
//
//        try {
//            //Send to message broker
//            String url = "localhost:9092";
//            if(KAFKA_URL != null)
//            {
//                url = KAFKA_URL;
//            }
//
//            logger.info("["+ds.VERSION+"]-[BILLTRUST/INFO] : " + " [RABBITMQ-CONSUMER] URL : " + url + " Topic : " + topic + " Message : " + message);
//
//            Properties props = new Properties();
//            props.put("bootstrap.servers", url);
//            props.put("linger.ms", 1);
//            props.put("metadata.fetch.timeout.ms", 10000);
//            props.put("max.block.ms", 10000);
//            props.put("group.id", "notification-email");
//            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
//            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaJsonSerializer.class.getName());
//            Producer<String, Payment> producer = new KafkaProducer<String, Payment>(props);
//            ProducerRecord<String, Payment> record = new ProducerRecord<String, Payment>(topic, uuid, message);
//
//            Future<RecordMetadata> resp = producer.send(record);
//
//            LogSystem.info(resp.get().toString());
//
//            producer.close();
//            LogSystem.info("Success kafka");
//            return true;
//        }catch(Exception e)
//        {
//            subSegment.addException(e);
//
//        	try
//			{
//				TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
//				bot.execute(new SendMessage(213382980,  "Failed process to Kafka send data payment  " + "\n Message : " + e));
//			}catch(Exception t)
//			{
//				LogSystem.error(t.toString());
//			}
//            e.printStackTrace();
//            throw new Exception(e.toString());
//        }
//        finally {
//            subSegment.end();
//        }
//    }
//
//
//    public Boolean sendPayment(Payment message, String topic, String uuid, HttpServletRequest request) throws Exception
//    {
//        try {
//            //Send to message broker
//            String url = "localhost:9092";
//            String schemaurl = "localhost:8081";
//
//            if(KAFKA_URL != null)
//            {
//                url = KAFKA_URL;
//            }
//
//            if(KAFKA_SCHEMA_URL != null)
//            {
//                schemaurl = KAFKA_SCHEMA_URL;
//            }
//
//            LogSystem.info("URL : " + url + " Topic : " + topic + " Message : " + message);
//
//            Properties props = new Properties();
//            props.put("bootstrap.servers", url);
//            props.put("linger.ms", 1);
//            props.put("metadata.fetch.timeout.ms", 10000);
//            props.put("max.block.ms", 10000);
//            props.put("group.id", "notification-email");
//            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
//            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaJsonSerializer.class.getName());
////            props.put("schema.registry.url", schemaurl);
//            Producer<String, Payment> producer = new KafkaProducer<String, Payment>(props);
//            ProducerRecord<String, Payment> record = new ProducerRecord<String, Payment>(topic, uuid, message);
//
//            Future<RecordMetadata> resp = producer.send(record);
//
////            System.out.print(resp.get());
//
//            LogSystem.info(resp.get().toString());
//
//            producer.close();
//            LogSystem.info("Success kafka");
//            return true;
//        }catch(Exception e)
//        {
//        	try
//			{
//				TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
//				bot.execute(new SendMessage(213382980,  "Failed process to Kafka send data payment  " + "\n Message : " + e));
//			}catch(Exception t)
//			{
//				LogSystem.error("Failed send message telegram");
//				LogSystem.error(t.toString());
//			}
//            e.printStackTrace();
//            throw new Exception(e.toString());
//        }
//    }

//    public Boolean sendNotification(NotificationAlert message, String topic, String uuid) throws Exception
//    {
//
//        try {
//            String url = "localhost:9092";
//            String schemaurl = "localhost:8081";
//
//            if(KAFKA_URL != null)
//            {
//                url = KAFKA_URL;
//            }
//
//            if(KAFKA_SCHEMA_URL != null)
//            {
//                schemaurl = KAFKA_SCHEMA_URL;
//            }
//
//            LogSystem.info("URL : " + url + " Topic : " + topic + " Message : " + message);
//
//            Properties props = new Properties();
//            props.put("bootstrap.servers", url);
//            props.put("linger.ms", 1);
//            props.put("metadata.fetch.timeout.ms", 10000);
//            props.put("max.block.ms", 10000);
//            props.put("group.id", "notification-email");
//            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
//            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaJsonSerializer.class.getName());
////            props.put("schema.registry.url", schemaurl);
//
//            Producer<String, NotificationAlert> producer = new KafkaProducer<String, NotificationAlert>(props);
//            ProducerRecord<String, NotificationAlert> record = new ProducerRecord<String, NotificationAlert>(topic, uuid, message);
//
//            Future<RecordMetadata> resp = producer.send(record);
//
//            LogSystem.info(resp.get().toString());
//
//            producer.close();
//            LogSystem.info("Success kafka");
//
//
//            return true;
//        }catch(Exception e)
//        {
//
//        	try
//			{
//				TelegramBot bot=new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
//				bot.execute(new SendMessage(213382980,  "Failed process to Kafka send notification alert balance  " + "\n Message : " + e));
//			}catch(Exception t)
//			{
//				LogSystem.error("Failed send message telegram");
//				LogSystem.error(t.toString());
//			}
//            e.printStackTrace();
//            throw new Exception(e.toString());
//        }
//    }
}

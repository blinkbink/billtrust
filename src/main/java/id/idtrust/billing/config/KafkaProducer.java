package id.idtrust.billing.config;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import id.idtrust.billing.broker.kafka.NotificationAlert;
import id.idtrust.billing.util.Description;
import io.confluent.kafka.serializers.KafkaJsonSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.ProducerListener;

import java.util.HashMap;
import java.util.Map;

import static id.idtrust.billing.util.Description.KAFKA_URL;

@EnableAutoConfiguration
@Configuration
@EnableKafka
class KafkaProducer {
    private static final Logger logger = LogManager.getLogger();
    @Bean
    public ProducerFactory<String, id.idtrust.billing.broker.kafka.Payment> producerFactory() {

        String url = "localhost:9092";

        if(KAFKA_URL != null)
        {
            url = KAFKA_URL;
        }

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                url);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                KafkaJsonSerializer.class.getName());
        props.put("group.id", "notification-email");

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public ProducerFactory<String, NotificationAlert> producerFactoryNotification() {

        String url = "localhost:9092";

        if(KAFKA_URL != null)
        {
            url = KAFKA_URL;
        }

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                url);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                KafkaJsonSerializer.class.getName());
        props.put("group.id", "notification-email");

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, id.idtrust.billing.broker.kafka.Payment> kafkaPayment() {
        Description ds = new Description();
        KafkaTemplate<String, id.idtrust.billing.broker.kafka.Payment> kafkaTemplate =
                new KafkaTemplate<>(producerFactory());

        kafkaTemplate.setProducerListener(new ProducerListener<String, id.idtrust.billing.broker.kafka.Payment>() {
            @Override
            public void onSuccess(
                    ProducerRecord<String, id.idtrust.billing.broker.kafka.Payment> producerRecord,
                    RecordMetadata recordMetadata) {
                logger.info("["+ds.VERSION+"]-[BILLTRUST/KAFKA] : ACK from ProducerListener message " + producerRecord.value() + " : " + recordMetadata.offset());

            }

            @Override
            public void onError(ProducerRecord<String, id.idtrust.billing.broker.kafka.Payment> producerRecord,
                                RecordMetadata recordMetadata, Exception exception) {
                logger.info("[" + ds.VERSION + "]-[BILLTRUST/KAFKA] : Failed " + producerRecord.value() + " : " + exception.toString());
                try {
                    TelegramBot bot = new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
                    bot.execute(new SendMessage(213382980, "Failed send at callback process topic " + producerRecord.topic()));
                } catch (Exception t) {
                    logger.info("[" + ds.VERSION + "]-[BILLTRUST/KAFKAPRODUCER] : Failed " + t);
                }
            }

        });
        return kafkaTemplate;
    }

    @Bean
    public KafkaTemplate<String, NotificationAlert> kafkaNotification() {
        Description ds = new Description();
        KafkaTemplate<String, NotificationAlert> kafkaTemplate =
                new KafkaTemplate<>(producerFactoryNotification());
        kafkaTemplate.setProducerListener(new ProducerListener<String, NotificationAlert>() {
            @Override
            public void onSuccess(
                    ProducerRecord<String, NotificationAlert> producerRecord,
                    RecordMetadata recordMetadata) {
                logger.info("["+ds.VERSION+"]-[BILLTRUST/KAFKA] : ProducerListener message " + producerRecord.value() + " : " + recordMetadata.offset());

            }

            @Override
            public void onError(ProducerRecord<String, NotificationAlert> producerRecord,
                                RecordMetadata recordMetadata, Exception exception) {
                logger.info("[" + ds.VERSION + "]-[BILLTRUST/KAFKA] : Failed " + producerRecord.value() + " : " + exception.toString());
                try {
						TelegramBot bot = new TelegramBot("912934463:AAGOhuRQyFtd5huj0mqsOjkdR8IARrdREYE");
						bot.execute(new SendMessage(213382980, "Failed send at callback process topic " + producerRecord.topic()));
					} catch (Exception t) {
                    logger.info("[" + ds.VERSION + "]-[BILLTRUST/KAFKAPRODUCER] : Failed " + t.toString());
					}
            }

        });
        return kafkaTemplate;
    }
}
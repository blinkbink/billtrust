package id.idtrust.billing.config;


import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

import static id.idtrust.billing.util.Description.KAFKA_SCHEMA_URL;
import static id.idtrust.billing.util.Description.KAFKA_URL;

@EnableKafka
@Configuration
public class KafkaConsumer {

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        String url = "localhost:9092";
        String schemaurl = "localhost:8081";

        if(KAFKA_URL != null)
        {
            url = KAFKA_URL;
        }

        if(KAFKA_SCHEMA_URL != null)
        {
            schemaurl = KAFKA_SCHEMA_URL;
        }

        Map<String, Object> props = new HashMap<>();
        props.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                url);
        props.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "billtrust");
        props.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        props.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        props.put("schema.registry.url", schemaurl);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String>
    kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
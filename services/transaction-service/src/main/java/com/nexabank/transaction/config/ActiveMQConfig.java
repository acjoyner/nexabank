package com.nexabank.transaction.config;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

/**
 * ActiveMQ (JMS) Configuration.
 *
 * JmsTemplate is the Spring abstraction for JMS messaging — analogous to
 * KafkaTemplate for Kafka. It handles connection pooling, serialization,
 * and exception translation.
 *
 * MappingJackson2MessageConverter serializes Java objects to JSON JMS messages,
 * with a TypeId header so the consumer can deserialize to the correct class.
 *
 * See docs/learning/04-activemq-mq-messaging.md
 */
@Configuration
public class ActiveMQConfig {

    @Value("${spring.activemq.broker-url:tcp://localhost:61616}")
    private String brokerUrl;

    @Value("${spring.activemq.user:nexabank}")
    private String brokerUser;

    @Value("${spring.activemq.password:nexabank123}")
    private String brokerPassword;

    @Bean
    public ActiveMQConnectionFactory connectionFactory() {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
        factory.setUserName(brokerUser);
        factory.setPassword(brokerPassword);
        return factory;
    }

    @Bean
    public MessageConverter jacksonJmsMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }

    @Bean
    public JmsTemplate jmsTemplate() {
        JmsTemplate template = new JmsTemplate(connectionFactory());
        template.setMessageConverter(jacksonJmsMessageConverter());
        return template;
    }
}

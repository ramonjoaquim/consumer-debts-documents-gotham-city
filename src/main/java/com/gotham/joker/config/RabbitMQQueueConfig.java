package com.gotham.joker.config;

import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class RabbitMQQueueConfig {

    private final RabbitMQProperties rabbitMQProperties;

    public RabbitMQQueueConfig(RabbitMQProperties rabbitMQProperties) {
        this.rabbitMQProperties = rabbitMQProperties;
    }

    @Bean
    public List<Queue> dynamicQueues() {
        List<Queue> queues = new ArrayList<>();
        for (String queueName : rabbitMQProperties.getQueues()) {
            queues.add(new Queue(queueName, true));  // true significa que as filas são duráveis
        }
        return queues;
    }

    @Bean
    public Declarables declareDynamicQueues() {
        List<Declarable> declarables = new ArrayList<>();
        for (Queue queue : dynamicQueues()) {
            declarables.add(queue);
        }
        return new Declarables(declarables);
    }
}
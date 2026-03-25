package com.flightapp.bookingservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean public NewTopic paymentRequested() {
        return TopicBuilder.name("payment.requested").partitions(3).replicas(1).build();
    }
    @Bean public NewTopic paymentSuccess() {
        return TopicBuilder.name("payment.success").partitions(3).replicas(1).build();
    }
    @Bean public NewTopic paymentFailed() {
        return TopicBuilder.name("payment.failed").partitions(3).replicas(1).build();
    }
    @Bean public NewTopic bookingConfirmed() {
        return TopicBuilder.name("booking.confirmed").partitions(3).replicas(1).build();
    }
    @Bean public NewTopic bookingFailed() {
        return TopicBuilder.name("booking.failed").partitions(3).replicas(1).build();
    }

    @Bean public NewTopic bookingCancelled() {
        return TopicBuilder.name("booking.cancelled").partitions(3).replicas(1).build();
    }

    @Bean public NewTopic paymentRefundRequested() {
        return TopicBuilder.name("payment.refund.requested").partitions(3).replicas(1).build();
    }
}

package cl.duoc.dsy1107.pedidos360.messaging.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

@Configuration
public class RabbitMessageConfig {

    @Bean
    public MessageConverter rabbitMessageConverter(@NonNull ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
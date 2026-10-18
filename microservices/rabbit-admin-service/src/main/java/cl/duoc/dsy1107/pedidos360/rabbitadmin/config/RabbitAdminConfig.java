package cl.duoc.dsy1107.pedidos360.rabbitadmin.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

@Configuration
public class RabbitAdminConfig {

    @Bean
    public RabbitAdmin rabbitAdmin(
            @NonNull ConnectionFactory connectionFactory) {

        RabbitAdmin rabbitAdmin =
                new RabbitAdmin(connectionFactory);

        rabbitAdmin.setAutoStartup(true);

        return rabbitAdmin;
    }
}
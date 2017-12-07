package com.schibsted.ping;

import feign.Client;
import feign.Feign;
import feign.RequestLine;
import feign.codec.Decoder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClientsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

@RestController
class HelloController {

    private final PongService service;

    @Inject
    public HelloController(PongService service) {
        this.service = service;
    }

    @RequestMapping("/")
    public ResponseEntity<String> index() {
        try {
            return new ResponseEntity<>(service.hello(), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}

@Component
class PongService {

    private final PongClient client;

    @Inject
    public PongService(PongClient client) {
        this.client = client;
    }

    String hello() {
        return client.hello().getBody();
    }
}

interface PongClient {
    @RequestLine("GET /")
    ResponseEntity<String> hello();
}

@Configuration
@Import(FeignClientsConfiguration.class)
class FeignConfiguration {

    @Bean
    public PongClient defaultFeignBuilder(Client defaultClient, Decoder decoder) {
        return Feign.builder()
                .client(defaultClient)
                .decoder(decoder)
                .target(PongClient.class, "http://pong");
    }
}

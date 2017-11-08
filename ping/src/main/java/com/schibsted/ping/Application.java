package com.schibsted.ping;

import feign.Client;
import feign.RequestLine;
import feign.hystrix.HystrixFeign;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableHystrix
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

interface PongClient {
    @RequestLine("GET /")
    String hello();
}

@Component
class PongService {

    private PongClient client;

    @Inject
    public PongService(PongClient client) {
        this.client = client;
    }

    String hello() {
        return client.hello();
    }
}

@Configuration
class FeignConfiguration {

    @Bean
    public PongClient defaultFeignBuilder(Client defaultClient) {
        PongClient fallback = () -> "Some fallback from ping";
        return HystrixFeign.builder()
                .client(defaultClient)
                .target(PongClient.class, "http://pong", fallback);
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
    public String index() {
        return service.hello();
    }
}

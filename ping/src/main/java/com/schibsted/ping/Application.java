package com.schibsted.ping;

import feign.Client;
import feign.RequestLine;
import feign.codec.Decoder;
import feign.hystrix.HystrixFeign;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClientsConfiguration;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

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
    ResponseEntity<String> hello();
}

@Component
class PongService {

    private final PongClient client;
    private final MeterRegistry meterRegistry;
    private final Counter counter;
    private final Timer timer;

    @Inject
    public PongService(PongClient client, MeterRegistry meterRegistry) {
        this.client = client;
        this.meterRegistry = meterRegistry;
        this.timer = Timer.builder("timer.http.clientes.requests")
                .tags("uri", "/hello", "method", "GET", "status", "200")
                .publishPercentiles(0.5, 0.95)
                .register(meterRegistry);
        this.counter = Counter.builder("http.clientes.requests")
                .tags("uri", "/hello", "method", "GET", "status", "200")
                .register(meterRegistry);
    }

    String hello() {
        long startTime = System.nanoTime();
        ResponseEntity<String> response = client.hello();
        counter.increment();
        timer.record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        return response.getBody();
    }
}

@Configuration
@Import(FeignClientsConfiguration.class)
class FeignConfiguration {

    @Bean
    public PongClient defaultFeignBuilder(Client defaultClient, Decoder decoder) {
        PongClient fallback = () -> ResponseEntity.ok("Some fallback from ping");
        return HystrixFeign.builder()
                .client(defaultClient)
                .decoder(decoder)
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

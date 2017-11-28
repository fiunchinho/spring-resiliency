package com.schibsted.ping;

import com.netflix.hystrix.strategy.HystrixPlugins;
import feign.Client;
import feign.RequestLine;
import feign.codec.Decoder;
import feign.hystrix.HystrixFeign;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.hystrix.MicrometerMetricsPublisher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClientsConfiguration;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.netflix.hystrix.dashboard.EnableHystrixDashboard;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
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
    ResponseEntity<String> hello();
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

@Configuration
@Import(FeignClientsConfiguration.class)
class FeignConfiguration {

    @PostConstruct
    public void hystrixMetrics() {
        HystrixPlugins.getInstance().registerMetricsPublisher(new MicrometerMetricsPublisher(Metrics.globalRegistry));
    }

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

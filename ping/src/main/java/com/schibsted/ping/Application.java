package com.schibsted.ping;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableHystrix
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

@FeignClient("pong")
interface PongClient {
    @RequestMapping(value = "/", method = GET)
    String hello();
}

@Component
class PongService {

    @Inject
    PongClient client;

    @HystrixCommand(fallbackMethod = "fallback")
    public String hello() {
        return client.hello();
    }

    public String fallback() {
        return "Some fallback from ping";
    }
}

@RestController
class HelloController {

    @Inject
    PongService service;

    @RequestMapping("/")
    public String index() {
        return service.hello();
    }
}

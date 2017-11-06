package com.schibsted.pong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication
@EnableDiscoveryClient
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

@RestController
class HelloController {

    private static final Logger LOGGER = LoggerFactory.getLogger(HelloController.class);

    private boolean toggleError = false;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ResponseEntity<String> index() throws InterruptedException, UnknownHostException {
        if (toggleError) {
            Thread.sleep(2000);
            LOGGER.info("Replying with error");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        LOGGER.info("Replying success");
        return new ResponseEntity<>("Pong! from " + InetAddress.getLocalHost().getHostName(), HttpStatus.OK);
    }

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public String toggle() {
        toggleError = !toggleError;
        return "Switched error mode to " + toggleError;
    }
}

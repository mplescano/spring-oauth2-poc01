package com.mplescano.oauth.poc.poc01;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

@SpringBootApplication
@EnableZuulProxy
public class Poc01ClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(Poc01ClientApplication.class, args);
    }
}
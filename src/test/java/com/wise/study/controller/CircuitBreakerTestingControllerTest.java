package com.wise.study.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.time.Duration;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CircuitBreakerTestingControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    Random random = new Random();

    @Test
    void testCB() throws InterruptedException {
        int numberOfRequest = 5000;
        double successRate = 1;

        while (numberOfRequest-- > 0) {
            Thread.sleep( random.nextInt(10) + 10);
            successRate = alterSuccessRatio(successRate);
            boolean isSuccess = random.nextDouble() < successRate;
            // Call the REST API
            String url = "/toggle?flag=" + isSuccess;
            String result = restTemplate.getForObject(url, String.class);

//            if(random.nextDouble() < 0.1)
//                log.info("test {} : result {}", numberOfRequest, result);
        }
        String url = "/count";
        String result = restTemplate.getForObject(url, String.class);
        log.info("test {} : result {}", numberOfRequest, result);
    }

    private double alterSuccessRatio(double s) {
        if (random.nextDouble() > 0.007)
            return s;

        double res = random.nextDouble();
        log.info("Altering successRate to {}", res);
        return res;
    }
}

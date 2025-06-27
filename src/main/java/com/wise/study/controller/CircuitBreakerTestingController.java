package com.wise.study.controller;

import com.wise.study.basiccircuitbreaker.CBConfig;
import com.wise.study.basiccircuitbreaker.CircuitBreaker;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CircuitBreakerTestingController {

    private final String dummyCBName = "dummyCB";
    private final CircuitBreaker toggler = CircuitBreaker.get(dummyCBName, CBConfig.defaultConfig());
    @GetMapping("/toggle")
    public String toggleFlag(@RequestParam Boolean flag) {
        if(toggler.isClosed()) {
            toggler.appendRequest(flag);
            return "Flag is" + flag;
        } else {
            return "toggler state" + toggler.getCbState().name();
        }

    }
    @GetMapping("/count")
    public int[] count() {
        return toggler.getRequestCount();

    }
}


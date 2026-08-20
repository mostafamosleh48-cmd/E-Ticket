package com.mostafa.eticket.service;

import org.springframework.stereotype.Service;

@Service
public class AspectTestService {

    public String doWork(String input) {
        return "result:" + input;
    }

    public void failWork() {
        throw new IllegalStateException("boom");
    }
}
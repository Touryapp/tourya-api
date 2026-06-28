package com.tourya.api.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/v1")
@Tag(name = "Test")
public class TestController {

    @GetMapping("/connectionTest")
    public Map<String, String> connectionTourya(){
        HashMap<String, String> stringHashMap = new HashMap<>();
        stringHashMap.put("connection", "Successful connection Api Tourya con Actions");
        return stringHashMap;
    }
}

package com.imooc.diners.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description:
 * @author:LWQ
 * @version:1.0
 */
@RestController
@RequestMapping("/hello")
public class HelloController {
    @RequestMapping("/test")
    public String hello(String name){
        return name + " redis";
    }
}

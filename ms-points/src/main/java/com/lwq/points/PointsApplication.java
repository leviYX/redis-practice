package com.lwq.points;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.lwq.points.mapper")
@SpringBootApplication
public class PointsApplication {
    public static void main(String[] args) {
        SpringApplication.run(PointsApplication.class, args);
    }
}

package com.kbsystem.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 后端应用启动类。
 * 负责启动 Spring Boot 并扫描 MyBatis Mapper。
 */
@SpringBootApplication
@MapperScan("com.kbsystem.backend.**.mapper")
@EnableAsync
public class KbBackendApplication {

    /**
     * 程序主入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(KbBackendApplication.class, args);
    }
}


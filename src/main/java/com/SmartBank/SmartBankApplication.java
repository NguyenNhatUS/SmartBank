package com.SmartBank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SmartBankApplication {
	public static void main(String[] args) {
		SpringApplication.run(SmartBankApplication.class, args);
	}

}

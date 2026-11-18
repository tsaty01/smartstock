package com.smartstock;

import org.springframework.boot.SpringApplication;

public class TestSmartstockServerApplication {

	public static void main(String[] args) {
		SpringApplication.from(SmartstockServerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

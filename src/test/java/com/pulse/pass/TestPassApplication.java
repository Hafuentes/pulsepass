package com.pulse.pass;

import org.springframework.boot.SpringApplication;

public class TestPassApplication {

	public static void main(String[] args) {
		SpringApplication.from(PassApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

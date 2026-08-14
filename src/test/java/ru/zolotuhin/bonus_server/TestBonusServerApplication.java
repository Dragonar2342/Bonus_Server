package ru.zolotuhin.bonus_server;

import org.springframework.boot.SpringApplication;

public class TestBonusServerApplication {

	public static void main(String[] args) {
		SpringApplication.from(BonusServerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

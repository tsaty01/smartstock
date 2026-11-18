package com.smartstock.common;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SmartstockProperties.class)
class AppConfig {

	/** Store clock: business dates such as "today" and expiry use the configured zone, not the server's. */
	@Bean
	Clock clock(SmartstockProperties properties) {
		return Clock.system(properties.zone());
	}

}

package com.smartstock;

import java.io.IOException;
import java.io.UncheckedIOException;

import javax.sql.DataSource;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/** One PostgreSQL server per test JVM, shared by every cached application context. */
@TestConfiguration(proxyBeanMethods = false)
class EmbeddedPostgresConfiguration {

	private static final EmbeddedPostgres POSTGRES = start();

	@Bean
	DataSource dataSource() {
		return POSTGRES.getPostgresDatabase();
	}

	private static EmbeddedPostgres start() {
		try {
			return EmbeddedPostgres.start();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}

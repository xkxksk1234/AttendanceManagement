package com.maemong.attendance.db;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

public class MigrationRunner {
	public static void migrate(DataSource ds) {
		Flyway.configure()
				.dataSource(ds)
				.locations("classpath:db/migration")
				.baselineOnMigrate(true)
				.load()
				.migrate();
	}
}
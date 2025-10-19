package com.maemong.attendance.db;

import org.jdbi.v3.core.Jdbi;
import javax.sql.DataSource;

public class JdbiProvider {
	public static Jdbi create(DataSource ds) {
		return Jdbi.create(ds);
	}
}
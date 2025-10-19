package com.maemong.attendance.db;

import com.maemong.attendance.config.AppConfig;


import javax.sql.DataSource;
import org.sqlite.SQLiteDataSource;
import java.nio.file.*;

public class DataSourceFactory {
	public static DataSource create(AppConfig config) {
		try { Files.createDirectories(config.dataDir()); } catch (Exception ignored) {}
		SQLiteDataSource ds = new SQLiteDataSource();
		ds.setUrl("jdbc:sqlite:" + config.dbPath());
		return ds;
	}
}
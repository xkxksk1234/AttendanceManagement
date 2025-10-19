package com.maemong.attendance.config;

import java.nio.file.*;

public class AppConfig {
	private final Path dataDir;
	private final Path dbPath;


	public AppConfig() {
		String home = System.getProperty("user.home");
		this.dataDir = Paths.get(home, ".maemong", "attendance");
		this.dbPath = dataDir.resolve("attendance.db");
	}


	public Path dataDir() { return dataDir; }
	public Path dbPath() { return dbPath; }
}
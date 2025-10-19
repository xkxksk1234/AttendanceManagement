package com.maemong.attendance.bootstrap;

import com.maemong.attendance.config.AppConfig;

public class ConfigLoader {
	public static AppConfig load() {
		// 간단 버전: application.conf 의 경로를 계산만 하고 AppConfig 에서 처리
		return new AppConfig();
	}
}
package com.maemong.attendance.util;

import java.time.format.DateTimeFormatter;

@SuppressWarnings("unused")
public class DateTimeUtil {
	public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
}
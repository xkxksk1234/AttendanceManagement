package com.maemong.attendance.domain;

import java.time.*;

public record AttendanceRecord(
		Long id,
		Long employeeId,
		LocalDate workDate,
		LocalTime clockIn,
		LocalTime clockOut,
		String memo
) {}
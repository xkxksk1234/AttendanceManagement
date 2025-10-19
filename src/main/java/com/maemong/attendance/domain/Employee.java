package com.maemong.attendance.domain;

import com.maemong.attendance.domain.enums.Rank;

import java.time.*;

public record Employee(
		Long id,
		String name,
		Rank rank,
		String rrn,
		String phone,
		Integer hourlyWage,
		String bank,
		String account,
		String address,
		LocalDate contractDate,
		String note
) {}
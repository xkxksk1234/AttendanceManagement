package com.maemong.attendance.services.attendance.commands;

import java.time.LocalDate;
import java.time.LocalTime;

/** 근태 저장을 위한 커맨드 DTO (신규/수정 공용) */
public record SaveAttendanceCommand(
        Long id, // null이면 신규
        long employeeId,
        LocalDate workDate,
        LocalTime clockIn,
        LocalTime clockOut,
        String memo
) {}

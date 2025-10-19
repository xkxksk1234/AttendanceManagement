package com.maemong.attendance.services.attendance.queries;

import com.maemong.attendance.domain.AttendanceRecord;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public interface AttendanceQueries {
    List<AttendanceRecord> byDate(LocalDate d);
    List<AttendanceRecord> byMonth(YearMonth ym);
    List<AttendanceRecord> byEmpRange(long empId, LocalDate from, LocalDate to);

    /** 하루 단위 조회(편의 헬퍼) */
    default List<AttendanceRecord> byEmpDate(long empId, LocalDate date) {
        return byEmpRange(empId, date, date);
    }
}

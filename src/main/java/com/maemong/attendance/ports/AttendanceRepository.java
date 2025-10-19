package com.maemong.attendance.ports;

import com.maemong.attendance.domain.AttendanceRecord;
import java.time.*;
import java.util.*;

public interface AttendanceRepository {
	AttendanceRecord save(AttendanceRecord r);
	Optional<AttendanceRecord> findById(long id);
	List<AttendanceRecord> findByDate(LocalDate date);
	List<AttendanceRecord> findByMonth(YearMonth ym);
	List<AttendanceRecord> findByEmployeeAndRange(long employeeId, LocalDate from, LocalDate to);
	boolean deleteById(long id);
}
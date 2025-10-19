// package 그대로 유지
package com.maemong.attendance.services;

import com.maemong.attendance.domain.AttendanceRecord;
import com.maemong.attendance.ports.AttendanceRepository;
import com.maemong.attendance.ports.EmployeeRepository;
import com.maemong.attendance.services.attendance.AttendanceAppService;
import com.maemong.attendance.services.attendance.commands.SaveAttendanceCommand;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * 호환용 포워더 서비스.
 * 내부적으로 AttendanceAppService에 위임하여
 * 기존 호출부를 깨지 않으면서 유스케이스 계층을 표준화한다.
 */
public class AttendanceService {
    private final AttendanceAppService app;

    /** 기존 생성자 유지(엄격 모드 기본값 true) */
    public AttendanceService(AttendanceRepository repo, EmployeeRepository employees) {
        this(repo, employees, true);
    }

    /** 플래그 제어 생성자 */
    public AttendanceService(AttendanceRepository repo, EmployeeRepository employees, boolean enforceNoOverlap) {
        this.app = new AttendanceAppService(repo, employees, enforceNoOverlap);
    }

    // === 기존 API 시그니처는 그대로 유지 ===
    public AttendanceRecord upsert(AttendanceRecord r) {
        if (r == null) throw new IllegalArgumentException("근태 기록이 제공되지 않았습니다.");
        // 기존 DTO → 커맨드로 변환하여 위임
        return app.save(new SaveAttendanceCommand(
                r.id(),
                r.employeeId(),
                r.workDate(),
                r.clockIn(),
                r.clockOut(),
                r.memo()
        ));
    }

    public Optional<AttendanceRecord> find(long id) { return app.find(id); }
    public List<AttendanceRecord> byDate(LocalDate d) { return app.byDate(d); }
    public List<AttendanceRecord> byMonth(YearMonth ym) { return app.byMonth(ym); }
    public List<AttendanceRecord> byEmpRange(long empId, LocalDate from, LocalDate to) { return app.byEmpRange(empId, from, to); }
    public List<AttendanceRecord> byEmpDate(long empId, LocalDate date) { return app.byEmpDate(empId, date); }
    public boolean remove(long id) { return app.remove(id); }
}

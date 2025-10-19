// package 그대로 유지
package com.maemong.attendance.services;

import com.maemong.attendance.domain.AttendanceRecord;
import com.maemong.attendance.ports.AttendanceRepository;
import com.maemong.attendance.ports.EmployeeRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public class AttendanceService {
    private final AttendanceRepository repo;
    private final EmployeeRepository employees;

    /** ✅ 추가: 겹침 금지 여부(기본 true = 기존과 동일하게 엄격) */
    private final boolean enforceNoOverlap;

    // 기존 생성자 유지(호환성)
    public AttendanceService(AttendanceRepository repo, EmployeeRepository employees) {
        this(repo, employees, true);
    }

    // ✅ 추가 생성자: 플래그 제어
    public AttendanceService(AttendanceRepository repo, EmployeeRepository employees, boolean enforceNoOverlap) {
        this.repo = repo;
        this.employees = employees;
        this.enforceNoOverlap = enforceNoOverlap;
    }

    public AttendanceRecord upsert(AttendanceRecord r) {
        if (r == null) throw new IllegalArgumentException("근태 기록이 제공되지 않았습니다.");
        if (r.employeeId() == 0) throw new IllegalArgumentException("사번이 올바르지 않습니다.");

        if (employees.findById(r.employeeId()).isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 사번입니다: " + r.employeeId());
        }

        if (r.clockIn() != null && r.clockOut() != null) {
            if (r.clockOut().equals(r.clockIn())) {
                throw new IllegalArgumentException("출근·퇴근 시간이 동일할 수 없습니다.");
            }
        }

        // ✅ 변경: 겹침 검사는 'enforceNoOverlap'가 true일 때만 차단
        if (r.clockIn() != null && r.clockOut() != null && enforceNoOverlap) {
            LocalDate from = r.workDate().minusDays(1);
            LocalDate to = r.workDate().plusDays(1);
            List<AttendanceRecord> around = repo.findByEmployeeAndRange(r.employeeId(), from, to);

            for (AttendanceRecord ex : around) {
                if (r.id() != null && r.id().equals(ex.id())) continue;
                if (ex.clockIn() == null || ex.clockOut() == null) continue;

                if (isOverlapped(r, ex)) {
                    throw new IllegalArgumentException(
                            "다른 근무 기록과 시간이 겹칩니다. (기존: "
                                    + ex.workDate() + " " + ex.clockIn() + "~" + ex.clockOut() + ")"
                    );
                }
            }
        }

        return repo.save(r);
    }

    private static boolean isOverlapped(AttendanceRecord a, AttendanceRecord b) {
        Interval ia = toInterval(a.workDate(), a.clockIn(), a.clockOut());
        Interval ib = toInterval(b.workDate(), b.clockIn(), b.clockOut());
        return ia.start < ib.end && ib.start < ia.end;
    }

    private static Interval toInterval(LocalDate baseDate, java.time.LocalTime in, java.time.LocalTime out) {
        int start = in.getHour() * 60 + in.getMinute();
        int end = out.getHour() * 60 + out.getMinute();
        if (end <= start) end += 24 * 60;
        return new Interval(start, end);
    }

    private static final class Interval {
        final int start; final int end;
        Interval(int start, int end) { this.start = start; this.end = end; }
    }

    // ── 조회/삭제 메서드들 (그대로) ─────────────────────────────
    public Optional<AttendanceRecord> find(long id) { return repo.findById(id); }
    public List<AttendanceRecord> byDate(LocalDate d) { return repo.findByDate(d); }
    public List<AttendanceRecord> byMonth(YearMonth ym) { return repo.findByMonth(ym); }
    public List<AttendanceRecord> byEmpRange(long empId, LocalDate from, LocalDate to) { return repo.findByEmployeeAndRange(empId, from, to); }

    /** ✅ 추가: 하루 단위 조회 (Saver가 사용) */
    public List<AttendanceRecord> byEmpDate(long empId, LocalDate date) { return repo.findByEmployeeAndRange(empId, date, date); }

    public boolean remove(long id) { return repo.deleteById(id); }
}

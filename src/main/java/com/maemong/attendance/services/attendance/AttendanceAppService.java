package com.maemong.attendance.services.attendance;

import com.maemong.attendance.domain.AttendanceRecord;
import com.maemong.attendance.ports.AttendanceRepository;
import com.maemong.attendance.ports.EmployeeRepository;
import com.maemong.attendance.services.attendance.commands.SaveAttendanceCommand;
import com.maemong.attendance.services.attendance.queries.AttendanceQueries;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * 근태 유스케이스 서비스 (저장/조회/삭제)
 * - enforceNoOverlap=true: 겹침 금지 (엄격)
 * - enforceNoOverlap=false: 겹침 허용 (UI에서 확인)
 */
public class AttendanceAppService implements AttendanceQueries {
    private final AttendanceRepository repo;
    private final EmployeeRepository employees;
    private final boolean enforceNoOverlap;

    public AttendanceAppService(AttendanceRepository repo, EmployeeRepository employees, boolean enforceNoOverlap) {
        this.repo = repo;
        this.employees = employees;
        this.enforceNoOverlap = enforceNoOverlap;
    }

    public AttendanceRecord save(SaveAttendanceCommand cmd) {
        if (cmd == null) throw new IllegalArgumentException("근태 기록이 제공되지 않았습니다.");
        if (cmd.employeeId() == 0) throw new IllegalArgumentException("사번이 올바르지 않습니다.");
        if (employees.findById(cmd.employeeId()).isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 사번입니다: " + cmd.employeeId());
        }
        if (cmd.clockIn() != null && cmd.clockOut() != null && cmd.clockOut().equals(cmd.clockIn())) {
            throw new IllegalArgumentException("출근·퇴근 시간이 동일할 수 없습니다.");
        }

        AttendanceRecord r = new AttendanceRecord(
                cmd.id(),
                cmd.employeeId(),
                cmd.workDate(),
                cmd.clockIn(),
                cmd.clockOut(),
                (cmd.memo() == null || cmd.memo().isBlank()) ? null : cmd.memo().trim()
        );

        // 겹침 검사 (strict 모드일 때만)
        if (r.clockIn() != null && r.clockOut() != null && enforceNoOverlap) {
            LocalDate from = r.workDate().minusDays(1);
            LocalDate to   = r.workDate().plusDays(1);
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
        Interval ia = toInterval(a.clockIn(), a.clockOut());
        Interval ib = toInterval(b.clockIn(), b.clockOut());
        return ia.start < ib.end && ib.start < ia.end;
    }
    private static Interval toInterval(java.time.LocalTime in, java.time.LocalTime out) {
        int start = in.getHour() * 60 + in.getMinute();
        int end   = out.getHour() * 60 + out.getMinute();
        if (end <= start) end += 24 * 60;
        return new Interval(start, end);
    }
    private record Interval(int start, int end) {}

    // ===== 조회/삭제 =====
    public Optional<AttendanceRecord> find(long id) { return repo.findById(id); }
    @Override public List<AttendanceRecord> byDate(LocalDate d) { return repo.findByDate(d); }
    @Override public List<AttendanceRecord> byMonth(YearMonth ym) { return repo.findByMonth(ym); }
    @Override public List<AttendanceRecord> byEmpRange(long empId, LocalDate from, LocalDate to) {
        return repo.findByEmployeeAndRange(empId, from, to);
    }
    public List<AttendanceRecord> byEmpDate(long empId, LocalDate date) {
        return repo.findByEmployeeAndRange(empId, date, date);
    }
    public boolean remove(long id) { return repo.deleteById(id); }
}

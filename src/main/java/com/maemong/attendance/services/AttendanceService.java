package com.maemong.attendance.services;

import com.maemong.attendance.domain.AttendanceRecord;
import com.maemong.attendance.ports.AttendanceRepository;
import com.maemong.attendance.ports.EmployeeRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * AttendanceService
 * - 출퇴근 기록 저장/조회/삭제를 담당
 * - 저장 시 employeeId 유효성 검사로 데이터 무결성 보장
 */
public class AttendanceService {
	private final AttendanceRepository repo;
	private final EmployeeRepository employees;

	public AttendanceService(AttendanceRepository repo, EmployeeRepository employees) {
		this.repo = repo;
		this.employees = employees;
	}

    /**
     * 저장(신규/수정) — 입력 유효성 검사 수행.
     * 예외 발생 시 호출자는 적절히 사용자에게 메시지를 보여주어야 함.
     */
	public AttendanceRecord upsert(AttendanceRecord r) {
        // basic null/id check
        if (r == null) throw new IllegalArgumentException("근태 기록이 제공되지 않았습니다.");
        if (r.employeeId() == 0) throw new IllegalArgumentException("사번이 올바르지 않습니다.");

        // 직원 존재 확인
        if (employees.findById(r.employeeId()).isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 사번입니다: " + r.employeeId());
        }

        // 출근/퇴근 시간 논리 체크
        if (r.clockIn() != null && r.clockOut() != null) {
            if (r.clockOut().equals(r.clockIn())) {
                throw new IllegalArgumentException("출근·퇴근 시간이 동일할 수 없습니다.");
            }
        }

        // --- 겹침 금지 검사 추가 (tin/tout 둘 다 있을 때만 검사) ---
        if (r.clockIn() != null && r.clockOut() != null) {
            LocalDate from = r.workDate().minusDays(1);
            LocalDate to = r.workDate().plusDays(1);
            // 주변 기록 로드 (자정 넘김 고려)
            List<AttendanceRecord> around = repo.findByEmployeeAndRange(r.employeeId(), from, to);

            for (AttendanceRecord ex : around) {
                // 자기 자신은 제외(수정 시)
                if (r.id() != null && r.id().equals(ex.id())) continue;

                // 비교 대상도 in/out 둘 다 있어야 겹침 검사 가능
                if (ex.clockIn() == null || ex.clockOut() == null) continue;

                if (isOverlapped(r, ex)) {
                    throw new IllegalArgumentException(
                            "다른 근무 기록과 시간이 겹칩니다. (기존: "
                                    + ex.workDate() + " " + ex.clockIn() + "~" + ex.clockOut() + ")"
                    );
                }
            }
        }

        // 모든 검증 통과 -> 저장 위임
        return repo.save(r);
	}

    /** rA와 rB가 자정 넘김을 고려했을 때 시간이 겹치는지 여부 */
    private static boolean isOverlapped(AttendanceRecord a, AttendanceRecord b) {
        // a의 시작/끝을 'a.workDate' 기준 48시간 라인에 올림
        Interval ia = toInterval(a.workDate(), a.clockIn(), a.clockOut());
        Interval ib = toInterval(b.workDate(), b.clockIn(), b.clockOut());

        // [start, end) 구간 겹침: a.start < b.end && b.start < a.end
        return ia.start < ib.end && ib.start < ia.end;
    }

    /** 날짜+시간을 기준으로 48시간 라인의 분 단위 구간으로 매핑 */
    private static Interval toInterval(LocalDate baseDate, java.time.LocalTime in, java.time.LocalTime out) {
        int start = in.getHour() * 60 + in.getMinute();     // 0~1439
        int end   = out.getHour() * 60 + out.getMinute();   // 0~1439

        // 다음날 퇴근이면 +24h
        if (end <= start) end += 24 * 60;

        // 구간을 분 단위로 표현
        return new Interval(start, end);
    }

    /** 단순 구간 표현용(분 단위, [start,end) ) */
    private static final class Interval {
        final int start; // inclusive
        final int end;   // exclusive
        Interval(int start, int end) { this.start = start; this.end = end; }
    }

    public Optional<AttendanceRecord> find(long id) { return repo.findById(id); }
    public List<AttendanceRecord> byDate(LocalDate d) { return repo.findByDate(d); }
    public List<AttendanceRecord> byMonth(YearMonth ym) { return repo.findByMonth(ym); }
    public List<AttendanceRecord> byEmpRange(long empId, LocalDate from, LocalDate to) { return repo.findByEmployeeAndRange(empId, from, to); }
    public boolean remove(long id) { return repo.deleteById(id); }
}

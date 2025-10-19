package com.maemong.attendance.ui.actions;

import com.maemong.attendance.bootstrap.Bootstrap;
import com.maemong.attendance.domain.AttendanceRecord;
import com.maemong.attendance.ui.model.EmployeeItem;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public final class AttendanceSaver {
    @FunctionalInterface
    public interface ConfirmPrompt {
        boolean confirm(String title, String message);
    }

    public static final class ValidationException extends RuntimeException {
        public ValidationException(String msg) { super(msg); }
    }

    private final Bootstrap boot;
    private final ConfirmPrompt prompt;

    public AttendanceSaver(Bootstrap boot, ConfirmPrompt prompt) {
        this.boot = boot;
        this.prompt = prompt;
    }

    public AttendanceRecord save(EmployeeItem emp,
                                 LocalDate workDate,
                                 LocalTime timeIn,
                                 LocalTime timeOut,
                                 String memo) {

        // 1) 기본 검증
        if (emp == null || emp.id == null) throw new ValidationException("사번/이름을 선택하세요.");
        if (workDate == null) throw new ValidationException("날짜를 선택하세요.");
        if (timeIn == null || timeOut == null) throw new ValidationException("출근/퇴근 시간을 확인하세요.");
        if (timeIn.equals(timeOut)) throw new ValidationException("출근·퇴근 시간이 동일할 수 없습니다.");

        boolean overnight = timeOut.isBefore(timeIn);

        // 2) 다음날 퇴근 확인
        if (overnight) {
            String msg = "퇴근 시간이 출근보다 이릅니다.\n"
                    + "다음날 퇴근으로 처리할까요?\n"
                    + "(" + timeIn + " → " + timeOut + ", 총 " + prettyDuration(timeIn, timeOut) + ")";
            boolean ok = (prompt == null) || prompt.confirm("다음날 퇴근 확인", msg);
            if (!ok) throw new ValidationException("저장을 취소했습니다.");
        }

        // 3) 겹침(Overlap) 검사
        //    - 같은 사번, workDate의 기존 기록과 겹치는지 확인
        //    - 또한 '전일(overnight) 기록이 오늘로 넘어온 경우'와도 충돌 체크
        List<AttendanceRecord> neighbors = fetchNeighborRecords(emp.id, workDate);

        List<AttendanceRecord> conflicts = new ArrayList<>();
        for (AttendanceRecord ex : neighbors) {
            if (overlaps(timeIn, timeOut, ex.clockIn(), ex.clockOut())) {
                conflicts.add(ex);
            }
        }

        if (!conflicts.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("아래 기록과 시간이 겹칩니다. 그래도 저장할까요?\n\n");
            for (AttendanceRecord c : conflicts) {
                sb.append("- ")
                        .append("[").append(c.workDate()).append("] ")
                        .append(c.clockIn()).append(" ~ ").append(c.clockOut());
                if (c.memo() != null && !c.memo().isBlank()) sb.append(" (").append(c.memo()).append(")");
                sb.append("\n");
            }
            sb.append("\n※ '예'를 누르면 겹침을 허용하고 별도 근무로 저장합니다.");
            boolean ok = (prompt == null) || prompt.confirm("겹치는 기록 감지", sb.toString());
            if (!ok) throw new ValidationException("겹치는 기록으로 인해 저장을 취소했습니다.");
        }

        // 4) 저장(upsert)
        AttendanceRecord record = new AttendanceRecord(
                null,
                emp.id,
                workDate,
                timeIn,
                timeOut,
                (memo == null || memo.isBlank()) ? null : memo.trim()
        );
        return boot.attendance().upsert(record);
    }

    // ── 겹침 검사 유틸 ────────────────────────────────────────────
    /**
     * 자정 넘김을 분 단위 선형 구간으로 정규화하여 겹침 여부 판단
     * in <= out:  [in, out)
     * in >  out:  [in, out+24h)  (overnight)
     */
    static boolean overlaps(LocalTime aIn, LocalTime aOut, LocalTime bIn, LocalTime bOut) {
        int a1 = toMinutes(aIn);
        int a2 = toMinutes(aOut);
        if (a2 <= a1) a2 += 24 * 60; // overnight

        int b1 = toMinutes(bIn);
        int b2 = toMinutes(bOut);
        if (b2 <= b1) b2 += 24 * 60; // overnight

        // 두 반열린구간 [a1,a2)와 [b1,b2)의 교집합 판정
        return (a1 < b2) && (b1 < a2);
    }

    private static int toMinutes(LocalTime t) {
        return t.getHour() * 60 + t.getMinute();
    }

    private static int durationMinutes(LocalTime in, LocalTime out) {
        int a = toMinutes(in);
        int b = toMinutes(out);
        int diff = b - a;
        if (diff < 0) diff += 24 * 60;
        return diff;
    }
    private static String prettyDuration(LocalTime in, LocalTime out) {
        int minutes = durationMinutes(in, out);
        int h = minutes / 60, m = minutes % 60;
        return String.format("%02d:%02d", h, m);
    }

    // ── 이웃(전일/당일) 기록 로딩 ─────────────────────────────────
    private List<AttendanceRecord> fetchNeighborRecords(Long empId, LocalDate base) {
        LocalDate prev = base.minusDays(1);

        // ✅ Repository에 다음 시그니처가 있으면 사용하는 것을 권장:
        // List<AttendanceRecord> listByEmployeeAndDate(long empId, LocalDate date);
        // 없으면 범위조회 후 필터링(아래 Fallback)으로 구현

        List<AttendanceRecord> result = new ArrayList<>();

        try {
            result.addAll(boot.attendance().byEmpDate(empId, base));
            result.addAll(boot.attendance().byEmpDate(empId, prev));
        } catch (Throwable noSuchMethod) {
            // Fallback: 범위 조회 가능 시 사용 (예시)
            // var all = boot.attendance().listByEmployee(empId); // 가정
            // result = all.stream()
            //        .filter(r -> r.workDate().equals(base) || r.workDate().equals(prev))
            //        .toList();
        }

        return result;
    }
}

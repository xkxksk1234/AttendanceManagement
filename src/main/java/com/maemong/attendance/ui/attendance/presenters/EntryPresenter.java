package com.maemong.attendance.ui.attendance.presenters;

import com.maemong.attendance.bootstrap.Bootstrap;
import com.maemong.attendance.domain.AttendanceRecord;
import com.maemong.attendance.events.AppEvents;
import com.maemong.attendance.services.AttendanceService;

import java.time.LocalDate;
import java.time.LocalTime;

/** 출퇴근 등록 탭 Presenter: AttendanceService(포워더)만 사용 */
public class EntryPresenter {
    private final Bootstrap boot;
    private final AttendanceService svc;

    public EntryPresenter(Bootstrap boot) {
        this.boot = boot;
        this.svc = boot.attendance(); // 포워더 타입 그대로 사용 (캐스팅/리플렉션 없음)
    }

    /** 저장 후 AppEvents.AttendanceSaved 발행 */
    public AttendanceRecord save(long empId, LocalDate date, LocalTime in, LocalTime out, String memo) {
        AttendanceRecord toSave = new AttendanceRecord(
                null,           // 신규
                empId,
                date,
                in,
                out,
                (memo == null || memo.isBlank()) ? null : memo.trim()
        );
        AttendanceRecord saved = svc.upsert(toSave);
        boot.events().fireAttendanceSaved(
                new AppEvents.AttendanceSavedEvent(saved.id(), saved.employeeId(), saved.workDate())
        );
        return saved;
    }
}

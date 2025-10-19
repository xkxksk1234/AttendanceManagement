package com.maemong.attendance.ui.attendance.presenters;

import com.maemong.attendance.bootstrap.Bootstrap;
import com.maemong.attendance.domain.AttendanceRecord;
import com.maemong.attendance.services.AttendanceService;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** 조회 탭 Presenter: AttendanceService(포워더)만 사용 */
public class RecordsPresenter {
    private final AttendanceService svc;

    public RecordsPresenter(Bootstrap boot) {
        this.svc = boot.attendance(); // 포워더 그대로
    }

    public List<AttendanceRecord> byMonth(YearMonth ym) { return svc.byMonth(ym); }
    public List<AttendanceRecord> byDate(LocalDate d) { return svc.byDate(d); }
    public List<AttendanceRecord> byEmpRange(long empId, LocalDate from, LocalDate to) { return svc.byEmpRange(empId, from, to); }
    public boolean remove(long id) { return svc.remove(id); }
}

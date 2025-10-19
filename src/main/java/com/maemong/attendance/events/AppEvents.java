package com.maemong.attendance.events;

import javax.swing.event.EventListenerList;
import java.time.LocalDate;
import java.util.EventListener;

/** 애플리케이션 공용 이벤트 버스 */
public final class AppEvents {

    private final EventListenerList listeners = new EventListenerList();

    // ── 이벤트 페이로드 ──
    public static final class AttendanceSavedEvent {
        public final Long recordId;
        public final long employeeId;
        public final LocalDate workDate;

        public AttendanceSavedEvent(Long recordId, long employeeId, LocalDate workDate) {
            this.recordId = recordId;
            this.employeeId = employeeId;
            this.workDate = workDate;
        }
    }

    // ── 리스너 인터페이스: 반드시 EventListener 상속! ──
    @FunctionalInterface
    public interface AttendanceSavedListener extends EventListener {
        void onAttendanceSaved(AttendanceSavedEvent e);
    }

    // ── 구독/해제 ──
    public void addAttendanceSavedListener(AttendanceSavedListener l) {
        listeners.add(AttendanceSavedListener.class, l);
    }
    public void removeAttendanceSavedListener(AttendanceSavedListener l) {
        listeners.remove(AttendanceSavedListener.class, l);
    }

    // ── 발행 ──
    public void fireAttendanceSaved(AttendanceSavedEvent e) {
        for (AttendanceSavedListener l : listeners.getListeners(AttendanceSavedListener.class)) {
            try {
                l.onAttendanceSaved(e);
            } catch (Throwable ignore) {
                // 개별 리스너 예외는 버스 전체에 영향 주지 않게 무시
            }
        }
    }
}

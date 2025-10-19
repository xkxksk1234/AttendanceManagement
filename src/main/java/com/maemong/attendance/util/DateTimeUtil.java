package com.maemong.attendance.util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtil {
    private DateTimeUtil() {}

    /** 공용 HH:mm 포맷터(24시간) */
    public static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    /** 자정 넘김(overnight) 고려 총 분 계산 */
    public static int durationMinutes(LocalTime in, LocalTime out) {
        int a = in.getHour() * 60 + in.getMinute();
        int b = out.getHour() * 60 + out.getMinute();
        int diff = b - a;
        if (diff < 0) diff += 24 * 60;
        return diff;
    }

    /** 총 분을 "HH:mm" 문자열로 예쁘게 반환 */
    public static String prettyDuration(LocalTime in, LocalTime out) {
        int minutes = durationMinutes(in, out);
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }
}

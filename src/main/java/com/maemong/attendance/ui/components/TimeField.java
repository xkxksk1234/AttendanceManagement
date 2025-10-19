package com.maemong.attendance.ui.components;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class TimeField extends JFormattedTextField {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm");

    public TimeField(String defaultValue) {
        super(defaultValue);
        setColumns(5);
        setHorizontalAlignment(SwingConstants.CENTER);
        // 입력 필터: 숫자/콜론만, 최대 5글자
        ((AbstractDocument) getDocument()).setDocumentFilter(new TimeFilter());
        setInputVerifier(new TimeVerifier());
        setToolTipText("24시간 형식 HH:mm (예: 09:00)");
    }

    public LocalTime getTimeOrThrow() {
        String s = getText().trim();
        try {
            return LocalTime.parse(s, FMT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("시간 형식이 올바르지 않습니다: " + s + " (예: 09:00)");
        }
    }
    public void setTime(LocalTime t) {
        setText(t.format(FMT));
    }

    // ── 내부: 형식 보조 ────────────────────────────────────────────────
    private static final class TimeFilter extends DocumentFilter {
        @Override public void insertString(FilterBypass fb, int offs, String str, AttributeSet a) throws BadLocationException {
            if (str != null) replace(fb, offs, 0, str, a);
        }
        @Override public void replace(FilterBypass fb, int offs, int len, String str, AttributeSet a) throws BadLocationException {
            Document doc = fb.getDocument();
            StringBuilder sb = new StringBuilder(doc.getText(0, doc.getLength()));
            if (len > 0) sb.replace(offs, offs + len, "");
            if (str != null) sb.insert(offs, str);

            String next = sb.toString();
            if (next.length() > 5) return;
            // 숫자와 콜론만 허용
            if (!next.matches("[0-9:]*")) return;
            // 콜론은 최대 1개 위치(예: "12:34")
            if (next.chars().filter(c -> c == ':').count() > 1) return;
            fb.replace(offs, len, str, a);
        }
    }
    private static final class TimeVerifier extends InputVerifier {
        @Override public boolean verify(JComponent input) {
            String s = ((JFormattedTextField) input).getText().trim();
            // 공란은 통과시키지 않음
            if (s.isEmpty() || !s.matches("\\d{2}:\\d{2}")) return false;
            try {
                LocalTime.parse(s, FMT);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        @Override public boolean shouldYieldFocus(JComponent input) {
            boolean ok = verify(input);
            if (!ok) {
                Toolkit.getDefaultToolkit().beep();
                input.setBackground(new Color(255, 240, 240));
            } else {
                input.setBackground(Color.white);
            }
            return ok;
        }
    }
}

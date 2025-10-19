package com.maemong.attendance.ui.components;

import com.maemong.attendance.util.DateTimeUtil;

import javax.swing.*;
import java.awt.*;
import java.time.LocalTime;

public final class TimeRangeField extends JPanel {
    private final TimeField inField;
    private final TimeField outField;

    // 🔴 커스텀 PropertyChangeSupport 및 add/remove 오버라이드 제거!
    // JComponent의 기본 addPropertyChangeListener / firePropertyChange 사용

    public TimeRangeField(String defaultIn, String defaultOut, boolean showQuickButtons) {
        super(new GridBagLayout());

        this.inField  = new TimeField(defaultIn);
        this.outField = new TimeField(defaultOut);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0,6,0,6);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; add(new JLabel("출근"), c);
        c.gridx = 1; add(inField, c);
        c.gridx = 2; add(new JLabel("퇴근"), c);
        c.gridx = 3; add(outField, c);

        if (showQuickButtons) {
            c.gridx = 4; JButton btnNowIn = new JButton("지금(출)");
            btnNowIn.addActionListener(e -> setIn(LocalTime.now().withSecond(0).withNano(0)));
            add(btnNowIn, c);

            c.gridx = 5; JButton btnNowOut = new JButton("지금(퇴)");
            btnNowOut.addActionListener(e -> setOut(LocalTime.now().withSecond(0).withNano(0)));
            add(btnNowOut, c);

            c.gridx = 6; JButton btnPlus30 = new JButton("+30m");
            btnPlus30.setToolTipText("퇴근 시간을 30분 증가");
            btnPlus30.addActionListener(e -> setOut(getOut().plusMinutes(30)));
            add(btnPlus30, c);
        }

        // ✅ 필드 초기화 이후에 문서 리스너 바인딩 & JComponent.firePropertyChange 사용
        var docListener = new javax.swing.event.DocumentListener() {
            private void fireAll() {
                try {
                    firePropertyChange("timeIn",  null, getIn());
                    firePropertyChange("timeOut", null, getOut());
                    firePropertyChange("duration", null, getPrettyDuration());
                } catch (Exception ignore) { /* 입력 도중 파싱 실패 가능 → 무시 */ }
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e){ fireAll(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e){ fireAll(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e){ fireAll(); }
        };
        inField.getDocument().addDocumentListener(docListener);
        outField.getDocument().addDocumentListener(docListener);
    }

    public LocalTime getIn()  { return inField.getTimeOrThrow(); }
    public LocalTime getOut() { return outField.getTimeOrThrow(); }
    public void setIn(LocalTime t)  { inField.setTime(t); }
    public void setOut(LocalTime t) { outField.setTime(t); }

    /** "HH:mm" 형태의 총 근무시간 문자열 반환 */
    public String getPrettyDuration() {
        try {
            return DateTimeUtil.prettyDuration(getIn(), getOut());
        } catch (Exception e) {
            return "--:--";
        }
    }
}

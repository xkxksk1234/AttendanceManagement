package com.maemong.attendance.ui.panels;

import com.maemong.attendance.bootstrap.Bootstrap;
import com.maemong.attendance.domain.AttendanceRecord;
import com.maemong.attendance.ui.actions.AttendanceSaver;
import com.maemong.attendance.ui.components.DatePicker3;
import com.maemong.attendance.ui.components.EmployeePicker;
import com.maemong.attendance.ui.components.TimeRangeField;
import com.maemong.attendance.events.AppEvents;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;

public class PanelAttendance extends JPanel {

    // ── 주입(최근 기록 조회/제안에 필요) ──
    private final Bootstrap boot;

    // 날짜
    private final DatePicker3 datePicker = new DatePicker3(true);

    // 직원
    private final EmployeePicker employeePicker;

    // 시간/저장
    private final TimeRangeField timeRange = new TimeRangeField("09:00", "18:00", true);
    private final JButton btnSave  = new JButton("등록");

    // 6단계: UX 확장
    private final JLabel lblDuration = new JLabel("총 00:00");
    private final JCheckBox chkKeepAfterSave = new JCheckBox("저장 후 폼 유지", true);
    private final JComboBox<String> cbMemoTpl = new JComboBox<>(new String[]{"(없음)", "지각", "교육", "수습"});
    private final JTextArea taMemo = new JTextArea(3, 20);

    private final AttendanceSaver saver;

    public PanelAttendance(Bootstrap boot) {
        this.boot = boot;
        this.employeePicker = new EmployeePicker(boot);

        setLayout(new BorderLayout());
        buildTopPanel();

        // 저장 전용 서비스 주입 (JOptionPane confirm)
        this.saver = new AttendanceSaver(boot, (title, message) -> {
            int ans = JOptionPane.showConfirmDialog(
                    this, message, title,
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            return ans == JOptionPane.YES_OPTION;
        });

        // 실시간 근무시간 미리보기
        timeRange.addPropertyChangeListener(evt -> {
            if ("duration".equals(evt.getPropertyName())) {
                lblDuration.setText("총 " + timeRange.getPrettyDuration());
            }
        });

        // 메모 템플릿 → 메모 반영(덮어쓰기)
        cbMemoTpl.addActionListener(e -> {
            String sel = (String) cbMemoTpl.getSelectedItem();
            if (sel == null || "(없음)".equals(sel)) return;
            taMemo.setText(sel);
        });

        // “어제 퇴근 → 오늘 출근” 자동 제안 트리거
        javax.swing.Timer suggestTimer = new javax.swing.Timer(200, e -> suggestRecentIn());
        suggestTimer.setRepeats(false);
        datePicker.getYearBox().addActionListener(e -> suggestTimer.restart());
        datePicker.getMonthBox().addActionListener(e -> suggestTimer.restart());
        datePicker.getDayBox().addActionListener(e -> suggestTimer.restart());
        btnSave.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { suggestTimer.restart(); }
        });

        // 단축키 바인딩(Enter=저장, Esc=리셋)
        installKeyBindings(this);

        btnSave.addActionListener(e -> onSave());
    }

    private void buildTopPanel() {
        JPanel top = new JPanel(new GridBagLayout());
        top.setBorder(new EmptyBorder(6,6,6,6));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.fill = GridBagConstraints.HORIZONTAL;

        // 0) 날짜
        c.gridx = 0; c.gridy = 0; c.gridwidth = 6; c.weightx = 1;
        top.add(datePicker, c);

        // 1) 직원
        c.gridy = 1; c.gridwidth = 6; c.weightx = 1;
        top.add(employeePicker, c);

        // 2) 시간 + 미리보기 + 저장
        c.gridy = 2; c.gridx = 0; c.gridwidth = 4; c.weightx = 1;
        top.add(timeRange, c);

        c.gridx = 4; c.gridwidth = 1; c.weightx = 0;
        lblDuration.setHorizontalAlignment(SwingConstants.CENTER);
        lblDuration.setBorder(BorderFactory.createEtchedBorder());
        lblDuration.setToolTipText("출근/퇴근 입력에 따라 총 근무시간을 실시간 표시합니다.");
        top.add(lblDuration, c);

        c.gridx = 5; c.gridwidth = 1; c.weightx = 0;
        top.add(btnSave, c);

        // 3) 메모 템플릿 + 폼 유지 옵션 + 메모 입력
        JPanel memoPanel = new JPanel(new BorderLayout(6,6));
        JPanel memoNorth = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        memoNorth.add(new JLabel("메모 템플릿"));
        memoNorth.add(cbMemoTpl);
        memoNorth.add(chkKeepAfterSave);
        memoPanel.add(memoNorth, BorderLayout.NORTH);

        taMemo.setLineWrap(true);
        taMemo.setWrapStyleWord(true);
        memoPanel.add(new JScrollPane(taMemo,
                        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER),
                BorderLayout.CENTER);

        c.gridx = 0; c.gridy = 3; c.gridwidth = 6; c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        top.add(memoPanel, c);

        add(top, BorderLayout.NORTH);
    }

    private void onSave() {
        try {
            LocalDate date = datePicker.getDate();
            var emp  = employeePicker.getSelected();
            LocalTime tin  = timeRange.getIn();
            LocalTime tout = timeRange.getOut();
            String memo = taMemo.getText().isBlank() ? null : taMemo.getText().trim();

            var saved = saver.save(emp, date, tin, tout, memo);
            JOptionPane.showMessageDialog(this, "저장됨 id=" + saved.id());

            boot.events().fireAttendanceSaved(
                    new AppEvents.AttendanceSavedEvent(saved.id(), saved.employeeId(), saved.workDate())
            );

            if (!chkKeepAfterSave.isSelected()) {
                resetForm();
            }
        } catch (AttendanceSaver.ValidationException vex) {
            JOptionPane.showMessageDialog(this, vex.getMessage(), "안내", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── 단축키: Enter=저장, Esc=리셋 ──
    private void installKeyBindings(JComponent root) {
        InputMap im = root.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am = root.getActionMap();

        im.put(KeyStroke.getKeyStroke("ENTER"), "save");
        am.put("save", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                btnSave.doClick();
            }
        });

        im.put(KeyStroke.getKeyStroke("ESCAPE"), "reset");
        am.put("reset", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                resetForm();
            }
        });
    }

    // ── 폼 리셋 ──
    private void resetForm() {
        try {
            timeRange.setIn(LocalTime.parse("09:00"));
            timeRange.setOut(LocalTime.parse("18:00"));
        } catch (Exception ignore) {}
        cbMemoTpl.setSelectedIndex(0);
        taMemo.setText("");
        datePicker.setDate(LocalDate.now());
        // EmployeePicker의 검색 입력 초기화가 필요하면 EmployeePicker에 메서드 추가해 호출
    }

    // ── “어제 퇴근 → 오늘 출근” 자동 제안 ──
    private void suggestRecentIn() {
        var emp = employeePicker.getSelected();
        var date = datePicker.getDate();
        if (emp == null || emp.id == null || date == null) return;

        try {
            var from = date.minusDays(7);
            var list = boot.attendance().byEmpRange(emp.id, from, date.minusDays(1));
            if (list == null || list.isEmpty()) return;

            AttendanceRecord last = list.stream()
                    .filter(r -> r.clockIn() != null && r.clockOut() != null)
                    .max(Comparator.<AttendanceRecord, LocalDate>comparing(AttendanceRecord::workDate)
                            .thenComparing(AttendanceRecord::clockOut))
                    .orElse(null);
            if (last == null) return;

            if (last.workDate().equals(date.minusDays(1))) {
                LocalTime suggestIn = last.clockOut();
                timeRange.setIn(suggestIn);
                lblDuration.setToolTipText("어제 퇴근 " + suggestIn + " 기준으로 출근 시간이 제안되었습니다.");
            }
        } catch (Exception ignore) {
            // 조용히 무시
        }
    }
}

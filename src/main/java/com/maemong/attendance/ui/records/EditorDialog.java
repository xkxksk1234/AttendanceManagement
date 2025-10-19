package com.maemong.attendance.ui.records;

import com.maemong.attendance.bootstrap.Bootstrap;
import com.maemong.attendance.domain.AttendanceRecord;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public final class EditorDialog {
    private static final DateTimeFormatter ISO_TIME = DateTimeFormatter.ofPattern("HH:mm");

    private EditorDialog() {}

    /** PanelRecords에서 호출하는 진입점 */
    public static void open(Component parent,
                            Bootstrap boot,
                            AttendanceRecord current,
                            Map<Long, String> nameCache,
                            Runnable onSavedOrDeleted) {
        // --- 모델에서 현재 값 읽기 ---
        Long id      = current.id();
        LocalDate date = current.workDate();
        Long empId   = current.employeeId();
        String name  = nameCache.getOrDefault(empId, "");
        String inStr  = formatTime(current.clockIn());
        String outStr = formatTime(current.clockOut());
        String memo   = current.memo() == null ? "" : current.memo();

        // --- 다이얼로그/폼 구성 ---
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(parent), "근무 수정", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6,6,6,6);
        g.gridx=0; g.gridy=0; g.anchor=GridBagConstraints.EAST;

        JTextField tfId    = new JTextField(String.valueOf(id)); tfId.setEditable(false);
        JTextField tfEmpId = new JTextField(empId == null ? "" : String.valueOf(empId));
        JTextField tfName  = new JTextField(name == null ? "" : name); tfName.setEditable(false);
        JTextField tfDate  = new JTextField(date == null ? "" : date.toString()); // yyyy-MM-dd
        JTextField tfIn    = new JTextField(inStr  == null ? "" : inStr);  // HH:mm
        JTextField tfOut   = new JTextField(outStr == null ? "" : outStr); // HH:mm
        JTextArea  taMemo  = new JTextArea(memo, 4, 24);
        JCheckBox  cbNextDay = new JCheckBox("다음날 퇴근", false);
        JLabel     lbDur  = new JLabel("근무시간: -");

        pAdd(p, g, new JLabel("ID"));      pAddField(p, g, tfId);
        pAdd(p, g, new JLabel("사번"));     pAddField(p, g, tfEmpId);
        pAdd(p, g, new JLabel("이름"));     pAddField(p, g, tfName);
        pAdd(p, g, new JLabel("날짜"));     pAddField(p, g, tfDate);
        pAdd(p, g, new JLabel("출근"));     pAddField(p, g, tfIn);
        pAdd(p, g, new JLabel("퇴근"));     pAddField(p, g, tfOut);
        pAdd(p, g, new JLabel("옵션"));     pAddField(p, g, cbNextDay);
        pAdd(p, g, new JLabel("메모"));
        pAddField(p, g, new JScrollPane(taMemo));

        // 근무시간 라벨 줄
        g.gridx = 1;
        g.fill = GridBagConstraints.NONE;
        g.anchor = GridBagConstraints.WEST;
        p.add(lbDur, g);
        g.gridy++;

        // 버튼들
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btSave = new JButton("저장");
        JButton btDel  = new JButton("삭제");
        JButton btCancel = new JButton("닫기");
        btns.add(btDel); btns.add(btSave); btns.add(btCancel);

        // ====== UX 보완 (이미 네 코드에 있던 개선들 포함) ======
        // 1) Enter=저장 / Esc=닫기
        dlg.getRootPane().setDefaultButton(btSave);
        dlg.getRootPane().registerKeyboardAction(e2 -> dlg.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        // 2) 사번 입력 시 이름 자동 채움
        tfEmpId.getDocument().addDocumentListener(new DocumentListener() {
            private void sync() {
                String s = tfEmpId.getText().trim();
                if (s.isEmpty()) { tfName.setText(name == null ? "" : name); return; }
                try {
                    long eid = Long.parseLong(s);
                    tfName.setText(nameCache.getOrDefault(eid, ""));
                } catch (NumberFormatException ignore) {
                    tfName.setText("");
                }
            }
            @Override public void insertUpdate(DocumentEvent e) { sync(); }
            @Override public void removeUpdate(DocumentEvent e) { sync(); }
            @Override public void changedUpdate(DocumentEvent e) { sync(); }
        });

        // 3) 시간 입력 보조: Ctrl+↑/↓ 10분
        KeyAdapter incDec10m = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == 0) return;
                int code = e.getKeyCode();
                if (code != KeyEvent.VK_UP && code != KeyEvent.VK_DOWN) return;

                JTextField src = (JTextField) e.getSource();
                LocalTime t;
                try { t = parseHm(src.getText().trim()); } catch (Exception ex) { return; }
                if (t == null) return;

                int delta = (code == KeyEvent.VK_UP) ? +10 : -10;
                int m = (t.getHour() * 60 + t.getMinute() + delta + 1440) % 1440;
                LocalTime nt = LocalTime.of(m / 60, m % 60);
                src.setText(String.format("%02d:%02d", nt.getHour(), nt.getMinute()));
                src.selectAll();
                e.consume();
            }
        };
        tfIn.addKeyListener(incDec10m);
        tfOut.addKeyListener(incDec10m);

        // 포커스 얻으면 전체 선택
        FocusAdapter selAll = new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                SwingUtilities.invokeLater(() -> {
                    if (e.getComponent() instanceof JTextField f) f.selectAll();
                });
            }
        };
        tfEmpId.addFocusListener(selAll);
        tfDate.addFocusListener(selAll);
        tfIn.addFocusListener(selAll);
        tfOut.addFocusListener(selAll);

        // 시간 미리보기 + '다음날' 배지/색 강조
        Runnable refreshDur = () -> {
            try {
                LocalTime tin  = parseHm(tfIn.getText().trim());
                LocalTime tout = parseHm(tfOut.getText().trim());
                if (tin == null || tout == null) {
                    lbDur.setText("근무시간: -");
                    lbDur.setForeground(UIManager.getColor("Label.foreground"));
                    return;
                }
                int min = durationMinutes(tin, tout);
                boolean nextDay = cbNextDay.isSelected() && !tout.isAfter(tin);
                if (nextDay) min += 24 * 60;

                String text = String.format("근무시간: %02d:%02d", (min / 60), (min % 60));
                if (nextDay) text += " (다음날)";

                lbDur.setText(text);
                lbDur.setForeground(nextDay ? new Color(0, 102, 204) : UIManager.getColor("Label.foreground"));
            } catch (Exception ex) {
                lbDur.setText("근무시간: -");
                lbDur.setForeground(UIManager.getColor("Label.foreground"));
            }
        };
        KeyAdapter ka = new KeyAdapter(){ public void keyReleased(KeyEvent e){ refreshDur.run(); }};
        tfIn.addKeyListener(ka); tfOut.addKeyListener(ka);
        cbNextDay.addActionListener(e -> refreshDur.run());
        refreshDur.run();

        // 시간 형식 실시간 검증
        Border normalBorder = tfIn.getBorder();
        Border errorBorder  = BorderFactory.createLineBorder(new Color(220, 50, 47));
        Runnable validateTimes = () -> {
            JTextField[] arr = { tfIn, tfOut };
            for (JTextField f : arr) {
                String s = f.getText().trim();
                boolean ok = false;
                if (!s.isEmpty()) {
                    try { ok = (parseHm(s) != null); } catch (Exception ex) { ok = false; }
                }
                f.setBorder(ok ? normalBorder : errorBorder);
                f.setToolTipText(ok ? null : "시간 형식: HH:mm (예: 09:00)");
            }
        };
        DocumentListener timeDoc = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { validateTimes.run(); refreshDur.run(); }
            @Override public void removeUpdate(DocumentEvent e) { validateTimes.run(); refreshDur.run(); }
            @Override public void changedUpdate(DocumentEvent e) { validateTimes.run(); refreshDur.run(); }
        };
        tfIn.getDocument().addDocumentListener(timeDoc);
        tfOut.getDocument().addDocumentListener(timeDoc);
        validateTimes.run();
        refreshDur.run();

        // 저장
        btSave.addActionListener(e -> {
            try {
                long newEmpId = Long.parseLong(tfEmpId.getText().trim());
                LocalDate newDate = LocalDate.parse(tfDate.getText().trim()); // yyyy-MM-dd
                LocalTime tin  = parseHm(tfIn.getText().trim());
                LocalTime tout = parseHm(tfOut.getText().trim());
                if (tin == null || tout == null) throw new IllegalArgumentException("시간 형식은 HH:mm 입니다.");

                if (!cbNextDay.isSelected() && !tout.isAfter(tin)) {
                    int ans = JOptionPane.showConfirmDialog(dlg,
                            "퇴근이 출근보다 빠릅니다. '다음날 퇴근'으로 처리할까요?",
                            "확인", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (ans == JOptionPane.YES_OPTION) {
                        cbNextDay.setSelected(true);
                    } else {
                        return;
                    }
                }

                AttendanceRecord rec = new AttendanceRecord(id, newEmpId, newDate, tin, tout, taMemo.getText());
                boot.attendance().upsert(rec);
                if (onSavedOrDeleted != null) onSavedOrDeleted.run();
                dlg.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
            }
        });

        // 삭제
        btDel.addActionListener(e -> {
            int a = JOptionPane.showConfirmDialog(dlg, "이 기록을 삭제할까요?", "삭제",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (a != JOptionPane.YES_OPTION) return;
            try {
                if (id != null) boot.attendance().remove(id);
                if (onSavedOrDeleted != null) onSavedOrDeleted.run();
                dlg.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
            }
        });

        btCancel.addActionListener(e -> dlg.dispose());

        dlg.getContentPane().setLayout(new BorderLayout(8,8));
        dlg.getContentPane().add(p, BorderLayout.CENTER);
        dlg.getContentPane().add(btns, BorderLayout.SOUTH);
        dlg.pack();
        dlg.setLocationRelativeTo(parent);

        // 초기 포커스
        SwingUtilities.invokeLater(() -> tfIn.requestFocusInWindow());

        dlg.setVisible(true);
    }

    // ---- 내부 유틸 (PanelRecords와 분리) ----
    private static void pAdd(JPanel p, GridBagConstraints g, JComponent comp){
        g.gridx = 0;
        g.fill = GridBagConstraints.NONE;
        g.anchor = GridBagConstraints.EAST;
        p.add(comp, g);
    }
    private static void pAddField(JPanel p, GridBagConstraints g, JComponent field){
        g.gridx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;
        g.weightx = 1;
        p.add(field, g);
        g.gridy++;
    }
    private static String formatTime(LocalTime t) { return t == null ? "" : t.toString(); }
    private static LocalTime parseHm(String s) {
        if (s == null || s.isBlank()) return null;
        s = s.trim();
        if (s.matches("\\d{1,2}:\\d{1,2}")) {
            String[] p = s.split(":");
            int h = Integer.parseInt(p[0]);
            int m = Integer.parseInt(p[1]);
            if (h < 0 || h > 23 || m < 0 || m > 59) throw new IllegalArgumentException("시간 범위를 벗어났습니다: " + s);
            return LocalTime.of(h, m);
        }
        return LocalTime.parse(s, ISO_TIME);
    }
    private static int durationMinutes(LocalTime in, LocalTime out) {
        int a = in.getHour()*60 + in.getMinute();
        int b = out.getHour()*60 + out.getMinute();
        int diff = b - a;
        if (diff < 0) diff += 24*60;
        return diff;
    }
}

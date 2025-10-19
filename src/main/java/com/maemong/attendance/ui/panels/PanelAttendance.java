package com.maemong.attendance.ui.panels;

import com.maemong.attendance.bootstrap.Bootstrap;
import com.maemong.attendance.domain.AttendanceRecord;
import com.maemong.attendance.domain.Employee;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

public class PanelAttendance extends JPanel {

    private final Bootstrap boot;

    // 날짜 콤보
    private final JComboBox<Integer> year  = new JComboBox<>();
    private final JComboBox<Integer> month = new JComboBox<>();
    private final JComboBox<Integer> day   = new JComboBox<>();

    // 직원 선택(검색 + 콤보)
    private final JTextField tfSearch = new JTextField();               // 이름 검색 입력창
    private final DefaultComboBoxModel<EmployeeItem> empModel = new DefaultComboBoxModel<>();
    private final JComboBox<EmployeeItem> cbEmployee = new JComboBox<>(empModel);

    // 시간/저장
    private final JTextField tfIn  = new JTextField("09:00");
    private final JTextField tfOut = new JTextField("18:00");
    private final JButton btnSave  = new JButton("기록");

    public PanelAttendance(Bootstrap boot) {
        this.boot = boot;
        setLayout(new BorderLayout());

        buildDateSelectors();     // 기존 날짜 안전 로직 유지
        buildEmployeeSelectors(); // 이름검색 + 콤보
        buildTopPanel();          // 상단 UI 배치

        btnSave.addActionListener(e -> onSave());
    }

    // ─────────────────────────────────────────────────────────────────────
    private void buildTopPanel() {
        JPanel top = new JPanel(new GridLayout(0, 8, 6, 6));
        top.add(new JLabel("연"));   top.add(year);
        top.add(new JLabel("월"));   top.add(month);
        top.add(new JLabel("일"));   top.add(day);

        top.add(new JLabel("이름검색"));
        top.add(tfSearch);

        top.add(new JLabel("사번/이름"));
        top.add(cbEmployee);

        top.add(new JLabel("출근")); top.add(tfIn);
        top.add(new JLabel("퇴근")); top.add(tfOut);

        top.add(new JLabel());     top.add(btnSave);

        add(top, BorderLayout.NORTH);
    }

    private void buildDateSelectors() {
        LocalDate today = LocalDate.now();

        for (int y = today.getYear() - 3; y <= today.getYear() + 1; y++) year.addItem(y);
        for (int m = 1; m <= 12; m++) month.addItem(m);

        Runnable refreshDays = () -> {
            Integer y = (Integer) year.getSelectedItem();
            Integer m = (Integer) month.getSelectedItem();
            if (y == null || m == null) return;
            YearMonth ym = YearMonth.of(y, m);
            day.removeAllItems();
            for (int d = 1; d <= ym.lengthOfMonth(); d++) day.addItem(d);
        };

        year.addActionListener(e -> refreshDays.run());
        month.addActionListener(e -> refreshDays.run());

        year.setSelectedItem(today.getYear());
        month.setSelectedItem(today.getMonthValue());
        refreshDays.run();
        day.setSelectedItem(today.getDayOfMonth());
    }

    private void buildEmployeeSelectors() {
        // 콤보 렌더러: (사번) 이름
        cbEmployee.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof EmployeeItem it) setText("(" + it.id + ") " + it.name);
                return this;
            }
        });

        // 초기 전체 로드
        reloadEmployees("");

        // 이름 검색 실시간 반영
        tfSearch.getDocument().addDocumentListener(new DocumentListener() {
            private void changed() { reloadEmployees(tfSearch.getText()); }
            @Override public void insertUpdate(DocumentEvent e) { changed(); }
            @Override public void removeUpdate(DocumentEvent e) { changed(); }
            @Override public void changedUpdate(DocumentEvent e) { changed(); }
        });
    }

    private void reloadEmployees(String query) {
        empModel.removeAllElements();
        List<Employee> list = (query == null || query.isBlank())
                ? boot.employees().list()
                : boot.employees().searchByName(query);

        for (Employee e : list) {
            empModel.addElement(new EmployeeItem(e.id(), e.name()));
        }
        if (empModel.getSize() > 0) cbEmployee.setSelectedIndex(0);
    }

    // ─────────────────────────────────────────────────────────────────────
    private void onSave() {
        // 날짜 안전 추출
        Integer y = (Integer) year.getSelectedItem();
        Integer m = (Integer) month.getSelectedItem();
        Integer d = (Integer) day.getSelectedItem();
        if (y == null || m == null || d == null) {
            JOptionPane.showMessageDialog(this, "날짜를 선택하세요.");
            return;
        }
        LocalDate date = LocalDate.of(y, m, d);

        // 직원 선택 확인
        EmployeeItem it = (EmployeeItem) cbEmployee.getSelectedItem();
        if (it == null || it.id == null) {
            JOptionPane.showMessageDialog(this, "사번/이름을 선택하세요.");
            return;
        }

        // 시간 파싱
        LocalTime tin, tout;
        try {
            tin  = LocalTime.parse(tfIn.getText().trim());
            tout = LocalTime.parse(tfOut.getText().trim());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "시간 형식이 올바르지 않습니다. (예: 09:00)");
            return;
        }

        // ✅ 자정 넘김(다음날 퇴근) 확인
        if (tout.isBefore(tin)) {
            int ans = JOptionPane.showConfirmDialog(
                    this,
                    "퇴근 시간이 출근보다 이릅니다.\n다음날 퇴근으로 처리할까요?\n"
                            + "(" + tin + " → " + tout + ", 총 " + prettyDuration(tin, tout) + ")",
                    "다음날 퇴근 확인",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            if (ans != JOptionPane.YES_OPTION) return;
            // 저장은 그대로 진행(출근일=선택 날짜, 퇴근만 다음날로 해석)
            // 총 근무시간 계산은 목록/합계 로직(durationMinutes)에서 처리됩니다.
        } else if (tout.equals(tin)) {
            JOptionPane.showMessageDialog(this, "출근·퇴근 시간이 동일할 수 없습니다.");
            return;
        }

        try {
            AttendanceRecord r = new AttendanceRecord(null, it.id, date, tin, tout, null);
            r = boot.attendance().upsert(r);
            JOptionPane.showMessageDialog(this, "저장됨 id=" + r.id());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 자정 넘김을 감안해 총 분(minute) 계산
    private static int durationMinutes(LocalTime in, LocalTime out) {
        int a = in.getHour() * 60 + in.getMinute();
        int b = out.getHour() * 60 + out.getMinute();
        int diff = b - a;
        if (diff < 0) diff += 24 * 60; // 다음날 퇴근
        return diff;
    }

    private static String prettyDuration(LocalTime in, LocalTime out) {
        int minutes = durationMinutes(in, out);
        int h = minutes / 60, m = minutes % 60;
        return String.format("%02d:%02d", h, m);
    }

    // 콤보 내부 표현용
    private static class EmployeeItem {
        final Long id; final String name;
        EmployeeItem(Long id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; } // 에디터 영역엔 이름만
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EmployeeItem other)) return false;
            return Objects.equals(id, other.id);
        }
        @Override public int hashCode() { return Objects.hashCode(id); }
    }
}

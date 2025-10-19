package com.maemong.attendance.ui.components;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * 연/월/일 콤보 3개를 묶은 간단한 날짜 선택기.
 * - 말일 자동 조정
 * - "오늘" 버튼 옵션 제공
 * - getDate() / setDate() 메서드로 LocalDate 입출력
 */
public final class DatePicker3 extends JPanel {
    private final JComboBox<Integer> cbYear  = new JComboBox<>();
    private final JComboBox<Integer> cbMonth = new JComboBox<>();
    private final JComboBox<Integer> cbDay   = new JComboBox<>();
    private final JButton btnToday;

    public DatePicker3(boolean showTodayButton) {
        super(new FlowLayout(FlowLayout.LEFT, 6, 0));

        LocalDate today = LocalDate.now();

        // 연/월 초기 세팅
        for (int y = today.getYear() - 3; y <= today.getYear() + 1; y++) cbYear.addItem(y);
        for (int m = 1; m <= 12; m++) cbMonth.addItem(m);

        // 말일 조정 로직
        Runnable refreshDays = () -> {
            Integer y = (Integer) cbYear.getSelectedItem();
            Integer m = (Integer) cbMonth.getSelectedItem();
            if (y == null || m == null) return;
            YearMonth ym = YearMonth.of(y, m);
            int selectedDay = cbDay.getSelectedItem() == null ? 1 : (Integer) cbDay.getSelectedItem();
            cbDay.removeAllItems();
            for (int d = 1; d <= ym.lengthOfMonth(); d++) cbDay.addItem(d);
            // 말일 넘어가는 경우 자동 보정
            if (selectedDay > ym.lengthOfMonth()) selectedDay = ym.lengthOfMonth();
            cbDay.setSelectedItem(selectedDay);
        };

        cbYear.addActionListener(e -> refreshDays.run());
        cbMonth.addActionListener(e -> refreshDays.run());

        // 초기값
        cbYear.setSelectedItem(today.getYear());
        cbMonth.setSelectedItem(today.getMonthValue());
        refreshDays.run();
        cbDay.setSelectedItem(today.getDayOfMonth());

        // "오늘" 버튼
        if (showTodayButton) {
            btnToday = new JButton("오늘");
            btnToday.setToolTipText("오늘 날짜로 설정");
            btnToday.addActionListener(e -> setDate(LocalDate.now()));
        } else {
            btnToday = null;
        }

        add(new JLabel("연"));   add(cbYear);
        add(new JLabel("월"));   add(cbMonth);
        add(new JLabel("일"));   add(cbDay);
        if (btnToday != null) add(btnToday);
    }

    /** 현재 선택된 날짜를 반환 */
    public LocalDate getDate() {
        Integer y = (Integer) cbYear.getSelectedItem();
        Integer m = (Integer) cbMonth.getSelectedItem();
        Integer d = (Integer) cbDay.getSelectedItem();
        if (y == null || m == null || d == null) return null;
        return LocalDate.of(y, m, d);
    }

    /** 지정된 날짜를 선택 상태로 세팅 */
    public void setDate(LocalDate date) {
        cbYear.setSelectedItem(date.getYear());
        cbMonth.setSelectedItem(date.getMonthValue());
        // 월이 바뀌면 refreshDays 호출되므로 이후에 일 선택
        cbDay.setSelectedItem(date.getDayOfMonth());
    }

    /** UI 접근자 (필요 시 외부에서 개별 콤보 참조 가능) */
    public JComboBox<Integer> getYearBox()  { return cbYear; }
    public JComboBox<Integer> getMonthBox() { return cbMonth; }
    public JComboBox<Integer> getDayBox()   { return cbDay; }
}

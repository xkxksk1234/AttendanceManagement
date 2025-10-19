package com.maemong.attendance.ui.records;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

public final class QueryBar {
    private final JLabel lbEmp   = new JLabel("사번");
    private final JTextField tfEmpId = new JTextField();
    private final JLabel lbYear  = new JLabel("연");
    private final JComboBox<Integer> cbYear = new JComboBox<>();
    private final JLabel lbMonth = new JLabel("월");
    private final JComboBox<Integer> cbMonth = new JComboBox<>();
    private final JButton btnQuery = new JButton("조회");
    private final JLabel lbDayFrom = new JLabel("일(From)");
    private final JComboBox<Integer> cbDayFrom = new JComboBox<>();
    private final JLabel lbDayTo   = new JLabel("To");
    private final JComboBox<Integer> cbDayTo   = new JComboBox<>();

    private int prevYear;
    private int prevMonth;

    public QueryBar() {
        // 연/월 바인딩 + 기본값
        int nowY = LocalDate.now().getYear();
        for (int y = nowY - 3; y <= nowY + 1; y++) cbYear.addItem(y);
        for (int m = 1; m <= 12; m++) cbMonth.addItem(m);
        cbYear.setSelectedItem(nowY);
        cbMonth.setSelectedItem(LocalDate.now().getMonthValue());

        prevYear  = getSelectedYear();
        prevMonth = getSelectedMonth();

        refreshDayCombos();
        setDefaultDays();

        cbYear.addActionListener(e -> rebuildDaysPreserveSelectionWithMonthCarry());
        cbMonth.addActionListener(e -> rebuildDaysPreserveSelectionWithMonthCarry());
    }

    private void rebuildDaysPreserveSelectionWithMonthCarry() {
        // 변경 전 선택/달 정보 백업
        Integer prevFromSel = (Integer) cbDayFrom.getSelectedItem();
        Integer prevToSel   = (Integer) cbDayTo.getSelectedItem();
        if (prevFromSel == null) prevFromSel = 1;
        if (prevToSel   == null) prevToSel   = lengthOfMonth(prevYear, prevMonth); // 이전 달 말일 기본

        int oldLast = lengthOfMonth(prevYear, prevMonth);

        // 콤보를 "신 달" 기준으로 재생성
        refreshDayCombos();
        int newLast = lengthOfSelectedMonth();

        // From: 기존 선택 유지(범위 밖이면 클램프)
        int newFrom = Math.min(Math.max(prevFromSel, 1), newLast);

        // To:
        // 1) 이전 To가 "이전 달의 말일"이었다면 → 신 달의 말일로 자동 확장
        // 2) 아니면 기존 숫자 유지(범위 밖이면 클램프)
        int newTo = (prevToSel == oldLast) ? newLast
                : Math.min(Math.max(prevToSel, 1), newLast);

        // From ≤ To 보장
        if (newFrom > newTo) newTo = newFrom;

        cbDayFrom.setSelectedItem(newFrom);
        cbDayTo.setSelectedItem(newTo);

        // 마지막으로 "이전 달" 기록 갱신
        prevYear  = getSelectedYear();
        prevMonth = getSelectedMonth();
    }

    // 유틸: 특정 연/월의 말일
    private int lengthOfMonth(int y, int m) {
        return LocalDate.of(y, m, 1).lengthOfMonth();
    }

    /** GridLayout(1,0,...)에 바로 넣을 수 있도록 컴포넌트들을 순서대로 반환 */
    public Component[] components() {
        return new Component[]{ lbEmp, tfEmpId, lbYear, cbYear, lbMonth, cbMonth, lbDayFrom, cbDayFrom, lbDayTo, cbDayTo, btnQuery };
    }

    /** 조회 버튼을 눌렀을 때 실행할 콜백 연결 */
    public void onQuery(Runnable r) {
        btnQuery.addActionListener(e -> { if (r != null) r.run(); });
    }

    public Integer getSelectedYear()   { return (Integer) cbYear.getSelectedItem(); }
    public Integer getSelectedMonth()  { return (Integer) cbMonth.getSelectedItem(); }
    public String  getEmpIdText()      { return tfEmpId.getText().trim(); }

    public int getDayFrom() {
        Integer v = (Integer) cbDayFrom.getSelectedItem();
        return v == null ? 1 : v;
    }

    public int getDayTo() {
        Integer v = (Integer) cbDayTo.getSelectedItem();
        int last = lengthOfSelectedMonth();
        return v == null ? last : v;
    }
    private int lengthOfSelectedMonth() {
        Integer y = getSelectedYear();
        Integer m = getSelectedMonth();
        if (y == null || m == null) return 31;
        return LocalDate.of(y, m, 1).lengthOfMonth();
    }

    private void refreshDayCombos() {
        cbDayFrom.removeAllItems();
        cbDayTo.removeAllItems();
        int last = lengthOfSelectedMonth();
        for (int d = 1; d <= last; d++) {
            cbDayFrom.addItem(d);
            cbDayTo.addItem(d);
        }
    }

    private void setDefaultDays() {
        cbDayFrom.setSelectedItem(1);
        cbDayTo.setSelectedItem(lengthOfSelectedMonth());
    }
}

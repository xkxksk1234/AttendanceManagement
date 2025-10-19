package com.maemong.attendance.ui.records;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.Objects;

public final class QueryBar {
    private final JLabel lbEmp   = new JLabel("사번");
    private final JTextField tfEmpId = new JTextField();
    private final JLabel lbYear  = new JLabel("연");
    private final JComboBox<Integer> cbYear = new JComboBox<>();
    private final JLabel lbMonth = new JLabel("월");
    private final JComboBox<Integer> cbMonth = new JComboBox<>();
    private final JButton btnQuery = new JButton("조회");

    public QueryBar() {
        // 연/월 바인딩 + 기본값
        int nowY = LocalDate.now().getYear();
        for (int y = nowY - 3; y <= nowY + 1; y++) cbYear.addItem(y);
        for (int m = 1; m <= 12; m++) cbMonth.addItem(m);
        cbYear.setSelectedItem(nowY);
        cbMonth.setSelectedItem(LocalDate.now().getMonthValue());
    }

    /** GridLayout(1,0,...)에 바로 넣을 수 있도록 컴포넌트들을 순서대로 반환 */
    public Component[] components() {
        return new Component[]{ lbEmp, tfEmpId, lbYear, cbYear, lbMonth, cbMonth, btnQuery };
    }

    /** 조회 버튼을 눌렀을 때 실행할 콜백 연결 */
    public void onQuery(Runnable r) {
        btnQuery.addActionListener(e -> { if (r != null) r.run(); });
    }

    public Integer getSelectedYear()   { return (Integer) cbYear.getSelectedItem(); }
    public Integer getSelectedMonth()  { return (Integer) cbMonth.getSelectedItem(); }
    public String  getEmpIdText()      { return tfEmpId.getText().trim(); }

    /** 필요 시 외부에서 기본값 세팅 */
    public void setYearMonth(int year, int month) {
        cbYear.setSelectedItem(year);
        cbMonth.setSelectedItem(month);
    }

    /** 외부에서 사번 입력값 세팅 */
    public void setEmpIdText(String s) {
        tfEmpId.setText(Objects.requireNonNullElse(s, ""));
    }

    /** 사번 입력 필드에 포커스 주고 싶을 때 */
    public void focusEmpId() { tfEmpId.requestFocusInWindow(); }
}

package com.maemong.attendance.ui.records;

import com.maemong.attendance.domain.Employee;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public final class SummaryDialog {
    private SummaryDialog(){}

    public static void show(java.awt.Component parent,
                            TableModel model,
                            java.util.function.IntFunction<String> minutesToHHmm, // ex) i -> String.format(...)
                            List<Employee> employees) {
        // 이름 캐시
        Map<Long,String> nameMap = employees.stream()
                .collect(Collectors.toMap(Employee::id, Employee::name));

        // 사번별 합계/날짜별 합계 집계
        Map<Long,Integer> byEmp = new LinkedHashMap<>();
        Map<LocalDate,Integer> byDate = new LinkedHashMap<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            Object d  = model.getValueAt(i, 1);
            Object in = model.getValueAt(i, 4);
            Object out= model.getValueAt(i, 5);
            Object emp= model.getValueAt(i, 2);
            if (!(d instanceof LocalDate) || !(in instanceof String) || !(out instanceof String) || !(emp instanceof Number))
                continue;
            String is = (String) in, os = (String) out;
            if (is.isBlank() || os.isBlank()) continue;
            try {
                LocalTime tin = LocalTime.parse(is);
                LocalTime tout= LocalTime.parse(os);
                int minutes = (int)java.time.Duration.between(tin, tout.isBefore(tin)? tout.plusHours(24): tout).toMinutes();
                byEmp.merge(((Number)emp).longValue(), minutes, Integer::sum);
                byDate.merge((LocalDate)d, minutes, Integer::sum);
            } catch (Exception ignore) {}
        }

        // 테이블 모델 구성
        String[] empCols = {"사번","이름","합계"};
        Object[][] empData = byEmp.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new Object[]{e.getKey(), nameMap.getOrDefault(e.getKey(), ""), minutesToHHmm.apply(e.getValue())})
                .toArray(Object[][]::new);

        String[] dateCols = {"날짜","합계"};
        Object[][] dateData = byDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new Object[]{e.getKey(), minutesToHHmm.apply(e.getValue())})
                .toArray(Object[][]::new);

        JTable empTable = new JTable(empData, empCols);
        JTable dayTable = new JTable(dateData, dateCols);

        empTable.setDefaultEditor(Object.class, null);
        dayTable.setDefaultEditor(Object.class, null);

        // 기본 정렬(사번/날짜 오름차순) + 트라이스테이트
        TableUtils.installTriStateSort(empTable, List.of(new javax.swing.RowSorter.SortKey(0, javax.swing.SortOrder.ASCENDING)));
        TableUtils.installTriStateSort(dayTable, List.of(new javax.swing.RowSorter.SortKey(0, javax.swing.SortOrder.ASCENDING)));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("사번별 합계", new JScrollPane(empTable));
        tabs.addTab("날짜별 합계", new JScrollPane(dayTable));

        JOptionPane.showMessageDialog(parent, tabs, "월별 요약", JOptionPane.PLAIN_MESSAGE);
    }
}

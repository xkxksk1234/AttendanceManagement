package com.maemong.attendance.ui.export;

import javax.swing.*;
import java.io.File;
import java.nio.charset.StandardCharsets;

public final class CsvSelectionExporter {

    private CsvSelectionExporter() {}

    /** 선택된 셀(뷰 인덱스 기준)을 CSV로 저장. 선택이 없으면 안내 */
    public static void saveSelectionAsCSV(JTable table, boolean includeHeader) {
        int[] rows = table.getSelectedRows();
        int[] cols = table.getSelectedColumns();
        if (rows == null || cols == null || rows.length == 0 || cols.length == 0) {
            JOptionPane.showMessageDialog(table, "CSV로 저장할 셀(또는 행)을 선택하세요.");
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("선택 영역을 CSV로 저장");
        fc.setSelectedFile(new File("selection.csv"));
        if (fc.showSaveDialog(table) != JFileChooser.APPROVE_OPTION) return;

        File file = fc.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getParentFile(), file.getName() + ".csv");
        }

        try (var w = java.nio.file.Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            // UTF-8 BOM (한글 Excel 호환)
            w.write('\uFEFF');

            // 헤더
            if (includeHeader) {
                for (int ci = 0; ci < cols.length; ci++) {
                    int vc = cols[ci];
                    w.write(csvEscape(table.getColumnName(vc)));
                    if (ci < cols.length - 1) w.write(',');
                }
                w.write("\r\n");
            }

            // 데이터 (뷰 인덱스 기준: 화면 그대로)
            boolean firstRow = true;
            for (int vr : rows) {
                if (!firstRow) w.write('\n');
                firstRow = false;

                for (int i = 0; i < cols.length; i++) {
                    int vc = cols[i];
                    Object val = table.getValueAt(vr, vc);
                    w.write(csvEscape(val == null ? "" : val.toString()));
                    if (i < cols.length - 1) w.write(',');
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(table, "CSV 저장 실패: " + ex.getMessage(),
                    "오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(table, "CSV 저장 완료: " + file.getAbsolutePath());
    }

    // RFC 스타일 이스케이프: 콤마/쌍따옴표/개행 포함 시 "..." 및 내부 " -> ""
    private static String csvEscape(String s) {
        if (s == null) return "";
        boolean needQuote = s.contains(",") || s.contains("\"") || s.contains("\r") || s.contains("\n");
        if (!needQuote) return s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}

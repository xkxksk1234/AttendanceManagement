package com.maemong.attendance.ui.export;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

import javax.swing.*;
import java.awt.Component;
import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.*;

public final class XlsxExporter {

    private XlsxExporter() {}

    public static void exportFile(Component parent,
                                  javax.swing.table.DefaultTableModel model,
                                  Map<Long, Set<LocalDate>> multiKeys) {
        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(parent, "내보낼 데이터가 없습니다.");
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("XLSX 파일로 내보내기");
        String ts = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        fc.setSelectedFile(new File("출퇴근기록_" + ts + ".xlsx"));
        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;

        File file = fc.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".xlsx")) {
            file = new File(file.getParentFile(), file.getName() + ".xlsx");
        }

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sh = wb.createSheet("records");

            // 헤더 스타일
            CellStyle head = wb.createCellStyle();
            XSSFFont headFont = wb.createFont(); // XSSFFont
            headFont.setBold(true);
            head.setFont(headFont);
            head.setAlignment(HorizontalAlignment.CENTER);

            // 날짜/시간 스타일
            CreationHelper ch = wb.getCreationHelper();
            CellStyle dateStyle = wb.createCellStyle();
            dateStyle.setDataFormat(ch.createDataFormat().getFormat("yyyy-mm-dd"));
            CellStyle timeStyle = wb.createCellStyle();
            timeStyle.setDataFormat(ch.createDataFormat().getFormat("hh:mm"));

            // 헤더 작성
            String[] headers = {"ID", "날짜", "사번", "이름", "출근", "퇴근", "메모"};
            Row header = sh.createRow(0);
            for (int c = 0; c < headers.length; c++) {
                Cell cell = header.createCell(c);
                cell.setCellValue(headers[c]);
                cell.setCellStyle(head);
            }

            XSSFCellStyle hl = wb.createCellStyle();
            hl.setFillForegroundColor(new XSSFColor(new Color(255, 245, 200), null));
            hl.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            int r = 1;
            for (int i = 0; i < model.getRowCount(); i++) {
                Row row = sh.createRow(r++);
                Object dateObjM = model.getValueAt(i, 1);
                Object empObjM  = model.getValueAt(i, 2);
                boolean isMulti = false;
                if (dateObjM instanceof LocalDate d && empObjM instanceof Number n) {
                    Set<LocalDate> set = multiKeys.get(n.longValue());
                    isMulti = set != null && set.contains(d);
                }

                for (int c = 0; c < headers.length; c++) {
                    Cell cell = row.createCell(c);
                    Object val = model.getValueAt(i, c);
                    if (val == null) continue;

                    if (c == 1 && val instanceof LocalDate ld) {
                        cell.setCellValue(java.sql.Date.valueOf(ld));
                        cell.setCellStyle(dateStyle);
                    } else if ((c == 4 || c == 5) && val instanceof String s && !s.isBlank()) {
                        putTimeCell(cell, s, timeStyle);
                    } else if (val instanceof Number n) {
                        cell.setCellValue(n.doubleValue());
                    } else {
                        cell.setCellValue(String.valueOf(val));
                    }

                    if (isMulti) {
                        XSSFCellStyle merged = wb.createCellStyle();
                        merged.cloneStyleFrom(cell.getCellStyle());
                        merged.setFillForegroundColor(hl.getFillForegroundXSSFColor());
                        merged.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                        cell.setCellStyle(merged);
                    }
                }
            }

            for (int c = 0; c < headers.length; c++) sh.autoSizeColumn(c);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
            JOptionPane.showMessageDialog(parent, "XLSX 내보내기 완료: " + file.getAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "내보내기 실패: " + ex.getMessage(),
                    "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** "HH:mm" 혹은 "H:m" 문자열을 Excel 시간 셀로 기록하고 스타일 적용 */
    private static void putTimeCell(Cell cell, Object val, CellStyle timeStyle) {
        if (val == null) return;
        String s = String.valueOf(val).trim();
        if (s.isEmpty()) return;
        try {
            java.time.LocalTime t;
            if (s.matches("\\d{1,2}:\\d{1,2}")) {
                String[] p = s.split(":");
                t = java.time.LocalTime.of(Integer.parseInt(p[0]), Integer.parseInt(p[1]));
            } else {
                t = java.time.LocalTime.parse(s, java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            }
            double fraction = (t.getHour()*3600.0 + t.getMinute()*60.0 + t.getSecond()) / 86400.0;
            cell.setCellValue(fraction);
            cell.setCellStyle(timeStyle);
        } catch (Exception e) {
            cell.setCellValue(s); // 파싱 실패 시 텍스트로 보관
        }
    }
}
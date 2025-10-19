package com.maemong.attendance.ui.export;

import com.maemong.attendance.bootstrap.Bootstrap;
import com.maemong.attendance.domain.AttendanceRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class XlsxImporter {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter ISO_TIME = DateTimeFormatter.ofPattern("HH:mm");

    private XlsxImporter() {}

    public static void importFile(Component parent, Bootstrap boot) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("XLSX 파일 가져오기");
        if (fc.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return;

        File file = fc.getSelectedFile();
        int ok = 0, fail = 0;

        try (FileInputStream fis = new FileInputStream(file);
             XSSFWorkbook wb = new XSSFWorkbook(fis)) {

            Sheet sh = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
            if (sh == null) {
                JOptionPane.showMessageDialog(parent, "빈 파일입니다.");
                return;
            }

            Map<String, Integer> idx = readHeaderIndexes(sh.getRow(sh.getFirstRowNum()));
            Integer cDate = firstMatch(idx, "날짜", "date");
            Integer cEmp  = firstMatch(idx, "사번", "employee_id", "employeeid");
            Integer cIn   = firstMatch(idx, "출근", "clock_in", "clockin");
            Integer cOut  = firstMatch(idx, "퇴근", "clock_out", "clockout");
            Integer cMemo = firstMatch(idx, "메모", "memo");

            if (cDate == null || cEmp == null) {
                JOptionPane.showMessageDialog(parent, "필수 컬럼이 없습니다: 날짜, 사번");
                return;
            }

            for (int r = sh.getFirstRowNum() + 1; r <= sh.getLastRowNum(); r++) {
                Row row = sh.getRow(r);
                if (row == null) continue;

                try {
                    LocalDate date = readLocalDate(cellAt(row, cDate));
                    if (date == null) throw new IllegalArgumentException("날짜 누락");

                    String empStr = getCellString(cellAt(row, cEmp));
                    if (empStr == null || empStr.isBlank()) throw new IllegalArgumentException("사번 누락");
                    long empId = Long.parseLong(empStr.replaceAll("\\D+", ""));

                    LocalTime tin = readLocalTime(cellAt(row, cIn));
                    LocalTime tout = readLocalTime(cellAt(row, cOut));
                    String memo = getCellString(cellAt(row, cMemo));

                    AttendanceRecord rec = new AttendanceRecord(null, empId, date, tin, tout, memo);
                    boot.attendance().upsert(rec);
                    ok++;
                } catch (Exception ex) {
                    fail++;
                    System.err.println("row " + (r + 1) + ": " + ex.getMessage());
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "가져오기 실패: " + ex.getMessage(),
                    "오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(parent, "가져오기 완료: 성공 " + ok + "건, 실패 " + fail + "건");
    }

    // ===== 내부 유틸 =====
    private static Cell cellAt(Row row, Integer idx) {
        return (row == null || idx == null) ? null : row.getCell(idx);
    }

    private static Map<String, Integer> readHeaderIndexes(Row head) {
        Map<String, Integer> idx = new HashMap<>();
        if (head == null) return idx;
        for (int c = head.getFirstCellNum(); c < head.getLastCellNum(); c++) {
            Cell cell = head.getCell(c);
            if (cell == null) continue;
            String key = getCellString(cell);
            if (key != null) idx.put(key.trim().toLowerCase(), c);
        }
        return idx;
    }

    private static Integer firstMatch(Map<String, Integer> map, String... keys) {
        for (String k : keys) {
            Integer idx = map.get(k.toLowerCase());
            if (idx != null) return idx;
        }
        return null;
    }

    private static String getCellString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) yield null;
                double d = cell.getNumericCellValue();
                long l = (long) d;
                yield (d == l) ? String.valueOf(l) : String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield cell.getStringCellValue(); }
                catch (Exception e) { yield null; }
            }
            default -> null;
        };
    }

    private static LocalDate readLocalDate(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        }
        String s = getCellString(cell);
        if (s == null || s.isBlank()) return null;
        return LocalDate.parse(s.strip(), ISO_DATE);
    }

    private static LocalTime readLocalTime(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            double v = cell.getNumericCellValue(); // 0.5 = 12:00
            int seconds = (int) Math.round(v * 86400);
            return LocalTime.ofSecondOfDay(seconds);
        }
        String s = getCellString(cell);
        if (s == null || s.isBlank()) return null;
        s = s.trim();
        if (s.matches("\\d{1,2}:\\d{1,2}")) {
            String[] p = s.split(":");
            return LocalTime.of(Integer.parseInt(p[0]), Integer.parseInt(p[1]));
        }
        return LocalTime.parse(s, ISO_TIME);
    }
}
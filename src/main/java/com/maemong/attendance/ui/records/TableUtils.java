package com.maemong.attendance.ui.records;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/** JTable 관련 공용 유틸 */
public final class TableUtils {

    private TableUtils() {}

    /** 헤더 3회 클릭: ASC → DESC → 기본정렬(restore) */
    public static TableRowSorter<TableModel> installTriStateSort(JTable table, List<SortKey> defaultKeys) {
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel()) {
            @Override public void toggleSortOrder(int column) {
                List<? extends SortKey> keys = getSortKeys();
                SortOrder cur = null;
                if (keys != null && !keys.isEmpty() && keys.get(0).getColumn() == column) {
                    cur = keys.get(0).getSortOrder();
                }
                if (cur == null || cur == SortOrder.UNSORTED) {
                    setSortKeys(List.of(new SortKey(column, SortOrder.ASCENDING)));
                } else if (cur == SortOrder.ASCENDING) {
                    setSortKeys(List.of(new SortKey(column, SortOrder.DESCENDING)));
                } else {
                    // 세 번째 클릭 → 기본 정렬 복귀
                    setSortKeys(defaultKeys);
                }
            }
        };
        sorter.setSortKeys(defaultKeys);
        table.setRowSorter(sorter);
        return sorter;
    }

    /** "H:m" / "HH:mm" 모두 안전하게 비교하는 Comparator (빈 값은 항상 뒤로) */
    public static final Comparator<String> TIME_COMPARATOR = new Comparator<>() {
        private final Pattern HM = Pattern.compile("\\d{1,2}:\\d{1,2}");
        private final DateTimeFormatter ISO_TIME = DateTimeFormatter.ofPattern("HH:mm");

        @Override public int compare(String a, String b) {
            boolean ea = (a == null || a.isBlank());
            boolean eb = (b == null || b.isBlank());
            if (ea && eb) return 0;
            if (ea) return 1;
            if (eb) return -1;
            try {
                String sa = a.trim(), sb = b.trim();
                LocalTime ta = HM.matcher(sa).matches()
                        ? LocalTime.of(Integer.parseInt(sa.split(":")[0]), Integer.parseInt(sa.split(":")[1]))
                        : LocalTime.parse(sa, ISO_TIME);
                LocalTime tb = HM.matcher(sb).matches()
                        ? LocalTime.of(Integer.parseInt(sb.split(":")[0]), Integer.parseInt(sb.split(":")[1]))
                        : LocalTime.parse(sb, ISO_TIME);
                return ta.compareTo(tb);
            } catch (Exception ignore) {
                // 파싱 실패 시 문자열 비교
                return a.compareTo(b);
            }
        }
    };
}

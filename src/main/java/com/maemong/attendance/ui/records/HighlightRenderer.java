package com.maemong.attendance.ui.records;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

public class HighlightRenderer extends DefaultTableCellRenderer {

    private final JTable table;
    private final Map<Long, Set<LocalDate>> multiKeys;
    private final Color hoverBg;
    private final Color multiBg = new Color(255, 245, 200);

    public HighlightRenderer(JTable table,
                             Map<Long, Set<LocalDate>> multiKeys,
                             Color hoverBg) {
        this.table = table;
        this.multiKeys = multiKeys;
        this.hoverBg = hoverBg;
    }

    @Override
    public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);

        // 선택된 셀은 기본 선택색 유지
        if (isSelected) {
            c.setBackground(tbl.getSelectionBackground());
            if (c instanceof JComponent jc) jc.setToolTipText(null);
            return c;
        }

        // 현재 호버 행 (HoverHighlighter가 clientProperty에 넣어줌)
        Integer hoverRow = (Integer) table.getClientProperty("hoverRow");

        // 모델 값 기준 멀티키 여부
        boolean isMulti = false;
        int mr = tbl.convertRowIndexToModel(row);
        Object eidObj  = tbl.getModel().getValueAt(mr, 2);
        Object dateObj = tbl.getModel().getValueAt(mr, 1);
        if (eidObj instanceof Number && dateObj instanceof LocalDate d) {
            long eid = ((Number) eidObj).longValue();
            Set<LocalDate> set = multiKeys.get(eid);
            isMulti = (set != null && set.contains(d));
        }

        // 우선순위: Hover > Multi > White
        if (hoverRow != null && hoverRow == row) {
            c.setBackground(hoverBg);
            if (c instanceof JComponent jc) jc.setToolTipText(null);
        } else if (isMulti) {
            c.setBackground(multiBg);
            if (c instanceof JComponent jc) jc.setToolTipText("같은 날 다중 근무");
        } else {
            c.setBackground(Color.WHITE);
            if (c instanceof JComponent jc) jc.setToolTipText(null);
        }

        return c;
    }
}

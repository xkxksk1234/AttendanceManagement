package com.maemong.attendance.ui.records;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

public class RecordsTable extends JTable {
    private final Map<Long, Set<LocalDate>> multiKeys;

    public RecordsTable(DefaultTableModel model,
                        Map<Long, Set<LocalDate>> multiKeys,
                        Color hoverBg) {
        super(model);
        this.multiKeys = multiKeys;

        // 렌더러 & 호버 설치 (기존 클래스를 내부에서 사용)
        HighlightRenderer hr = new HighlightRenderer(this, multiKeys, hoverBg);
        setDefaultRenderer(Object.class,  hr);
        setDefaultRenderer(Number.class,  hr);
        setDefaultRenderer(Long.class,    hr);
        setDefaultRenderer(Integer.class, hr);

        HoverHighlighter.install(this);
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    @Override public String getToolTipText(MouseEvent e) {
        int vr = rowAtPoint(e.getPoint());
        int vc = columnAtPoint(e.getPoint());
        if (vr < 0 || vc < 0) return null;
        int mr = convertRowIndexToModel(vr);
        Object emp  = getModel().getValueAt(mr, 2);
        Object date = getModel().getValueAt(mr, 1);
        if (emp instanceof Number n && date instanceof java.time.LocalDate d) {
            java.util.Set<java.time.LocalDate> set = multiKeys.get(n.longValue());
            if (set != null && set.contains(d)) return "같은 날 다중 근무";
        }
        return null;
    }
}

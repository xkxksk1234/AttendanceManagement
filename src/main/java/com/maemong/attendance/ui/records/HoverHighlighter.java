package com.maemong.attendance.ui.records;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** 마우스 호버 행을 추적해서 테이블에 clientProperty("hoverRow")로 저장하고 필요한 부분만 리페인트 */
public final class HoverHighlighter {

    private HoverHighlighter() {}

    public static void install(JTable table) {
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int r = table.rowAtPoint(e.getPoint());
                Integer old = (Integer) table.getClientProperty("hoverRow");
                if (old == null) old = -1;
                if (r != old) {
                    table.putClientProperty("hoverRow", r);
                    repaintRow(table, old);
                    repaintRow(table, r);
                }
            }
            @Override public void mouseExited(MouseEvent e) {
                Integer old = (Integer) table.getClientProperty("hoverRow");
                if (old == null || old == -1) return;
                table.putClientProperty("hoverRow", -1);
                repaintRow(table, old);
            }
        };
        table.addMouseMotionListener(ma);
        table.addMouseListener(ma);
    }

    private static void repaintRow(JTable table, int row) {
        if (row < 0) return;
        Rectangle rect = table.getCellRect(row, 0, true);
        rect.width = table.getWidth();
        table.repaint(rect);
    }
}

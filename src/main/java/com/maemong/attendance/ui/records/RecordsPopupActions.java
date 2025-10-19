package com.maemong.attendance.ui.records;

import com.maemong.attendance.ui.export.CsvSelectionExporter;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.function.Consumer;

public final class RecordsPopupActions {

    private RecordsPopupActions() {}

    /**
     * 조회 테이블의 우클릭 메뉴 및 단축키를 설치한다.
     * @param table 대상 JTable
     * @param onOpenEditorModelRow 선택된 '뷰 행'을 모델 행 인덱스로 변환하여 콜백 (없으면 무시)
     * @param onDeleteSelected     삭제 콜백 (없으면 무시)
     */
    public static void install(JTable table,
                               Consumer<Integer> onOpenEditorModelRow,
                               Runnable onDeleteSelected) {

        // --- Actions ---
        Action actOpen = new AbstractAction("열기/편집") {
            @Override public void actionPerformed(ActionEvent e) {
                int vr = table.getSelectedRow();
                if (vr < 0) return;
                int mr = table.convertRowIndexToModel(vr);
                if (onOpenEditorModelRow != null) onOpenEditorModelRow.accept(mr);
            }
        };

        Action actDelete = new AbstractAction("선택 삭제") {
            @Override public void actionPerformed(ActionEvent e) {
                if (onDeleteSelected != null) onDeleteSelected.run();
            }
        };

        Action actCopy = new AbstractAction("선택 영역 복사") {
            @Override public void actionPerformed(ActionEvent e) {
                copySelectedCellsToClipboard(table, false);
            }
        };

        Action actCopyWithHead = new AbstractAction("헤더 포함 복사") {
            @Override public void actionPerformed(ActionEvent e) {
                copySelectedCellsToClipboard(table, true);
            }
        };

        Action actCopyRowFull = new AbstractAction("선택 행 전체 복사") {
            @Override public void actionPerformed(ActionEvent e) {
                copyWholeSelectedRowsToClipboard(table, false);
            }
        };

        Action actSaveCSV = new AbstractAction("선택 영역 CSV로 저장…") {
            @Override public void actionPerformed(ActionEvent e) {
                CsvSelectionExporter.saveSelectionAsCSV(table, true);
            }
        };

        // --- 단축키 ---
        int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(); // Win=Ctrl, mac=Cmd
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke('C', menuMask), "copyCells");
        table.getActionMap().put("copyCells", actCopy);

        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuMask | InputEvent.SHIFT_DOWN_MASK), "copyWithHeader");
        table.getActionMap().put("copyWithHeader", actCopyWithHead);

        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_R, menuMask), "copyRowFull");
        table.getActionMap().put("copyRowFull", actCopyRowFull);

        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_S, menuMask), "saveSelCsv");
        table.getActionMap().put("saveSelCsv", actSaveCSV);

        // --- Popup ---
        JPopupMenu popup = new JPopupMenu();
        popup.add(new JMenuItem(actOpen));
        popup.add(new JMenuItem(actDelete));
        popup.addSeparator();
        popup.add(new JMenuItem(actCopy));
        JMenuItem miCopyHead = new JMenuItem(actCopyWithHead);
        popup.add(miCopyHead);
        popup.add(new JMenuItem(actCopyRowFull));
        popup.addSeparator();
        popup.add(new JMenuItem(actSaveCSV));

        // 표시되는 메뉴의 단축키 라벨(가시성)
        miCopyHead.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuMask | InputEvent.SHIFT_DOWN_MASK));

        // 우클릭 시 선택 이동 + 팝업 표시
        MouseAdapter ma = new MouseAdapter() {
            private void showIfPopup(MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row >= 0 && col >= 0) {
                    if (!table.isRowSelected(row) || !table.isColumnSelected(col)) {
                        table.setRowSelectionInterval(row, row);
                        table.setColumnSelectionInterval(col, col);
                    }
                }
                refreshEnable(popup, table, actOpen, actDelete, actCopy, actCopyWithHead, actCopyRowFull, actSaveCSV);
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
            @Override public void mousePressed(MouseEvent e) { showIfPopup(e); }
            @Override public void mouseReleased(MouseEvent e) { showIfPopup(e); }
        };
        table.addMouseListener(ma);
    }

    private static void refreshEnable(JPopupMenu popup, JTable table, Action... acts) {
        boolean hasSelCells = table.getSelectedRowCount() > 0 && table.getSelectedColumnCount() > 0;
        boolean hasSelRows  = table.getSelectedRowCount() > 0;

        // 순서: open, delete, copy, copyWithHead, copyRowFull, saveCSV
        acts[0].setEnabled(hasSelRows);
        acts[1].setEnabled(hasSelRows);
        acts[2].setEnabled(hasSelCells);
        acts[3].setEnabled(hasSelCells);
        acts[4].setEnabled(hasSelRows);
        acts[5].setEnabled(hasSelCells);
    }

    /** 선택된 셀들을 탭/줄바꿈으로 복사 (옵션: 헤더 포함) */
    private static void copySelectedCellsToClipboard(JTable table, boolean includeHeader) {
        int[] rows = table.getSelectedRows();
        int[] cols = table.getSelectedColumns();
        if (rows == null || cols == null || rows.length == 0 || cols.length == 0) return;

        StringBuilder sb = new StringBuilder();

        // 헤더(보이는 컬럼명)
        if (includeHeader) {
            for (int vc : cols) {
                String name = table.getColumnName(vc);
                sb.append(name == null ? "" : name).append('\t');
            }
            if (!sb.isEmpty()) sb.setLength(sb.length() - 1);
            sb.append('\n');
        }

        // 데이터(뷰 인덱스 기준)
        boolean firstRow = true;
        for (int vr : rows) {
            if (!firstRow) sb.append('\n');
            firstRow = false;
            for (int i = 0; i < cols.length; i++) {
                int vc = cols[i];
                Object val = table.getValueAt(vr, vc);
                sb.append(val == null ? "" : val.toString());
                if (i < cols.length - 1) sb.append('\t');
            }
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.toString()), null);
    }

    /** 선택된 전체 행(보이는 모든 컬럼)을 탭/줄바꿈으로 복사 */
    private static void copyWholeSelectedRowsToClipboard(JTable table, boolean includeHeader) {
        int[] rows = table.getSelectedRows();
        if (rows == null || rows.length == 0) return;

        int colCount = table.getColumnCount();
        StringBuilder sb = new StringBuilder();

        if (includeHeader) {
            for (int vc = 0; vc < colCount; vc++) {
                String name = table.getColumnName(vc);
                sb.append(name == null ? "" : name);
                if (vc < colCount - 1) sb.append('\t');
            }
            sb.append('\n');
        }

        for (int ri = 0; ri < rows.length; ri++) {
            int vr = rows[ri];
            for (int vc = 0; vc < colCount; vc++) {
                Object val = table.getValueAt(vr, vc);
                sb.append(val == null ? "" : val.toString());
                if (vc < colCount - 1) sb.append('\t');
            }
            if (ri < rows.length - 1) sb.append('\n');
        }

        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.toString()), null);
    }
}

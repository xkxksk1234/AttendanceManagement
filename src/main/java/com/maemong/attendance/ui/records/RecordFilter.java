package com.maemong.attendance.ui.records;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class RecordFilter {
    private final JLabel lb = new JLabel("키워드");
    private final JTextField tf = new JTextField(14);
    private final JButton btnClear = new JButton("초기화");

    private TableRowSorter<TableModel> sorter;
    private JTable table;
    private Runnable afterApply;
    private int[] columns;

    public RecordFilter() {
        // Enter=적용
        tf.addActionListener(e -> applyNow());

        // ESC=초기화 (키워드 필드 포커스 시)
        tf.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearFilter");
        tf.getActionMap().put("clearFilter", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                tf.setText("");
                applyNow();
            }
        });

        // 입력 즉시 반영
        tf.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyNow(); }
            @Override public void removeUpdate(DocumentEvent e) { applyNow(); }
            @Override public void changedUpdate(DocumentEvent e) { applyNow(); }
        });

        // 버튼 클릭 = 초기화
        btnClear.addActionListener(e -> {
            tf.setText("");
            applyNow();
        });
    }

    /** sorter/table/콜백/검색대상 컬럼을 연결. sorter가 준비된 뒤 한 번만 호출하세요. */
    public void attach(TableRowSorter<TableModel> sorter, JTable table, Runnable afterApply, int... columns) {
        this.sorter = sorter;
        this.table = table;
        this.afterApply = afterApply;
        this.columns = columns.clone();
        applyNow(); // 초기 적용(빈 필터)
    }

    /** 상단 패널에 붙일 때 각각 꺼내 쓰기 (GridLayout에 맞추기 좋음) */
    public JLabel label() { return lb; }
    public JTextField field() { return tf; }
    public JButton clearButton() { return btnClear; }

    /** 테이블이 포커스일 때 ESC로도 초기화하고 싶으면 이 메서드를 호출 */
    public void bindEscWhenTableFocused(JTable table) {
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearFilterGlobal");
        table.getActionMap().put("clearFilterGlobal", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if (!tf.getText().isEmpty()) {
                    tf.setText("");
                    applyNow();
                }
            }
        });
    }

    private void applyNow() {
        if (sorter == null) return;
        String kw = tf.getText().trim();
        if (kw.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            String pat = "(?i)" + Pattern.quote(kw);
            List<RowFilter<TableModel,Object>> or = new ArrayList<>();
            for (int col : columns) or.add(RowFilter.regexFilter(pat, col));
            sorter.setRowFilter(RowFilter.orFilter(or));
        }
        if (afterApply != null) afterApply.run(); // 보이는 행 기준 합계 갱신 등
    }
}

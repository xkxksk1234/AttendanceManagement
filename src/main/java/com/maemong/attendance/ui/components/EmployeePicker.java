package com.maemong.attendance.ui.components;

import com.maemong.attendance.bootstrap.Bootstrap;
import com.maemong.attendance.domain.Employee;
import com.maemong.attendance.ui.model.EmployeeItem;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;

public final class EmployeePicker extends JPanel {
    private final Bootstrap boot;

    private final JTextField tfSearch = new JTextField();
    private final DefaultComboBoxModel<EmployeeItem> model = new DefaultComboBoxModel<>();
    private final JComboBox<EmployeeItem> combo = new JComboBox<>(model);

    public EmployeePicker(Bootstrap boot) {
        super(new GridBagLayout());
        this.boot = boot;

        combo.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof EmployeeItem it) setText("(" + it.id + ") " + it.name);
                return this;
            }
        });

        // 레이아웃
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 6);
        c.weightx = 0; c.fill = GridBagConstraints.HORIZONTAL;
        add(new JLabel("이름검색"), c);

        c.weightx = 1; c.gridx = 1;
        add(tfSearch, c);

        c.insets = new Insets(0, 12, 0, 6);
        c.weightx = 0; c.gridx = 2;
        add(new JLabel("사번/이름"), c);

        c.weightx = 1; c.gridx = 3;
        add(combo, c);

        // 초기 로드 & 검색 반영
        reload("");

        tfSearch.getDocument().addDocumentListener(new DocumentListener() {
            private void changed() { reload(tfSearch.getText()); }
            @Override public void insertUpdate(DocumentEvent e) { changed(); }
            @Override public void removeUpdate(DocumentEvent e) { changed(); }
            @Override public void changedUpdate(DocumentEvent e) { changed(); }
        });
    }

    private void reload(String q) {
        model.removeAllElements();
        List<Employee> list = (q == null || q.isBlank())
                ? boot.employees().list()
                : boot.employees().searchByName(q);
        for (Employee e : list) model.addElement(new EmployeeItem(e.id(), e.name()));
        if (model.getSize() > 0) combo.setSelectedIndex(0);
    }

    public EmployeeItem getSelected() {
        return (EmployeeItem) combo.getSelectedItem();
    }

    public void focusSearch() {
        tfSearch.requestFocusInWindow();
        tfSearch.selectAll();
    }
}

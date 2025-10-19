package com.maemong.attendance.ui.panels;

import com.maemong.attendance.bootstrap.Bootstrap;
import com.maemong.attendance.domain.Employee;
import com.maemong.attendance.domain.enums.Rank;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class PanelEmployees extends JPanel {

    private final Bootstrap boot;

    // 폼 구성요소
    private final JTextField tfName = new JTextField();
    private final JComboBox<Rank> cbRank = new JComboBox<>(Rank.values());
    private final JTextField tfPhone = new JTextField();
    private final JButton btnSave = new JButton("저장");
    private final JButton btnReset = new JButton("초기화");

    // 목록 구성요소
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "이름", "직급", "전화"}, 0
    ) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JButton btnRefresh = new JButton("새로고침");
    private final JButton btnDelete = new JButton("삭제");

    // 현재 편집 중인 직원 ID (null이면 신규)
    private Long editingId = null;

	public PanelEmployees(Bootstrap boot) {
        this.boot = boot;

		setLayout(new BorderLayout(8, 8));

        // ── 상단: 입력 폼 ───────────────────────────────────────────────
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));

        // 직급 콤보 렌더러(한글 라벨)
        cbRank.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Rank r) setText(r.label());
                return this;
            }
        });
        cbRank.setSelectedItem(Rank.PART_TIME);

        form.add(new JLabel("이름"));  form.add(tfName);
        form.add(new JLabel("직급"));  form.add(cbRank);
        form.add(new JLabel("전화"));  form.add(tfPhone);

        JPanel formButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        formButtons.add(btnReset);
        formButtons.add(btnSave);
        form.add(new JLabel());
        form.add(formButtons);

        add(form, BorderLayout.NORTH);

        // ── 중앙: 테이블 ────────────────────────────────────────────────
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(60);  // ID
        table.getColumnModel().getColumn(1).setPreferredWidth(180); // 이름
        table.getColumnModel().getColumn(2).setPreferredWidth(120); // 직급
        table.getColumnModel().getColumn(3).setPreferredWidth(160); // 전화
        add(new JScrollPane(table), BorderLayout.CENTER);

        // ── 하단: 목록 버튼들 ────────────────────────────────────────────
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        bottom.add(btnRefresh);
        bottom.add(btnDelete);
        add(bottom, BorderLayout.SOUTH);

        // ── 이벤트: 저장/초기화/새로고침/삭제/행 선택 ───────────────────
        btnSave.addActionListener(e -> onSave());
        btnReset.addActionListener(e -> clearForm());
        btnRefresh.addActionListener(e -> refreshTable());
        btnDelete.addActionListener(e -> onDelete());

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onRowSelected();
        });

        // 최초 로딩
        refreshTable();
	}

    private void onSave() {
        String name = tfName.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "이름을 입력하세요.");
            tfName.requestFocus();
            return;
        }
        Rank rank = (Rank) cbRank.getSelectedItem();
        String phone = tfPhone.getText().trim();

        try {
            Employee saved = boot.employees().upsert(new Employee(
                    editingId,
                    name,
                    rank,
                    null,           // rrn
                    phone,
                    0,              // hourlyWage (폼에 아직 없으므로 0)
                    null, null, null,
                    null,           // contractDate
                    null            // note
            ));
            JOptionPane.showMessageDialog(this,
                    (editingId == null ? "등록 완료: " : "수정 완료: ") + "id=" + saved.id());
            clearForm();
            refreshTable();
            // 저장 후 방금 저장한 row로 selection 맞추기(선택)
            selectRowById(saved.id());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onDelete() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "삭제할 항목을 선택하세요.");
            return;
        }
        Long id = (Long) model.getValueAt(row, 0);
        int ans = JOptionPane.showConfirmDialog(this,
                "정말 삭제하시겠습니까? (id=" + id + ")", "확인",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ans != JOptionPane.YES_OPTION) return;

        boolean ok = boot.employees().remove(id);
        if (ok) {
            refreshTable();
            clearForm();
        } else {
            JOptionPane.showMessageDialog(this, "삭제 실패(이미 삭제되었거나 존재하지 않습니다).",
                    "알림", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void onRowSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        // 테이블 선택 → 폼에 로드하여 수정 모드로 전환
        editingId = (Long) model.getValueAt(row, 0);
        tfName.setText((String) model.getValueAt(row, 1));
        // 직급 라벨 → enum 역매핑
        String rankLabel = (String) model.getValueAt(row, 2);
        Rank found = null;
        for (Rank r : Rank.values()) {
            if (r.label().equals(rankLabel)) { found = r; break; }
        }
        cbRank.setSelectedItem(found != null ? found : Rank.OTHER);
        tfPhone.setText((String) model.getValueAt(row, 3));
        btnSave.setText("수정");
    }

    private void clearForm() {
        editingId = null;
        tfName.setText("");
        tfPhone.setText("");
        cbRank.setSelectedItem(Rank.PART_TIME);
        table.clearSelection();
        btnSave.setText("저장");
    }

    private void refreshTable() {
        model.setRowCount(0);
        List<Employee> list = boot.employees().list();
        for (Employee e : list) {
            model.addRow(new Object[]{
                    e.id(),
                    e.name(),
                    e.rank() == null ? "" : e.rank().label(),
                    e.phone() == null ? "" : e.phone()
            });
        }
    }

    private void selectRowById(Long id) {
        if (id == null) return;
        for (int r = 0; r < model.getRowCount(); r++) {
            Long rid = (Long) model.getValueAt(r, 0);
            if (id.equals(rid)) {
                table.setRowSelectionInterval(r, r);
                table.scrollRectToVisible(table.getCellRect(r, 0, true));
                break;
            }
        }
    }
}
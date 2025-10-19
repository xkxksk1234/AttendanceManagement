package com.maemong.attendance.ui.panels;

import com.maemong.attendance.bootstrap.Bootstrap;
import com.maemong.attendance.domain.AttendanceRecord;
import com.maemong.attendance.domain.Employee;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.Color;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

import com.maemong.attendance.ui.export.XlsxExporter;
import com.maemong.attendance.ui.export.XlsxImporter;
import com.maemong.attendance.ui.records.*;

public class PanelRecords extends JPanel {
    private final Bootstrap boot;
    // 같은 날 다중 근무 키(사번+날짜) 모음. 이 키에 해당하는 행은 하이라이트.
    private final Map<Long, Set<LocalDate>> multiKeys = new HashMap<>();
    private static final DateTimeFormatter ISO_TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final Color HOVER_BG = new Color(240, 240, 240);

    // 컬럼 인덱스 상수 (모델 헤더: ID, 날짜, 사번, 이름, 출근, 퇴근, 메모)
    private static final int COL_EMP  = 2;
    private static final int COL_NAME = 3;
    private static final int COL_MEMO = 6;

    private final RecordFilter recordFilter = new RecordFilter();

    // 상단 컨트롤
    private final QueryBar queryBar = new QueryBar();
    private final JButton btnDelete = new JButton("선택 삭제");
    private final JButton btnImport = new JButton("엑셀(XLSX) 가져오기");
    private final JButton btnExport = new JButton("엑셀(XLSX) 내보내기");
    private final JButton btnSummary = new JButton("요약 보기");
    private final JLabel lbTotal = new JLabel("총 근무시간: 00:00");

    private TableRowSorter<TableModel> sorter;
    private List<RowSorter.SortKey> baseSortKeys;

    /** 헤더 3회 클릭 시 기본정렬로 돌아가는 트라이-스테이트 정렬 설치 */
    private static TableRowSorter<TableModel> installTriStateSort(JTable t, List<RowSorter.SortKey> defaultKeys) {
        // 기본 동작과 충돌하지 않도록: MouseListener를 쓰지 않고 toggleSortOrder를 커스터마이즈
        TableRowSorter<TableModel> s = new TableRowSorter<>(t.getModel()) {
            @Override public void toggleSortOrder(int column) {
                List<? extends RowSorter.SortKey> keys = getSortKeys();
                SortOrder cur = null;
                if (keys != null && !keys.isEmpty() && keys.getFirst().getColumn() == column) {
                    cur = keys.getFirst().getSortOrder();
                }
                if (cur == null || cur == SortOrder.UNSORTED) {
                    setSortKeys(List.of(new RowSorter.SortKey(column, SortOrder.ASCENDING)));
                } else if (cur == SortOrder.ASCENDING) {
                    setSortKeys(List.of(new RowSorter.SortKey(column, SortOrder.DESCENDING)));
                } else {
                    setSortKeys(defaultKeys); // 🔁 3번째 클릭: 기본 정렬 복귀
                }
            }
        };

        // 시작 시 기본 정렬 적용
        s.setSortKeys(defaultKeys);
        t.setRowSorter(s);
        return s;
    }

    // 테이블
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "날짜", "사번", "이름", "출근", "퇴근", "메모"}, 0
    ) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
        @Override public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 0, 2 -> Long.class;   // ID, 사번
                case 1 -> LocalDate.class; // 날짜
                case 4, 5 -> String.class; // 시간 문자열(가독성 위해)
                default -> String.class;
            };
        }
    };

    private final JTable table = new RecordsTable(model, multiKeys, HOVER_BG);

    public PanelRecords(Bootstrap boot) {
        this.boot = boot;

        setLayout(new BorderLayout(8, 8));
        buildTop();
        buildTable();
        buildBottom();
        doQuery();
    }

    private void buildTop() {
        JPanel top = new JPanel(new GridLayout(1, 0, 6, 6));

        // QueryBar 컴포넌트들을 순서대로 배치
        for (Component c : queryBar.components()) top.add(c);

        add(top, BorderLayout.NORTH);

        // FilterBar UI만 추가 (이미 분리한 필터바)
        top.add(recordFilter.label());
        top.add(recordFilter.field());
        top.add(recordFilter.clearButton());

        // 조회 버튼 콜백 연결
        queryBar.onQuery(this::doQuery);
    }

    private void buildTable() {
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setCellSelectionEnabled(true); // ✅ 셀/사각형 선택 켜기

        // ID 컬럼은 숨김(삭제용으로만 사용)
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setPreferredWidth(0);

        // 컬럼 폭 약간 조정
        table.getColumnModel().getColumn(1).setPreferredWidth(100); // 날짜
        table.getColumnModel().getColumn(2).setPreferredWidth(70);  // 사번
        table.getColumnModel().getColumn(3).setPreferredWidth(160); // 이름
        table.getColumnModel().getColumn(4).setPreferredWidth(70);  // 출근
        table.getColumnModel().getColumn(5).setPreferredWidth(70);  // 퇴근
        table.getColumnModel().getColumn(6).setPreferredWidth(240); // 메모

        // 하이라이트 렌더러 설치 (Object/Number/Long/Integer 모두 동일 렌더러)
        HighlightRenderer hr = new HighlightRenderer(table, multiKeys, HOVER_BG);
        table.setDefaultRenderer(Object.class, hr);
        table.setDefaultRenderer(Number.class, hr);
        table.setDefaultRenderer(Long.class, hr);
        table.setDefaultRenderer(Integer.class, hr);

        // 호버 하이라이트 리스너 설치
        HoverHighlighter.install(table);

        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int vr = table.rowAtPoint(e.getPoint());
                    if (vr < 0) return;
                    int mr = table.convertRowIndexToModel(vr);
                    openEditorForRow(mr); // ↓ 2번에서 추가할 메서드
                }
            }
        });

        baseSortKeys = List.of(
                new RowSorter.SortKey(1, SortOrder.ASCENDING),  // 날짜
                new RowSorter.SortKey(2, SortOrder.ASCENDING)   // 사번
        );

        sorter = installTriStateSort(table, baseSortKeys);
        sorter.setComparator(4, timeStringComparator); // 출근
        sorter.setComparator(5, timeStringComparator); // 퇴근

        // 키워드 필터 동작 연결: 이름, 메모, 사번 컬럼
        recordFilter.attach(sorter, table, this::updateTotalHours, COL_NAME, COL_MEMO, COL_EMP);

        // (선택) 테이블 포커스 상태에서도 ESC로 초기화
        recordFilter.bindEscWhenTableFocused(table);

        add(new JScrollPane(table), BorderLayout.CENTER);

        // 툴팁 동작시키기 위해 등록 (getToolTipText 오버라이드 방식)
        ToolTipManager.sharedInstance().registerComponent(table);

        RecordsPopupActions.install(
                table,
                this::openEditorForRow,
                this::onDeleteSelected
        );
    }

    private static final Comparator<String> timeStringComparator = (a, b) -> {
        // 빈 값은 항상 뒤로
        boolean ea = (a == null || a.isBlank());
        boolean eb = (b == null || b.isBlank());
        if (ea && eb) return 0;
        if (ea) return 1;
        if (eb) return -1;

        // "H:m" / "HH:mm" 모두 허용
        try {
            String sa = a.trim();
            String sb = b.trim();
            LocalTime ta = sa.matches("\\d{1,2}:\\d{1,2}") ?
                    LocalTime.of(Integer.parseInt(sa.split(":")[0]), Integer.parseInt(sa.split(":")[1])) :
                    LocalTime.parse(sa, ISO_TIME);
            LocalTime tb = sb.matches("\\d{1,2}:\\d{1,2}") ?
                    LocalTime.of(Integer.parseInt(sb.split(":")[0]), Integer.parseInt(sb.split(":")[1])) :
                    LocalTime.parse(sb, ISO_TIME);
            return ta.compareTo(tb);
        } catch (Exception ignore) {
            // 파싱 실패 시 문자열 비교로 폴백
            return a.compareTo(b);
        }
    };

    private void openEditorForRow(int mr) {
        // 모델 → AttendanceRecord로 변환
        Long id = (Long) model.getValueAt(mr, 0);
        LocalDate d = (LocalDate) model.getValueAt(mr, 1);
        Long empId = (Long) model.getValueAt(mr, 2);
        String in = (String) model.getValueAt(mr, 4);
        String out = (String) model.getValueAt(mr, 5);
        String memo = (String) model.getValueAt(mr, 6);

        LocalTime tin  = (in  == null || in.isBlank())  ? null : LocalTime.parse(in);
        LocalTime tout = (out == null || out.isBlank()) ? null : LocalTime.parse(out);

        AttendanceRecord current = new AttendanceRecord(id, empId, d, tin, tout, memo);

        Map<Long, String> nameCache = buildNameCache();

        EditorDialog.open(
                this,
                boot,
                current,
                nameCache,
                this::doQuery // 저장/삭제 후 테이블 갱신
        );
    }

    // 월 데이터 기준 사번별 총 근무 분
    private Map<Long, Integer> summarizeByEmployee() {
        Map<Long, Integer> byEmp = new LinkedHashMap<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            Long empId = (Long) model.getValueAt(i, 2);
            String inStr  = (String) model.getValueAt(i, 4);
            String outStr = (String) model.getValueAt(i, 5);
            if (empId == null || inStr == null || outStr == null || inStr.isBlank() || outStr.isBlank()) continue;
            try {
                LocalTime in  = LocalTime.parse(inStr);
                LocalTime out = LocalTime.parse(outStr);
                int min = durationMinutes(in, out);
                byEmp.merge(empId, min, Integer::sum);
            } catch (Exception ignore) {}
        }
        return byEmp;
    }

    // 월 데이터 기준 날짜별 총 근무 분
    private Map<LocalDate, Integer> summarizeByDate() {
        Map<LocalDate, Integer> byDate = new LinkedHashMap<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            LocalDate d = (LocalDate) model.getValueAt(i, 1);
            String inStr  = (String) model.getValueAt(i, 4);
            String outStr = (String) model.getValueAt(i, 5);
            if (d == null || inStr == null || outStr == null || inStr.isBlank() || outStr.isBlank()) continue;
            try {
                LocalTime in  = LocalTime.parse(inStr);
                LocalTime out = LocalTime.parse(outStr);
                int min = durationMinutes(in, out);
                byDate.merge(d, min, Integer::sum);
            } catch (Exception ignore) {}
        }
        return byDate;
    }

    private void buildBottom() {
        JPanel bottom = new JPanel(new BorderLayout());

        // 오른쪽: 버튼들
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        right.add(btnImport);
        right.add(btnExport);
        right.add(btnDelete);
        right.add(btnSummary);
        btnSummary.addActionListener(e -> onShowSummary());
        btnDelete.addActionListener(e -> onDeleteSelected());
        btnImport.addActionListener(e -> onImportXlsx());
        btnExport.addActionListener(e -> onExportXlsx());
        bottom.add(right, BorderLayout.EAST);

        // 왼쪽: 합계 라벨
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        left.add(lbTotal);
        bottom.add(left, BorderLayout.WEST);

        add(bottom, BorderLayout.SOUTH);
    }

    private String mm(int minutes) { return String.format("%02d:%02d", minutes/60, minutes%60); }

    private void onShowSummary() {
        SummaryDialog.show(this, model, this::mm, boot.employees().list());
    }


    private void onImportXlsx() {
        XlsxImporter.importFile(this, boot);
        doQuery();
    }

    private void onExportXlsx() {
        XlsxExporter.exportFile(this, model, multiKeys);
    }

    private void doQuery() {
        Integer y = queryBar.getSelectedYear();
        Integer m = queryBar.getSelectedMonth();
        if (y == null || m == null) {
            JOptionPane.showMessageDialog(this, "연/월을 선택하세요.");
            return;
        }
        YearMonth ym = YearMonth.of(y, m);

        // 데이터 로드
        List<AttendanceRecord> rows = boot.attendance().byMonth(ym);

        // 사번 필터
        String empIdText = queryBar.getEmpIdText();
        Long filterEmpId = null;
        if (!empIdText.isEmpty()) {
            try {
                filterEmpId = Long.parseLong(empIdText);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "사번은 숫자만 입력하세요.");
                return;
            }
        }

        // ===== (여기) 같은 날 다중 근무 키 계산 =====
        multiKeys.clear();
        {
            // 먼저 이번 조회에서 사용할 레코드만 필터링
            java.util.List<AttendanceRecord> filtered = new java.util.ArrayList<>();
            for (AttendanceRecord r : rows) {
                if (filterEmpId != null && !filterEmpId.equals(r.employeeId())) continue;
                filtered.add(r);
            }
            // (사번, 날짜)별 카운트
            java.util.Map<Long, java.util.Map<java.time.LocalDate, Integer>> cnt = new java.util.HashMap<>();
            for (AttendanceRecord r : filtered) {
                cnt.computeIfAbsent(r.employeeId(), k -> new java.util.HashMap<>())
                        .merge(r.workDate(), 1, Integer::sum);
            }
            // 2건 이상인 키만 multiKeys에 저장
            for (var e : cnt.entrySet()) {
                long eid = e.getKey();
                for (var e2 : e.getValue().entrySet()) {
                    if (e2.getValue() >= 2) {
                        multiKeys.computeIfAbsent(eid, k -> new java.util.HashSet<>()).add(e2.getKey());
                    }
                }
            }
        }

        // 이름 캐시(한 번만 조회)
        Map<Long, String> nameCache = buildNameCache();

        model.setRowCount(0);
        for (AttendanceRecord r : rows) {
            if (filterEmpId != null && !filterEmpId.equals(r.employeeId())) continue;
            String name = nameCache.getOrDefault(r.employeeId(), "");
            String inStr  = formatTime(r.clockIn());
            String outStr = formatTime(r.clockOut());
            model.addRow(new Object[]{ r.id(), r.workDate(), r.employeeId(), name, inStr, outStr, r.memo()==null?"":r.memo() });
        }

        updateTotalHours();
        table.repaint();

        // 기본 정렬 복구(데이터 새로 채운 뒤)
        if (sorter != null && baseSortKeys != null) sorter.setSortKeys(baseSortKeys);
    }

    private void updateTotalHours() {
        int minutes = 0;

        // 소터가 있으면 '보이는 행(view)' 기준
        if (table.getRowSorter() != null) {
            for (int v = 0; v < table.getRowCount(); v++) {
                int m = table.convertRowIndexToModel(v);
                String inStr  = (String) model.getValueAt(m, 4);
                String outStr = (String) model.getValueAt(m, 5);
                if (inStr == null || outStr == null || inStr.isBlank() || outStr.isBlank()) continue;
                try {
                    minutes += durationMinutes(LocalTime.parse(inStr), LocalTime.parse(outStr));
                } catch (Exception ignore) {}
            }
        } else {
            // 폴백: 모델 전체
            for (int i = 0; i < model.getRowCount(); i++) {
                String inStr  = (String) model.getValueAt(i, 4);
                String outStr = (String) model.getValueAt(i, 5);
                if (inStr == null || outStr == null || inStr.isBlank() || outStr.isBlank()) continue;
                try {
                    minutes += durationMinutes(LocalTime.parse(inStr), LocalTime.parse(outStr));
                } catch (Exception ignore) {}
            }
        }

        String total = String.format("%02d:%02d", minutes/60, minutes%60);
        String suffix = "";
        String empIdText = queryBar.getEmpIdText();
        if (!empIdText.isBlank()) suffix = " (사번 " + empIdText + ")";
        lbTotal.setText("총 근무시간: " + total + suffix);
    }

    // PanelRecords 안에 헬퍼 추가(Attendance와 동일 로직)
    private static int durationMinutes(LocalTime in, LocalTime out) {
        int a = in.getHour()*60 + in.getMinute();
        int b = out.getHour()*60 + out.getMinute();
        int diff = b - a;
        if (diff < 0) diff += 24*60;
        return diff;
    }

    private Map<Long, String> buildNameCache() {
        Map<Long, String> map = new HashMap<>();
        for (Employee e : boot.employees().list()) {
            map.put(e.id(), e.name());
        }
        return map;
    }

    private static String formatTime(LocalTime t) {
        return t == null ? "" : t.toString(); // 필요시 "HH:mm" 포맷터로 변경 가능
    }

    private void onDeleteSelected() {
        int[] viewRows = table.getSelectedRows();
        if (viewRows.length == 0) {
            JOptionPane.showMessageDialog(this, "삭제할 항목을 선택하세요.");
            return;
        }
        int ans = JOptionPane.showConfirmDialog(
                this,
                "선택한 " + viewRows.length + "건을 삭제하시겠습니까?",
                "확인",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (ans != JOptionPane.YES_OPTION) return;

        // RowSorter 활성화 상태이므로, 모델 인덱스로 변환 후 ID 추출
        List<Long> ids = new ArrayList<>();
        for (int vr : viewRows) {
            int mr = table.convertRowIndexToModel(vr);
            Long id = (Long) model.getValueAt(mr, 0);
            if (id != null) ids.add(id);
        }

        int okCount = 0;
        for (Long id : ids) {
            try {
                if (boot.attendance().remove(id)) okCount++;
            } catch (Exception ignore) { /* 개별 실패 무시 후 일괄 요약 */ }
        }

        JOptionPane.showMessageDialog(this,
                okCount + "건 삭제 완료" + (okCount < ids.size() ? " (일부 실패)" : ""));

        doQuery(); // 삭제 후 재조회
    }
}

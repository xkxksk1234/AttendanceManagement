-- 직원 테이블
CREATE TABLE IF NOT EXISTS employees (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    rank TEXT,
    rrn TEXT, -- 주민번호(선택)
    phone TEXT,
    hourly_wage INTEGER DEFAULT 0,
    bank TEXT,
    account TEXT,
    address TEXT,
    contract_date TEXT,
    note TEXT,
    created_at TEXT DEFAULT (datetime('now')),
    updated_at TEXT
);


-- 근태 테이블
CREATE TABLE IF NOT EXISTS attendance (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    employee_id INTEGER NOT NULL,
    work_date TEXT NOT NULL, -- YYYY-MM-DD
    clock_in TEXT, -- HH:mm
    clock_out TEXT, -- HH:mm
    memo TEXT,
    created_at TEXT DEFAULT (datetime('now')),
    updated_at TEXT,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);


CREATE INDEX IF NOT EXISTS idx_attendance_emp_date ON attendance (employee_id, work_date);
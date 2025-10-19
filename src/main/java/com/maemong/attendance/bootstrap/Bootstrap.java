package com.maemong.attendance.bootstrap;

import com.maemong.attendance.config.AppConfig;
import com.maemong.attendance.db.DataSourceFactory;
import com.maemong.attendance.db.JdbiProvider;
import com.maemong.attendance.db.MigrationRunner;
import com.maemong.attendance.adapters.db.AttendanceRepositoryJdbi;
import com.maemong.attendance.adapters.db.EmployeeRepositoryJdbi;
import com.maemong.attendance.events.AppEvents;
import com.maemong.attendance.ports.AttendanceRepository;
import com.maemong.attendance.ports.EmployeeRepository;
import com.maemong.attendance.services.AttendanceService;
import com.maemong.attendance.services.EmployeeService;
import org.jdbi.v3.core.Jdbi;


import javax.sql.DataSource;

/**
 * Bootstrap: 애플리케이션 초기화(파일설정, DB 마이그레이션, Jdbi 초기화, 서비스 객체 생성)
 * - config()와 jdbi()는 외부에서 필요 시(테스트/디버깅/직접 DB 접근) 사용하도록 공개해 두었습니다.
 */
public class Bootstrap {
	private AppConfig config;
    private AppEvents events;
	private Jdbi jdbi;

	// Services
	private EmployeeService employeeService;
	private AttendanceService attendanceService;

    public void init() {
        this.config = ConfigLoader.load();
        this.events = new AppEvents();
        DataSource ds = DataSourceFactory.create(config);
        MigrationRunner.migrate(ds);
        this.jdbi = JdbiProvider.create(ds);

        EmployeeRepository empRepo = new EmployeeRepositoryJdbi(jdbi);
        AttendanceRepository attRepo = new AttendanceRepositoryJdbi(jdbi);

        this.employeeService = new EmployeeService(empRepo);
        this.attendanceService = new AttendanceService(attRepo, empRepo, false);
    }


	// 외부에서 참조 가능하도록 공개한 접근자들(현재 사용처가 없으면 IDE 경고 발생할 수 있음)
	@SuppressWarnings("unused")
	public AppConfig config() { return config; }

	@SuppressWarnings("unused")
	public Jdbi jdbi() { return jdbi; }

	public EmployeeService employees() { return employeeService; }
	public AttendanceService attendance() { return attendanceService; }

    public AppEvents events() { return events; }
}
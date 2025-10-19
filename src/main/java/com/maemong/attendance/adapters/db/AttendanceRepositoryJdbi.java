package com.maemong.attendance.adapters.db;

import com.maemong.attendance.domain.AttendanceRecord;
import com.maemong.attendance.ports.AttendanceRepository;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.util.*;

public class AttendanceRepositoryJdbi implements AttendanceRepository {
	private final Jdbi jdbi;
	public AttendanceRepositoryJdbi(Jdbi jdbi) { this.jdbi = jdbi; }

	private static final RowMapper<AttendanceRecord> MAPPER = (rs, ctx) -> mapRow(rs);

	private static AttendanceRecord mapRow(ResultSet rs) throws SQLException {
		return new AttendanceRecord(
				rs.getLong("id"),
				rs.getLong("employee_id"),
				LocalDate.parse(rs.getString("work_date")),
				rs.getString("clock_in") == null ? null : LocalTime.parse(rs.getString("clock_in")),
				rs.getString("clock_out") == null ? null : LocalTime.parse(rs.getString("clock_out")),
				rs.getString("memo")
		);
	}

	@Override public AttendanceRecord save(AttendanceRecord r) {
		if (r.id() == null) {
			long id = jdbi.withHandle(h -> h.createUpdate(
							"INSERT INTO attendance(employee_id,work_date,clock_in,clock_out,memo,updated_at) " +
									"VALUES (:eid,:date,:in,:out,:memo,datetime('now'))")
					.bind("eid", r.employeeId())
					.bind("date", r.workDate().toString())
					.bind("in", r.clockIn() == null ? null : r.clockIn().toString())
					.bind("out", r.clockOut() == null ? null : r.clockOut().toString())
					.bind("memo", r.memo())
					.executeAndReturnGeneratedKeys("id")
					.mapTo(Long.class).one());
			return new AttendanceRecord(id, r.employeeId(), r.workDate(), r.clockIn(), r.clockOut(), r.memo());
		} else {
			jdbi.useHandle(h -> h.createUpdate(
							"UPDATE attendance SET employee_id=:eid, work_date=:date, clock_in=:in, clock_out=:out, memo=:memo, updated_at=datetime('now') WHERE id=:id")
					.bind("id", r.id())
					.bind("eid", r.employeeId())
					.bind("date", r.workDate().toString())
					.bind("in", r.clockIn() == null ? null : r.clockIn().toString())
					.bind("out", r.clockOut() == null ? null : r.clockOut().toString())
					.bind("memo", r.memo())
					.execute());
			return r;
		}
	}

	@Override public Optional<AttendanceRecord> findById(long id) {
		return jdbi.withHandle(h -> h.createQuery("SELECT * FROM attendance WHERE id=:id")
				.bind("id", id)
				.map(MAPPER)
				.findOne());
	}

	@Override public List<AttendanceRecord> findByDate(LocalDate date) {
		return jdbi.withHandle(h -> h.createQuery("SELECT * FROM attendance WHERE work_date=:d ORDER BY employee_id")
				.bind("d", date.toString())
				.map(MAPPER)
				.list());
	}

	@Override public List<AttendanceRecord> findByMonth(YearMonth ym) {
		String prefix = ym.toString(); // e.g. 2025-10
		return jdbi.withHandle(h -> h.createQuery("SELECT * FROM attendance WHERE work_date LIKE :p || '%' ORDER BY work_date, employee_id")
				.bind("p", prefix)
				.map(MAPPER)
				.list());
	}

	@Override public List<AttendanceRecord> findByEmployeeAndRange(long employeeId, LocalDate from, LocalDate to) {
		return jdbi.withHandle(h -> h.createQuery("SELECT * FROM attendance WHERE employee_id=:eid AND work_date BETWEEN :f AND :t ORDER BY work_date")
				.bind("eid", employeeId)
				.bind("f", from.toString())
				.bind("t", to.toString())
				.map(MAPPER)
				.list());
	}

	@Override public boolean deleteById(long id) {
		int n = jdbi.withHandle(h -> h.createUpdate("DELETE FROM attendance WHERE id=:id").bind("id", id).execute());
		return n > 0;
	}
}
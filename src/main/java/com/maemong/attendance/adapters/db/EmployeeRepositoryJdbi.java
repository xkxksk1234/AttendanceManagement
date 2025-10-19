package com.maemong.attendance.adapters.db;


import com.maemong.attendance.domain.Employee;
import com.maemong.attendance.domain.enums.Rank;
import com.maemong.attendance.ports.EmployeeRepository;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class EmployeeRepositoryJdbi implements EmployeeRepository {
	private final Jdbi jdbi;
	public EmployeeRepositoryJdbi(Jdbi jdbi) { this.jdbi = jdbi; }

	private static final RowMapper<Employee> MAPPER = (rs, ctx) -> mapRow(rs);

	private static Employee mapRow(ResultSet rs) throws SQLException {
		// hourly_wage: NULL 허용
		int hourlyInt = rs.getInt("hourly_wage");
		Integer hourly = rs.wasNull() ? null : hourlyInt;

		String contractStr = rs.getString("contract_date");
		LocalDate contractDate = (contractStr == null || contractStr.isBlank())
				? null
				: LocalDate.parse(contractStr);

		// rank: DB TEXT ↔ enum
		String rankDb = rs.getString("rank");

		return new Employee(
				rs.getLong("id"),
				rs.getString("name"),
				Rank.fromDb(rankDb),
				rs.getString("rrn"),
				rs.getString("phone"),
				hourly,
				rs.getString("bank"),
				rs.getString("account"),
				rs.getString("address"),
				contractDate,
				rs.getString("note")
		);
	}


	@Override public Employee save(Employee e) {
		if (e.id() == null) {
			long id = jdbi.withHandle(h -> h.createUpdate(
							"INSERT INTO employees(name,rank,rrn,phone,hourly_wage,bank,account,address,contract_date,note,updated_at) " +
									"VALUES (:name,:rank,:rrn,:phone,:hourly,:bank,:account,:address,:contract,:note,datetime('now'))")
					.bind("name", e.name())
					.bind("rank", Rank.toDb(e.rank()))
					.bind("rrn", e.rrn())
					.bind("phone", e.phone())
					.bind("hourly", e.hourlyWage())
					.bind("bank", e.bank())
					.bind("account", e.account())
					.bind("address", e.address())
					.bind("contract", e.contractDate() == null ? null : e.contractDate().toString())
					.bind("note", e.note())
					.executeAndReturnGeneratedKeys("id")
					.mapTo(Long.class)
					.one());
			return new Employee(id, e.name(), e.rank(), e.rrn(), e.phone(), e.hourlyWage(), e.bank(), e.account(), e.address(), e.contractDate(), e.note());
		} else {
			jdbi.useHandle(h -> h.createUpdate(
							"UPDATE employees SET name=:name, rank=:rank, rrn=:rrn, phone=:phone, hourly_wage=:hourly, bank=:bank, account=:account, " +
									"address=:address, contract_date=:contract, note=:note, updated_at=datetime('now') WHERE id=:id")
					.bind("id", e.id())
					.bind("name", e.name())
					.bind("rank", Rank.toDb(e.rank()))
					.bind("rrn", e.rrn())
					.bind("phone", e.phone())
					.bind("hourly", e.hourlyWage())
					.bind("bank", e.bank())
					.bind("account", e.account())
					.bind("address", e.address())
					.bind("contract", e.contractDate() == null ? null : e.contractDate().toString())
					.bind("note", e.note())
					.execute());
			return e;
		}
	}


	@Override public Optional<Employee> findById(long id) {
		return jdbi.withHandle(h -> h.createQuery("SELECT * FROM employees WHERE id = :id")
				.bind("id", id)
				.map(MAPPER)
				.findOne());
	}


	@Override public List<Employee> findAll() {
		return jdbi.withHandle(h -> h.createQuery("SELECT * FROM employees ORDER BY id DESC")
				.map(MAPPER)
				.list());
	}


	@Override public List<Employee> searchByName(String nameLike) {
		return jdbi.withHandle(h -> h.createQuery("SELECT * FROM employees WHERE name LIKE :q ORDER BY name")
				.bind("q", "%" + nameLike + "%")
				.map(MAPPER)
				.list());
	}


	@Override public boolean deleteById(long id) {
		int n = jdbi.withHandle(h -> h.createUpdate("DELETE FROM employees WHERE id=:id").bind("id", id).execute());
		return n > 0;
	}
}
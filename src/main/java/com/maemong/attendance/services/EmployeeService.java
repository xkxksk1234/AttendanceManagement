package com.maemong.attendance.services;

import com.maemong.attendance.domain.Employee;
import com.maemong.attendance.ports.EmployeeRepository;

import java.util.List;
import java.util.Optional;

public class EmployeeService {
	private final EmployeeRepository repo;
	public EmployeeService(EmployeeRepository repo) { this.repo = repo; }

	public Employee upsert(Employee e) {
		String name = trimToNull(e.name());
		if (name == null) throw new IllegalArgumentException("이름은 비워둘 수 없습니다.");

		// 전화번호 간단 정규화: 숫자만 남김 (UI 표시 형식은 별도 처리)
		String phone = normalizePhone(e.phone());

		Employee sanitized = new Employee(
				e.id(),
				name,
				e.rank(),
				e.rrn(),
				phone,
				e.hourlyWage(),
				trim(e.bank()),
				trim(e.account()),
				trim(e.address()),
				e.contractDate(),
				trim(e.note())
		);
		return repo.save(sanitized);
	}

	@SuppressWarnings("unused")
	public Optional<Employee> find(long id) { return repo.findById(id); }
	@SuppressWarnings("unused")
	public List<Employee> list() { return repo.findAll(); }

	// 포트 메서드 노출: 이름 검색(콤보/오토컴플리트/필터에 사용 예정)
	@SuppressWarnings("unused")
	public List<Employee> searchByName(String q) { return repo.searchByName(q == null ? "" : q.trim()); }

	@SuppressWarnings("unused")
	public boolean remove(long id) { return repo.deleteById(id); }

	// ---- helpers ----
	private static String trim(String s) { return s == null ? null : s.trim(); }
	private static String trimToNull(String s) {
		if (s == null) return null;
		String t = s.trim();
		return t.isEmpty() ? null : t;
	}
	private static String normalizePhone(String s) {
		if (s == null) return null;
		String digits = s.replaceAll("\\D+", ""); // 숫자만
		return digits.isEmpty() ? null : digits;
	}
}

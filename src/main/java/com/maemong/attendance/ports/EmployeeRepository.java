package com.maemong.attendance.ports;

import com.maemong.attendance.domain.Employee;
import java.util.*;

public interface EmployeeRepository {
	Employee save(Employee e);
	Optional<Employee> findById(long id);
	List<Employee> findAll();
	@SuppressWarnings("unused")
	List<Employee> searchByName(String nameLike);
	boolean deleteById(long id);
}
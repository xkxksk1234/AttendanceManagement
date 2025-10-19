package com.maemong.attendance.ui.model;

import java.util.Objects;

public final class EmployeeItem {
    public final Long id;
    public final String name;
    public EmployeeItem(Long id, String name) {
        this.id = id; this.name = name;
    }
    @Override public String toString() { return name; } // 에디터엔 이름만
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmployeeItem other)) return false;
        return Objects.equals(id, other.id);
    }
    @Override public int hashCode() { return Objects.hashCode(id); }
}

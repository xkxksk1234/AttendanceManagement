package com.maemong.attendance.domain.enums;

public enum Rank {
	OWNER("경영주"),
	FULL_TIME("직원"),
	PART_TIME("아르바이트"),
	TRAINEE("수습직원"),
	OTHER("기타");

	private final String label;
	Rank(String label) { this.label = label; }
	public String label() { return label; }

	// DB TEXT ↔ enum 변환 유틸 (대소문자/공백/한글 라벨 대응)
	public static Rank fromDb(String s) {
		if (s == null || s.isBlank()) return null;
		String t = s.trim();
		// 한글 라벨 우선 매칭
		for (Rank r : values()) if (r.label.equals(t)) return r;
		// 영문 상수명도 허용
		try { return Rank.valueOf(t.toUpperCase().replace(' ', '_')); }
		catch (IllegalArgumentException ex) { return OTHER; }
	}

	public static String toDb(Rank r) {
		return r == null ? null : r.label(); // DB에는 한글 라벨로 저장
	}
}
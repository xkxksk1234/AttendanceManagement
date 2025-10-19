package com.maemong.attendance.util;


import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;

@SuppressWarnings("unused")
public class CsvUtil {
	public static void write(File file, List<String[]> rows) throws IOException {
		try (var w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
			for (String[] r: rows) w.write(String.join(",", r) + "\n");
		}
	}
}
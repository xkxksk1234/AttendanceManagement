package com.maemong.attendance.ui.components;

import javax.swing.*;

@SuppressWarnings("unused")
public class Placeholder extends JPanel {
	public Placeholder() { this("준비 중 화면"); }
	public Placeholder(String text) { add(new JLabel(text)); }
}

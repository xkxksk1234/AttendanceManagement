package com.maemong.attendance.common;

import javax.swing.*;import java.awt.*;

@SuppressWarnings("unused")
public class Ui {

	public static JPanel vstack(Component... comps) {
		JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		for (Component c: comps) { p.add(c); }
		return p;
	}

	public static JPanel hstack(Component... comps) {
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
		for (Component c : comps) p.add(c);
		return p;
	}
}
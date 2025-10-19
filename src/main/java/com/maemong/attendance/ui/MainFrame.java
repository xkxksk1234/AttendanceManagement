package com.maemong.attendance.ui;

import com.maemong.attendance.bootstrap.Bootstrap;
import com.maemong.attendance.ui.panels.PanelAttendance;
import com.maemong.attendance.ui.panels.PanelEmployees;
import com.maemong.attendance.ui.panels.PanelRecords;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
	public MainFrame(Bootstrap boot) {
		setTitle("근태관리");

        var images = new java.util.ArrayList<java.awt.Image>();
        for (int sz : new int[]{16, 32, 48, 128, 256}) {
            var url = MainFrame.class.getResource("/icons/attendance_" + sz + ".png");
            if (url != null) images.add(new ImageIcon(url).getImage());
        }
        if (!images.isEmpty()) setIconImages(images);

		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setSize(1100, 720);
		setLocationRelativeTo(null);


		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("직원관리", new PanelEmployees(boot));
		tabs.addTab("출퇴근", new PanelAttendance(boot));
		tabs.addTab("조회", new PanelRecords(boot));


		setLayout(new BorderLayout());
		add(tabs, BorderLayout.CENTER);
	}
}
package com.maemong.attendance.bootstrap;

import com.formdev.flatlaf.FlatLightLaf;
import com.maemong.attendance.ui.MainFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;


public class App {
	private static final Logger log = LoggerFactory.getLogger(App.class);


	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			try {
				FlatLightLaf.setup();
				Bootstrap boot = new Bootstrap();
				boot.init();
				new MainFrame(boot).setVisible(true);
			} catch (Exception e) {
				log.error("Failed to start application", e);
				JOptionPane.showMessageDialog(null, e.getMessage(), "Startup Error", JOptionPane.ERROR_MESSAGE);
				System.exit(1);
			}
		});
	}
}
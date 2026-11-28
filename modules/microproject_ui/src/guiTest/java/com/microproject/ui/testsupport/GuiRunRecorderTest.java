/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.testsupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.EventQueue;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The recorder reports an EDT stall, an off-EDT Swing change, an uncaught exception and a SEVERE log. */
class GuiRunRecorderTest {
	private JFrame frame;
	private JLabel label;

	@BeforeEach
	void showFrame() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			frame = new JFrame("recorder test");
			label = new JLabel("before");
			frame.add(label);
			frame.setSize(200, 100);
			frame.setVisible(true);
		});
	}

	@AfterEach
	void closeFrame() throws Exception {
		SwingUtilities.invokeAndWait(() -> frame.dispose());
	}

	@Test
	void edtStallIsRecordedOnceWithItsDump() throws Exception {
		GuiRunRecorder.Session recorder = GuiRunRecorder.current();
		EventQueue.invokeLater(() -> {
			try {
				Thread.sleep(800);
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
			}
		});
		GuiAcceptanceSupport.await(() -> recorder.events("edt-stall").stream()
			.anyMatch(event -> event.containsKey("durationMillis")), "the EDT stall was not recorded");
		List<Map<String, Object>> stalls = recorder.events("edt-stall");
		assertEquals(1, stalls.size());
		assertTrue(((Number) stalls.get(0).get("durationMillis")).longValue() >= 500L);
		assertTrue(Files.exists(recorder.directory().resolve("edt-stall-1.txt")));
	}

	@Test
	void swingChangeOffTheEdtIsRecorded() throws Exception {
		GuiRunRecorder.Session recorder = GuiRunRecorder.current();
		Thread thread = new Thread(() -> label.setText("changed"), "off-edt-writer");
		thread.start();
		thread.join();
		SwingUtilities.invokeAndWait(() -> { });
		assertEquals(1, recorder.events("edt-violation").size());
		assertEquals(1, recorder.count("edt-violation"));
	}

	@Test
	void plainRepaintOffTheEdtIsNotAViolation() throws Exception {
		Thread thread = new Thread(() -> label.repaint(), "off-edt-repaint");
		thread.start();
		thread.join();
		assertEquals(0, GuiRunRecorder.current().count("edt-violation"));
	}

	@Test
	void uncaughtExceptionIsRecorded() throws Exception {
		GuiRunRecorder.Session recorder = GuiRunRecorder.current();
		Thread thread = new Thread(() -> {
			throw new IllegalStateException("expected by GuiRunRecorderTest");
		}, "throwing-thread");
		thread.start();
		thread.join();
		assertEquals(1, recorder.events("uncaught").size());
		assertEquals(1, recorder.count("uncaught"));
		assertEquals(IllegalStateException.class.getName(), recorder.events("uncaught").get(0).get("exception"));
	}

	@Test
	void severeLogIsCountedOnce() {
		GuiRunRecorder.Session recorder = GuiRunRecorder.current();
		Logger.getLogger("com.microproject.runtime").severe("expected by GuiRunRecorderTest");
		assertEquals(1, recorder.count("log-severe"));
		assertEquals(0, recorder.count("log-warning"));
	}

	@Test
	void stepRecordsAGanttItem() {
		GuiRunRecorder.step("noop", Map.of("k", "v"));
		List<Map<String, Object>> steps = GuiRunRecorder.current().events("step");
		assertEquals(1, steps.size());
		assertTrue(steps.get(0).containsKey("gantt"));
	}
}

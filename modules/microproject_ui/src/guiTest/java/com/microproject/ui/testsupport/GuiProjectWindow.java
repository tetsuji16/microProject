/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.testsupport;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import com.microproject.job.JobQueue;
import com.microproject.model.task.Project;
import com.microproject.session.SessionFactory;
import com.microproject.ui.frame.GraphicManager;
import com.microproject.ui.frame.MainRibbonFrame;
import com.microproject.ui.ribbon.RibbonDisplayMode;
import com.microproject.ui.ribbon.RibbonDisplayPreferences;
import com.microproject.ui.util.SwingFileChooserProvider;
import com.microproject.util.Environment;
import com.microproject.util.UiServices;

/**
 * A real main window showing one project, opened the way the ribbon acceptance tests need it, and
 * closed with every process-wide setting it changed restored.
 */
public final class GuiProjectWindow implements AutoCloseable {
	private final MainRibbonFrame window;
	private final GraphicManager manager;
	private final Project project;
	private final Robot robot;
	private final boolean previousRibbonUi;
	private final boolean previousNewLook;
	private final RibbonDisplayMode previousDisplayMode;
	private final UiServices.FileChooserProvider previousFileChooser;
	private final String previousSystemChooserProperty;
	private final AtomicInteger chooserFileNameCalls;
	private final AtomicInteger chooserFileNamesCalls;
	private final JobQueue previousSessionJobQueue;

	private GuiProjectWindow(MainRibbonFrame window, GraphicManager manager, Project project, Robot robot,
		boolean ribbonUi, boolean newLook, RibbonDisplayMode displayMode,
		UiServices.FileChooserProvider previousFileChooser, String previousSystemChooserProperty,
		AtomicInteger chooserFileNameCalls, AtomicInteger chooserFileNamesCalls, JobQueue previousSessionJobQueue) {
		this.window = window;
		this.manager = manager;
		this.project = project;
		this.robot = robot;
		this.previousRibbonUi = ribbonUi;
		this.previousNewLook = newLook;
		this.previousDisplayMode = displayMode;
		this.previousFileChooser = previousFileChooser;
		this.previousSystemChooserProperty = previousSystemChooserProperty;
		this.chooserFileNameCalls = chooserFileNameCalls;
		this.chooserFileNamesCalls = chooserFileNamesCalls;
		this.previousSessionJobQueue = previousSessionJobQueue;
	}

	/** Opens the main window on {@code project} and waits until its task table shows. */
	public static GuiProjectWindow open(Project project) throws Exception {
		boolean ribbonUi = Environment.isRibbonUI();
		boolean newLook = Environment.isNewLook();
		RibbonDisplayMode displayMode = RibbonDisplayPreferences.load();
		UiServices.FileChooserProvider previousFileChooser = UiServices.getFileChooserProvider();
		String previousSystemChooserProperty = System.getProperty("flatlaf.useSystemFileChooser");
		JobQueue previousSessionJobQueue = SessionFactory.getInstance().getLocalSession().getJobQueue();
		RibbonDisplayPreferences.save(RibbonDisplayMode.ALWAYS_SHOW);
		System.setProperty("flatlaf.useSystemFileChooser", "false");
		SwingFileChooserProvider swingChooser = new SwingFileChooserProvider();
		AtomicInteger chooserFileNameCalls = new AtomicInteger();
		AtomicInteger chooserFileNamesCalls = new AtomicInteger();
		UiServices.setFileChooserProvider(new UiServices.FileChooserProvider() {
			@Override public String chooseFileName(boolean save, String selectedFileName, Object parent) {
				chooserFileNameCalls.incrementAndGet();
				return swingChooser.chooseFileName(save, selectedFileName, parent);
			}
			@Override public List<String> chooseFileNames(boolean save, String selectedFileName, Object parent) {
				chooserFileNamesCalls.incrementAndGet();
				return swingChooser.chooseFileNames(save, selectedFileName, parent);
			}
		});
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);

		MainRibbonFrame[] frame = new MainRibbonFrame[1];
		GraphicManager[] manager = new GraphicManager[1];
		SwingUtilities.invokeAndWait(() -> {
			for (Window candidate : Window.getWindows()) {
				if (candidate instanceof MainRibbonFrame) candidate.dispose();
			}
			frame[0] = new MainRibbonFrame("microProject ribbon command acceptance", null);
			manager[0] = new GraphicManager(frame[0]);
			// StartupFactory wires the application's LocalSession to the active manager's queue.
			// A standalone ribbon fixture must follow that same initialization path for file commands.
			SessionFactory.getInstance().setJobQueue(manager[0].getJobQueue());
			frame[0].setGraphicManager(manager[0]);
			manager[0].initView();
			manager[0].addProjectFrame(project);
			frame[0].setSize(1600, 700);
			frame[0].setLocationByPlatform(true);
			frame[0].setAlwaysOnTop(true);
			frame[0].setVisible(true);
			frame[0].toFront();
			frame[0].requestFocus();
		});
		Robot robot = new GuiRobot();
		robot.setAutoDelay(45);
		GuiProjectWindow opened = new GuiProjectWindow(frame[0], manager[0], project, robot, ribbonUi, newLook,
			displayMode, previousFileChooser, previousSystemChooserProperty, chooserFileNameCalls,
			chooserFileNamesCalls, previousSessionJobQueue);
		GuiAcceptanceSupport.await(() -> frame[0].isShowing() && manager[0].getCurrentFrame() != null
			&& manager[0].getCurrentFrame().getActiveSpreadSheet() != null, "the main window did not show its project");
		opened.activate();
		return opened;
	}

	public MainRibbonFrame window() {
		return window;
	}

	public GraphicManager manager() {
		return manager;
	}

	public Project project() {
		return project;
	}

	public Robot robot() {
		return robot;
	}

	/** The chooser that was installed before this window replaced it. */
	public UiServices.FileChooserProvider previousFileChooser() {
		return previousFileChooser;
	}

	public AtomicInteger chooserFileNameCalls() {
		return chooserFileNameCalls;
	}

	public AtomicInteger chooserFileNamesCalls() {
		return chooserFileNamesCalls;
	}

	/** Clicks the title bar so the window owns the keyboard focus. */
	public void activate() throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> bounds[0] = window.isShowing()
			? new Rectangle(window.getLocationOnScreen(), window.getSize()) : null);
		robot.mouseMove(bounds[0].x + Math.min(40, bounds[0].width / 2), bounds[0].y + 12);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	@Override
	public void close() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			for (Window candidate : Window.getWindows()) {
				if (candidate != window && candidate.isShowing() && (candidate instanceof Dialog || candidate instanceof Frame)) {
					candidate.dispose();
				}
			}
			manager.cleanUp();
			window.dispose();
		});
		SessionFactory.getInstance().setJobQueue(previousSessionJobQueue);
		RibbonDisplayPreferences.save(previousDisplayMode);
		Environment.setRibbonUI(previousRibbonUi);
		Environment.setNewLook(previousNewLook);
		UiServices.setFileChooserProvider(previousFileChooser);
		if (previousSystemChooserProperty == null) System.clearProperty("flatlaf.useSystemFileChooser");
		else System.setProperty("flatlaf.useSystemFileChooser", previousSystemChooserProperty);
	}
}

/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.KeyEventDispatcher;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JMenuItem;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.configuration.Dictionary;
import com.microproject.graphic.configuration.BarStyles;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.exchange.MpoFileImporter;
import com.microproject.pm.graphic.gantt.Gantt;
import com.microproject.pm.graphic.gantt.GanttUI;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.frames.MainRibbonFrame;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.views.GanttView;
import com.microproject.pm.graphic.graph.GraphZone;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.strings.Messages;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.DateTime;
import com.microproject.util.Environment;

/** Robot coverage for moving a task bar and propagating an FS successor date. */
class GanttBarDateDragGuiAcceptanceTest {
	private JFrame frame;
	private Gantt gantt;
	private MainRibbonFrame projectWindow;
	private GraphicManager projectManager;
	private boolean previousRibbonUi;
	private boolean previousNewLook;
	private boolean environmentCaptured;
	private KeyEventDispatcher activeKeyObserver;

	@AfterEach
	void closeWindow() throws Exception {
		if (activeKeyObserver != null) {
			KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(activeKeyObserver);
			activeKeyObserver = null;
		}
		if (frame != null) SwingUtilities.invokeAndWait(() -> {
			frame.dispose();
			frame = null;
		});
		if (gantt != null) {
			gantt.cleanUp();
			gantt = null;
		}
		if (projectManager != null) SwingUtilities.invokeAndWait(() -> projectManager.cleanUp());
		if (projectWindow != null) SwingUtilities.invokeAndWait(() -> projectWindow.dispose());
		if (environmentCaptured) {
			Environment.setRibbonUI(previousRibbonUi);
			Environment.setNewLook(previousNewLook);
		}
	}

	@Test
	void robotCtrlNumpadDivideAndMultiplyZoomActiveGanttFromGanttAndSpreadsheetFocus() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		environmentCaptured = true;
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		Fixture fixture = createFixture(DependencyType.FS, false);
		DocumentFrame[] document = new DocumentFrame[1];
		SwingUtilities.invokeAndWait(() -> {
			projectWindow = new MainRibbonFrame("Gantt keypad zoom GUI acceptance", null);
			projectManager = new GraphicManager(projectWindow);
			projectWindow.setGraphicManager(projectManager);
			projectManager.initView();
			document[0] = projectManager.addProjectFrame(fixture.project);
			projectWindow.setSize(1050, 650);
			projectWindow.setLocationByPlatform(true);
			projectWindow.setAlwaysOnTop(true);
			projectWindow.setVisible(true);
			document[0].activateGanttView();
		});
		GuiAcceptanceSupport.await(() -> projectWindow.isShowing() && document[0].isShowing(),
			"Gantt project window did not become visible");
		GanttView[] view = new GanttView[1];
		int[] initialScale = new int[1];
		int[] shortcutInvocations = new int[1];
		int[] divideScale = new int[1];
		Point[] tableViewportPosition = new Point[1];
		int[] ganttViewportY = new int[1];
		long[] initialStart = new long[1];
		long[] initialEnd = new long[1];
		SwingUtilities.invokeAndWait(() -> {
			view[0] = (GanttView) document[0].getActiveTopView();
			if (!view[0].canZoomIn())
				view[0].zoomOut();
			initialScale[0] = view[0].getScale();
			javax.swing.JViewport tableViewport = (javax.swing.JViewport) SwingUtilities.getAncestorOfClass(
				javax.swing.JViewport.class, view[0].getSpreadSheet());
			javax.swing.JViewport ganttViewport = (javax.swing.JViewport) SwingUtilities.getAncestorOfClass(
				javax.swing.JViewport.class, view[0].getGantt());
			assertTrue(tableViewport != null, "task table must be hosted by a viewport");
			assertTrue(ganttViewport != null, "Gantt must be hosted by a viewport");
			tableViewportPosition[0] = tableViewport.getViewPosition();
			ganttViewportY[0] = ganttViewport.getViewPosition().y;
			initialStart[0] = fixture.predecessor.getStart();
			initialEnd[0] = fixture.predecessor.getEnd();
			view[0].getGantt().requestFocusInWindow();
			assertTrue(projectManager.isDocumentActive(), "project document must be the active frame");
			assertEquals(document[0], projectManager.getCurrentFrame());
			var divide = javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_DIVIDE, InputEvent.CTRL_DOWN_MASK);
			Object actionKey = projectWindow.getRootPane()
				.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).get(divide);
			assertEquals("GanttKeypadZoomIn", actionKey);
			Action zoomAction = projectWindow.getRootPane().getActionMap().get(actionKey);
			projectWindow.getRootPane().getActionMap().put(actionKey, new AbstractAction() {
				private static final long serialVersionUID = 1L;
				@Override public void actionPerformed(java.awt.event.ActionEvent event) {
					shortcutInvocations[0]++;
					zoomAction.actionPerformed(event);
				}
			});
			var multiply = javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_MULTIPLY, InputEvent.CTRL_DOWN_MASK);
			Object multiplyActionKey = projectWindow.getRootPane()
				.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).get(multiply);
			assertEquals("GanttKeypadZoomOut", multiplyActionKey);
			Action zoomOutAction = projectWindow.getRootPane().getActionMap().get(multiplyActionKey);
			projectWindow.getRootPane().getActionMap().put(multiplyActionKey, new AbstractAction() {
				private static final long serialVersionUID = 1L;
				@Override public void actionPerformed(java.awt.event.ActionEvent event) {
					shortcutInvocations[0]++;
					zoomOutAction.actionPerformed(event);
				}
			});
		});
		assertTrue(view[0].canZoomIn(), "fixture must allow zooming in");
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		int[] observedDivideEvents = new int[1];
		int[] observedMultiplyEvents = new int[1];
		String[] observedMultiplyDetails = new String[1];
		KeyEventDispatcher keyObserver = event -> {
			if (event.getID() == KeyEvent.KEY_PRESSED) {
				if (event.getKeyCode() == KeyEvent.VK_DIVIDE) observedDivideEvents[0]++;
			if (event.getKeyCode() == KeyEvent.VK_MULTIPLY) {
				observedMultiplyEvents[0]++;
				javax.swing.KeyStroke stroke = javax.swing.KeyStroke.getKeyStrokeForEvent(event);
				Object resolved = projectWindow.getRootPane()
					.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).get(stroke);
				javax.swing.InputMap focusedInputMap = view[0].getSpreadSheet()
					.getInputMap(javax.swing.JComponent.WHEN_FOCUSED);
				javax.swing.InputMap ancestorInputMap = view[0].getSpreadSheet()
					.getInputMap(javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
				observedMultiplyDetails[0] = "code=" + event.getKeyCode() + ", location=" + event.getKeyLocation()
					+ ", modifiersEx=" + event.getModifiersEx() + ", char=" + event.getKeyChar()
					+ ", source=" + event.getSource().getClass().getName() + ", stroke=" + stroke
					+ ", resolvedAction=" + resolved + ", spreadsheetFocusedBinding=" + focusedInputMap.get(stroke)
					+ ", spreadsheetAncestorBinding=" + ancestorInputMap.get(stroke);
			}
			}
			return false;
		};
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyObserver);
		activeKeyObserver = keyObserver;
		projectWindow.toFront();
		projectWindow.requestFocus();
		robot.mouseMove(projectWindow.getX() + 35, projectWindow.getY() + 12);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(projectWindow::isActive, "Gantt project window did not become the active native window");
		java.awt.Point ganttLocation = view[0].getGantt().getLocationOnScreen();
		robot.mouseMove(ganttLocation.x + Math.max(20, view[0].getGantt().getWidth() / 2),
			ganttLocation.y + Math.max(20, view[0].getGantt().getHeight() / 2));
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
		SwingUtilities.invokeAndWait(() -> {
			view[0].getGantt().requestFocus();
			view[0].getGantt().requestFocusInWindow();
		});
		GuiAcceptanceSupport.await(() -> projectWindow.isActive()
			&& KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() != null,
			"Gantt project window did not acquire a keyboard focus owner");
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_DIVIDE);
		robot.keyRelease(KeyEvent.VK_DIVIDE);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		GuiAcceptanceSupport.await(() -> shortcutInvocations[0] > 0,
			"physical Ctrl+Numpad Divide did not dispatch through the document root pane (observed key events: "
				+ observedDivideEvents[0] + ")");
		assertEquals(1, shortcutInvocations[0], "physical shortcut must dispatch through the document root pane once");
		assertTrue(view[0].getScale() != initialScale[0], "Ctrl+Numpad Divide must move one step toward zoom in");
		assertViewportPositionsUnchanged(view[0], tableViewportPosition[0], ganttViewportY[0]);
		assertEquals(initialStart[0], fixture.predecessor.getStart(), "zoom changed the task start date");
		assertEquals(initialEnd[0], fixture.predecessor.getEnd(), "zoom changed the task end date");
		divideScale[0] = view[0].getScale();
		shortcutInvocations[0] = 0;
		SwingUtilities.invokeAndWait(() -> {
			view[0].getGantt().requestFocus();
			view[0].getGantt().requestFocusInWindow();
		});
		GuiAcceptanceSupport.await(() -> KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()
			== view[0].getGantt(), "Gantt did not retain keyboard focus before Ctrl+Numpad Multiply");
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_MULTIPLY);
		robot.keyRelease(KeyEvent.VK_MULTIPLY);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(() -> shortcutInvocations[0] > 0,
			"physical Ctrl+Numpad Multiply did not dispatch through the document root pane (observed key events: "
				+ observedMultiplyEvents[0] + ", details=" + observedMultiplyDetails[0] + ")");
		assertEquals(1, shortcutInvocations[0], "physical Ctrl+Numpad Multiply must dispatch through the document root pane once");
		assertEquals(initialScale[0], view[0].getScale(), "Ctrl+Numpad Multiply must reverse the prior zoom-in step");
		assertTrue(view[0].getScale() != divideScale[0], "physical Ctrl+Numpad Multiply did not zoom out");
		assertViewportPositionsUnchanged(view[0], tableViewportPosition[0], ganttViewportY[0]);
		assertEquals(initialStart[0], fixture.predecessor.getStart(), "zoom out changed the task start date");
		assertEquals(initialEnd[0], fixture.predecessor.getEnd(), "zoom out changed the task end date");

		java.awt.Point sheetLocation = view[0].getSpreadSheet().getLocationOnScreen();
		robot.mouseMove(sheetLocation.x + Math.max(20, view[0].getSpreadSheet().getWidth() / 2),
			sheetLocation.y + Math.max(20, view[0].getSpreadSheet().getHeight() / 2));
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
		SwingUtilities.invokeAndWait(() -> {
			view[0].getSpreadSheet().requestFocus();
			view[0].getSpreadSheet().requestFocusInWindow();
		});
		GuiAcceptanceSupport.await(() -> KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()
			== view[0].getSpreadSheet(), "task table did not acquire keyboard focus before keypad zoom");
		shortcutInvocations[0] = 0;
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_DIVIDE);
		robot.keyRelease(KeyEvent.VK_DIVIDE);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(() -> shortcutInvocations[0] > 0,
			"Ctrl+Numpad Divide did not dispatch from spreadsheet focus");
		assertEquals(1, shortcutInvocations[0], "spreadsheet-focused Divide must dispatch exactly once");
		assertTrue(view[0].getScale() != initialScale[0], "spreadsheet-focused Divide must zoom in");
		assertViewportPositionsUnchanged(view[0], tableViewportPosition[0], ganttViewportY[0]);
		assertEquals(initialStart[0], fixture.predecessor.getStart(), "spreadsheet-focused zoom changed the task start date");
		assertEquals(initialEnd[0], fixture.predecessor.getEnd(), "spreadsheet-focused zoom changed the task end date");
		shortcutInvocations[0] = 0;
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_MULTIPLY);
		robot.keyRelease(KeyEvent.VK_MULTIPLY);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		robot.waitForIdle();
		KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyObserver);
		activeKeyObserver = null;
		GuiAcceptanceSupport.await(() -> shortcutInvocations[0] > 0,
			"Ctrl+Numpad Multiply did not dispatch from spreadsheet focus; " + observedMultiplyDetails[0]);
		assertEquals(1, shortcutInvocations[0], "spreadsheet-focused Multiply must dispatch exactly once");
		assertEquals(initialScale[0], view[0].getScale(), "spreadsheet-focused Multiply must reverse Divide");
		assertViewportPositionsUnchanged(view[0], tableViewportPosition[0], ganttViewportY[0]);
		assertEquals(initialStart[0], fixture.predecessor.getStart(), "spreadsheet-focused zoom changed the task start date");
		assertEquals(initialEnd[0], fixture.predecessor.getEnd(), "spreadsheet-focused zoom changed the task end date");
	}

	/** Physical wheel coverage from both synchronized panes; each gesture has one observable owner. */
	@Test
	void robotWheelModifiersHaveOneOwnerFromGanttAndTaskTable() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		environmentCaptured = true;
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		Fixture fixture = createFixture(DependencyType.FS, false);
		for (int index = 0; index < 40; index++) task(fixture.project, "Wheel task " + index, 1L);
		fixture.project.recalculate();
		DocumentFrame[] document = new DocumentFrame[1];
		SwingUtilities.invokeAndWait(() -> {
			projectWindow = new MainRibbonFrame("Gantt wheel GUI acceptance", null);
			projectManager = new GraphicManager(projectWindow);
			projectWindow.setGraphicManager(projectManager);
			projectManager.initView();
			document[0] = projectManager.addProjectFrame(fixture.project);
			projectWindow.setSize(760, 650);
			projectWindow.setLocation(20, 20);
			projectWindow.setAlwaysOnTop(true);
			projectWindow.setVisible(true);
			document[0].activateGanttView();
		});
		GuiAcceptanceSupport.await(() -> projectWindow.isShowing() && document[0].isShowing(),
			"Gantt wheel project window did not become visible");
		GanttView[] view = new GanttView[1];
		javax.swing.JViewport[] tableViewport = new javax.swing.JViewport[1];
		javax.swing.JViewport[] ganttViewport = new javax.swing.JViewport[1];
		javax.swing.JScrollPane[] tableScroll = new javax.swing.JScrollPane[1];
		javax.swing.JScrollPane[] ganttScroll = new javax.swing.JScrollPane[1];
		long[] taskStart = new long[1];
		long[] taskEnd = new long[1];
		SwingUtilities.invokeAndWait(() -> {
			view[0] = (GanttView) document[0].getActiveTopView();
			tableViewport[0] = (javax.swing.JViewport) SwingUtilities.getAncestorOfClass(
				javax.swing.JViewport.class, view[0].getSpreadSheet());
			tableScroll[0] = (javax.swing.JScrollPane) SwingUtilities.getAncestorOfClass(
				javax.swing.JScrollPane.class, view[0].getSpreadSheet());
			ganttViewport[0] = (javax.swing.JViewport) SwingUtilities.getAncestorOfClass(
				javax.swing.JViewport.class, view[0].getGantt());
			ganttScroll[0] = (javax.swing.JScrollPane) SwingUtilities.getAncestorOfClass(
				javax.swing.JScrollPane.class, view[0].getGantt());
			assertTrue(tableViewport[0] != null && ganttViewport[0] != null, "both panes must have viewports");
			while (!view[0].canZoomIn()) view[0].zoomOut();
			assertTrue(ganttViewport[0].getViewSize().height > ganttViewport[0].getExtentSize().height,
				"fixture must provide vertical scroll range");
			assertTrue(tableScroll[0].getHorizontalScrollBar().getMaximum()
				> tableScroll[0].getHorizontalScrollBar().getVisibleAmount(),
				"fixture must provide task-table horizontal scroll range");
			taskStart[0] = fixture.predecessor.getStart();
			taskEnd[0] = fixture.predecessor.getEnd();
		});
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		projectWindow.toFront();
		projectWindow.requestFocus();
		robot.delay(250);
		for (boolean overGantt : new boolean[] { true, false }) {
			SwingUtilities.invokeAndWait(() -> {
				ganttViewport[0].setViewPosition(new Point(0, 0));
				tableViewport[0].setViewPosition(new Point(0, 0));
			});
			java.awt.Component target = overGantt ? view[0].getGantt() : view[0].getSpreadSheet();
			java.awt.Component hitRegion = overGantt ? ganttViewport[0] : tableViewport[0];
			Point location = hitRegion.getLocationOnScreen();
			int x = location.x + Math.max(15, hitRegion.getWidth() / 2);
			int y = location.y + Math.max(15, hitRegion.getHeight() / 2);
			robot.mouseMove(x, y);
			robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
			robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
			SwingUtilities.invokeAndWait(() -> {
				target.requestFocus();
				target.requestFocusInWindow();
			});
			GuiAcceptanceSupport.await(target::isFocusOwner,
				(overGantt ? "Gantt" : "task table") + " did not acquire focus before wheel route");
			Point ganttLocation = ganttViewport[0].getLocationOnScreen();
			robot.mouseMove(ganttLocation.x + Math.max(15, ganttViewport[0].getWidth() / 2),
				ganttLocation.y + Math.max(15, ganttViewport[0].getHeight() / 2));
			int initialGanttX = ganttViewport[0].getViewPosition().x;
			int initialTableX = tableViewport[0].getViewPosition().x;
			int beforeY = ganttViewport[0].getViewPosition().y;
			int rowStep = view[0].getGantt().getRowHeight() * 5;
			robot.mouseWheel(1);
			GuiAcceptanceSupport.await(() -> ganttViewport[0].getViewPosition().y != beforeY,
				"plain wheel from " + (overGantt ? "Gantt" : "task table") + " did not scroll");
			assertEquals(beforeY + rowStep, ganttViewport[0].getViewPosition().y,
				"plain wheel must advance exactly one owner step");
			assertEquals(ganttViewport[0].getViewPosition().y, tableViewport[0].getViewPosition().y,
				"plain wheel must keep task table and Gantt vertically synchronized");
			assertEquals(initialGanttX, ganttViewport[0].getViewPosition().x, "plain wheel must preserve Gantt horizontal position");
			assertEquals(initialTableX, tableViewport[0].getViewPosition().x, "plain wheel must preserve table horizontal position");
			assertEquals(taskStart[0], fixture.predecessor.getStart(), "wheel must preserve task start");
			assertEquals(taskEnd[0], fixture.predecessor.getEnd(), "wheel must preserve task end");

			javax.swing.JScrollPane targetScroll = ganttScroll[0];
			javax.swing.JViewport targetViewport = ganttViewport[0];
			javax.swing.JViewport otherViewport = tableViewport[0];
			int beforeX = targetViewport.getViewPosition().x;
			int otherX = otherViewport.getViewPosition().x;
			int beforeVertical = ganttViewport[0].getViewPosition().y;
			int horizontalStep = targetScroll.getHorizontalScrollBar().getUnitIncrement(1);
			int beforeHorizontalWheel = beforeX;
			robot.keyPress(KeyEvent.VK_SHIFT);
			try { robot.mouseWheel(1); } finally { robot.keyRelease(KeyEvent.VK_SHIFT); }
			GuiAcceptanceSupport.await(() -> targetViewport.getViewPosition().x != beforeHorizontalWheel,
				"Shift+wheel from " + (overGantt ? "Gantt" : "task table") + " did not scroll horizontally");
			assertEquals(beforeX + horizontalStep, targetViewport.getViewPosition().x,
				"Shift+wheel must move one horizontal unit");
			assertEquals(otherX, otherViewport.getViewPosition().x, "Shift+wheel must not scroll the other pane horizontally");
			assertEquals(beforeVertical, ganttViewport[0].getViewPosition().y, "Shift+wheel must not scroll vertically");
			assertEquals(tableViewport[0].getViewPosition().y, ganttViewport[0].getViewPosition().y,
				"Shift+wheel must preserve synchronized vertical positions");

			int beforeScale = view[0].getScale();
			int direction = beforeScale == view[0].getScaleCount() - 1 ? -1 : 1;
			beforeVertical = ganttViewport[0].getViewPosition().y;
			Point tableBeforeZoom = tableViewport[0].getViewPosition();
			robot.keyPress(KeyEvent.VK_CONTROL);
			try { robot.mouseWheel(direction); } finally { robot.keyRelease(KeyEvent.VK_CONTROL); }
			GuiAcceptanceSupport.await(() -> view[0].getScale() != beforeScale,
				"Ctrl+wheel from " + (overGantt ? "Gantt" : "task table") + " did not zoom");
			assertEquals(beforeScale + direction, view[0].getScale(), "Ctrl+wheel must change zoom by one step");
			assertEquals(beforeVertical, ganttViewport[0].getViewPosition().y, "Ctrl+wheel must preserve vertical position");
			assertEquals(tableBeforeZoom, tableViewport[0].getViewPosition(), "Ctrl+wheel must preserve table viewport position");
			assertEquals(taskStart[0], fixture.predecessor.getStart(), "wheel zoom must preserve task start");
			assertEquals(taskEnd[0], fixture.predecessor.getEnd(), "wheel zoom must preserve task end");
		}
	}

	@Test
	void robotCtrlNumpadZoomIsNoOpWhenResourceViewIsActive() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		environmentCaptured = true;
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		Fixture fixture = createFixture(DependencyType.FS, false);
		DocumentFrame[] document = new DocumentFrame[1];
		SwingUtilities.invokeAndWait(() -> {
			projectWindow = new MainRibbonFrame("Non-Gantt keypad zoom GUI acceptance", null);
			projectManager = new GraphicManager(projectWindow);
			projectWindow.setGraphicManager(projectManager);
			projectManager.initView();
			document[0] = projectManager.addProjectFrame(fixture.project);
			projectWindow.setSize(1050, 650);
			projectWindow.setLocationByPlatform(true);
			projectWindow.setAlwaysOnTop(true);
			projectWindow.setVisible(true);
			document[0].activateGanttView();
		});
		GuiAcceptanceSupport.await(() -> projectWindow.isShowing() && document[0].isShowing(),
			"project window did not become visible");
		GanttView[] ganttView = new GanttView[1];
		Point[] ganttViewportPosition = new Point[1];
		int[] scale = new int[1];
		long[] start = new long[1];
		long[] end = new long[1];
		long[] successorStart = new long[1];
		long[] successorEnd = new long[1];
		SwingUtilities.invokeAndWait(() -> {
			ganttView[0] = (GanttView) document[0].getActiveTopView();
			javax.swing.JViewport viewport = (javax.swing.JViewport) SwingUtilities.getAncestorOfClass(
				javax.swing.JViewport.class, ganttView[0].getGantt());
			assertTrue(viewport != null, "Gantt must have a viewport before switching views");
			ganttViewportPosition[0] = viewport.getViewPosition();
			scale[0] = ganttView[0].getScale();
			start[0] = fixture.predecessor.getStart();
			end[0] = fixture.predecessor.getEnd();
			successorStart[0] = fixture.successor.getStart();
			successorEnd[0] = fixture.successor.getEnd();
			document[0].activateResourceView();
			assertTrue(document[0].getActiveTopView() instanceof ResourceView,
				"Resource View must be the active non-Gantt view before keypad input");
		});
		GuiAcceptanceSupport.await(() -> document[0].getResourceView().getSpreadSheet().isShowing(),
			"Resource View spreadsheet did not become visible");
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		projectWindow.toFront();
		projectWindow.requestFocus();
		robot.delay(250);
		for (int key : new int[] { KeyEvent.VK_DIVIDE, KeyEvent.VK_MULTIPLY }) {
			robot.keyPress(KeyEvent.VK_CONTROL);
			robot.keyPress(key);
			robot.keyRelease(key);
			robot.keyRelease(KeyEvent.VK_CONTROL);
			robot.waitForIdle();
		}
		SwingUtilities.invokeAndWait(() -> {
			assertTrue(document[0].getActiveTopView() instanceof ResourceView,
				"keypad input must not switch away from Resource View");
			assertEquals(scale[0], ganttView[0].getScale(), "non-Gantt keypad zoom changed the Gantt scale");
			javax.swing.JViewport viewport = (javax.swing.JViewport) SwingUtilities.getAncestorOfClass(
				javax.swing.JViewport.class, ganttView[0].getGantt());
			assertEquals(ganttViewportPosition[0], viewport.getViewPosition(),
				"non-Gantt keypad zoom changed the hidden Gantt viewport");
			assertEquals(start[0], fixture.predecessor.getStart(), "non-Gantt keypad zoom changed predecessor start");
			assertEquals(end[0], fixture.predecessor.getEnd(), "non-Gantt keypad zoom changed predecessor end");
			assertEquals(successorStart[0], fixture.successor.getStart(), "non-Gantt keypad zoom changed successor start");
			assertEquals(successorEnd[0], fixture.successor.getEnd(), "non-Gantt keypad zoom changed successor end");
		});
	}

	private void assertViewportPositionsUnchanged(GanttView view, Point expectedTablePosition, int expectedGanttY)
			throws Exception {
		Point[] tablePosition = new Point[1];
		int[] ganttY = new int[1];
		SwingUtilities.invokeAndWait(() -> {
			javax.swing.JViewport tableViewport = (javax.swing.JViewport) SwingUtilities.getAncestorOfClass(
				javax.swing.JViewport.class, view.getSpreadSheet());
			javax.swing.JViewport ganttViewport = (javax.swing.JViewport) SwingUtilities.getAncestorOfClass(
				javax.swing.JViewport.class, view.getGantt());
			tablePosition[0] = tableViewport.getViewPosition();
			ganttY[0] = ganttViewport.getViewPosition().y;
		});
		assertEquals(expectedTablePosition, tablePosition[0], "zoom changed the non-target task-table viewport position");
		assertEquals(expectedGanttY, ganttY[0], "zoom changed the non-target Gantt vertical viewport position");
	}

	@Test
	void robotDragMovesBarAndRecalculatesFsSuccessor() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(DependencyType.FS, true, false,
			DateTime.calendarInstance(2026, java.util.Calendar.JUNE, 10).getTimeInMillis());
		dragBarAndAssertSuccessor(fixture);
	}

	@Test
	void robotDragMovesBarAndRecalculatesFfSuccessor() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(DependencyType.FF);
		prepareOutcomeDocument(fixture);
		dragBarAndAssertSuccessor(fixture);
	}

	@Test
	void robotDragMovesBarAndRecalculatesSsSuccessor() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		dragBarAndAssertSuccessor(createFixture(DependencyType.SS));
	}

	@Test
	void robotDragMovesBarAndRecalculatesSfSuccessor() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		dragBarAndAssertSuccessor(createFixture(DependencyType.SF));
	}

	@Test
	void robotDragBetweenBarsCreatesDependencyAndUndoRestoresModel() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(DependencyType.FS, false);
		showFixture(fixture);
		prepareOutcomeDocument(fixture);
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
			gantt.requestFocusInWindow();
		});
		GuiAcceptanceSupport.await(gantt::isShowing, "Gantt was not visible");
		activateWindow(robot);
		Rectangle source = barBounds(fixture, fixture.predecessor);
		Rectangle target = barBounds(fixture, fixture.successor);
		int startX = source.x + source.width / 2;
		int startY = source.y + source.height / 2;
		int endX = target.x + target.width / 2;
		int endY = target.y + target.height / 2;
		assertTrue(fixture.successor.getPredecessorList().isEmpty(), "fixture must begin without a dependency");
		SwingUtilities.invokeAndWait(() -> {
			GraphZone sourceZone = gantt.getUI().getNodeAt(startX - gantt.getLocationOnScreen().x,
				startY - gantt.getLocationOnScreen().y);
			GraphZone targetZone = gantt.getUI().getNodeAt(endX - gantt.getLocationOnScreen().x,
				endY - gantt.getLocationOnScreen().y);
			assertTrue(sourceZone != null && sourceZone.getObject() instanceof GraphicNode sourceNode
				&& sourceNode.getNode().getImpl() == fixture.predecessor, "source point must hit predecessor bar");
			assertTrue(targetZone != null && targetZone.getObject() instanceof GraphicNode targetNode
				&& targetNode.getNode().getImpl() == fixture.successor, "target point must hit successor bar");
		});
		robot.mouseMove(startX, startY);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		for (int step = 1; step <= 10; step++)
			robot.mouseMove(startX + (endX - startX) * step / 10, startY + (endY - startY) * step / 10);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> !fixture.successor.getPredecessorList().isEmpty(),
			"Gantt bar-to-bar drag did not create a dependency");
		GuiAcceptanceSupport.await(() -> fixture.cache.getEdgesSize() == 1,
			"Gantt projection did not publish the newly created dependency edge");
		var outcome = projectManager.getLastRibbonCommandResult();
		assertEquals("gantt.dependency.create", outcome.commandId());
		assertEquals(com.microproject.ribbon.RibbonCommandResult.Status.CHANGED, outcome.status());
		assertEquals(List.of(fixture.predecessor.getUniqueId(), fixture.successor.getUniqueId()),
			outcome.selectedTaskIds());
		assertEquals(List.of(fixture.predecessor.getUniqueId(), fixture.successor.getUniqueId()),
			outcome.affectedTaskIds());
		assertEquals("Gantt", outcome.activeViewId());
		assertEquals(fixture.predecessor,
			((Dependency) fixture.successor.getPredecessorList().iterator().next()).getPredecessor());
		assertEquals(DependencyType.FS,
			((Dependency) fixture.successor.getPredecessorList().iterator().next()).getDependencyType(),
			"Gantt drag must create a Finish-to-Start link");
		assertEquals(1, fixture.successor.getPredecessorList().size(), "drag must create exactly one link");
		// Physical Ctrl+Z / Ctrl+Y (Office Undo/Redo shortcuts) through the shared key route.
		// The document root pane owns Undo/Redo, so give the project window the native keyboard focus first.
		SwingUtilities.invokeAndWait(() -> {
			projectWindow.setSize(1050, 650);
			projectWindow.setLocation(frame.getX() + 30, frame.getY() + 30);
			projectWindow.setAlwaysOnTop(true);
			projectWindow.setVisible(true);
			projectWindow.toFront();
			projectWindow.requestFocus();
		});
		GuiAcceptanceSupport.await(projectWindow::isShowing, "project window was not visible for physical Undo");
		robot.mouseMove(projectWindow.getX() + 35, projectWindow.getY() + 12);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(projectWindow::isActive, "project window did not become the active native window");
		robot.keyPress(java.awt.event.KeyEvent.VK_CONTROL);
		robot.keyPress(java.awt.event.KeyEvent.VK_Z);
		robot.keyRelease(java.awt.event.KeyEvent.VK_Z);
		robot.keyRelease(java.awt.event.KeyEvent.VK_CONTROL);
		GuiAcceptanceSupport.await(() -> fixture.successor.getPredecessorList().isEmpty(),
			"one Ctrl+Z must remove the Gantt-created dependency");
		GuiAcceptanceSupport.await(() -> fixture.cache.getEdgesSize() == 0, "Undo must remove the Gantt edge");
		robot.keyPress(java.awt.event.KeyEvent.VK_CONTROL);
		robot.keyPress(java.awt.event.KeyEvent.VK_Y);
		robot.keyRelease(java.awt.event.KeyEvent.VK_Y);
		robot.keyRelease(java.awt.event.KeyEvent.VK_CONTROL);
		GuiAcceptanceSupport.await(() -> fixture.successor.getPredecessorList().size() == 1,
			"Ctrl+Y must restore the dependency");
		GuiAcceptanceSupport.await(() -> fixture.cache.getEdgesSize() == 1, "Redo must restore the Gantt edge");
		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		MpoFileImporter importer = new MpoFileImporter();
		assertTrue(importer.saveProject(fixture.project, saved), "the Gantt-created dependency must be serializable");
		Project reopened = importer.loadProject(new ByteArrayInputStream(saved.toByteArray()));
		assertEquals(1, reopened.getTaskList().stream().filter(task -> "Drag successor".equals(task.getName()))
			.findFirst().orElseThrow().getPredecessorList().size(), "the Gantt-created dependency must survive MPO reload");
		capture(robot);
	}

	@Test
	void robotDragToChartEdgeAutoScrollsAndPersistsMovedTaskDate() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(DependencyType.FS, false);
		NormalTask viewportExtent = task(fixture.project, "Viewport extent", 180L);
		java.util.Calendar extentStart = java.util.Calendar.getInstance();
		extentStart.setTimeInMillis(fixture.predecessor.getStart());
		extentStart.add(java.util.Calendar.DAY_OF_MONTH, 45);
		viewportExtent.getCurrentSchedule().setStart(extentStart.getTimeInMillis());
		fixture.project.recalculate();
		SwingUtilities.invokeAndWait(() -> gantt.updateSize());
		showFixture(fixture);
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateGantt(robot);
		javax.swing.JViewport viewport = (javax.swing.JViewport) SwingUtilities.getAncestorOfClass(
			javax.swing.JViewport.class, gantt);
		assertTrue(viewport != null, "Gantt must be hosted by a viewport for edge auto-scroll");
		assertTrue(gantt.getWidth() > viewport.getExtentSize().width,
			"fixture timeline must exceed the visible chart width to exercise horizontal auto-scroll");
		SwingUtilities.invokeAndWait(() -> {
			int taskStartX = (int) Math.round(gantt.getCoord().toX(fixture.predecessor.getStart()));
			viewport.setViewPosition(new Point(Math.max(0, taskStartX - 120), 0));
		});
		Rectangle initialBar = barBounds(fixture);
		Point origin = initialBar.getLocation();
		origin.translate(initialBar.width / 2, initialBar.height / 2);
		long originalStart = fixture.predecessor.getStart();
		int originalViewX = viewportViewPosition(viewport).x;
		Rectangle viewportBounds = viewportScreenBounds(viewport);
		int edgeX = viewportBounds.x + viewportBounds.width - 3;
		int edgeY = origin.y;
		assertTaskBarHit(fixture, origin);
		robot.mouseMove(origin.x, origin.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		for (int step = 1; step <= 12; step++) {
			int x = origin.x + (edgeX - origin.x) * step / 12;
			robot.mouseMove(x, edgeY);
		}
		for (int step = 0; step < 24; step++) {
			robot.mouseMove(edgeX - (step % 2 == 0 ? 8 : 0), edgeY);
			robot.delay(60);
		}
		robot.mouseMove(edgeX - 30, edgeY);
		robot.delay(60);
		int scrolledViewX = viewportViewPosition(viewport).x;
		assertTrue(scrolledViewX > originalViewX,
			"dragging at the visible chart edge must auto-scroll horizontally: before="
				+ originalViewX + " after=" + scrolledViewX);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(() -> fixture.predecessor.getStart() > originalStart,
			"edge auto-scroll drag must move the task to a later date");
		long movedStart = fixture.predecessor.getStart();
		assertTrue(movedStart > originalStart,
			"the task model date must reflect the rightward edge drag: before=" + originalStart + " after=" + movedStart);
		Rectangle movedBar = barBounds(fixture);
		assertTrue(movedBar.width > 0, "the moved task bar must be rendered after edge auto-scroll");
		Rectangle visibleChart = viewportScreenBounds(viewport);
		assertTrue(visibleChart.intersects(movedBar),
			"the moved task bar must remain inside the visible chart after releasing at the edge: bar="
				+ movedBar + " viewport=" + visibleChart);
		SwingUtilities.invokeAndWait(() -> fixture.project.getUndoController().undo());
		assertEquals(originalStart, fixture.predecessor.getStart(), "one Undo must restore the original task date");
		SwingUtilities.invokeAndWait(() -> fixture.project.getUndoController().redo());
		assertEquals(movedStart, fixture.predecessor.getStart(), "one Redo must restore the auto-scrolled task date");
		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		MpoFileImporter importer = new MpoFileImporter();
		assertTrue(importer.saveProject(fixture.project, saved), "the auto-scrolled task move must be serializable");
		Project reopened = importer.loadProject(new ByteArrayInputStream(saved.toByteArray()));
		assertEquals(movedStart, reopened.getTaskList().stream()
			.filter(task -> "Drag predecessor".equals(task.getName())).findFirst().orElseThrow().getStart(),
			"the moved task date must survive MPO reload");
		capture(robot, "gantt-edge-auto-scroll.png");
	}

	@Test
	void robotResizeProgressAndSplitUseTypedScheduleGatewayWithUndoAndPersistence() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(DependencyType.FS, false, true);
		showFixture(fixture);
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		activateGantt(robot);
		capture(robot, "gantt-schedule-gestures-initial.png");

		long originalStart = fixture.predecessor.getStart();
		int originalConstraintType = fixture.predecessor.getConstraintType();
		long originalConstraintDate = fixture.predecessor.getConstraintDate();
		Point startHandle = findResizeHandle(robot, fixture, true, Cursor.W_RESIZE_CURSOR, "start resize");
		assertTaskBarHit(fixture, startHandle);
		assertResizeCursor(robot, startHandle, Cursor.W_RESIZE_CURSOR, "start resize");
		java.util.Calendar earlierStartCalendar = java.util.Calendar.getInstance();
		earlierStartCalendar.setTimeInMillis(originalStart);
		earlierStartCalendar.add(java.util.Calendar.DAY_OF_MONTH, -3);
		long earlierRequestedStart = earlierStartCalendar.getTimeInMillis();
		long normalizedEarlierStart = fixture.predecessor.getEffectiveWorkCalendar()
			.adjustInsideCalendar(earlierRequestedStart, false);
		assertTrue(normalizedEarlierStart < originalStart,
			"the resize destination must normalize to a working time before the original start");
		Point earlierDate = screenPointForTaskDate(fixture, normalizedEarlierStart);
		drag(robot, startHandle, earlierDate);
		try {
			GuiAcceptanceSupport.await(() -> fixture.predecessor.getStart() < originalStart,
				"dragging the leading bar handle did not resize the task start");
		} catch (AssertionError failure) {
			throw new AssertionError("Gantt start resize did not commit: expectedBefore=" + originalStart
				+ " actualAfter=" + fixture.predecessor.getStart() + " pressPoint=" + startHandle
				+ " dragPoint=" + earlierDate, failure);
		}
		assertTrue(barBounds(fixture).width > 0, "the resized bar must remain visible after redraw");
		long resizedStart = fixture.predecessor.getStart();
		SwingUtilities.invokeAndWait(() -> fixture.project.getUndoController().undo());
		assertEquals(originalStart, fixture.predecessor.getStart(), "one Undo must restore the original start");
		assertEquals(originalConstraintType, fixture.predecessor.getConstraintType(),
			"one Undo must restore the original start constraint");
		assertEquals(originalConstraintDate, fixture.predecessor.getConstraintDate(),
			"one Undo must restore the original constraint date");
		SwingUtilities.invokeAndWait(() -> fixture.project.getUndoController().redo());
		assertEquals(resizedStart, fixture.predecessor.getStart(), "one Redo must restore the resized start");
		assertEquals(com.microproject.pm.scheduling.ConstraintType.Kind.SNET, fixture.predecessor.getConstraintTypeKind(),
			"start resize must retain its start constraint after Redo");
		assertEquals(resizedStart, fixture.predecessor.getConstraintDate(),
			"start resize must retain its constraint date after Redo");

		long originalEnd = fixture.predecessor.getEnd();
		int startConstraintType = fixture.predecessor.getConstraintType();
		long startConstraintDate = fixture.predecessor.getConstraintDate();
		Point endHandle = findResizeHandle(robot, fixture, false, Cursor.E_RESIZE_CURSOR, "finish resize");
		assertTaskBarHit(fixture, endHandle);
		assertResizeCursor(robot, endHandle, Cursor.E_RESIZE_CURSOR, "finish resize");
		Point laterDate = screenPointForTaskDate(fixture,
			originalEnd + com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
		drag(robot, endHandle, laterDate);
		GuiAcceptanceSupport.await(() -> fixture.predecessor.getEnd() > originalEnd,
			"dragging the trailing bar handle did not resize the task finish");
		long resizedEnd = fixture.predecessor.getEnd();
		assertEquals(resizedStart, fixture.predecessor.getStart(),
			"finish resize must retain the task start");
		assertEquals(startConstraintType, fixture.predecessor.getConstraintType(),
			"finish resize must preserve the existing start constraint");
		assertEquals(startConstraintDate, fixture.predecessor.getConstraintDate(),
			"finish resize must preserve the existing constraint date");
		SwingUtilities.invokeAndWait(() -> fixture.project.getUndoController().undo());
		assertEquals(originalEnd, fixture.predecessor.getEnd(), "one Undo must restore the original finish");
		assertEquals(startConstraintType, fixture.predecessor.getConstraintType(),
			"one Undo must preserve the start constraint after finish resize");
		SwingUtilities.invokeAndWait(() -> fixture.project.getUndoController().redo());
		assertEquals(resizedEnd, fixture.predecessor.getEnd(), "one Redo must restore the resized finish");
		assertEquals(startConstraintType, fixture.predecessor.getConstraintType(),
			"one Redo must preserve the start constraint after finish resize");

		SwingUtilities.invokeAndWait(() -> fixture.predecessor.setPercentComplete(0.4d));
		SwingUtilities.invokeAndWait(fixture.project::recalculate);
		long originalProgress = fixture.predecessor.getCompletedThrough();
		Point progressHandle = screenPointForTaskDate(fixture, originalProgress);
		assertProgressBarHit(fixture, progressHandle);
		long progressTarget = originalProgress + (fixture.predecessor.getEnd() - originalProgress) / 6L;
		assertTrue(progressTarget > originalProgress && progressTarget < fixture.predecessor.getEnd(),
			"progress target must remain inside the scheduled interval: progress=" + originalProgress
				+ " end=" + fixture.predecessor.getEnd() + " target=" + progressTarget);
		Point laterProgress = screenPointForTaskDate(fixture, progressTarget);
		drag(robot, progressHandle, laterProgress);
		GuiAcceptanceSupport.await(() -> fixture.predecessor.getCompletedThrough() > originalProgress,
			"dragging the progress handle did not update task progress: before=" + originalProgress
				+ " requested=" + progressTarget + " actual=" + fixture.predecessor.getCompletedThrough()
				+ " end=" + fixture.predecessor.getEnd() + " origin=" + progressHandle + " target=" + laterProgress);
		long updatedProgress = fixture.predecessor.getCompletedThrough();
		assertTrue(barBounds(fixture).width > 0, "the updated progress bar must be visible after redraw");
		SwingUtilities.invokeAndWait(() -> fixture.project.getUndoController().undo());
		assertEquals(originalProgress, fixture.predecessor.getCompletedThrough(), "one Undo must restore task progress");
		SwingUtilities.invokeAndWait(() -> fixture.project.getUndoController().redo());
		assertEquals(updatedProgress, fixture.predecessor.getCompletedThrough(), "one Redo must restore updated progress");

		long currentProgress = fixture.predecessor.getCompletedThrough();
		List<String> originalIntervals = assignmentIntervals(fixture.predecessor);
		List<String> originalTaskIntervals = taskIntervals(fixture.predecessor);
		List<String> originalContours = assignmentContours(fixture.predecessor);
		List<com.microproject.pm.task.TaskSplitInterval> originalTaskSplits = fixture.predecessor.getTaskSplitIntervals();
		long remainingWorkStart = Math.max(currentProgress, fixture.predecessor.getResume());
		long splitAt = originalTaskIntervals.stream().mapToLong(interval -> {
			String[] bounds = interval.split(":");
			long intervalStart = Math.max(Long.parseLong(bounds[0]), remainingWorkStart);
			long intervalEnd = Long.parseLong(bounds[1]);
			return intervalEnd - intervalStart > 1L
				? intervalStart + (intervalEnd - intervalStart) / 3L
				: Long.MIN_VALUE;
		}).filter(candidate -> candidate != Long.MIN_VALUE).findFirst().orElseThrow(() ->
			new AssertionError("progress drag must leave a schedulable task interval for splitting: resume="
				+ fixture.predecessor.getResume() + " completedThrough=" + currentProgress
				+ " taskIntervals=" + originalTaskIntervals));
		assertTrue(splitAt > remainingWorkStart && splitAt < fixture.predecessor.getEnd(),
			"split point must be inside remaining scheduled work: resume=" + fixture.predecessor.getResume()
				+ " completedThrough=" + currentProgress + " splitAt=" + splitAt
				+ " intervals=" + originalIntervals + " end=" + fixture.predecessor.getEnd());
		Point splitStart = screenPointForTaskDate(fixture, splitAt);
		long clickedSplitAt = taskDateAt(splitStart);
		List<String> originalAssignmentState = assignmentScheduleStates(fixture.predecessor);
		assertTaskBarHit(fixture, splitStart);
		openPopupAndChoose(robot, splitStart, Messages.getString("Gantt.Popup.splitMode"));
		click(robot, splitStart);
		try {
			GuiAcceptanceSupport.await(() -> !fixture.predecessor.getTaskSplitIntervals().equals(originalTaskSplits),
				"Gantt split gesture did not add a task-owned nonworking interval");
		} catch (AssertionError failure) {
			throw new AssertionError(failure.getMessage() + ": intervals before=" + originalIntervals
				+ " after=" + assignmentIntervals(fixture.predecessor)
				+ " task splits before=" + originalTaskSplits
				+ " after=" + fixture.predecessor.getTaskSplitIntervals()
				+ " contours before=" + originalContours
				+ " after=" + assignmentContours(fixture.predecessor)
				+ " requested=" + splitAt + " clicked=" + clickedSplitAt
				+ " expectedTaskProgress=" + currentProgress
				+ " assignments before=" + originalAssignmentState
				+ " after=" + assignmentScheduleStates(fixture.predecessor), failure);
		}
		List<String> splitIntervals = assignmentIntervals(fixture.predecessor);
		assertEquals(originalIntervals, splitIntervals, "task-owned split must preserve resource assignment intervals");
		assertEquals(originalContours, assignmentContours(fixture.predecessor),
			"task-owned split must preserve resource assignment contours");
		List<String> splitTaskIntervals = taskIntervals(fixture.predecessor);
		long normalizedSplitStart = fixture.predecessor.getStart()
			+ fixture.predecessor.getTaskSplitIntervals().getFirst().startOffset();
		Point normalizedSplitPoint = screenPointForTaskDate(fixture, normalizedSplitStart);
		assertTrue(gapStartsAtPixel(fixture, splitTaskIntervals, normalizedSplitPoint.x),
			"the task intervals consumed by the Gantt renderer must expose the visible split gap: "
				+ splitTaskIntervals + " splits=" + fixture.predecessor.getTaskSplitIntervals()
				+ " schedule=" + fixture.predecessor.getStart() + ".." + fixture.predecessor.getEnd()
				+ " clicked=" + clickedSplitAt + " clickPixel=" + splitStart.x
				+ " normalizedPixel=" + normalizedSplitPoint.x);
		assertTrue(barBounds(fixture).width > 0, "the split task must remain visible after redraw");
		SwingUtilities.invokeAndWait(() -> fixture.project.getUndoController().undo());
		assertEquals(originalIntervals, assignmentIntervals(fixture.predecessor),
			"task-owned split must not change assignment work intervals");
		assertEquals(originalTaskIntervals, taskIntervals(fixture.predecessor),
			"one Undo must restore the original intervals consumed by Gantt");
		assertEquals(originalTaskSplits, fixture.predecessor.getTaskSplitIntervals(),
			"one Undo must remove the task-owned split interval");
		SwingUtilities.invokeAndWait(() -> fixture.project.getUndoController().redo());
		assertEquals(originalIntervals, assignmentIntervals(fixture.predecessor),
			"one Redo must preserve assignment work intervals");
		assertEquals(splitTaskIntervals, taskIntervals(fixture.predecessor),
			"one Redo must restore the split gap consumed by Gantt");
		List<com.microproject.pm.task.TaskSplitInterval> persistedTaskSplits = fixture.predecessor.getTaskSplitIntervals();

		List<String> persistedIntervals = assignmentIntervals(fixture.predecessor);
		List<String> persistedTaskIntervals = taskIntervals(fixture.predecessor);
		long scheduleStartBeforeSave = fixture.predecessor.getStart();
		long scheduleFinishBeforeSave = fixture.predecessor.getEnd();

		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		MpoFileImporter importer = new MpoFileImporter();
		assertTrue(importer.saveProject(fixture.project, saved), "edited schedule must be serializable");
		byte[] snapshotXml = projectXmlEntry(saved.toByteArray());
		net.sf.mpxj.ProjectFile mspSnapshot = new net.sf.mpxj.mspdi.MSPDIReader()
			.read(new ByteArrayInputStream(snapshotXml));
		net.sf.mpxj.Task mspTask = mspSnapshot.getTasks().stream()
			.filter(task -> "Drag predecessor".equals(task.getName())).findFirst().orElseThrow();
		Project reopened = importer.loadProject(new ByteArrayInputStream(saved.toByteArray()));
		NormalTask reopenedTask = reopened.getTaskList().stream()
			.filter(task -> "Drag predecessor".equals(task.getName())).map(NormalTask.class::cast).findFirst().orElseThrow();
		assertEquals(scheduleStartBeforeSave, reopenedTask.getStart(), "final schedule start must survive MPO reload: project="
			+ reopened.getStart() + " actualStart=" + reopenedTask.getActualStart() + " constraint="
			+ reopenedTask.getConstraintTypeKind() + " constraintDate=" + reopenedTask.getConstraintDate()
			+ " snapshotStart=" + mspTask.getStart() + " snapshotActualStart=" + mspTask.getActualStart()
			+ " snapshotConstraint=" + mspTask.getConstraintType() + " snapshotConstraintDate=" + mspTask.getConstraintDate());
		assertEquals(scheduleFinishBeforeSave, reopenedTask.getEnd(), "final schedule finish must survive MPO reload");
		int savedProgressPixel = screenPointForTaskDate(fixture, updatedProgress).x;
		int restoredProgressPixel = screenPointForTaskDate(fixture, reopenedTask.getCompletedThrough()).x;
		List<String> snapshotAssignments = mspSnapshot.getResourceAssignments().stream()
			.filter(assignment -> java.util.Objects.equals(assignment.getTaskUniqueID(), mspTask.getUniqueID()))
			.map(assignment -> "uid=" + assignment.getUniqueID() + " bounds=" + assignment.getStart() + ".."
				+ assignment.getFinish() + " work=" + assignment.getWork() + " actual=" + assignment.getActualWork()
				+ " remaining=" + assignment.getRemainingWork() + " units=" + assignment.getUnits()
				+ " actualRows=" + assignment.getTimephasedActualWork() + " remainingRows="
				+ assignment.getTimephasedWork()).toList();
		assertTrue(Math.abs(savedProgressPixel - restoredProgressPixel) <= 1,
			"progress must survive MPO reload within one rendered pixel: saved=" + updatedProgress
				+ " restored=" + reopenedTask.getCompletedThrough() + " savedPixel=" + savedProgressPixel
				+ " restoredPixel=" + restoredProgressPixel + " modelPercent=" + fixture.predecessor.getPercentComplete()
				+ " restoredPercent=" + reopenedTask.getPercentComplete() + " snapshotPercent="
				+ mspTask.getPercentageComplete() + " snapshotPercentWork=" + mspTask.getPercentageWorkComplete()
				+ " actualDuration=" + mspTask.getActualDuration() + " stop=" + mspTask.getStop()
				+ " resume=" + mspTask.getResume() + " start=" + mspTask.getStart() + " finish=" + mspTask.getFinish()
				+ " modelAssignments=" + assignmentScheduleStates(fixture.predecessor)
				+ " snapshotAssignments=" + snapshotAssignments + " restoredAssignments="
				+ assignmentScheduleStates(reopenedTask) + " taskSplits=" + persistedTaskSplits);
		assertIntervalsMatchAtRenderedResolution(fixture, persistedIntervals, assignmentIntervals(reopenedTask),
			"assignment intervals must survive MPO reload");
		assertEquals(persistedTaskSplits, reopenedTask.getTaskSplitIntervals(),
			"task-owned split intervals must survive MPO reload");
		assertEquals(persistedTaskIntervals, taskIntervals(reopenedTask),
			"the split gap rendered by Gantt must survive MPO reload");
		capture(robot, "gantt-schedule-gestures-final.png");
	}

	private void dragBarAndAssertSuccessor(Fixture fixture) throws Exception {
		long oldStart = fixture.predecessor.getStart();
		long oldEnd = fixture.predecessor.getEnd();
		long oldSuccessorStart = fixture.successor.getStart();
		long oldSuccessorEnd = fixture.successor.getEnd();
		showFixture(fixture);
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
			gantt.requestFocusInWindow();
		});
		GuiAcceptanceSupport.await(gantt::isShowing, "Gantt was not visible");
		activateWindow(robot);
		Rectangle bar = barBounds(fixture);
		Rectangle oldPredecessorBar = fixture.fridayFsFixture ? barBounds(fixture, fixture.predecessor) : null;
		Rectangle oldSuccessorBar = fixture.fridayFsFixture ? barBounds(fixture, fixture.successor) : null;
		capture(robot);
		SwingUtilities.invokeAndWait(() -> assertTrue(gantt.getUI().getNodeAt(
			bar.x - gantt.getLocationOnScreen().x + bar.width / 2,
			bar.y - gantt.getLocationOnScreen().y + bar.height / 2) != null,
			"computed drag point must hit the visible Gantt bar: " + bar + " gantt=" + gantt.getBounds()));
		long twoDays = 2L * com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		int delta = (int) Math.round(gantt.getCoord().toW(twoDays));
		assertTrue(delta > 0, "Gantt scale must produce a positive drag distance");
		robot.mouseMove(bar.x + Math.max(2, bar.width / 2), bar.y + bar.height / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		int targetX = bar.x + Math.max(2, bar.width / 2) + delta;
		int targetY = bar.y + bar.height / 2;
		int startX = bar.x + Math.max(2, bar.width / 2);
		for (int step = 1; step <= 8; step++)
			robot.mouseMove(startX + (targetX - startX) * step / 8, targetY);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> fixture.predecessor.getStart() > oldStart,
			"Gantt bar drag did not move the predecessor: oldStart=" + oldStart
				+ " actual=" + fixture.predecessor.getStart() + " delta=" + delta
				+ " bar=" + bar + " gantt=" + gantt.getBounds());
		if (projectManager != null) {
			var outcome = projectManager.getLastRibbonCommandResult();
			assertEquals(com.microproject.ribbon.RibbonCommandResult.Status.CHANGED, outcome.status());
			assertEquals(List.of(fixture.predecessor.getUniqueId()), outcome.selectedTaskIds());
			assertEquals(List.of(fixture.predecessor.getUniqueId()), outcome.affectedTaskIds());
			assertEquals("Gantt", outcome.activeViewId());
		}
		SwingUtilities.invokeAndWait(fixture.project::recalculate);
		if (fixture.dependency.getDependencyType() == DependencyType.FF) {
			assertEquals(fixture.predecessor.getEnd(), fixture.successor.getEnd(),
				"FF successor finish must match the dragged predecessor finish: pred="
					+ fixture.predecessor.getStart() + ".." + fixture.predecessor.getEnd()
					+ " successor=" + fixture.successor.getStart() + ".." + fixture.successor.getEnd()
					+ " early=" + fixture.successor.getEarlyStart() + ".." + fixture.successor.getEarlyFinish());
		} else if (fixture.dependency.getDependencyType() == DependencyType.SF) {
			// Microsoft defines SF as a successor that cannot finish until its
			// predecessor starts; it may finish later. This is the minimum finish
			// boundary, not a maximum. The separate upper bound below is only this
			// one-day fixture's Standard-calendar schedule expectation. Source:
			// https://support.microsoft.com/en-us/project/how-project-schedules-tasks-behind-the-scenes
			assertTrue(fixture.successor.getEnd() >= fixture.predecessor.getStart(),
				"SF successor must not finish before the dragged predecessor starts: predecessor="
					+ fixture.predecessor.getStart() + ".." + fixture.predecessor.getEnd() + " successor="
					+ fixture.successor.getStart() + ".." + fixture.successor.getEnd() + " mode="
					+ (fixture.successor.isManuallyScheduled() ? "manual" : "auto") + " calendar="
					+ fixture.successor.getEffectiveWorkCalendar().getName());
			long fixtureLatestFinish = addStandardWork(fixture.predecessor.getStart(), 8L * 60L * 60L * 1000L);
			assertTrue(fixture.successor.getEnd() <= fixtureLatestFinish,
				"one-day Standard-calendar successor should finish within one workday of the SF boundary: "
					+ "predecessor=" + fixture.predecessor.getStart() + ".." + fixture.predecessor.getEnd()
					+ " successor=" + fixture.successor.getStart() + ".." + fixture.successor.getEnd()
					+ " fixtureLatestFinish=" + fixtureLatestFinish);
		} else if (fixture.dependency.getDependencyType() == DependencyType.FS && fixture.fridayFsFixture) {
			long expected = fixture.dependency.calcForwardDependencyDate(fixture.predecessor.getStart(), fixture.predecessor.getEnd(), true);
			java.util.Calendar fridaySeventeen = DateTime.calendarInstance(2026, java.util.Calendar.JUNE, 12);
			fridaySeventeen.set(java.util.Calendar.HOUR_OF_DAY, 17);
			java.util.Calendar mondayEight = DateTime.calendarInstance(2026, java.util.Calendar.JUNE, 15);
			mondayEight.set(java.util.Calendar.HOUR_OF_DAY, 8);
			// Microsoft defines FS as a minimum start boundary; task calendars move
			// an ASAP successor to the next available work time. Here Friday 17:00
			// therefore schedules Monday 08:00. This is document-derived, not MSP-measured.
			// https://support.microsoft.com/en-us/project/how-project-schedules-tasks-behind-the-scenes
			assertEquals(fridaySeventeen.getTimeInMillis(), fixture.predecessor.getEnd(),
				"dragged predecessor must finish Friday at the Standard workday end");
			assertTrue(fixture.successor.getStart() >= expected,
				"FS successor must not start before the predecessor finish: boundary=" + expected
					+ " predecessor=" + fixture.predecessor.getStart() + ".." + fixture.predecessor.getEnd());
			assertEquals(mondayEight.getTimeInMillis(), fixture.successor.getStart(),
				"Standard-calendar ASAP successor must start Monday after Friday's finish boundary");
			long movedStart = fixture.predecessor.getStart();
			long movedEnd = fixture.predecessor.getEnd();
			long movedSuccessorStart = fixture.successor.getStart();
			long movedSuccessorEnd = fixture.successor.getEnd();
			Rectangle movedPredecessorBar = barBounds(fixture, fixture.predecessor);
			Rectangle movedSuccessorBar = barBounds(fixture, fixture.successor);
			long predecessorId = fixture.predecessor.getUniqueId();
			long successorId = fixture.successor.getUniqueId();
			assertTrue(predecessorId > 0 && successorId > 0,
				"the MPO replay check needs stable positive task IDs");
			assertTrue(fixture.dependency != null && fixture.dependency.getDependencyType() == DependencyType.FS,
				"the existing FS link must remain part of the dragged-date transaction");

			// The Robot above proves the physical drag route. Exercise its one history
			// entry through the project controller, matching the neighboring auto-scroll
			// test's history layer without inventing a separate keyboard binding path.
			SwingUtilities.invokeAndWait(() -> fixture.project.getUndoController().undo());
			assertEquals(oldStart, fixture.predecessor.getStart(), "one Undo must restore predecessor start");
			assertEquals(oldEnd, fixture.predecessor.getEnd(), "one Undo must restore predecessor finish");
			assertEquals(oldSuccessorStart, fixture.successor.getStart(), "one Undo must restore FS successor start");
			assertEquals(oldSuccessorEnd, fixture.successor.getEnd(), "one Undo must restore FS successor finish");
			assertTrue(fixture.predecessor.getSuccessorList().contains(fixture.dependency),
				"Undo of a date drag must keep the FS link attached to the predecessor");
			assertTrue(fixture.successor.getPredecessorList().contains(fixture.dependency),
				"Undo of a date drag must keep the FS link attached to the successor");
			SwingUtilities.invokeAndWait(() -> {
				gantt.updateSize();
				gantt.revalidate();
				gantt.repaint();
				frame.repaint();
			});
			SwingUtilities.invokeAndWait(() -> { });
			assertEquals(oldPredecessorBar, barBounds(fixture, fixture.predecessor),
				"redrawn predecessor bar must return to its original position after Undo");
			assertEquals(oldSuccessorBar, barBounds(fixture, fixture.successor),
				"redrawn FS successor bar must return to its original position after Undo");
			SwingUtilities.invokeAndWait(() -> fixture.project.getUndoController().redo());
			assertEquals(movedStart, fixture.predecessor.getStart(), "one Redo must restore dragged predecessor start");
			assertEquals(movedEnd, fixture.predecessor.getEnd(), "one Redo must restore dragged predecessor finish");
			assertEquals(movedSuccessorStart, fixture.successor.getStart(), "one Redo must restore FS successor start");
			assertEquals(movedSuccessorEnd, fixture.successor.getEnd(), "one Redo must restore FS successor finish");
			assertTrue(fixture.predecessor.getSuccessorList().contains(fixture.dependency),
				"Redo must retain the FS link attached to the predecessor");
			assertTrue(fixture.successor.getPredecessorList().contains(fixture.dependency),
				"Redo must retain the FS link attached to the successor");
			SwingUtilities.invokeAndWait(() -> {
				gantt.updateSize();
				gantt.revalidate();
				gantt.repaint();
				frame.repaint();
			});
			SwingUtilities.invokeAndWait(() -> { });
			assertEquals(movedPredecessorBar, barBounds(fixture, fixture.predecessor),
				"redrawn predecessor bar must return to the dragged position after Redo");
			assertEquals(movedSuccessorBar, barBounds(fixture, fixture.successor),
				"redrawn FS successor bar must return to its recalculated position after Redo");

			ByteArrayOutputStream saved = new ByteArrayOutputStream();
			MpoFileImporter importer = new MpoFileImporter();
			assertTrue(importer.saveProject(fixture.project, saved), "dragged linked schedules must be serializable");
			Project reopened = importer.loadProject(new ByteArrayInputStream(saved.toByteArray()));
			assertTrue(reopened != null, "MPO must reopen the dragged linked schedule");
			reopened.recalculate();
			NormalTask reopenedPredecessor = reopened.getTaskList().stream()
				.map(NormalTask.class::cast)
				.filter(task -> task.getUniqueId() == predecessorId).findFirst().orElseThrow();
			NormalTask reopenedSuccessor = reopened.getTaskList().stream()
				.map(NormalTask.class::cast)
				.filter(task -> task.getUniqueId() == successorId).findFirst().orElseThrow();
			assertEquals(movedStart, reopenedPredecessor.getStart(), "MPO reload must preserve dragged predecessor start");
			assertEquals(movedEnd, reopenedPredecessor.getEnd(), "MPO reload must preserve dragged predecessor finish");
			assertEquals(movedSuccessorStart, reopenedSuccessor.getStart(), "MPO reload must preserve FS successor start");
			assertEquals(movedSuccessorEnd, reopenedSuccessor.getEnd(), "MPO reload must preserve FS successor finish");
			Dependency reopenedLink = reopenedSuccessor.getPredecessorList().stream()
				.map(Dependency.class::cast)
				.filter(link -> ((NormalTask) link.getPredecessor()).getUniqueId() == predecessorId)
				.findFirst().orElseThrow();
			assertEquals(DependencyType.FS, reopenedLink.getDependencyType(),
				"MPO reload must preserve the predecessor endpoint and FS link type");
			assertTrue(reopenedPredecessor.getSuccessorList().contains(reopenedLink),
				"MPO reload must retain the reciprocal predecessor association");
			assertTrue(reopenedSuccessor.getPredecessorList().contains(reopenedLink),
				"MPO reload must retain the reciprocal successor association");
		} else {
			long expected = fixture.dependency.calcForwardDependencyDate(fixture.predecessor.getStart(), fixture.predecessor.getEnd(), true);
			assertEquals(expected, fixture.successor.getStart(), "Successor must match " + DependencyType.toLongString(fixture.dependency.getDependencyType())
				+ " date implied by the dragged bar");
		}
		capture(robot);
	}

	private static long addStandardWork(long start, long workMillis) {
		java.util.Calendar time = com.microproject.util.DateTime.calendarInstance();
		time.setTimeInMillis(start);
		long remaining = workMillis;
		while (remaining > 0L) {
			int day = time.get(java.util.Calendar.DAY_OF_WEEK);
			if (day == java.util.Calendar.SATURDAY || day == java.util.Calendar.SUNDAY) {
				time.add(java.util.Calendar.DAY_OF_MONTH, day == java.util.Calendar.SATURDAY ? 2 : 1);
				setStandardTime(time, 8);
				continue;
			}
			int hour = time.get(java.util.Calendar.HOUR_OF_DAY);
			if (hour < 8) setStandardTime(time, 8);
			else if (hour >= 17) {
				time.add(java.util.Calendar.DAY_OF_MONTH, 1);
				setStandardTime(time, 8);
			} else if (hour >= 12 && hour < 13) setStandardTime(time, 13);
			long sessionEnd = time.getTimeInMillis();
			time.set(java.util.Calendar.HOUR_OF_DAY, time.get(java.util.Calendar.HOUR_OF_DAY) < 12 ? 12 : 17);
			time.set(java.util.Calendar.MINUTE, 0);
			time.set(java.util.Calendar.SECOND, 0);
			time.set(java.util.Calendar.MILLISECOND, 0);
			long available = time.getTimeInMillis() - sessionEnd;
			long consumed = Math.min(remaining, available);
			time.setTimeInMillis(sessionEnd + consumed);
			remaining -= consumed;
		}
		return time.getTimeInMillis();
	}

	private static void setStandardTime(java.util.Calendar time, int hour) {
		time.set(java.util.Calendar.HOUR_OF_DAY, hour);
		time.set(java.util.Calendar.MINUTE, 0);
		time.set(java.util.Calendar.SECOND, 0);
		time.set(java.util.Calendar.MILLISECOND, 0);
	}

	private void activateWindow(Robot robot) throws Exception {
		Rectangle bounds = new Rectangle(frame.getLocationOnScreen(), frame.getSize());
		robot.mouseMove(bounds.x + Math.min(40, bounds.width / 2), bounds.y + 12);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	private void showFixture(Fixture fixture) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			frame = new JFrame("Gantt bar date drag GUI acceptance");
			frame.add(new JScrollPane(gantt));
			frame.setPreferredSize(new Dimension(1100, 520));
			frame.pack();
			frame.setLocationByPlatform(true);
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
		});
	}

	private void prepareOutcomeDocument(Fixture fixture) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			projectWindow = new MainRibbonFrame("Gantt schedule outcome GUI acceptance", null);
			projectManager = new GraphicManager(projectWindow);
			projectWindow.setGraphicManager(projectManager);
			projectManager.initView();
			DocumentFrame document = projectManager.addProjectFrame(fixture.project);
			document.activateGanttView();
		});
	}

	private Rectangle barBounds(Fixture fixture) throws Exception {
		return barBounds(fixture, fixture.predecessor);
	}

	private List<String> assignmentIntervals(NormalTask task) {
		AtomicReference<List<String>> snapshot = new AtomicReference<>();
		try {
			SwingUtilities.invokeAndWait(() -> {
				List<String> intervals = new ArrayList<>();
				if (task.getAssignments().isEmpty()) {
					snapshot.set(List.of());
					return;
				}
				((Assignment) task.getAssignments().get(0)).consumeIntervals(
					interval -> intervals.add(interval.getStart() + ":" + interval.getEnd()));
				snapshot.set(List.copyOf(intervals));
			});
		} catch (Exception exception) {
			throw new AssertionError("Could not snapshot task intervals on the EDT", exception);
		}
		return snapshot.get();
	}

	private List<String> taskIntervals(NormalTask task) {
		AtomicReference<List<String>> snapshot = new AtomicReference<>();
		try {
			SwingUtilities.invokeAndWait(() -> {
				List<String> intervals = new ArrayList<>();
				task.consumeIntervals(interval -> intervals.add(interval.getStart() + ":" + interval.getEnd()));
				snapshot.set(List.copyOf(intervals));
			});
		} catch (Exception exception) {
			throw new AssertionError("Could not snapshot rendered task intervals on the EDT", exception);
		}
		return snapshot.get();
	}

	private boolean gapStartsAtPixel(Fixture fixture, List<String> intervals, int screenX) throws Exception {
		Long previousEnd = null;
		for (String interval : intervals) {
			String[] boundaries = interval.split(":", -1);
			long start = Long.parseLong(boundaries[0]);
			long end = Long.parseLong(boundaries[1]);
			if (previousEnd != null && previousEnd < start
				&& screenPointForTaskDate(fixture, previousEnd).x == screenX)
				return true;
			previousEnd = end;
		}
		return false;
	}

	private List<String> assignmentContours(NormalTask task) {
		AtomicReference<List<String>> snapshot = new AtomicReference<>();
		try {
			SwingUtilities.invokeAndWait(() -> {
				List<String> contours = new ArrayList<>();
				for (var association : task.getAssignments()) {
					Assignment assignment = (Assignment) association;
					contours.add(assignment.getWorkContour().getTypeKind() + ":"
						+ assignment.getWorkContour().toString(assignment.getDuration()));
				}
				snapshot.set(List.copyOf(contours));
			});
		} catch (Exception exception) {
			throw new AssertionError("Could not snapshot assignment work contours on the EDT", exception);
		}
		return snapshot.get();
	}

	private List<String> assignmentScheduleStates(NormalTask task) {
		AtomicReference<List<String>> snapshot = new AtomicReference<>();
		try {
			SwingUtilities.invokeAndWait(() -> {
				List<String> states = new ArrayList<>();
				states.add("task start=" + task.getStart() + ",progress=" + task.getCompletedThrough()
					+ ",resume=" + task.getResume() + ",end=" + task.getEnd());
				for (var association : task.getAssignments()) {
					Assignment assignment = (Assignment) association;
					states.add("start=" + assignment.getStart() + ",resume=" + assignment.getResume()
						+ ",stop=" + assignment.getStop() + ",end=" + assignment.getEnd()
						+ ",completed=" + assignment.getCompletedThrough());
				}
				snapshot.set(List.copyOf(states));
			});
		} catch (Exception exception) {
			throw new AssertionError("Could not snapshot assignment schedule state on the EDT", exception);
		}
		return snapshot.get();
	}

	private long taskDateAt(Point screenPoint) {
		AtomicReference<Long> date = new AtomicReference<>();
		try {
			SwingUtilities.invokeAndWait(() -> {
				int localX = screenPoint.x - gantt.getLocationOnScreen().x;
				date.set((long)gantt.getCoord().toTime(localX));
			});
		} catch (Exception exception) {
			throw new AssertionError("Could not resolve the Gantt split point on the EDT", exception);
		}
		return date.get();
	}

	private void activateGantt(Robot robot) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
			gantt.requestFocusInWindow();
		});
		GuiAcceptanceSupport.await(gantt::isShowing, "Gantt was not visible");
		activateWindow(robot);
	}

	private void drag(Robot robot, Point start, Point end) {
		robot.mouseMove(start.x, start.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		for (int step = 1; step <= 8; step++)
			robot.mouseMove(start.x + (end.x - start.x) * step / 8,
				start.y + (end.y - start.y) * step / 8);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	private void click(Robot robot, Point point) {
		robot.mouseMove(point.x, point.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	private void openPopupAndChoose(Robot robot, Point point, String itemText) throws Exception {
		robot.mouseMove(point.x, point.y);
		robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
		robot.waitForIdle();
		JMenuItem[] target = new JMenuItem[1];
		GuiAcceptanceSupport.await(() -> findSelectedMenuItem(itemText, target),
			"Gantt popup item was not shown: " + itemText);
		Point[] itemCenter = new Point[1];
		SwingUtilities.invokeAndWait(() -> {
			Point itemLocation = target[0].getLocationOnScreen();
			itemCenter[0] = new Point(itemLocation.x + target[0].getWidth() / 2,
				itemLocation.y + target[0].getHeight() / 2);
		});
		robot.mouseMove(itemCenter[0].x, itemCenter[0].y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	private boolean findSelectedMenuItem(String text, JMenuItem[] target) {
		boolean[] found = new boolean[1];
		try {
			SwingUtilities.invokeAndWait(() -> {
				for (MenuElement element : MenuSelectionManager.defaultManager().getSelectedPath()) {
					for (MenuElement child : element.getSubElements()) {
						if (child.getComponent() instanceof JMenuItem item && text.equals(item.getText())) {
							target[0] = item;
							found[0] = true;
							break;
						}
					}
					if (found[0]) break;
				}
			});
		} catch (Exception exception) {
			throw new AssertionError("Could not inspect the Gantt popup on the EDT", exception);
		}
		return found[0];
	}

	private Point viewportViewPosition(javax.swing.JViewport viewport) throws Exception {
		AtomicReference<Point> position = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> position.set(viewport.getViewPosition()));
		return position.get();
	}

	private Rectangle viewportScreenBounds(javax.swing.JViewport viewport) throws Exception {
		AtomicReference<Rectangle> bounds = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> bounds.set(
			new Rectangle(viewport.getLocationOnScreen(), viewport.getExtentSize())));
		return bounds.get();
	}

	private Point screenPointForTaskDate(Fixture fixture, long date) throws Exception {
		Point[] result = new Point[1];
		SwingUtilities.invokeAndWait(() -> {
			var projection = fixture.cache.getVisibleNodes().getProjectionIndex();
			GraphicNode node = null;
			int row = -1;
			for (int candidate = 0; candidate < projection.size(); candidate++) {
				GraphicNode visible = projection.nodeAt(candidate);
				if (visible.getNode().getImpl() == fixture.predecessor) {
					node = visible;
					row = candidate;
					break;
				}
			}
			if (node == null)
				throw new AssertionError("Gantt task is absent from its projection");
			int x = (int)Math.round(gantt.getCoord().toX(date));
			int y = (int)Math.round(((GanttUI)gantt.getUI()).getBarY(row)
				+ node.getGanttShapeOffset() + node.getGanttShapeHeight() / 2.0d);
			Point location = gantt.getLocationOnScreen();
			result[0] = new Point(location.x + x, location.y + y);
		});
		return result[0];
	}

	private Point findResizeHandle(Robot robot, Fixture fixture, boolean leading, int expectedCursor, String gesture)
			throws Exception {
		int[] barRowAndY = new int[2];
		Point[] location = new Point[1];
		int[] range = new int[] {Integer.MAX_VALUE, Integer.MIN_VALUE};
		SwingUtilities.invokeAndWait(() -> {
			var projection = fixture.cache.getVisibleNodes().getProjectionIndex();
			for (int row = 0; row < projection.size(); row++) {
				GraphicNode node = projection.nodeAt(row);
				if (node.getNode().getImpl() == fixture.predecessor) {
					barRowAndY[0] = row;
					barRowAndY[1] = (int)Math.round(((GanttUI)gantt.getUI()).getBarY(row)
						+ node.getGanttShapeOffset() + 1);
					location[0] = gantt.getLocationOnScreen();
					for (int x = 0; x < gantt.getWidth(); x++) {
						GraphZone zone = gantt.getUI().getNodeAt(x, barRowAndY[1]);
						if (zone != null && zone.getObject() instanceof GraphicNode hitNode
								&& hitNode.getNode().getImpl() == fixture.predecessor) {
							range[0] = Math.min(range[0], x);
							range[1] = Math.max(range[1], x);
						}
					}
					return;
				}
			}
			throw new AssertionError("Gantt task is absent from its projection");
		});
		if (range[0] > range[1])
			throw new AssertionError("No rendered Gantt bar hit region for " + gesture);

		int step = leading ? 1 : -1;
		for (int x = leading ? range[0] : range[1]; leading ? x <= range[1] : x >= range[0]; x += step) {
			robot.mouseMove(location[0].x + x, location[0].y + barRowAndY[1]);
			robot.waitForIdle();
			if (cursorTypeAt() == expectedCursor)
				return new Point(location[0].x + x, location[0].y + barRowAndY[1]);
		}
		throw new AssertionError(gesture + " did not expose cursor " + expectedCursor
			+ " in the rendered task hit range " + range[0] + ".." + range[1]);
	}

	private void assertTaskBarHit(Fixture fixture, Point screenPoint) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Point location = gantt.getLocationOnScreen();
			GraphZone hit = gantt.getUI().getNodeAt(screenPoint.x - location.x, screenPoint.y - location.y);
			assertTrue(hit != null && hit.getObject() instanceof GraphicNode hitNode
				&& hitNode.getNode().getImpl() == fixture.predecessor,
				"gesture origin must hit the task's rendered Gantt bar: " + screenPoint);
		});
	}

	private void assertProgressBarHit(Fixture fixture, Point screenPoint) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Point location = gantt.getLocationOnScreen();
			GraphZone hit = gantt.getUI().getNodeAt(screenPoint.x - location.x, screenPoint.y - location.y);
			assertTrue(hit != null && hit.getObject() instanceof GraphicNode hitNode
					&& hitNode.getNode().getImpl() == fixture.predecessor
					&& hit.getZoneId() == GanttUI.PROGRESS_BAR_ZONE_ID,
				"progress gesture origin must hit the task's rendered progress bar: " + screenPoint);
		});
	}

	private void assertResizeCursor(Robot robot, Point screenPoint, int cursorType, String gesture) throws Exception {
		robot.mouseMove(screenPoint.x, screenPoint.y);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(() -> {
			int[] actualCursor = new int[1];
			try {
				SwingUtilities.invokeAndWait(() -> actualCursor[0] = gantt.getCursor().getType());
			} catch (Exception exception) {
				throw new AssertionError("Could not inspect Gantt resize cursor", exception);
			}
			return actualCursor[0] == cursorType;
		}, gesture + " handle did not expose the resize cursor at " + screenPoint
			+ " (expected=" + cursorType + ", actual=" + cursorTypeAt() + ")");
	}

	private int cursorTypeAt() {
		int[] actualCursor = new int[1];
		try {
			SwingUtilities.invokeAndWait(() -> actualCursor[0] = gantt.getCursor().getType());
		} catch (Exception exception) {
			throw new AssertionError("Could not inspect Gantt cursor after resize hit-test", exception);
		}
		return actualCursor[0];
	}

	private Rectangle barBounds(Fixture fixture, NormalTask task) throws Exception {
		Rectangle[] result = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> {
			int rowHeight = gantt.getRowHeight();
			Point location = gantt.getLocationOnScreen();
			for (int y = 0; y < Math.min(gantt.getHeight(), rowHeight * 10) && result[0] == null; y += 2) {
				for (int x = 0; x < gantt.getWidth() && result[0] == null; x += 2) {
					GraphZone zone = gantt.getUI().getNodeAt(x, y);
					if (zone != null && zone.getObject() instanceof GraphicNode node
							&& node.getNode().getImpl() == task) {
						int row = gantt.getModel().getCache().getVisibleNodes().getProjectionIndex().rowForNode(node);
						int barY = (int) Math.round(((GanttUI) gantt.getUI()).getBarY(row)
							+ node.getGanttShapeOffset() + node.getGanttShapeHeight() / 2.0d);
						int barCenter = (int) Math.round((gantt.getCoord().toX(task.getStart())
							+ gantt.getCoord().toX(task.getEnd())) / 2.0d);
						result[0] = new Rectangle(location.x + barCenter - 4,
							location.y + barY - 4, 8, 8);
					}
				}
			}
			if (result[0] == null)
				throw new AssertionError("no interactive Gantt bar was rendered; cacheSize=" + fixture.cache.getSize()
					+ " gantt=" + gantt.getSize());
		});
		return result[0];
	}

	private void capture(Robot robot) throws Exception {
		capture(robot, "gantt-bar-date-drag.png");
	}

	private void capture(Robot robot, String fileName) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> bounds[0] = new Rectangle(frame.getRootPane().getLocationOnScreen(), frame.getRootPane().getSize()));
		BufferedImage image = robot.createScreenCapture(bounds[0]);
		Path directory = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/guiTest-artifacts"));
		Files.createDirectories(directory);
		javax.imageio.ImageIO.write(image, "png", directory.resolve(fileName).toFile());
	}

	private Fixture createFixture(int dependencyType) throws Exception {
		return createFixture(dependencyType, true);
	}

	private Fixture createFixture(int dependencyType, boolean includeDependency) throws Exception {
		return createFixture(dependencyType, includeDependency, false);
	}

	private Fixture createFixture(int dependencyType, boolean includeDependency, boolean assignPredecessor)
			throws Exception {
		return createFixture(dependencyType, includeDependency, assignPredecessor, null);
	}

	private Fixture createFixture(int dependencyType, boolean includeDependency, boolean assignPredecessor,
			Long projectStart) throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("gui-gantt-date-drag", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		if (projectStart != null) {
			project.setStart(projectStart);
			project.setStartConstraint(projectStart);
		}
		NormalTask predecessor = task(project, "Drag predecessor", 3L);
		NormalTask successor = task(project, "Drag successor", 1L);
		long predecessorStart = projectStart != null ? projectStart
			: DateTime.calendarInstance(2026, java.util.Calendar.JUNE, 8).getTimeInMillis();
		predecessor.getCurrentSchedule().setStart(predecessorStart);
		if (projectStart != null) predecessor.setScheduleConstraint(
			com.microproject.pm.scheduling.ConstraintType.Kind.SNET, predecessorStart);
		predecessor.setDuration(3L * com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
		if (assignPredecessor) {
			var resource = project.getResourcePool().newResourceInstance();
			AssignmentService.getInstance().newAssignment(predecessor, resource, 1.0D, 0L, this);
		}
		Dependency dependency = includeDependency
			? DependencyService.getInstance().newDependency(predecessor, successor, dependencyType, 0L, project) : null;
		project.recalculate();
		final Fixture[] result = new Fixture[1];
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
				NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()), "gui-gantt-date-drag", null);
			SpreadSheetUtils.setFieldsAndContext(sheet, cache, SpreadSheetCategories.taskSpreadsheetCategory, "Spreadsheet.Task.entry", true);
			gantt = new Gantt(project, "Gantt");
			gantt.setCache(cache);
			gantt.setCoord(new CoordinatesConverter(project));
			gantt.setBarStyles((BarStyles) Dictionary.get(BarStyles.category, "standard"));
			gantt.updateSize();
			result[0] = new Fixture(project, predecessor, successor, dependency, cache, projectStart != null);
		});
		return result[0];
	}

	private static NormalTask task(Project project, String name, long days) {
		NormalTask task = project.createScriptedTask();
		task.setName(name);
		task.getCurrentSchedule().setStart(project.getStart());
		task.setDuration(days * com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
		return task;
	}

	private static byte[] projectXmlEntry(byte[] mpo) throws Exception {
		try (java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(new ByteArrayInputStream(mpo))) {
			java.util.zip.ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if ("content.xml".equals(entry.getName())) return zip.readAllBytes();
			}
		}
		throw new AssertionError("MPO snapshot is missing its MSPDI project entry");
	}

	private void assertIntervalsMatchAtRenderedResolution(Fixture fixture, List<String> expected,
			List<String> actual, String message) throws Exception {
		assertEquals(expected.size(), actual.size(), message + " interval count");
		for (int index = 0; index < expected.size(); index++) {
			String[] expectedBounds = expected.get(index).split(":");
			String[] actualBounds = actual.get(index).split(":");
			for (int bound = 0; bound < 2; bound++) {
				long expectedDate = Long.parseLong(expectedBounds[bound]);
				long actualDate = Long.parseLong(actualBounds[bound]);
				int expectedPixel = screenPointForTaskDate(fixture, expectedDate).x;
				int actualPixel = screenPointForTaskDate(fixture, actualDate).x;
				assertTrue(Math.abs(expectedPixel - actualPixel) <= 1,
					message + " interval=" + index + " bound=" + bound + " expected=" + expectedDate
						+ " actual=" + actualDate + " expectedPixel=" + expectedPixel + " actualPixel=" + actualPixel);
			}
		}
	}

	private record Fixture(Project project, NormalTask predecessor, NormalTask successor, Dependency dependency,
		NodeModelCache cache, boolean fridayFsFixture) { }
}

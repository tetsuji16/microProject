/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.util.Environment;

/**
 * The launcher starts the session from the main thread.  The first window must still be
 * built on the event dispatch thread, like every window opened later.
 */
class StartupThreadingGuiAcceptanceTest {
	private MainRibbonFrame window;
	private boolean previousRibbonUi;
	private boolean previousNewLook;
	private boolean previousClientSide;

	@AfterEach
	void closeWindow() throws Exception {
		if (window != null) SwingUtilities.invokeAndWait(window::dispose);
		Environment.setRibbonUI(previousRibbonUi);
		Environment.setNewLook(previousNewLook);
		Environment.setClientSide(previousClientSide);
	}

	@Test
	void sessionStartedFromTheLauncherThreadBuildsItsWindowOnTheEventDispatchThread() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		previousClientSide = Environment.isClientSide();
		Environment.setClientSide(true);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		assertFalse(SwingUtilities.isEventDispatchThread());
		SwingUtilities.invokeAndWait(() -> window = new MainRibbonFrame("microProject — startup threading", null));
		List<String> offEdt = java.util.Collections.synchronizedList(new ArrayList<>());
		List<String> steps = new ArrayList<>();
		List<String> additions = java.util.Collections.synchronizedList(new ArrayList<>());
		java.awt.event.ContainerAdapter recorder = new java.awt.event.ContainerAdapter() {
			@Override public void componentAdded(java.awt.event.ContainerEvent event) {
				additions.add(event.getChild().getClass().getSimpleName());
				if (!SwingUtilities.isEventDispatchThread()) offEdt.add("added " + event.getChild().getClass().getSimpleName());
			}
		};
		SwingUtilities.invokeAndWait(() -> {
			window.getContentPane().addContainerListener(recorder);
			window.getLayeredPane().addContainerListener(recorder);
			window.getRootPane().addContainerListener(recorder);
		});
		ApplicationStartupFactory factory = new ApplicationStartupFactory(new HashMap<>()) {
			@Override public void doStartupAction(GraphicManager gm, String[] projectUrls, boolean welcome) {
				record("startup action");
			}
			@Override public void doPostInitView(Container container) {
				record("post init view");
			}
			private void record(String step) {
				steps.add(step);
				if (!SwingUtilities.isEventDispatchThread()) offEdt.add(step);
			}
		};

		factory.instanceFromNewSession(window, false);

		GuiAcceptanceSupport.await(window::isShowing, "startup window did not become visible");
		assertEquals(List.of("startup action", "post init view"), steps);
		assertEquals(List.of(), offEdt, "startup steps ran outside the event dispatch thread");
		assertFalse(additions.isEmpty(), "startup did not populate the window");
	}
}

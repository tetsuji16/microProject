/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.testsupport.GuiAcceptanceSupport;

/**
 * Pressed state of the ribbon: an {@code EXCLUSIVE_VIEW} button stays pressed while its view is
 * active, a second click does not release it and switching views moves the state; a {@code MODE}
 * button (Format Painter) stays pressed until the mode ends. The view commands come from
 * {@code menuInternal.properties}.
 */
class RibbonViewStateGuiAcceptanceTest {
	private static final String FORMAT_PAINTER = "RibbonFormatPainter";

	@Test
	void aViewButtonStaysPressedOnReclickAndTheStateMovesWithTheView() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required");
		List<String> views = RibbonCommandGuiFixture.commandsWithActivation("EXCLUSIVE_VIEW");
		assertFalse(views.isEmpty(), "menuInternal.properties declares no EXCLUSIVE_VIEW command");
		List<String> failures = new ArrayList<>();
		try (RibbonCommandGuiFixture fixture = RibbonCommandGuiFixture.open(3)) {
			String previous = null;
			for (String view : views) {
				try {
					AbstractButton button = fixture.showCommand(view);
					if (button == null || !fixture.isEnabled(button)) continue;
					fixture.click(button);
					GuiAcceptanceSupport.await(() -> pressed(fixture, button), view + " is not pressed after its click");
					fixture.click(button);
					fixture.robot.waitForIdle();
					if (!fixture.isSelected(button)) failures.add(view + ": a second click released the button");
					if (previous != null) {
						AbstractButton old = fixture.showCommand(previous);
						if (old != null && fixture.isSelected(old)) {
							failures.add(previous + " stays pressed after switching to " + view);
						}
					}
					previous = view;
				} catch (AssertionError | Exception failure) {
					failures.add(view + ": " + failure.getMessage());
				}
			}
		}
		assertTrue(failures.isEmpty(), "EXCLUSIVE_VIEW buttons that did not behave:\n" + String.join("\n", failures));
	}

	@Test
	void formatPainterStaysPressedUntilTheModeEnds() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required");
		try (RibbonCommandGuiFixture fixture = RibbonCommandGuiFixture.open(3)) {
			fixture.clickRow(0);
			AbstractButton painter = fixture.showCommand(FORMAT_PAINTER);
			GuiAcceptanceSupport.await(() -> enabled(fixture, painter), "Format Painter is disabled with one task selected");

			fixture.click(painter);
			GuiAcceptanceSupport.await(() -> pressed(fixture, painter), "Format Painter is not pressed after its click");
			fixture.robot.waitForIdle();
			assertTrue(fixture.isSelected(painter), "Format Painter was released before the mode ended");

			fixture.clickRow(1);
			GuiAcceptanceSupport.await(() -> !pressed(fixture, painter), "Format Painter stays pressed after the paste");

			fixture.clickRow(0);
			fixture.click(painter);
			GuiAcceptanceSupport.await(() -> pressed(fixture, painter), "Format Painter did not re-arm");
			fixture.pressKey(KeyEvent.VK_ESCAPE);
			GuiAcceptanceSupport.await(() -> !pressed(fixture, painter), "Escape did not end the Format Painter mode");
		}
	}

	private static boolean pressed(RibbonCommandGuiFixture fixture, AbstractButton button) {
		try {
			return fixture.isSelected(button);
		} catch (Exception failure) {
			throw new AssertionError(failure);
		}
	}

	private static boolean enabled(RibbonCommandGuiFixture fixture, AbstractButton button) {
		try {
			return fixture.isEnabled(button);
		} catch (Exception failure) {
			throw new AssertionError(failure);
		}
	}
}

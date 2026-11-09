/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.testsupport;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;

/**
 * Marks a physical Robot test as the journey that proves one user input
 * mechanism end to end. The journey suite ({@code -PguiTestSuite=journeys})
 * runs exactly the tests carrying this annotation.
 *
 * <p>A journey proves that a real input reaches the canonical command and that
 * the result is visible. Command behavior (model change, rejection,
 * Undo/Redo, MPO reload) belongs to headless tests in {@code src/test}; do not
 * add a journey per command. See {@code docs/test-strategy.md}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Tag(GuiJourney.TAG)
public @interface GuiJourney {

	/** JUnit tag shared by every journey; the Gradle journey suite selects it. */
	String TAG = "gui-journey";

	/** The input mechanism this journey proves. */
	InputMechanism value();

	/** User input mechanisms that need one physical journey each. */
	enum InputMechanism {
		/** Ribbon button click that runs a document command. */
		RIBBON_CLICK,
		/** Keyboard shortcut handled by the active document. */
		KEYBOARD_SHORTCUT,
		/** Typing into a spreadsheet cell and committing the edit. */
		CELL_EDIT,
		/** Mouse drag on the Gantt chart. */
		GANTT_DRAG,
		/** Right-click context menu command. */
		CONTEXT_MENU,
		/** Column header click. */
		HEADER_CLICK,
		/** Opening a modal dialog and closing it with OK or Cancel. */
		DIALOG,
		/** Native or Swing file chooser for open and save. */
		FILE_CHOOSER,
		/** Window shell operations such as caption drag and close. */
		WINDOW_SHELL
	}
}

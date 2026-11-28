/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.testsupport.scenario;

/**
 * A scenario step that did not hold. The message names the scenario, the 1-based step number,
 * the step's JSON, and the expected and actual values, so a failed run can be read without
 * opening the run record.
 */
public final class GuiScenarioFailure extends AssertionError {
	private static final long serialVersionUID = 1L;

	private final String scenario;
	private final int step;
	private final String stepJson;
	private final String expected;
	private final String actual;

	public GuiScenarioFailure(String scenario, int step, String stepJson, String expected, String actual, Throwable cause) {
		super("Scenario \"" + scenario + "\" failed at step " + step + " " + stepJson
			+ ": expected " + expected + ", actual " + actual, cause);
		this.scenario = scenario;
		this.step = step;
		this.stepJson = stepJson;
		this.expected = expected;
		this.actual = actual;
	}

	public String scenario() {
		return scenario;
	}

	/** The 1-based step number. */
	public int step() {
		return step;
	}

	public String stepJson() {
		return stepJson;
	}

	public String expected() {
		return expected;
	}

	public String actual() {
		return actual;
	}
}

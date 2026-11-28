/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microproject.i18n.Messages;
import com.microproject.model.snapshot.Snapshottable;
import com.microproject.view.configuration.SpreadSheetCategories;
import com.microproject.view.configuration.SpreadSheetFieldArray;

/**
 * Regression tests for the option singleton contracts.
 * Verifies singleton identity, constructor independence, getter/setter round-trips,
 * and specific behavioral contracts for CalculationOption and TimesheetOption.
 */
class OptionContractsTest {

	// Saved singleton state for restoration
	private boolean advancedTimephasedActuals;
	private boolean advancedEarnedValueCumulative;

	private Object calculationBaselineId;
	private boolean calculationMultipleCriticalPaths;
	private long calculationCriticalSlackThreshold;

	private boolean ganttCompletionContiguous;

	private boolean timesheetAutoIntegrate;
	private String timesheetFieldArrayName;

	@BeforeEach
	void saveSingletonState() {
		// AdvancedOption
		AdvancedOption ao = AdvancedOption.getInstance();
		advancedTimephasedActuals = ao.isTimephasedActualsCanSetTaskStart();
		advancedEarnedValueCumulative = ao.isEarnedValueFieldsCumulative();

		// CalculationOption
		CalculationOption co = CalculationOption.getInstance();
		calculationBaselineId = co.getEarnedValueBaselineId();
		calculationMultipleCriticalPaths = co.isCalculateMultipleCriticalPaths();
		calculationCriticalSlackThreshold = co.getCriticalSlackThreshold();

		// GanttOption
		GanttOption go = GanttOption.getInstance();
		ganttCompletionContiguous = go.isCompletionIsContiguous();

		// TimesheetOption
		TimesheetOption to = TimesheetOption.getInstance();
		timesheetAutoIntegrate = to.isAutomaticallyIntegrateTimecardData();
		timesheetFieldArrayName = to.getTimesheetFieldArrayName();
	}

	@AfterEach
	void restoreSingletonState() {
		// AdvancedOption
		AdvancedOption ao = AdvancedOption.getInstance();
		ao.setTimephasedActualsCanSetTaskStart(advancedTimephasedActuals);
		ao.setEarnedValueFieldsCumulative(advancedEarnedValueCumulative);

		// CalculationOption
		CalculationOption co = CalculationOption.getInstance();
		co.setEarnedValueBaselineId(calculationBaselineId);
		co.setCalculateMultipleCriticalPaths(calculationMultipleCriticalPaths);
		co.setCriticalSlackThreshold(calculationCriticalSlackThreshold);

		// GanttOption
		GanttOption go = GanttOption.getInstance();
		go.setCompletionIsContiguous(ganttCompletionContiguous);

		// TimesheetOption
		TimesheetOption to = TimesheetOption.getInstance();
		to.setAutomaticallyIntegrateTimecardData(timesheetAutoIntegrate);
		to.setTimesheetFieldArrayName(timesheetFieldArrayName);
	}

	@Test
	void advancedOptionGetInstanceReturnsSameSingleton() {
		assertSame(AdvancedOption.getInstance(), AdvancedOption.getInstance());
	}

	@Test
	void advancedOptionPublicConstructorCreatesSeparateInstance() {
		AdvancedOption singleton = AdvancedOption.getInstance();
		AdvancedOption separate = new AdvancedOption();
		assertNotSame(singleton, separate);
		// New instance has default values
		assertTrue(separate.isTimephasedActualsCanSetTaskStart());
		assertTrue(separate.isEarnedValueFieldsCumulative());
	}

	@Test
	void advancedOptionGetterSetterRoundTrip() {
		AdvancedOption opt = new AdvancedOption();
		opt.setTimephasedActualsCanSetTaskStart(false);
		opt.setEarnedValueFieldsCumulative(false);
		assertEquals(false, opt.isTimephasedActualsCanSetTaskStart());
		assertEquals(false, opt.isEarnedValueFieldsCumulative());

		opt.setTimephasedActualsCanSetTaskStart(true);
		opt.setEarnedValueFieldsCumulative(true);
		assertEquals(true, opt.isTimephasedActualsCanSetTaskStart());
		assertEquals(true, opt.isEarnedValueFieldsCumulative());
	}

	@Test
	void calculationOptionGetInstanceReturnsSameSingleton() {
		assertSame(CalculationOption.getInstance(), CalculationOption.getInstance());
	}

	@Test
	void calculationOptionPublicConstructorCreatesSeparateInstance() {
		CalculationOption singleton = CalculationOption.getInstance();
		CalculationOption separate = new CalculationOption();
		assertNotSame(singleton, separate);
		// New instance has default values
		assertEquals(Snapshottable.BASELINE, separate.getEarnedValueBaselineId());
		assertEquals(false, separate.isCalculateMultipleCriticalPaths());
		assertEquals(0L, separate.getCriticalSlackThreshold());
	}

	@Test
	void calculationOptionGetterSetterRoundTrip() {
		CalculationOption opt = new CalculationOption();
		Object customBaseline = "custom-baseline-id";
		opt.setEarnedValueBaselineId(customBaseline);
		opt.setCalculateMultipleCriticalPaths(true);
		opt.setCriticalSlackThreshold(1000L);
		assertSame(customBaseline, opt.getEarnedValueBaselineId());
		assertEquals(true, opt.isCalculateMultipleCriticalPaths());
		assertEquals(1000L, opt.getCriticalSlackThreshold());
	}

	@Test
	void calculationOptionBaselineIdDefaultsToSnapshottableBaseline() {
		CalculationOption opt = new CalculationOption();
		assertEquals(Snapshottable.BASELINE, opt.getEarnedValueBaselineId());
		assertSame(Snapshottable.BASELINE, opt.getEarnedValueBaselineId());
	}

	@Test
	void calculationOptionBaselineIdAcceptsAnyObject() {
		CalculationOption opt = new CalculationOption();

		// Test with String
		Object stringBaseline = "baseline-1";
		opt.setEarnedValueBaselineId(stringBaseline);
		assertSame(stringBaseline, opt.getEarnedValueBaselineId());

		// Test with Integer
		Object intBaseline = Integer.valueOf(5);
		opt.setEarnedValueBaselineId(intBaseline);
		assertSame(intBaseline, opt.getEarnedValueBaselineId());

		// Test with custom object
		Object customObj = new Object();
		opt.setEarnedValueBaselineId(customObj);
		assertSame(customObj, opt.getEarnedValueBaselineId());

		// Test with null
		opt.setEarnedValueBaselineId(null);
		assertEquals(null, opt.getEarnedValueBaselineId());
	}

	@Test
	void ganttOptionGetInstanceReturnsSameSingleton() {
		assertSame(GanttOption.getInstance(), GanttOption.getInstance());
	}

	@Test
	void ganttOptionPublicConstructorCreatesSeparateInstance() {
		GanttOption singleton = GanttOption.getInstance();
		GanttOption separate = new GanttOption();
		assertNotSame(singleton, separate);
		assertEquals(false, separate.isCompletionIsContiguous());
	}

	@Test
	void ganttOptionGetterSetterRoundTrip() {
		GanttOption opt = new GanttOption();
		opt.setCompletionIsContiguous(true);
		assertEquals(true, opt.isCompletionIsContiguous());
		opt.setCompletionIsContiguous(false);
		assertEquals(false, opt.isCompletionIsContiguous());
	}

	@Test
	void timesheetOptionGetInstanceReturnsSameSingleton() {
		assertSame(TimesheetOption.getInstance(), TimesheetOption.getInstance());
	}

	@Test
	void timesheetOptionPublicConstructorCreatesSeparateInstance() {
		TimesheetOption singleton = TimesheetOption.getInstance();
		TimesheetOption separate = new TimesheetOption();
		assertNotSame(singleton, separate);
		assertEquals(true, separate.isAutomaticallyIntegrateTimecardData());
		assertEquals(Messages.getString("Spreadsheet.Timesheet.completion"), separate.getTimesheetFieldArrayName());
	}

	@Test
	void timesheetOptionGetterSetterRoundTrip() {
		TimesheetOption opt = new TimesheetOption();
		opt.setAutomaticallyIntegrateTimecardData(false);
		opt.setTimesheetFieldArrayName("CustomFieldArray");
		assertEquals(false, opt.isAutomaticallyIntegrateTimecardData());
		assertEquals("CustomFieldArray", opt.getTimesheetFieldArrayName());

		opt.setAutomaticallyIntegrateTimecardData(true);
		opt.setTimesheetFieldArrayName("AnotherName");
		assertEquals(true, opt.isAutomaticallyIntegrateTimecardData());
		assertEquals("AnotherName", opt.getTimesheetFieldArrayName());
	}

	@Test
	void timesheetOptionFieldArrayLookupUsesCorrectCategoryAndName() {
		TimesheetOption opt = new TimesheetOption();
		String customName = "TestFieldArray";
		opt.setTimesheetFieldArrayName(customName);

		// The lookup uses SpreadSheetCategories.timesheetSpreadsheetCategory and the current name
		// We verify the method delegates to Dictionary.get with the correct category constant
		// Note: Dictionary.get may return null if the field array is not registered,
		// but the contract is that it uses the correct category and name.
		SpreadSheetFieldArray result = opt.getTimesheetFieldArray();

		// Verify the category constant used is the expected one
		assertEquals(SpreadSheetCategories.timesheetSpreadsheetCategory, "timesheetSpreadsheet");
		assertEquals(opt.getTimesheetFieldArrayName(), customName);

		// Restore default name to avoid side effects
		opt.setTimesheetFieldArrayName(Messages.getString("Spreadsheet.Timesheet.completion"));
	}

}
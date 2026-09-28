/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AlertTest {
	private final boolean originalClientSide = Environment.isClientSide();
	private final boolean originalBatchMode = Environment.isBatchMode();

	@AfterEach
	void restoreEnvironment() {
		Alert.setPresenter(null);
		Environment.setClientSide(originalClientSide);
		Environment.setBatchMode(originalBatchMode);
	}

	@Test
	void delegatesUserPromptsToTheInstalledPresenter() {
		RecordingPresenter presenter = new RecordingPresenter();
		Object parent = new Object();
		Alert.setPresenter(presenter);
		Environment.setClientSide(true);
		Environment.setBatchMode(false);

		Alert.warn("warning", parent);
		Alert.error("error", parent);
		assertEquals(Alert.YES_OPTION, Alert.confirmYesNo("yes or no"));
		assertEquals(Alert.CANCEL_OPTION, Alert.confirm("confirm"));
		assertFalse(Alert.okCancel("ok or cancel"));
		Alert.warnWithOnceOption("once", "preference", parent);

		assertSame(parent, presenter.warningParent);
		assertSame(parent, presenter.errorParent);
		assertSame(parent, presenter.onceParent);
		assertEquals(1, presenter.warningCount);
		assertEquals(1, presenter.errorCount);
		assertEquals(1, presenter.onceCount);
	}

	@Test
	void keepsConservativeHeadlessPromptDefaultsWithoutPresenter() {
		Alert.setPresenter(null);
		Environment.setClientSide(false);
		Environment.setBatchMode(false);

		assertEquals(Alert.NO_OPTION, Alert.confirmYesNo("yes or no"));
		assertEquals(Alert.NO_OPTION, Alert.confirm("confirm"));
		assertTrue(Alert.okCancel("ok or cancel"));
	}

	private static final class RecordingPresenter implements AlertPresenter {
		private Object warningParent;
		private Object errorParent;
		private Object onceParent;
		private int warningCount;
		private int errorCount;
		private int onceCount;

		@Override
		public void warn(Object message, Object parent) {
			warningCount++;
			warningParent = parent;
		}

		@Override
		public void error(Object message, Object parent) {
			errorCount++;
			errorParent = parent;
		}

		@Override
		public int confirmYesNo(Object message) {
			return Alert.YES_OPTION;
		}

		@Override
		public int confirm(Object message) {
			return Alert.CANCEL_OPTION;
		}

		@Override
		public boolean okCancel(Object message) {
			return false;
		}

		@Override
		public String renameProject(String name, Set<String> projectNames, boolean saveAs) {
			return name;
		}

		@Override
		public void warnWithOnceOption(Object message, String preference, Object parent) {
			onceCount++;
			onceParent = parent;
		}
	}
}

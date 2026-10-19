/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.lang.reflect.Method;

import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

import com.microproject.menu.MenuActionConstants;

import sun.misc.Unsafe;

class GraphicManagerSaveStatusTest {
	@Test
	void refreshSaveStatusHandlesAnActiveFrameBeforeItsProjectIsAttached() throws Exception {
		DocumentFrame initializingFrame = allocateUninitializedDocumentFrame();
		GraphicManager manager = new GraphicManager(new JPanel()) {
			@Override public DocumentFrame getCurrentFrame() { return initializingFrame; }
		};

		assertDoesNotThrow(() -> manager.refreshSaveStatus(false));
		assertFalse(manager.getAction(MenuActionConstants.ACTION_SAVE_PROJECT).isEnabled(),
			"Save must remain disabled until the active frame has a project");
	}

	@Test
	void ganttRibbonRefreshIgnoresAnActiveFrameBeforeItsProjectIsAttached() throws Exception {
		DocumentFrame initializingFrame = allocateUninitializedDocumentFrame();
		GraphicManager manager = new GraphicManager(new JPanel()) {
			@Override public DocumentFrame getCurrentFrame() { return initializingFrame; }
		};
		Method refresh = GraphicManager.class.getDeclaredMethod("syncGanttViewRibbonState");
		refresh.setAccessible(true);

		assertDoesNotThrow(() -> refresh.invoke(manager),
			"a transient active frame without a project must not dereference its Gantt view");
	}

	private static DocumentFrame allocateUninitializedDocumentFrame() throws Exception {
		var field = Unsafe.class.getDeclaredField("theUnsafe");
		field.setAccessible(true);
		return (DocumentFrame) ((Unsafe) field.get(null)).allocateInstance(DocumentFrame.class);
	}
}

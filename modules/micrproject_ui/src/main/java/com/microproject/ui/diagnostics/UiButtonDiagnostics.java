/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.diagnostics;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;

import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet;
import com.microproject.grouping.core.Node;
import com.microproject.pm.key.HasKey;

/**
 * Adds opt-in action lifecycle diagnostics to command buttons. It is used
 * only by development/debug launches, so normal desktop behavior keeps the
 * original action instance and listener wiring.
 */
public final class UiButtonDiagnostics {
	private static final String UI_DEBUG_PROPERTY = "microproject.ui.debug";
	private static final Logger logger = Logger.getLogger(UiButtonDiagnostics.class.getName());

	private UiButtonDiagnostics() {
	}

	public static Action wrapAction(String buttonId, Action delegate) {
		if (!Boolean.getBoolean(UI_DEBUG_PROPERTY) || delegate == null)
			return delegate;
		return new TracedAction(buttonId, delegate);
	}

	private static final class TracedAction extends AbstractAction {
		private static final long serialVersionUID = 1L;
		private final String buttonId;
		private final Action delegate;

		private TracedAction(String buttonId, Action delegate) {
			this.buttonId = buttonId;
			this.delegate = delegate;
			copyValue(Action.NAME);
			copyValue(Action.SHORT_DESCRIPTION);
			copyValue(Action.LONG_DESCRIPTION);
			copyValue(Action.SMALL_ICON);
			copyValue(Action.LARGE_ICON_KEY);
			copyValue(Action.ACTION_COMMAND_KEY);
			copyValue(Action.ACCELERATOR_KEY);
			copyValue(Action.MNEMONIC_KEY);
			copyValue(Action.SELECTED_KEY);
			setEnabled(delegate.isEnabled());
		}

		private void copyValue(String key) {
			Object value = delegate.getValue(key);
			if (value != null)
				putValue(key, value);
		}

		@Override
		public void actionPerformed(ActionEvent event) {
			Component source = event != null && event.getSource() instanceof Component component ? component : null;
			boolean enabledBefore = source == null || source.isEnabled();
			boolean visibleBefore = source == null || source.isShowing();
			boolean selectedBefore = source instanceof javax.swing.AbstractButton button && button.isSelected();
			UiState stateBefore = UiState.capture(source);
			logger.fine("UI_BUTTON action-start id=" + buttonId
				+ " delegate=" + delegate.getClass().getSimpleName()
				+ " command=" + (event == null ? "null" : event.getActionCommand())
				+ " source=" + (source == null ? "null" : source.getClass().getSimpleName())
				+ " enabled=" + enabledBefore
				+ " visible=" + visibleBefore
				+ " selected=" + selectedBefore);
			logger.fine("UI_COMMAND id=" + buttonId
				+ " selection=" + stateBefore.selection
				+ " precondition=" + (enabledBefore ? "pass" : "fail")
				+ " activeView=" + stateBefore.activeView
				+ " modelBefore=" + stateBefore.modelSummary()
				+ " viewBefore=" + stateBefore.viewSummary()
				+ " undo=" + stateBefore.undoSummary());
			if (!enabledBefore) {
				logger.warning("UI_COMMAND_FAILURE id=" + buttonId
					+ " reason=precondition-fail activeView=" + stateBefore.activeView
					+ " selection=" + stateBefore.selection);
			}
			try {
				delegate.actionPerformed(event);
				UiState stateAfter = UiState.capture(source);
				boolean enabledAfter = source == null || source.isEnabled();
				boolean visibleAfter = source == null || source.isShowing();
				boolean selectedAfter = source instanceof javax.swing.AbstractButton button && button.isSelected();
				logger.fine("UI_BUTTON action-complete id=" + buttonId
					+ " enabled=" + enabledAfter
					+ " visible=" + visibleAfter
					+ " selected=" + selectedAfter
					+ " stateChanged=" + (enabledBefore != enabledAfter
						|| visibleBefore != visibleAfter || selectedBefore != selectedAfter));
				logger.fine("UI_COMMAND id=" + buttonId
					+ " selection=" + stateAfter.selection
					+ " precondition=pass"
					+ " activeView=" + stateAfter.activeView
					+ " modelAfter=" + stateAfter.modelSummary()
					+ " viewAfter=" + stateAfter.viewSummary()
					+ " undo=" + stateAfter.undoSummary());
				if (stateBefore.equals(stateAfter) && selectedBefore == selectedAfter
						&& enabledBefore == enabledAfter && visibleBefore == visibleAfter) {
					logger.warning("UI_COMMAND_FAILURE id=" + buttonId
						+ " reason=no-observable-state-change activeView=" + stateAfter.activeView
						+ " selection=" + stateAfter.selection);
				}
			} catch (RuntimeException | Error e) {
				logger.log(Level.WARNING, "UI_BUTTON_FAILURE id=" + buttonId
					+ " reason=action-threw delegate=" + delegate.getClass().getName(), e);
				throw e;
			}
		}
	}

	private static final class UiState {
		private final String selection;
		private final String activeView;
		private final int rows;
		private final boolean dirty;
		private final boolean canUndo;
		private final boolean canRedo;
		private final int visibleWindows;

		private UiState(String selection, String activeView, int rows, boolean dirty,
				boolean canUndo, boolean canRedo, int visibleWindows) {
			this.selection = selection;
			this.activeView = activeView;
			this.rows = rows;
			this.dirty = dirty;
			this.canUndo = canUndo;
			this.canRedo = canRedo;
			this.visibleWindows = visibleWindows;
		}

		private static UiState capture(Component source) {
			GraphicManager manager = null;
			try {
				manager = source == null ? GraphicManager.getInstance() : GraphicManager.getInstance(source);
			} catch (RuntimeException ignored) {
				// A diagnostic must never break a user command while a window is closing.
			}
			try {
				DocumentFrame frame = manager == null ? null : manager.getCurrentFrame();
				CommonSpreadSheet sheet = frame == null ? null : frame.getActiveSpreadSheet();
				List<String> selected = new ArrayList<>();
				if (sheet != null) {
					for (Node node : sheet.getSelectedNodes()) {
						Object impl = node == null ? null : node.getImpl();
						if (impl instanceof HasKey key)
							selected.add(impl.getClass().getSimpleName() + "#" + key.getUniqueId());
						else if (impl != null)
							selected.add(impl.getClass().getSimpleName());
					}
				}
				Collections.sort(selected);
				String activeView = "none";
				if (frame != null && frame.getActiveTopView() != null && frame.getActiveTopView().getViewName() != null)
					activeView = frame.getActiveTopView().getViewName();
				boolean dirty = frame != null && frame.getProject() != null && frame.getProject().needsSaving();
				boolean canUndo = frame != null && frame.getUndoController() != null && frame.getUndoController().canUndo();
				boolean canRedo = frame != null && frame.getUndoController() != null && frame.getUndoController().canRedo();
				int visibleWindows = 0;
				for (Window window : Window.getWindows())
					if (window.isShowing()) visibleWindows++;
				return new UiState(selected.toString(), activeView, sheet == null ? -1 : sheet.getRowCount(), dirty,
					canUndo, canRedo, visibleWindows);
			} catch (RuntimeException | Error ignored) {
				return new UiState("unavailable", "unavailable", -1, false, false, false, -1);
			}
		}

		private String modelSummary() {
			return "dirty=" + dirty + ",selection=" + selection;
		}

		private String viewSummary() {
			return "view=" + activeView + ",rows=" + rows + ",windows=" + visibleWindows;
		}

		private String undoSummary() {
			return "canUndo=" + canUndo + ",canRedo=" + canRedo;
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof UiState state)) return false;
			return rows == state.rows && dirty == state.dirty && canUndo == state.canUndo
				&& canRedo == state.canRedo && visibleWindows == state.visibleWindows
				&& selection.equals(state.selection) && activeView.equals(state.activeView);
		}

		@Override
		public int hashCode() {
			return java.util.Objects.hash(selection, activeView, rows, dirty, canUndo, canRedo, visibleWindows);
		}
	}
}

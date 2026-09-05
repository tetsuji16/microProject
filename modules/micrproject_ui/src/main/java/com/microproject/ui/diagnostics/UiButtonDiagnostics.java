/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.diagnostics;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;

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
			logger.fine("UI_BUTTON action-start id=" + buttonId
				+ " delegate=" + delegate.getClass().getSimpleName()
				+ " command=" + (event == null ? "null" : event.getActionCommand())
				+ " source=" + (source == null ? "null" : source.getClass().getSimpleName())
				+ " enabled=" + enabledBefore
				+ " visible=" + visibleBefore
				+ " selected=" + selectedBefore);
			try {
				delegate.actionPerformed(event);
				boolean enabledAfter = source == null || source.isEnabled();
				boolean visibleAfter = source == null || source.isShowing();
				boolean selectedAfter = source instanceof javax.swing.AbstractButton button && button.isSelected();
				logger.fine("UI_BUTTON action-complete id=" + buttonId
					+ " enabled=" + enabledAfter
					+ " visible=" + visibleAfter
					+ " selected=" + selectedAfter
					+ " stateChanged=" + (enabledBefore != enabledAfter
						|| visibleBefore != visibleAfter || selectedBefore != selectedAfter));
			} catch (RuntimeException | Error e) {
				logger.log(Level.WARNING, "UI_BUTTON_FAILURE id=" + buttonId
					+ " reason=action-threw delegate=" + delegate.getClass().getName(), e);
				throw e;
			}
		}
	}
}

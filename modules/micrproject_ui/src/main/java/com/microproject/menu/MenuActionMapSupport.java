package com.microproject.menu;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

/**
 * Small ActionMap helper used by tests and lightweight UI wiring.
 */
public final class MenuActionMapSupport {
	private MenuActionMapSupport() {
	}

	public static ProjectMenuActionMap noopActionMap() {
		return new ProjectMenuActionMap() {
			@Override
			public Action getAction(String key) {
				return new AbstractAction(key) {
					@Override
					public void actionPerformed(ActionEvent e) {
					}
				};
			}

			@Override
			public String getStringFromAction(Action action) {
				Object value = action.getValue(Action.NAME);
				return value == null ? "" : value.toString();
			}
		};
	}
}

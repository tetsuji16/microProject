/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.menu;

import java.util.Collection;
import java.util.List;

import javax.swing.AbstractButton;

import com.microproject.ui.ribbon.RibbonCommandSource;

/** Adapts the legacy resource/menu command factory to the public ribbon API. */
public final class LegacyRibbonCommandSourceAdapter implements RibbonCommandSource {
	private final ExtToolBarFactory factory;

	public LegacyRibbonCommandSourceAdapter(ExtToolBarFactory factory) {
		this.factory = java.util.Objects.requireNonNull(factory);
	}

	@Override
	public AbstractButton createButton(String commandId) {
		try {
			return factory.createJButton(commandId);
		} catch (Exception exception) {
			throw new IllegalStateException("Unable to create ribbon command " + commandId, exception);
		}
	}

	@Override
	public AbstractButton createTransientButton(String commandId) {
		try {
			return factory.createUnregisteredJButton(commandId);
		} catch (Exception exception) {
			throw new IllegalStateException("Unable to create transient ribbon command " + commandId, exception);
		}
	}

	@Override
	public String getActionId(String commandId) {
		return factory.getActionStringFromId(commandId);
	}

	@Override
	public List<AbstractButton> getButtons(String actionId) {
		List<AbstractButton> buttons = factory.getButtonsFromId(actionId);
		return buttons == null ? List.of() : List.copyOf(buttons);
	}

	@Override
	public void unregisterButtons(Collection<? extends AbstractButton> buttons) {
		factory.unregisterButtons(buttons);
	}
}

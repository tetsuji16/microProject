/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.menu;

import java.util.Collection;
import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.Action;

import com.microproject.ribbon.RibbonCommandInvocation;
import com.microproject.ribbon.RibbonCommandResult;
import com.microproject.ribbon.RibbonCommandSource;

/** Adapts the legacy resource/menu command factory to the public ribbon API. */
public final class LegacyRibbonCommandSourceAdapter implements RibbonCommandSource {
	private final ExtToolBarFactory factory;
	private final Map<String, Action> actionsByCommand = new LinkedHashMap<>();

	public LegacyRibbonCommandSourceAdapter(ExtToolBarFactory factory) {
		this.factory = java.util.Objects.requireNonNull(factory);
	}

	@Override
	public AbstractButton createButton(String commandId) {
		try {
			return prepareForRibbonDispatch(factory.createJButton(commandId), commandId);
		} catch (Exception exception) {
			throw new IllegalStateException("Unable to create ribbon command " + commandId, exception);
		}
	}

	@Override
	public AbstractButton createTransientButton(String commandId) {
		try {
			return prepareForRibbonDispatch(factory.createUnregisteredJButton(commandId), commandId);
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

	@Override
	public RibbonCommandResult dispatch(RibbonCommandInvocation invocation) {
		Action action = actionsByCommand.get(invocation.commandId());
		if (action == null) return RibbonCommandResult.rejected(invocation.commandId(), "unknown-command");
		if (!action.isEnabled()) return RibbonCommandResult.rejected(invocation.commandId(), "disabled");
		try {
			action.actionPerformed(new ActionEvent(invocation.source(), ActionEvent.ACTION_PERFORMED, invocation.commandId()));
			Object semantic = action.getValue("MicroProject.ribbonOutcome");
			Object ids = action.getValue("MicroProject.ribbonAffectedTaskIds");
			List<Long> affected = ids instanceof List<?> list ? list.stream().filter(Number.class::isInstance).map(value -> ((Number) value).longValue()).toList() : List.of();
			if (semantic == RibbonCommandResult.Status.CHANGED) return RibbonCommandResult.changed(invocation.commandId(), affected);
			if (semantic == RibbonCommandResult.Status.NO_CHANGE) return RibbonCommandResult.noChange(invocation.commandId(), affected);
			return RibbonCommandResult.dispatched(invocation.commandId());
		} catch (RuntimeException | Error failure) {
			return RibbonCommandResult.failed(invocation.commandId(), failure);
		}
	}

	private AbstractButton prepareForRibbonDispatch(AbstractButton button, String commandId) {
		Action action = button.getAction();
		if (action == null) throw new IllegalStateException("Ribbon command has no Action: " + commandId);
		actionsByCommand.put(commandId, action);
		return button;
	}
}

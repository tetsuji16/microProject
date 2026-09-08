/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.menu;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;

import com.microproject.ribbon.RibbonCommandInvocation;
import com.microproject.ribbon.RibbonCommandResult;
import com.microproject.ribbon.RibbonCommandSource;
import com.microproject.ui.diagnostics.UiButtonDiagnostics;

/**
 * Connects the menu action registry to the standalone ribbon surface.
 *
 * <p>Every physical ribbon button, including a responsive overflow button,
 * invokes {@link #dispatch(RibbonCommandInvocation)}.  The menu action is kept
 * as the one command implementation; it is not also installed as a competing
 * button listener.</p>
 */
public final class MenuRibbonCommandSource implements RibbonCommandSource {
	private final ExtToolBarFactory factory;
	private final Map<String, Action> commandsById = new LinkedHashMap<>();
	private final Map<String, Action> dispatchActionsById = new LinkedHashMap<>();

	public MenuRibbonCommandSource(ExtToolBarFactory factory) {
		this.factory = java.util.Objects.requireNonNull(factory);
	}

	@Override
	public AbstractButton createButton(String commandId) {
		try {
			return bind(factory.createJButton(commandId), commandId);
		} catch (Exception exception) {
			throw new IllegalStateException("Unable to create ribbon command " + commandId, exception);
		}
	}

	@Override
	public AbstractButton createTransientButton(String commandId) {
		try {
			return bind(factory.createUnregisteredJButton(commandId), commandId);
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
		Action action = commandsById.get(invocation.commandId());
		if (action == null) return RibbonCommandResult.rejected(invocation.commandId(), "unknown-command");
		if (!action.isEnabled()) return RibbonCommandResult.rejected(invocation.commandId(), "disabled");
		try {
			action.actionPerformed(new ActionEvent(invocation.source(), ActionEvent.ACTION_PERFORMED, invocation.commandId()));
			Object semantic = action.getValue("MicroProject.ribbonOutcome");
			Object ids = action.getValue("MicroProject.ribbonAffectedTaskIds");
			List<Long> affected = ids instanceof List<?> list ? list.stream().filter(Number.class::isInstance)
				.map(value -> ((Number)value).longValue()).toList() : List.of();
			if (semantic == RibbonCommandResult.Status.CHANGED) return RibbonCommandResult.changed(invocation.commandId(), affected);
			if (semantic == RibbonCommandResult.Status.NO_CHANGE) return RibbonCommandResult.noChange(invocation.commandId(), affected);
			return RibbonCommandResult.dispatched(invocation.commandId());
		} catch (RuntimeException | Error failure) {
			return RibbonCommandResult.failed(invocation.commandId(), failure);
		}
	}

	private AbstractButton bind(AbstractButton button, String commandId) {
		Action command = UiButtonDiagnostics.unwrapAction(button.getAction());
		if (command == null) throw new IllegalStateException("Ribbon command has no Action: " + commandId);
		commandsById.putIfAbsent(commandId, command);
		Action dispatchAction = dispatchActionsById.computeIfAbsent(commandId,
			ignored -> UiButtonDiagnostics.wrapAction(commandId, new RibbonDispatchAction(commandId)));
		button.setAction(dispatchAction);
		button.setActionCommand(commandId);
		return button;
	}

	private final class RibbonDispatchAction extends AbstractAction {
		private static final long serialVersionUID = 1L;
		private final String commandId;

		private RibbonDispatchAction(String commandId) {
			this.commandId = commandId;
			Action command = commandsById.get(commandId);
			setEnabled(command != null && command.isEnabled());
			if (command != null) command.addPropertyChangeListener(event -> {
				if ("enabled".equals(event.getPropertyName())) setEnabled(command.isEnabled());
			});
		}

		@Override
		public void actionPerformed(ActionEvent event) {
			RibbonCommandResult result = dispatch(new RibbonCommandInvocation(commandId,
				RibbonCommandInvocation.Origin.RIBBON_BUTTON, event == null ? null : event.getSource()));
			// The visual action is now the dispatcher rather than the menu action.
			// Preserve the semantic result on that visual action so diagnostics and
			// physical-route tests observe the same outcome as programmatic callers.
			putValue("MicroProject.ribbonOutcome", result.status());
			putValue("MicroProject.ribbonAffectedTaskIds", result.affectedTaskIds());
		}
	}
}

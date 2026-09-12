/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.menu;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

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
	private static final Logger logger = Logger.getLogger(MenuRibbonCommandSource.class.getName());
	private static final String OUTCOME_KEY = "MicroProject.ribbonOutcome";
	private static final String REASON_KEY = "MicroProject.ribbonReason";
	private static final String AFFECTED_TASK_IDS_KEY = "MicroProject.ribbonAffectedTaskIds";
	private final ExtToolBarFactory factory;
	private final Map<String, CommandBinding> bindingsById = new LinkedHashMap<>();

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
		Objects.requireNonNull(invocation, "invocation");
		if (EventQueue.isDispatchThread())
			return dispatchOnEdt(invocation);
		return callOnEdtAndWait(() -> dispatchOnEdt(invocation));
	}

	private AbstractButton bind(AbstractButton button, String commandId) {
		CommandBinding binding = bindingsById.computeIfAbsent(commandId, ignored -> new CommandBinding(commandId));
		Action buttonAction = button.getAction();
		Action command = UiButtonDiagnostics.unwrapAction(buttonAction);
		// A transient popup may be created from a registered button that already
		// carries this source's dispatcher.  Never install that dispatcher as its
		// own legacy command: doing so makes the popup click recurse and appear to
		// do nothing.
		if (binding.isDispatchAction(buttonAction, command))
			command = binding.command;
		if (command == null) throw new IllegalStateException("Ribbon command has no Action: " + commandId);
		binding.setCommand(command);
		button.setAction(binding.dispatchAction);
		button.setActionCommand(commandId);
		return button;
	}

	private RibbonCommandResult dispatchOnEdt(RibbonCommandInvocation invocation) {
		CommandBinding binding = bindingsById.get(invocation.commandId());
		if (binding == null)
			return rejected(invocation.commandId(), "unknown-command");
		Action action = binding.command;
		if (action == null || !action.isEnabled()) {
			RibbonCommandResult result = rejected(invocation.commandId(), "disabled");
			binding.publishResult(result);
			return result;
		}
		// Semantic properties belong to this invocation.  Leaving the previous
		// value on the legacy action made a later NO_CHANGE/DISPATCHED click look
		// like the preceding CHANGED click.
		action.putValue(OUTCOME_KEY, null);
		action.putValue(REASON_KEY, null);
		action.putValue(AFFECTED_TASK_IDS_KEY, null);
		try {
			action.actionPerformed(new ActionEvent(invocation.source() == null ? action : invocation.source(),
				ActionEvent.ACTION_PERFORMED,
				invocation.commandId()));
			RibbonCommandResult result = resultFromAction(invocation.commandId(), action);
			binding.publishResult(result);
			return result;
		} catch (RuntimeException | Error failure) {
			RibbonCommandResult result = RibbonCommandResult.failed(invocation.commandId(), failure);
			binding.publishResult(result);
			logger.log(Level.WARNING, "UI_COMMAND_FAILURE id=" + invocation.commandId()
				+ " reason=" + result.reason(), failure);
			return result;
		}
	}

	private RibbonCommandResult resultFromAction(String commandId, Action action) {
		Object semantic = action.getValue(OUTCOME_KEY);
		Object ids = action.getValue(AFFECTED_TASK_IDS_KEY);
		List<Long> affected = ids instanceof List<?> list ? list.stream().filter(Number.class::isInstance)
			.map(value -> ((Number) value).longValue()).toList() : List.of();
		if (semantic == RibbonCommandResult.Status.CHANGED) return RibbonCommandResult.changed(commandId, affected);
		if (semantic == RibbonCommandResult.Status.NO_CHANGE) return RibbonCommandResult.noChange(commandId, affected);
		return RibbonCommandResult.dispatched(commandId);
	}

	private RibbonCommandResult rejected(String commandId, String reason) {
		RibbonCommandResult result = RibbonCommandResult.rejected(commandId, reason);
		logger.fine("UI_COMMAND_FAILURE id=" + commandId + " reason=" + reason);
		return result;
	}

	private static <T> T callOnEdtAndWait(java.util.concurrent.Callable<T> callable) {
		final java.util.concurrent.atomic.AtomicReference<T> result = new java.util.concurrent.atomic.AtomicReference<>();
		final java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
		try {
			EventQueue.invokeAndWait(() -> {
				try { result.set(callable.call()); }
				catch (Throwable exception) { failure.set(exception); }
			});
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while dispatching UI command", interrupted);
		} catch (java.lang.reflect.InvocationTargetException invocationFailure) {
			throw new IllegalStateException("Unable to dispatch UI command", invocationFailure.getCause());
		}
		if (failure.get() != null)
			throw new IllegalStateException("UI command failed on EDT", failure.get());
		return result.get();
	}

	private final class CommandBinding {
		private final String commandId;
		private final Action dispatchAction;
		private Action command;
		private PropertyChangeListener enabledListener;

		private CommandBinding(String commandId) {
			this.commandId = commandId;
			this.dispatchAction = UiButtonDiagnostics.wrapAction(commandId, new RibbonDispatchAction());
		}

		private boolean isDispatchAction(Action candidate, Action unwrappedCandidate) {
			return candidate == dispatchAction
				|| unwrappedCandidate == UiButtonDiagnostics.unwrapAction(dispatchAction);
		}

		private void setCommand(Action next) {
			if (command == next) return;
			if (command != null && enabledListener != null)
				command.removePropertyChangeListener(enabledListener);
			command = next;
			enabledListener = event -> {
				if ("enabled".equals(event.getPropertyName())) dispatchAction.setEnabled(command.isEnabled());
			};
			command.addPropertyChangeListener(enabledListener);
			dispatchAction.setEnabled(command.isEnabled());
		}

		private void publishResult(RibbonCommandResult result) {
			dispatchAction.putValue(OUTCOME_KEY, result.status());
			dispatchAction.putValue(AFFECTED_TASK_IDS_KEY, result.affectedTaskIds());
		}

		private final class RibbonDispatchAction extends AbstractAction {
			private static final long serialVersionUID = 1L;
			@Override public void actionPerformed(ActionEvent event) {
				dispatch(new RibbonCommandInvocation(commandId,
					RibbonCommandInvocation.Origin.RIBBON_BUTTON, event == null ? null : event.getSource()));
			}
		}
	}
}

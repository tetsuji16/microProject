/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ribbon;

import java.util.Collection;
import java.util.List;

import javax.swing.AbstractButton;

/**
 * Boundary between the ribbon presentation and an application's command
 * system.  The ribbon knows how to lay out buttons, but it does not know how
 * commands are looked up, enabled, or registered with a legacy menu system.
 *
 * <p>An embedding application can implement this interface without depending
 * on the desktop menu package.  Implementations must return buttons backed by
 * the same action instance for a command, so enablement and diagnostics stay
 * consistent across ribbon rebuilds.</p>
 */
public interface RibbonCommandSource {
	/** Creates and registers a normal ribbon command button. */
	AbstractButton createButton(String commandId);

	/** Creates a transient button for an overflow popup without registering it. */
	AbstractButton createTransientButton(String commandId);

	/** Returns the stable action id represented by a ribbon command id. */
	String getActionId(String commandId);

	/** Returns all registered buttons for a stable action id, never {@code null}. */
	List<AbstractButton> getButtons(String actionId);

	/** Removes buttons discarded by a responsive ribbon rebuild. */
	void unregisterButtons(Collection<? extends AbstractButton> buttons);

	/**
	 * Programmatic behavior entry point for a ribbon command. Physical buttons
	 * retain the host's shared Swing {@code Action}, so enablement and toggle
	 * state remain synchronized; this entry point invokes that same action for
	 * an embedding host, an overflow surface, or an automated client.
	 */
	RibbonCommandResult dispatch(RibbonCommandInvocation invocation);
}

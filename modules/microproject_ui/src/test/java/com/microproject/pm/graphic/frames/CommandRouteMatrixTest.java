/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.EnumMap;
import java.util.Map;

import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

import com.microproject.ribbon.CommandId;
import com.microproject.ribbon.RibbonCommandResult;

import sun.misc.Unsafe;

/** Contract matrix proving presentation routes converge on one frame route. */
class CommandRouteMatrixTest {
	@Test
	void everyCommandIdUsesTheSameCanonicalDocumentRoute() throws Exception {
		List<CommandId> routed = new ArrayList<>();
		Map<CommandId, Integer> invocations = new EnumMap<>(CommandId.class);
		TestFrame testFrame = allocate(TestFrame.class);
		testFrame.routed = routed;
		testFrame.invocations = invocations;
		GraphicManager manager = new GraphicManager(new JPanel()) {
			@Override public boolean isDocumentActive() { return true; }
			@Override public DocumentFrame getCurrentFrame() { return testFrame; }
		};
		Method dispatch = GraphicManager.class.getDeclaredMethod("dispatchTaskCommand", CommandId.class);
		dispatch.setAccessible(true);

		for (CommandId command : CommandId.values()) {
			RibbonCommandResult outcome = (RibbonCommandResult) dispatch.invoke(manager, command);
			assertEquals(command.actionId(), outcome.commandId());
			assertEquals(RibbonCommandResult.Status.CHANGED, outcome.status());
			assertEquals(List.of(42L), outcome.affectedTaskIds());
			assertEquals("task", outcome.activeViewId());
			assertSame(command, routed.removeFirst());
		}
		assertEquals(List.of(), routed);
		for (CommandId command : CommandId.values())
			assertEquals(1, invocations.get(command), "each CommandId must execute exactly once");
	}

	@Test
	void legacyActionIdsResolveToOneStableCommandIdentity() {
		for (CommandId command : CommandId.values())
			assertEquals(command, CommandId.fromActionId(command.actionId()));
	}

	private static <T> T allocate(Class<T> type) throws Exception {
		Field field = Unsafe.class.getDeclaredField("theUnsafe");
		field.setAccessible(true);
		return type.cast(((Unsafe) field.get(null)).allocateInstance(type));
	}

	private static final class TestFrame extends DocumentFrame {
		private List<CommandId> routed;
		private Map<CommandId, Integer> invocations;

		private TestFrame() {
			super(null, null, "test");
			throw new UnsupportedOperationException();
		}

		@Override public RibbonCommandResult routeTaskCommand(CommandId command) {
			routed.add(command);
			invocations.merge(command, 1, Integer::sum);
			return RibbonCommandResult.changed(command.actionId(), List.of(42L)).withActiveView("task");
		}
	}
}

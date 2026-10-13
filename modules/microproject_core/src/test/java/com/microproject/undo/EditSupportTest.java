/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.undo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEdit;

import org.junit.jupiter.api.Test;

class EditSupportTest {
	@Test
	void nestedUpdatesPublishOneCompoundWithUndoAndRedoInSwingOrder() {
		EditSupport support = new EditSupport();
		UndoController controller = new UndoController();
		List<String> changes = new ArrayList<>();
		support.addEditListener(controller::editPosted);
		support.addEditListener(edit -> changes.add("published"));
		support.beginUpdate();
		support.postEdit(edit("first", changes));
		support.beginUpdate();
		support.postEdit(edit("second", changes));
		support.endUpdate();
		assertTrue(changes.isEmpty());
		support.endUpdate();

		assertEquals(List.of("published"), changes);
		controller.undo();
		assertEquals(List.of("published", "undo:second", "undo:first"), changes);
		controller.redo();
		assertEquals(List.of("published", "undo:second", "undo:first", "redo:first", "redo:second"), changes);
	}

	@Test
	void finalEditsFollowOrdinaryEditsAndRollbackDiscardsThem() {
		UndoController controller = new UndoController();
		EditSupport support = controller.getEditSupport();
		List<String> changes = new ArrayList<>();
		EditSupport.Activation command = support.beginCommand();
		EditSupport.Group group = command.group();
		support.postEdit(edit("first", changes));
		EditSupport.Checkpoint checkpoint = support.checkpoint(group);
		support.postEdit(edit("rolled-back", changes));
		support.postFinalEdit(edit("rolled-back-final", changes));
		support.discardSince(group, checkpoint);
		support.postEdit(edit("topology", changes));
		support.postFinalEdit(edit("detail", changes));
		support.reserveCommit(group);
		command.close();
		support.completePublication(group);

		assertEquals(1, controller.getEditNames().size());
		controller.undo();
		assertEquals(List.of("undo:detail", "undo:topology", "undo:first"), changes);
		controller.redo();
		assertEquals(List.of("undo:detail", "undo:topology", "undo:first",
			"redo:first", "redo:topology", "redo:detail"), changes);
	}

	@Test
	void listenersRunInRegistrationOrderAndUndoControllerReceivesBeforeObserver() {
		UndoController controller = new UndoController();
		EditSupport support = controller.getEditSupport();
		List<String> notifications = new ArrayList<>();
		support.addEditListener(edit -> notifications.add(controller.canUndo() ? "history-ready" : "stale-history"));

		support.postEdit(new AbstractUndoableEdit());

		assertEquals(List.of("history-ready"), notifications);
	}

	@Test
	void historySettlesAfterReentrantQueuedEditsHaveBeenRegistered() {
		UndoController controller = new UndoController();
		EditSupport support = controller.getEditSupport();
		List<String> notifications = new ArrayList<>();
		support.addEditListener(new EditListener() {
			@Override public void editPosted(UndoableEdit edit) {
				String name = edit.getPresentationName();
				notifications.add(name);
				if ("outer".equals(name)) support.postEdit(edit("inner", new ArrayList<>()));
			}

			@Override public void historySettled() {
				assertFalse(support.hasPendingHistory(), "settled observers run only after the committed queue drains");
				notifications.add("settled");
			}
		});

		support.postEdit(edit("outer", new ArrayList<>()));

		assertEquals(List.of("outer", "inner", "settled"), notifications);
		assertEquals(2, controller.getEditNames().size());
		assertTrue(controller.canUndo());
		EditSupport.Activation noOp = support.beginCommand();
		EditSupport.Group noOpGroup = noOp.group();
		support.reserveCommit(noOpGroup);
		noOp.close();
		support.completePublication(noOpGroup);
		assertEquals(List.of("outer", "inner", "settled", "settled"), notifications,
			"an empty reserved group must not strand ready observers");
		assertEquals(2, controller.getEditNames().size(), "an empty group must not create undo history");
	}

	@Test
	void incompleteEarlierGroupPreventsFalseSettledNotification() throws Exception {
		UndoController controller = new UndoController();
		EditSupport support = controller.getEditSupport();
		AtomicBoolean settled = new AtomicBoolean();
		support.addEditListener(new EditListener() {
			@Override public void editPosted(UndoableEdit edit) { }
			@Override public void historySettled() { settled.set(true); }
		});
		EditSupport.Activation first = support.beginCommand();
		support.postEdit(edit("first", new ArrayList<>()));
		EditSupport.Group firstGroup = first.group();
		support.reserveCommit(firstGroup);
		AtomicReference<Throwable> failure = new AtomicReference<>();
		Thread secondWriter = new Thread(() -> {
			try { support.postEdit(edit("second", new ArrayList<>())); }
			catch (Throwable current) { failure.set(current); }
		}, "undo-later-commit");
		secondWriter.start();
		secondWriter.join(2000);
		assertFalse(secondWriter.isAlive());
		assertEquals(null, failure.get());
		assertTrue(support.hasPendingHistory());
		assertFalse(settled.get(), "a blocked later commit is not a history-settled state");

		first.close();
		support.completePublication(firstGroup);

		assertTrue(settled.get());
		assertEquals(2, controller.getEditNames().size());
	}

	@Test
	void dequeuedEditBlocksOldHistoryUntilControllerRegistrationCompletes() throws Exception {
		CountDownLatch registrationEntered = new CountDownLatch(1);
		CountDownLatch releaseRegistration = new CountDownLatch(1);
		AtomicBoolean pause = new AtomicBoolean();
		AtomicReference<Throwable> failure = new AtomicReference<>();
		UndoController controller = new UndoController() {
			@Override public void editPosted(UndoableEdit edit) {
				if (pause.get()) {
					registrationEntered.countDown();
					try {
						if (!releaseRegistration.await(2, TimeUnit.SECONDS))
							throw new AssertionError("controller registration was not released");
					} catch (InterruptedException interrupted) {
						Thread.currentThread().interrupt();
						throw new AssertionError(interrupted);
					}
				}
				super.editPosted(edit);
			}
		};
		EditSupport support = controller.getEditSupport();
		List<String> changes = new ArrayList<>();
		support.postEdit(edit("old", changes));
		assertTrue(controller.canUndo());
		List<Boolean> observerStates = new ArrayList<>();
		support.addEditListener(edit -> observerStates.add(controller.canUndo()));
		pause.set(true);
		Thread publisher = new Thread(() -> {
			try { support.postEdit(edit("new", changes)); }
			catch (Throwable current) { failure.set(current); }
		}, "undo-controller-registration");
		publisher.start();
		try {
			assertTrue(registrationEntered.await(2, TimeUnit.SECONDS));
			assertTrue(support.hasPendingHistory());
			assertFalse(controller.canUndo(), "a dequeued edit is still pending before UndoManager.addEdit");
			assertFalse(controller.canRedo());
			controller.undo();
			assertTrue(changes.isEmpty(), "old history must not undo while the new edit is unregistered");
		} finally {
			releaseRegistration.countDown();
			publisher.join(2000);
		}
		assertFalse(publisher.isAlive());
		assertEquals(null, failure.get());
		assertEquals(List.of(Boolean.TRUE), observerStates, "UI sees registered history before notification returns");
		assertFalse(support.hasPendingHistory());
		controller.undo();
		assertEquals(List.of("undo:new"), changes);
	}

	@Test
	void publicationReentryUsesAnotherGroupAndListenerFailureDoesNotStrandHistory() {
		UndoController controller = new UndoController();
		EditSupport support = controller.getEditSupport();
		List<String> changes = new ArrayList<>();
		EditSupport.Activation first = support.beginCommand();
		support.postEdit(edit("first", changes));
		EditSupport.Group firstGroup = first.group();
		support.reserveCommit(firstGroup);
		first.close();
		try (EditSupport.Activation publication = support.activatePublication(firstGroup)) {
			EditSupport.Activation second = support.beginCommand();
			support.postEdit(edit("second", changes));
			EditSupport.Group secondGroup = second.group();
			support.reserveCommit(secondGroup);
			second.close();
			support.completePublication(secondGroup);
			assertFalse(controller.canUndo());
			support.postEdit(edit("first-notification", changes));
		}
		AtomicBoolean settledAfterFailure = new AtomicBoolean();
		EditListener failing = new EditListener() {
			@Override public void editPosted(UndoableEdit edit) {
				throw new IllegalStateException("observer failure");
			}

			@Override public void historySettled() {
				settledAfterFailure.set(true);
			}
		};
		support.addEditListener(failing);
		assertThrows(IllegalStateException.class, () -> support.completePublication(firstGroup));
		support.removeEditListener(failing);
		assertFalse(support.hasPendingHistory());
		assertTrue(settledAfterFailure.get(), "observer failure must not strand completion notification");
		assertEquals(2, controller.getEditNames().size());
		controller.undo();
		controller.undo();
		assertEquals(List.of("undo:second", "undo:first-notification", "undo:first"), changes);
		controller.redo();
		controller.redo();
		assertEquals(List.of("undo:second", "undo:first-notification", "undo:first",
			"redo:first", "redo:first-notification", "redo:second"), changes);
	}

	@Test
	void historyReplaySuppressesDelayedEditsWithoutDisablingTheNextCommand() {
		UndoController controller = new UndoController();
		EditSupport support = controller.getEditSupport();
		EditSupport.Activation replay = support.beginCommand();
		try (EditSupport.RecordingScope recording = support.suppressRecording()) {
			support.postEdit(new AbstractUndoableEdit());
		}
		EditSupport.Group replayGroup = replay.group();
		support.reserveCommit(replayGroup);
		replay.close();
		try (EditSupport.Activation publication = support.activatePublication(replayGroup)) {
			support.postEdit(new AbstractUndoableEdit());
		}
		support.completePublication(replayGroup);
		assertTrue(controller.getEditNames().isEmpty());
		assertFalse(support.hasPendingHistory());
		EditSupport.Activation command = support.beginCommand();
		support.postEdit(new AbstractUndoableEdit());
		EditSupport.Group commandGroup = command.group();
		support.reserveCommit(commandGroup);
		command.close();
		support.completePublication(commandGroup);
		assertEquals(1, controller.getEditNames().size());
		assertTrue(controller.canUndo());
	}

	private static UndoableEdit edit(String name, List<String> changes) {
		return new AbstractUndoableEdit() {
			@Override
			public String getPresentationName() {
				return name;
			}

			@Override
			public void undo() {
				super.undo();
				changes.add("undo:" + name);
			}

			@Override
			public void redo() {
				super.redo();
				changes.add("redo:" + name);
			}
		};
	}
}

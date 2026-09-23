package com.microproject.undo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.pm.scheduling.Schedule;

class ScheduleBackupEditTest {
	@Test
	void undoRestoresEveryScheduleFromCollectionAndRedoDoesNotRestoreAgain() {
		List<RestoreCall> calls = new ArrayList<>();
		Schedule first = schedule("first-detail", calls);
		Schedule second = schedule("second-detail", calls);
		Object source = new Object();
		ScheduleBackupEdit edit = new ScheduleBackupEdit(List.of(first, second), source);

		edit.undo();

		assertEquals(2, calls.size());
		assertEquals(List.of("first-detail", "second-detail"),
				calls.stream().map(RestoreCall::detail).sorted().toList());
		for (RestoreCall call : calls) {
			assertSame(source, call.source());
			assertFalse(call.isChild());
		}

		edit.redo();

		assertEquals(2, calls.size());
	}

	@Test
	void acceptsASingleScheduleAndRestoresItsCapturedDetail() {
		List<RestoreCall> calls = new ArrayList<>();
		Schedule schedule = schedule("single-detail", calls);
		Object source = new Object();
		ScheduleBackupEdit edit = new ScheduleBackupEdit(schedule, source);

		edit.undo();

		assertEquals(1, calls.size());
		assertSame(schedule, calls.getFirst().schedule());
		assertSame(source, calls.getFirst().source());
		assertEquals("single-detail", calls.getFirst().detail());
		assertFalse(calls.getFirst().isChild());
	}

	private static Schedule schedule(Object detail, List<RestoreCall> calls) {
		return (Schedule) Proxy.newProxyInstance(Schedule.class.getClassLoader(), new Class<?>[] { Schedule.class },
				(proxy, method, arguments) -> {
					if (method.getName().equals("backupDetail")) {
						return detail;
					}
					if (method.getName().equals("restoreDetail")) {
						calls.add(new RestoreCall((Schedule) proxy, arguments[0], arguments[1], (boolean) arguments[2]));
						return null;
					}
					return switch (method.getName()) {
						case "hashCode" -> System.identityHashCode(proxy);
						case "equals" -> proxy == arguments[0];
						case "toString" -> "TestSchedule";
						default -> throw new UnsupportedOperationException(method.getName());
					};
				});
	}

	private record RestoreCall(Schedule schedule, Object source, Object detail, boolean isChild) {
	}
}

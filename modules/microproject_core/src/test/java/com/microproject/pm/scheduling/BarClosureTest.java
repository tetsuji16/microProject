package com.microproject.pm.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.microproject.pm.criticalpath.ScheduleWindow;

class BarClosureTest {
	@Test
	void resumeAtAnUnsplitScheduleWindowUsesStopForTheRenderedInterval() {
		AtomicReference<ScheduleInterval> rendered = new AtomicReference<>();
		BarClosure closure = new BarClosure();
		closure.initialize(rendered::set, schedule(true));

		closure.accept(new ScheduleInterval(10L, 30L));

		assertEquals(20L, rendered.get().getStart());
		assertEquals(30L, rendered.get().getEnd());
	}

	@Test
	void nonWindowScheduleKeepsItsOriginalIntervalStart() {
		AtomicReference<ScheduleInterval> rendered = new AtomicReference<>();
		BarClosure closure = new BarClosure();
		closure.initialize(rendered::set, schedule(false));

		closure.accept(new ScheduleInterval(10L, 30L));

		assertEquals(10L, rendered.get().getStart());
		assertEquals(30L, rendered.get().getEnd());
	}

	private static Schedule schedule(boolean window) {
		Class<?>[] interfaces = window ? new Class<?>[] { Schedule.class, ScheduleWindow.class }
				: new Class<?>[] { Schedule.class };
		return (Schedule) Proxy.newProxyInstance(Schedule.class.getClassLoader(), interfaces,
				(proxy, method, arguments) -> switch (method.getName()) {
					case "getResume" -> 10L;
					case "getStop" -> 20L;
					case "getSplitDuration" -> 0L;
					default -> throw new UnsupportedOperationException(method.getName());
				});
	}
}

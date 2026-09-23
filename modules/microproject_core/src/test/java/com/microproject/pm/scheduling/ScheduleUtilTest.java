package com.microproject.pm.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.summaries.DivisionSummaryVisitor;

class ScheduleUtilTest {
	@Test
	void percentCompleteClosureUsesScheduleValuesAndIgnoresOtherObjects() {
		DivisionSummaryVisitor visitor = ScheduleUtil.percentCompleteClosureInstance(false);
		Schedule schedule = schedule(0.4D, 3600_000L);

		visitor.accept(schedule);
		assertEquals(0.4D, visitor.getValue(), 0.0000001D);

		visitor.reset();
		visitor.accept(new Object());
		assertEquals(0.0D, visitor.getValue(), 0.0D);
	}

	private static Schedule schedule(double percentComplete, long duration) {
		return (Schedule) Proxy.newProxyInstance(Schedule.class.getClassLoader(), new Class<?>[] { Schedule.class },
				(proxy, method, arguments) -> switch (method.getName()) {
					case "getPercentComplete" -> percentComplete;
					case "getDuration" -> duration;
					default -> throw new UnsupportedOperationException(method.getName());
				});
	}
}

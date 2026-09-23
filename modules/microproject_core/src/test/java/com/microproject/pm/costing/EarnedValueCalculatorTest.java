package com.microproject.pm.costing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

class EarnedValueCalculatorTest {
	@Test
	void scheduleOffsetsAreZeroWhenRequiredScheduleFieldsAreMissing() {
		EarnedValueValues earnedValueValues = (EarnedValueValues) Proxy.newProxyInstance(
				EarnedValueValues.class.getClassLoader(), new Class<?>[] { EarnedValueValues.class },
				(proxy, method, arguments) -> 0.0D);

		EarnedValueCalculator calculator = EarnedValueCalculator.getInstance();
		assertEquals(0L, calculator.getStartOffset(earnedValueValues));
		assertEquals(0L, calculator.getFinishOffset(earnedValueValues));
	}
}

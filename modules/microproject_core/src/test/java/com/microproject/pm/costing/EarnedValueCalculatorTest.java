package com.microproject.pm.costing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

class EarnedValueCalculatorTest {
	@Test
	void bcwpAccessorReturnsEarnedValueRatherThanActualCost() {
		EarnedValueValues earnedValueValues = (EarnedValueValues) Proxy.newProxyInstance(
				EarnedValueValues.class.getClassLoader(), new Class<?>[] { EarnedValueValues.class },
				(proxy, method, arguments) -> switch (method.getName()) {
					case "acwp" -> 17.0D;
					case "bcwp" -> 29.0D;
					default -> 0.0D;
				});

		assertEquals(29.0D, EarnedValueCalculator.getInstance().bcwp(earnedValueValues));
	}

	@Test
	void zeroArgumentEarnedValueMetricsUseKnownValues() {
		EarnedValueValues earnedValueValues = (EarnedValueValues) Proxy.newProxyInstance(
				EarnedValueValues.class.getClassLoader(), new Class<?>[] { EarnedValueValues.class },
				(proxy, method, arguments) -> switch (method.getName()) {
					case "bcwp" -> 80.0D;
					case "bcws" -> 100.0D;
					case "acwp" -> 90.0D;
					default -> 0.0D;
				});

		EarnedValueCalculator calculator = EarnedValueCalculator.getInstance();
		assertEquals(-10.0D, calculator.cv(earnedValueValues));
		assertEquals(-20.0D, calculator.sv(earnedValueValues));
		assertEquals(80.0D / 90.0D, calculator.cpi(earnedValueValues));
		assertEquals(0.8D, calculator.spi(earnedValueValues));
	}

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

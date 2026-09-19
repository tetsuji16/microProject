package com.microproject.datatype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;

import com.microproject.core.time.TimeUtil;

/**
 * Issue #184: TimeUtil and the datatype formatters previously shared static
 * mutable Calendar / NumberFormat instances (SimpleDateFormat / DecimalFormat),
 * which are not thread-safe. Verify concurrent use never throws and always
 * produces output identical to the single-threaded reference.
 */
class FormatConcurrencyTest {

	private static final int THREADS = 8;
	private static final int ITERATIONS = 200;
	private static final long BASE_INSTANT = 1700000000000L; // 2023-11-14T22:13:20Z

	@Test
	void timeUtilIsStableUnderConcurrentUse() throws Exception {
		final long[][] referenceHours = new long[THREADS][ITERATIONS];
		final long[][] referencePlus = new long[THREADS][ITERATIONS];
		final long[][] referenceMinus = new long[THREADS][ITERATIONS];
		final String[][] referenceUtc = new String[THREADS][ITERATIONS];
		for (int t = 0; t < THREADS; t++) {
			for (int i = 0; i < ITERATIONS; i++) {
				long instant = instant(t, i);
				referenceHours[t][i] = TimeUtil.toHoursAndMinutes(instant);
				referencePlus[t][i] = TimeUtil.addTimeZoneOffset(instant);
				referenceMinus[t][i] = TimeUtil.removeTimeZoneOffset(instant);
				referenceUtc[t][i] = TimeUtil.toUTCString(instant);
			}
		}

		runConcurrently((t, i) -> {
			long instant = instant(t, i);
			assertEquals(referenceHours[t][i], TimeUtil.toHoursAndMinutes(instant));
			assertEquals(referencePlus[t][i], TimeUtil.addTimeZoneOffset(instant));
			assertEquals(referenceMinus[t][i], TimeUtil.removeTimeZoneOffset(instant));
			assertEquals(referenceUtc[t][i], TimeUtil.toUTCString(instant));
		});
	}

	@Test
	void datatypeFormattersAreStableUnderConcurrentUse() throws Exception {
		final String[][] referenceDuration = new String[THREADS][ITERATIONS];
		final String[][] referenceRate = new String[THREADS][ITERATIONS];
		final String[][] referencePercent = new String[THREADS][ITERATIONS];
		for (int t = 0; t < THREADS; t++) {
			for (int i = 0; i < ITERATIONS; i++) {
				long durationMillis = 60000L * (1 + (t * ITERATIONS + i) % 1000);
				double rateValue = (t * ITERATIONS + i) / 100.0;
				referenceDuration[t][i] = DurationFormat.format(durationMillis);
				referenceRate[t][i] = RateFormat.getInstance(null, true, false, true)
						.format(new Rate(rateValue, TimeUnit.HOURS));
				referencePercent[t][i] = PercentFormat.getInstance().format(Double.valueOf(rateValue));
			}
		}

		Object parsedDuration = DurationFormat.getInstance().parseObject("2d", new ParsePosition(0));
		assertNotNull(parsedDuration);
		final long parsedDurationMillis = ((Duration) parsedDuration).getEncodedMillis();
		Object parsedRate = RateFormat.getInstance(null, true, false, true).parseObject("1234.5", new ParsePosition(0));
		assertNotNull(parsedRate);
		final double parsedRateValue = ((Rate) parsedRate).getValue();
		Object parsedPercent = PercentFormat.getInstance().parseObject("50%", new ParsePosition(0));
		assertNotNull(parsedPercent);
		final double parsedPercentValue = ((Number) parsedPercent).doubleValue();

		runConcurrently((t, i) -> {
			long durationMillis = 60000L * (1 + (t * ITERATIONS + i) % 1000);
			double rateValue = (t * ITERATIONS + i) / 100.0;
			assertEquals(referenceDuration[t][i], DurationFormat.format(durationMillis));
			assertEquals(referenceRate[t][i], RateFormat.getInstance(null, true, false, true)
					.format(new Rate(rateValue, TimeUnit.HOURS)));
			assertEquals(referencePercent[t][i], PercentFormat.getInstance().format(Double.valueOf(rateValue)));

			Object duration = DurationFormat.getInstance().parseObject("2d", new ParsePosition(0));
			assertNotNull(duration);
			assertEquals(parsedDurationMillis, ((Duration) duration).getEncodedMillis());
			Object rate = RateFormat.getInstance(null, true, false, true).parseObject("1234.5", new ParsePosition(0));
			assertNotNull(rate);
			assertEquals(parsedRateValue, ((Rate) rate).getValue(), 1e-9);
			Object percent = PercentFormat.getInstance().parseObject("50%", new ParsePosition(0));
			assertNotNull(percent);
			assertEquals(parsedPercentValue, ((Number) percent).doubleValue(), 1e-9);
		});
	}

	private interface Body {
		void run(int t, int i);
	}

	private static long instant(int t, int i) {
		return BASE_INSTANT + (t * ITERATIONS + i) * 60000L;
	}

	private static void runConcurrently(Body body) throws Exception {
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(THREADS);
		List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
		for (int t = 0; t < THREADS; t++) {
			final int thread = t;
			new Thread(() -> {
				try {
					start.await();
					for (int i = 0; i < ITERATIONS; i++) {
						body.run(thread, i);
					}
				} catch (Throwable e) {
					failures.add(e);
				} finally {
					done.countDown();
				}
			}).start();
		}
		start.countDown();
		done.await();
		assertEquals(0, failures.size(), () -> "concurrent use failed: " + failures);
	}
}

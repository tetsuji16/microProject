package com.microproject.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.jupiter.api.Test;	import com.microproject.pm.time.Interval;
	import com.microproject.pm.time.MutableInterval;


/**
 * Issue #166: createIntervalField used to discard the interval-configured
 * FieldContext and set an empty one, which would make getLabel() throw a
 * NullPointerException (specialFieldContext.getInterval() == null).
 */
class FieldIntervalLabelTest {

	@Test
	void intervalFieldUsesTheConfiguredInterval() {
		Interval interval = new MutableInterval(1612137600000L, 1612224000000L); // 2021-02-01 (default TZ)
		Field field = Field.createIntervalField(new Field(), interval);

		String label = field.getLabel();
		assertNotNull(label);
		// getLabel() renders the day-of-week of the interval start
		String expected = new SimpleDateFormat("E").format(new Date(interval.getStart()));
		assertEquals(expected, label);
	}
}

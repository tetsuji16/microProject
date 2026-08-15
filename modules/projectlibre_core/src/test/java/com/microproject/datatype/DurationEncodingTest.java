package com.microproject.datatype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DurationEncodingTest {
    @Test
    void temporalUnitsRoundTripWithinOneMillisecondPrecision() {
		int[] units = {TimeUnit.MINUTES, TimeUnit.HOURS, TimeUnit.DAYS,
			TimeUnit.ELAPSED_MINUTES,
			TimeUnit.ELAPSED_HOURS, TimeUnit.ELAPSED_DAYS};
        for (int unit : units) {
            long encoded = Duration.getInstance(2.75D, unit);
            assertEquals(unit, Duration.getType(encoded));
            assertEquals(2.75D, Duration.getValue(encoded), 0.000001D);
        }
    }

    @Test
    void workMarkerDoesNotRewriteEncodedUnit() {
        Duration duration = new Duration(Duration.getInstance(3D, TimeUnit.HOURS));
        long encoded = duration.getEncodedMillis();
        duration.setWork(true);
        assertTrue(duration.isWork());
        assertEquals(encoded, duration.getEncodedMillis());
    }
}

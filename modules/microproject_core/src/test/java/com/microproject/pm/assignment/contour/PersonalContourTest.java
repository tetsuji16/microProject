/*
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
 */
package com.microproject.pm.assignment.contour;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class PersonalContourTest {
	@Test
	void setIntervalPreservesBucketsOnEitherSideOfTheReplacedRange() {
		PersonalContour contour = contour();

		PersonalContour updated = contour.setInterval(4L, 7L, 1.0D);

		assertBuckets(updated, new long[] { 4L, 3L, 3L }, new double[] { 0.5D, 1.0D, 0.5D });
	}

	@Test
	void insertBucketSplitsTheContainingBucketAndKeepsOrder() {
		PersonalContour contour = contour();

		PersonalContour updated = contour.insertBucket(5L, PersonalContourBucket.getInstance(2L, 0.25D));

		assertBuckets(updated, new long[] { 5L, 2L, 5L }, new double[] { 0.5D, 0.25D, 0.5D });
	}

	private static PersonalContour contour() {
		return PersonalContour.getInstance(List.of(PersonalContourBucket.getInstance(10L, 0.5D)));
	}

	private static void assertBuckets(PersonalContour contour, long[] durations, double[] units) {
		AbstractContourBucket[] buckets = contour.getContourBuckets();
		assertEquals(durations.length, buckets.length);
		for (int i = 0; i < buckets.length; i++) {
			PersonalContourBucket bucket = (PersonalContourBucket) buckets[i];
			assertEquals(durations[i], bucket.getDuration());
			assertEquals(units[i], bucket.getUnits());
		}
	}
}

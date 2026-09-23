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
package com.microproject.server.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class DistributionDataTest {
	@Test
	void projectIdParticipatesInDistributionIdentity() {
		DistributionData first = distribution(1, 2, 3, 4, (short) 5);
		DistributionData otherProject = distribution(9, 2, 3, 4, (short) 5);

		assertNotEquals(first, otherProject);
		assertEquals(2, Set.of(first, otherProject).size());
		assertNotEquals(0, new DistributionComparator().compare(first, otherProject));
	}

	@Test
	void nonKeyValuesDoNotAffectDistributionIdentity() {
		DistributionData first = distribution(1, 2, 3, 4, (short) 5);
		DistributionData sameKey = distribution(1, 2, 3, 4, (short) 5);
		first.setCost(12.5);
		first.setWork(7.25);
		first.setStatus(DistributionData.UPDATE);
		sameKey.setCost(99.0);
		sameKey.setWork(42.0);
		sameKey.setStatus(DistributionData.INSERT);

		assertEquals(first, sameKey);
		assertEquals(first.hashCode(), sameKey.hashCode());
	}

	private static DistributionData distribution(long projectId, long taskId, long resourceId, int timeId, short type) {
		DistributionData data = new DistributionData();
		data.setProjectId(projectId);
		data.setTaskId(taskId);
		data.setResourceId(resourceId);
		data.setTimeId(timeId);
		data.setType(type);
		return data;
	}
}

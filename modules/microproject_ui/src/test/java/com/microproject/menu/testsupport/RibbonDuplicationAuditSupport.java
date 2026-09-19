/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
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
package com.microproject.menu.testsupport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RibbonDuplicationAuditSupport {
	private RibbonDuplicationAuditSupport() {
	}

	public static Map<String, List<String>> duplicateButtonIdsByTask(RibbonInventory inventory) {
		Map<String, List<String>> duplicates = new LinkedHashMap<>();
		for (Map.Entry<String, List<String>> entry : inventory.bandsByTask().entrySet()) {
			Map<String, Integer> counts = new LinkedHashMap<>();
			for (String bandId : entry.getValue()) {
				for (String buttonId : inventory.buttonsByBand().getOrDefault(bandId, List.of())) {
					counts.merge(buttonId, 1, Integer::sum);
				}
			}
			List<String> duplicateIds = new ArrayList<>();
			for (Map.Entry<String, Integer> count : counts.entrySet()) {
				if (count.getValue() > 1) {
					duplicateIds.add(count.getKey());
				}
			}
			if (!duplicateIds.isEmpty()) {
				duplicates.put(entry.getKey(), duplicateIds);
			}
		}
		return duplicates;
	}

	public static Map<String, List<String>> duplicateIconKeysByTask(RibbonInventory inventory) {
		Map<String, List<String>> duplicates = new LinkedHashMap<>();
		for (String taskId : inventory.taskIds()) {
			Map<String, List<String>> byIcon = new LinkedHashMap<>();
			for (String buttonId : inventory.buttonIdsForTask(taskId)) {
				RibbonInventory.ButtonSpec spec = inventory.buttons().get(buttonId);
				if (spec == null || spec.iconKey() == null) {
					continue;
				}
				byIcon.computeIfAbsent(spec.iconKey(), key -> new ArrayList<>()).add(buttonId);
			}
			List<String> duplicateIcons = new ArrayList<>();
			for (Map.Entry<String, List<String>> entry : byIcon.entrySet()) {
				if (entry.getValue().size() > 1) {
					duplicateIcons.add(entry.getKey() + "=" + String.join(",", entry.getValue()));
				}
			}
			if (!duplicateIcons.isEmpty()) {
				duplicates.put(taskId, duplicateIcons);
			}
		}
		return duplicates;
	}
}

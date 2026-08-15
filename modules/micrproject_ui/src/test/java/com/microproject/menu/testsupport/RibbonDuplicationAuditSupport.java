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

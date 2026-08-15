package com.microproject.menu.testsupport;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import com.microproject.pm.graphic.IconManager;

public final class RibbonIconAuditSupport {
	private RibbonIconAuditSupport() {
	}

	public static Map<String, String> missingIconMappings(RibbonInventory inventory) {
		Map<String, String> missing = new LinkedHashMap<>();
		for (RibbonInventory.ButtonSpec spec : inventory.buttons().values()) {
			if (spec.requiresIcon() && (spec.iconFileName() == null || spec.iconFileName().isBlank())) {
				missing.put(spec.id(), spec.iconKey());
			}
		}
		return missing;
	}

	public static Map<String, URL> missingResources(RibbonInventory inventory) {
		Map<String, URL> missing = new LinkedHashMap<>();
		for (RibbonInventory.ButtonSpec spec : inventory.buttons().values()) {
			if (!spec.requiresIcon()) {
				continue;
			}
			URL resource = IconManager.resolveIconResource(spec.iconKey());
			if (resource == null) {
				missing.put(spec.id(), null);
			}
		}
		return missing;
	}

	public static Map<String, String> missingSvgResources(RibbonInventory inventory) {
		Map<String, String> missing = new LinkedHashMap<>();
		for (RibbonInventory.ButtonSpec spec : inventory.buttons().values()) {
			if (spec.requiresIcon() && !IconManager.hasSvgResource(spec.iconKey())) {
				missing.put(spec.id(), spec.iconKey());
			}
		}
		return missing;
	}
}

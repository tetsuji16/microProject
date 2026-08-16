package com.projectlibre.ui.ribbon;

import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * Resolves the explicit ribbon icon mapping without guessing from an action
 * name. The menu resource remains the source of truth for localization and
 * legacy toolbar compatibility; this class is the only ribbon-side resolver.
 */
public final class RibbonIconRegistry {
	private final ResourceBundle[] bundles;

	public RibbonIconRegistry(ResourceBundle... bundles) {
		this.bundles = Objects.requireNonNull(bundles);
	}

	public String resolve(String commandId) {
		Objects.requireNonNull(commandId);
		String key = commandId + ".icon";
		for (ResourceBundle bundle : bundles) {
			try {
				String value = bundle.getString(key);
				return value == null || value.isBlank() ? null : value.trim();
			} catch (MissingResourceException ignored) {
				// Continue through the configured locale fallback chain.
			}
		}
		return null;
	}
}

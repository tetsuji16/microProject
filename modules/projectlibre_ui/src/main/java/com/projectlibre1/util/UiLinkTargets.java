package com.projectlibre1.util;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.projectlibre1.configuration.Settings;

public final class UiLinkTargets {
	public static final String PROJECT_HOME = Settings.SITE_HOME;
	public static final String DOCUMENTATION_HOME = Settings.HELP_HOME;
	public static final String TRIAL_HOME = PROJECT_HOME;
	public static final String LOGIN_HOME = PROJECT_HOME;
	public static final String DONATE_HOME = "https://github.com/sponsors/tetsuji16";

	private UiLinkTargets() {
	}

	public static String resolveHelpUrl(String address) {
		if (address == null || address.isBlank()) {
			return DOCUMENTATION_HOME;
		}
		String normalized = address.toLowerCase(Locale.ROOT);
		if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
			return address;
		}
		return DOCUMENTATION_HOME + "?topic=" + URLEncoder.encode(address, StandardCharsets.UTF_8);
	}

	public static URL bundledThirdPartyLicenseUrl() {
		return UiLinkTargets.class.getClassLoader().getResource("license/third-party/index.html");
	}
}

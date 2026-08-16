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
package com.microproject.util;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.microproject.configuration.Settings;

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

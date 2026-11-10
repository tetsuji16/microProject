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
package com.microproject.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

/**
 * Decides whether a link address may be handed to the operating system.
 *
 * <p>Link addresses come from project files (task and resource hyperlinks) as
 * well as from the application itself, so an address is untrusted input. The
 * operating system resolves every registered URI scheme, including ones that
 * start installed programs, so only the schemes listed here are opened.
 */
public final class ExternalLinkPolicy {
	/** What the caller must do with an address. */
	public enum Decision {
		/** A web or mail address: open it without asking. */
		OPEN,
		/** A local or network file: ask the user before opening it. */
		CONFIRM,
		/** Any other address: never hand it to the operating system. */
		REJECT
	}

	private static final Set<String> OPEN_SCHEMES = Set.of("http", "https", "mailto");
	private static final String FILE_SCHEME = "file";

	private ExternalLinkPolicy() {
	}

	public static Decision classify(String address) {
		if (address == null)
			return Decision.REJECT;
		String trimmed = address.strip();
		if (trimmed.isEmpty() || hasControlCharacter(trimmed))
			return Decision.REJECT;
		if (isWindowsPath(trimmed))
			return Decision.CONFIRM;
		URI uri;
		try {
			uri = new URI(trimmed);
		} catch (URISyntaxException e) {
			return Decision.REJECT;
		}
		String scheme = uri.getScheme();
		if (scheme == null)
			return Decision.CONFIRM;
		scheme = scheme.toLowerCase(Locale.ROOT);
		if (OPEN_SCHEMES.contains(scheme))
			return Decision.OPEN;
		if (FILE_SCHEME.equals(scheme))
			return Decision.CONFIRM;
		return Decision.REJECT;
	}

	/** A drive path ({@code C:\plan.docx}) or a UNC path ({@code \server\share}). */
	private static boolean isWindowsPath(String address) {
		if (address.startsWith("\\\\"))
			return true;
		return address.length() >= 3 && Character.isLetter(address.charAt(0)) && address.charAt(1) == ':'
				&& (address.charAt(2) == '\\' || address.charAt(2) == '/');
	}

	private static boolean hasControlCharacter(String address) {
		for (int i = 0; i < address.length(); i++) {
			if (Character.isISOControl(address.charAt(i)))
				return true;
		}
		return false;
	}
}

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
 ******************************************************************************/
package com.microproject.util;


public class ClassLoaderUtils {
	protected static ClassLoaderTransformer transformer;

	public static ClassLoaderTransformer getTransformer() {
		return transformer;
	}

	public static void setTransformer(ClassLoaderTransformer transformer) {
		ClassLoaderUtils.transformer = transformer;
	}

	public static interface ClassLoaderTransformer{
		public ClassLoader transform(ClassLoader c);
	}

	public static ClassLoader getLocalClassLoader(){
		ClassLoader defaultClassLoader=ClassLoaderUtils.class.getClassLoader();
		if (transformer==null) return defaultClassLoader;
		else return transformer.transform(defaultClassLoader);
	}

	public static Class forName(String name) throws ClassNotFoundException{
		return Class.forName(name, true, getLocalClassLoader());
	}

	/**
	 * Compare a version string against the current JVM version.
	 * Returns negative if the given version is older, zero if equal, positive if newer.
	 */
	public static int compareJavaVersionTo(String version) {
		return compareJavaVersion(version, System.getProperty("java.version"));
	}

	/**
	 * Compare two Java version strings (e.g. "1.8.0_292" vs "11.0.12").
	 * Returns negative if v1 < v2, zero if equal, positive if v1 > v2.
	 */
	public static int compareJavaVersion(String v1, String v2) {
		String[] parts1 = normalizeVersion(v1);
		String[] parts2 = normalizeVersion(v2);
		int len = Math.max(parts1.length, parts2.length);
		for (int i = 0; i < len; i++) {
			int p1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
			int p2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
			if (p1 != p2) return Integer.compare(p1, p2);
		}
		return 0;
	}

	private static String[] normalizeVersion(String version) {
		// Strip pre-release suffixes like "-ea", "+10", etc.
		String normalized = version.replaceAll("[^0-9.]", ".");
		normalized = normalized.replaceAll("\\.+", ".");
		normalized = normalized.replaceAll("^\\.|\\.$", "");
		if (normalized.isEmpty()) return new String[]{"0"};
		// Handle old "1.x.y" format by stripping leading "1."
		if (normalized.startsWith("1.")) {
			normalized = normalized.substring(2);
		}
		return normalized.split("\\.", -1);
	}
}

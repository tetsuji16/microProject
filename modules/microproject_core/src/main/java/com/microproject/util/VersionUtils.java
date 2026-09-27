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

import java.math.BigInteger;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VersionUtils {
	private static final BigInteger ZERO = BigInteger.ZERO;
	private static final Logger logger = Logger.getLogger(VersionUtils.class.getName());
	public static String getVersion(){
		String version=null;
		try {
			ResourceBundle bundle=ResourceBundle.getBundle("com.microproject.version.version",Locale.ENGLISH,ClassLoaderUtils.getLocalClassLoader()); //$NON-NLS-1$
			if (bundle!=null) version=bundle.getString("microproject.version");
		} catch (Exception e) {
			logger.log(Level.FINE, "Failed to load version from com.microproject.version.version", e);
		}
		if (version==null){
			try {
				ResourceBundle bundle=ResourceBundle.getBundle("com.microproject.strings.version",Locale.ENGLISH,ClassLoaderUtils.getLocalClassLoader()); //$NON-NLS-1$
				if (bundle!=null) version=bundle.getString("microproject.version");
			} catch (Exception e) {
				logger.log(Level.FINE, "Failed to load version from com.microproject.strings.version", e);
			}
		}
		if (version!=null)
			return version; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		else return null;//return Messages.getString("Release.version"); 

	}
	/**
	 * Returns whether this runtime was assembled without an explicit release
	 * version.  Such installDist outputs are for local development and must not
	 * be mistaken for an installed public release by the update checker.
	 */
	public static boolean isDevelopmentBuild() {
		try {
			ResourceBundle bundle = ResourceBundle.getBundle("com.microproject.version.version", Locale.ENGLISH,
				ClassLoaderUtils.getLocalClassLoader());
			return Boolean.parseBoolean(bundle.getString("microproject.developmentBuild"));
		} catch (Exception e) {
			logger.log(Level.FINE, "Failed to load development-build marker", e);
			return false;
		}
	}

	/**
	 * Compares dotted numeric application versions. A leading {@code v} is
	 * ignored, missing trailing components are treated as zero, and a hyphen
	 * is accepted as a component separator for release tags. Non-numeric or
	 * empty versions are rejected instead of silently triggering an update.
	 */
	public static int compareVersions(String first, String second) {
		String[] left = versionParts(first);
		String[] right = versionParts(second);
		if (left == null || right == null) return 0;
		int length = Math.max(left.length, right.length);
		for (int i = 0; i < length; i++) {
			BigInteger leftPart = i < left.length ? new BigInteger(left[i]) : ZERO;
			BigInteger rightPart = i < right.length ? new BigInteger(right[i]) : ZERO;
			int comparison = leftPart.compareTo(rightPart);
			if (comparison != 0) return comparison;
		}
		return 0;
	}

	private static String[] versionParts(String version) {
		if (version == null) return null;
		String normalized = version.trim().toLowerCase(Locale.ROOT);
		if (normalized.startsWith("v")) normalized = normalized.substring(1);
		if (normalized.isEmpty()) return null;
		String[] parts = normalized.split("[.\\-]", -1);
		for (String part : parts) {
			if (part.isEmpty() || !part.chars().allMatch(Character::isDigit)) return null;
		}
		return parts;
	}

	public static String toAppletVersion(String v){
		StringBuilder sb = new StringBuilder();
		String vNumbers[]=v.split("\\.", -1);
		for (int i=0;i<4;i++){
			int vn=(i>=vNumbers.length)?0:parseVersionSegment(vNumbers[i]);
			if (i>0) sb.append('.');
			String hex=Integer.toHexString(vn);
			for (int j=0;j<hex.length()-4;j++) sb.append('0');
			sb.append(hex);
		}
		return sb.toString();
	}
	private static int parseVersionSegment(String segment) {
		try {
			return Integer.parseInt(segment);
		} catch (NumberFormatException e) {
			return 0; // non-numeric segment (e.g. a suffix) degrades to 0 (issue #186)
		}
	}

}

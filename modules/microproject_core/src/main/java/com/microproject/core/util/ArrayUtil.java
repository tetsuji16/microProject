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
package com.microproject.core.util;

import java.util.StringTokenizer;

/**
 * @author Laurent Chretienneau
 *
 */
public class ArrayUtil {
	public static double[][] stringToPath(String s)  throws ArrayFormatException {
		s=s.replaceAll("^\\s*,?\\s*[\\(\\{\\[]",""); //trim start
		s=s.replaceAll("[\\)\\}\\]]\\s*,?\\s*$",""); //trim end
		String[] coords=s.split("[\\)\\}\\]]\\s*,?\\s*[\\(\\{\\[]", -1);
		double[][] p=new double[coords.length][2];
		for (int i=0; i<coords.length; i++){
			double[] c=stringToCoordinates(coords[i]);
			p[i]=c;
		}
		return p;
	}
	public static String pathToString(double[][] p){
		if (p==null)
			return null;
		StringBuilder sb=new StringBuilder();
		for (int i=0; i<p.length; i++){
			if (i>0) sb.append(", ");
			coordinatesToString(p[i],sb);	
		}
		return sb.toString();
	}
	public static double[] stringToCoordinates(String s) throws ArrayFormatException {
		StringTokenizer st=new StringTokenizer(s.trim(),",");
		if (st.countTokens() != 2)
			throw ArrayFormatException.forInputString(s);
		try {
			return new double[]{Double.parseDouble(st.nextToken().trim()), Double.parseDouble(st.nextToken().trim())};
		} catch (NumberFormatException e) {
			throw ArrayFormatException.forInputString(s);
		}
	}
	public static String coordinatesToString(double[] c) {
		return coordinatesToString(c,new StringBuilder()).toString();
	}
	private static StringBuilder coordinatesToString(double[] c, StringBuilder sb) {
		return sb.append('{').append(c[0]).append(", ").append(c[1]).append("}");
	}

}

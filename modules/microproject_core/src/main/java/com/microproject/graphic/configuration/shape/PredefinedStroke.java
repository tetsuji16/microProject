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
package com.microproject.graphic.configuration.shape;

import java.awt.BasicStroke;
import java.awt.Stroke;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class PredefinedStroke {
	public static final BasicStroke DASHED = new BasicStroke (1, BasicStroke.CAP_BUTT, 
            BasicStroke.JOIN_ROUND, 0, new float[]{2,1}, 0);	
	public static final BasicStroke SPARSE_DASHED = new BasicStroke (1, BasicStroke.CAP_BUTT, 
            BasicStroke.JOIN_ROUND, 0, new float[]{1,2}, 0);	
	public static final BasicStroke FRAMED = new BasicStroke(); // Default
	public static final BasicStroke LARGE_FRAMED = new BasicStroke(3f); // Default
	public static final BasicStroke SOLID = null; // is always null - means no stroke
	
	private static volatile Map<String, Stroke> predefinedStrokeMap;
	
	private static Map<String, Stroke> initialize() {
		Map<String, Stroke> m = new HashMap<>();
		m.put("DASHED", DASHED);
		m.put("FRAMED", FRAMED);
		m.put("LARGE_FRAMED", LARGE_FRAMED);
		return m;
	}
	
	private static Map<String, Stroke> getPredefinedStrokeMap() {
		Map<String, Stroke> result = predefinedStrokeMap;
		if (result == null) {
			synchronized (PredefinedStroke.class) {
				result = predefinedStrokeMap;
				if (result == null) {
					result = Map.copyOf(initialize());
					predefinedStrokeMap = result;
				}
			}
		}
		return result;
	}
	
	public static Stroke find(String key) {
		if (key == null) {
			return null;
		}
		if ("SOLID".equals(key)) {
			return SOLID;
		}
		Stroke found = (Stroke) getPredefinedStrokeMap().get(key);
		return found;
	}
}	


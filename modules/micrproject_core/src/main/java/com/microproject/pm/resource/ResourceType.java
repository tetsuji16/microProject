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
package com.microproject.pm.resource;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import com.microproject.strings.*;

/**
 * @stereotype enumeration 
 */
public class ResourceType {
	public static final int WORK = 1;
	public static final int MATERIAL = 0;
	public static final int LOCATION = 2;
	public static final int MACHINE = 3;
	public static final int OTHER = 4;
	public static final int COST = 5;

	private static BidiMap RESOURCE_TYPE_MAP = new DualHashBidiMap();
	static {
		RESOURCE_TYPE_MAP.put(Messages.getString("ResourceType.Labor"), Integer.valueOf(WORK));
		RESOURCE_TYPE_MAP.put(Messages.getString("ResourceType.Material"), Integer.valueOf(MATERIAL));
		RESOURCE_TYPE_MAP.put(Messages.getString("ResourceType.Location"),  Integer.valueOf(LOCATION));
		RESOURCE_TYPE_MAP.put(Messages.getString("ResourceType.Machine"),  Integer.valueOf(MACHINE));
		RESOURCE_TYPE_MAP.put(Messages.getString("ResourceType.Cost"), Integer.valueOf(COST));
		RESOURCE_TYPE_MAP.put(Messages.getString("ResourceType.Other"), Integer.valueOf(OTHER));
	}
	
	
	public static BidiMap getMap() {
		return RESOURCE_TYPE_MAP;
	}
}



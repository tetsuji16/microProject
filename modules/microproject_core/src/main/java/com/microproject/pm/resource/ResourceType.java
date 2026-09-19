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
import org.apache.commons.collections4.bidimap.UnmodifiableBidiMap;

import com.microproject.strings.*;

/**
 * @stereotype enumeration 
 */
public class ResourceType {
	/** Type-safe replacement for the legacy serialized integer codes. */
	public enum Kind {
		MATERIAL(0), WORK(1), LOCATION(2), MACHINE(3), OTHER(4), COST(5);
		private final int code;
		Kind(int code) { this.code = code; }
		public int code() { return code; }
		public static Kind fromCode(int code) {
			for (Kind value : values()) if (value.code == code) return value;
			throw new IllegalArgumentException("Unknown resource type: " + code);
		}
	}

	/** @deprecated use {@link Kind} and {@link Kind#code()} at compatibility boundaries. */
	@Deprecated public static final int WORK = 1;
	/** @deprecated use {@link Kind} and {@link Kind#code()} at compatibility boundaries. */
	@Deprecated public static final int MATERIAL = 0;
	/** @deprecated use {@link Kind} and {@link Kind#code()} at compatibility boundaries. */
	@Deprecated public static final int LOCATION = 2;
	/** @deprecated use {@link Kind} and {@link Kind#code()} at compatibility boundaries. */
	@Deprecated public static final int MACHINE = 3;
	/** @deprecated use {@link Kind} and {@link Kind#code()} at compatibility boundaries. */
	@Deprecated public static final int OTHER = 4;
	/** @deprecated use {@link Kind} and {@link Kind#code()} at compatibility boundaries. */
	@Deprecated public static final int COST = 5;

	private static final BidiMap<String, Integer> RESOURCE_TYPE_MAP;
	static {
		BidiMap<String, Integer> map = new DualHashBidiMap<String, Integer>();
		map.put(Messages.getString("ResourceType.Labor"), Integer.valueOf(WORK));
		map.put(Messages.getString("ResourceType.Material"), Integer.valueOf(MATERIAL));
		map.put(Messages.getString("ResourceType.Location"), Integer.valueOf(LOCATION));
		map.put(Messages.getString("ResourceType.Machine"), Integer.valueOf(MACHINE));
		map.put(Messages.getString("ResourceType.Cost"), Integer.valueOf(COST));
		map.put(Messages.getString("ResourceType.Other"), Integer.valueOf(OTHER));
		RESOURCE_TYPE_MAP = UnmodifiableBidiMap.unmodifiableBidiMap(map);
	}
	
	
	public static BidiMap<String, Integer> getMap() {
		return RESOURCE_TYPE_MAP;
	}
}



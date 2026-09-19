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
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Proxy;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.pm.key.HasKey;

class FinderTest {
	@Test
	void findsKeysByNameIdAndUniqueId() {
		HasKey first = key(null, 1L, 10L);
		HasKey second = key("second", 2L, 20L);
		List<HasKey> keys = List.of(first, second);

		assertSame(first, TaskFinder.findByName(null, keys));
		assertSame(second, TaskFinder.findByName("second", keys));
		assertSame(second, TaskFinder.findById(Long.valueOf(2L), keys));
		assertSame(first, TaskFinder.findByUniqueId(Integer.valueOf(10), keys));
		assertNull(TaskFinder.findByUniqueId(Long.valueOf(99L), keys));
	}

	private static HasKey key(String name, long id, long uniqueId) {
		return (HasKey) Proxy.newProxyInstance(HasKey.class.getClassLoader(), new Class<?>[] { HasKey.class },
			(proxy, method, args) -> switch (method.getName()) {
				case "getName" -> name;
				case "getId" -> Long.valueOf(id);
				case "getUniqueId" -> Long.valueOf(uniqueId);
				case "isLocal", "renumber" -> Boolean.FALSE;
				default -> null;
			});
	}
}

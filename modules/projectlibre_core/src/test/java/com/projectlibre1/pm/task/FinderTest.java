package com.projectlibre1.pm.task;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Proxy;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.projectlibre1.pm.key.HasKey;

class FinderTest {
	@Test
	void findsKeysByNameIdAndUniqueId() {
		HasKey first = key(null, 1L, 10L);
		HasKey second = key("second", 2L, 20L);
		List<HasKey> keys = List.of(first, second);

		assertSame(first, Finder.findByName(null, keys));
		assertSame(second, Finder.findByName("second", keys));
		assertSame(second, Finder.findById(Long.valueOf(2L), keys));
		assertSame(first, Finder.findByUniqueId(Integer.valueOf(10), keys));
		assertNull(Finder.findByUniqueId(Long.valueOf(99L), keys));
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

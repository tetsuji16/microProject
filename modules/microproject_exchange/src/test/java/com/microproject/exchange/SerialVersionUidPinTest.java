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
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Serializable classes of this module must pin {@code serialVersionUID}: the JDK-computed
 * default depends on the class name, so moving an unpinned class to another package would
 * break reading existing MPO/POD files. Excluded: interfaces, enums and records (their UIDs are
 * ignored by the JDK), anonymous/local/synthetic classes (compiler-generated names) and
 * {@code javax.swing.JComponent} descendants (Swing components are not written to MPO/POD or
 * workspace streams). Fix a failure by running {@code scripts/PinSerialVersionUid.java} (see its header).
 */
class SerialVersionUidPinTest {
	@Test
	void serializableClassesDeclareSerialVersionUid() throws Exception {
		Path classes = classesDirectory();
		ClassLoader loader = com.microproject.server.data.SerializeUtil.class.getClassLoader();
		List<String> offenders = new ArrayList<>();
		try (Stream<Path> files = Files.walk(classes.resolve("com/microproject"))) {
			for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".class"))::iterator) {
				String relative = classes.relativize(file).toString().replace('\\', '/');
				String name = relative.substring(0, relative.length() - ".class".length()).replace('/', '.');
				Class<?> type = Class.forName(name, false, loader);
				if (isPinRequired(type) && !declaresSerialVersionUid(type)) offenders.add(name);
			}
		}
		Collections.sort(offenders);
		assertTrue(offenders.isEmpty(), "Serializable classes without a declared serialVersionUID: " + offenders);
	}

	private static Path classesDirectory() throws URISyntaxException, IOException {
		Path location = Path.of(com.microproject.server.data.SerializeUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		assertTrue(Files.isDirectory(location), "expected compiled class directory but found " + location);
		return location;
	}

	private static boolean isPinRequired(Class<?> type) {
		if (!Serializable.class.isAssignableFrom(type)) return false;
		if (type.isInterface() || type.isEnum() || type.isRecord() || type.isAnnotation()) return false;
		if (type.isAnonymousClass() || type.isLocalClass() || type.isSynthetic()) return false;
		return !javax.swing.JComponent.class.isAssignableFrom(type);
	}

	private static boolean declaresSerialVersionUid(Class<?> type) {
		try {
			type.getDeclaredField("serialVersionUID");
			return true;
		} catch (NoSuchFieldException e) {
			return false;
		}
	}
}

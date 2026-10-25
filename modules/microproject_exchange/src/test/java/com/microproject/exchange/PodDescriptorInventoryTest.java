/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

/** Detects descriptor inventory drift in the checked-in POD corpus. */
class PodDescriptorInventoryTest {
	private static final Map<String, String> FIXED_FIXTURE_SHA256 = Map.of(
		"June_1_sample.pod", "f286eccf43e661de40e7a21a70d9b87cdfb0b444543a0f3c10649ad235328854",
		"Commercial construction project plan.pod", "2d381d8b9cd2ea4e619444e5372b29b20183ba038cc64e595db3a7da762cda42",
		"Commercial construction project plan2.pod", "ef3fae19814517d10b225b131c251c6ade27cc9154d5138cf3347479e6b1aab4");

	@Test
	void fixedFixturesMatchSerializedClassDescriptorInventory() throws Exception {
		Map<String, Set<String>> expected = expectedInventories();
		Set<String> fixtures = fixedPodFixtures();
		assertEquals(fixtures, expected.keySet(), "inventory must cover every fixed POD fixture exactly once");
		for (String fixture : fixtures) {
			assertEquals(FIXED_FIXTURE_SHA256.get(fixture), sha256(fixture), "fixed fixture bytes changed: " + fixture);
			try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(fixturePath(fixture).toFile()))) {
				assertEquals("1.0.0", input.readObject(), "serialized POD version changed: " + fixture);
			}
			Set<String> descriptors = inventory(fixture);
			assertEquals(expected.get(fixture), descriptors, "serialized descriptors changed for " + fixture);
			assertTrue(descriptors.stream().anyMatch(value -> value.startsWith("com.projectlibre1.server.data.ProjectData|")), fixture);
			assertTrue(descriptors.stream().anyMatch(value -> value.startsWith("com.projectlibre1.server.data.DocumentData|")), fixture);
		}
	}

	private static String sha256(String name) throws Exception {
		byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(fixturePath(name)));
		return java.util.HexFormat.of().formatHex(digest);
	}

	private static Path fixturePath(String name) {
		Path path = Path.of("samples", name);
		return Files.isRegularFile(path) ? path : Path.of("../../samples", name);
	}

	private static Map<String, Set<String>> expectedInventories() throws Exception {
		Map<String, Set<String>> inventories = new HashMap<>();
		String currentFixture = null;
		try (var resource = PodDescriptorInventoryTest.class.getResourceAsStream("pod-descriptors.txt")) {
			if (resource == null) throw new AssertionError("Missing POD descriptor inventory resource");
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.startsWith("POD-INVENTORY ")) {
						currentFixture = line.substring("POD-INVENTORY ".length());
						if (inventories.put(currentFixture, new TreeSet<>()) != null)
							throw new AssertionError("Duplicate fixture inventory: " + currentFixture);
					} else if (!line.isBlank()) {
						if (currentFixture == null) throw new AssertionError("Descriptor without fixture header");
						inventories.get(currentFixture).add(line);
					}
				}
			}
		}
		return inventories;
	}

	private static Set<String> fixedPodFixtures() throws Exception {
		Path directory = Path.of("samples");
		if (!Files.isDirectory(directory)) directory = Path.of("../../samples");
		try (var paths = Files.list(directory)) {
			return paths.filter(path -> path.getFileName().toString().endsWith(".pod"))
				.map(path -> path.getFileName().toString()).collect(java.util.stream.Collectors.toCollection(TreeSet::new));
		}
	}

	private static Set<String> inventory(String name) throws Exception {
		java.io.File file = new java.io.File("samples", name);
		if (!file.isFile()) file = new java.io.File("../../samples", name);
		Set<String> result = new TreeSet<>();
		try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(file)) {
				@Override protected ObjectStreamClass readClassDescriptor() throws java.io.IOException, ClassNotFoundException {
					ObjectStreamClass descriptor = super.readClassDescriptor();
					StringBuilder value = new StringBuilder(descriptor.getName()).append('|').append(descriptor.getSerialVersionUID());
					for (var field : descriptor.getFields()) value.append('|').append(field.getTypeCode()).append(':').append(field.getName()).append(':').append(field.getTypeString());
					result.add(value.toString());
					return descriptor;
				}
				@Override protected Class<?> resolveClass(ObjectStreamClass descriptor) throws java.io.IOException, ClassNotFoundException {
					// The fixture uses old FQNs. This mapping only lets the test read the full graph
					// while recording the original wire descriptors; SafeObjectInput is tested separately.
					String legacy = descriptor.getName();
					String remapped = legacy.replace("com.projectlibre1.", "com.microproject.");
					if (!legacy.equals(remapped)) {
						try { return Class.forName(remapped); }
						catch (ClassNotFoundException ignored) { }
					}
					return super.resolveClass(descriptor);
				}
		}) {
			Object first = input.readObject();
			if (first instanceof String) input.readObject();
		}
		return result;
	}
}

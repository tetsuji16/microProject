package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

class ApplicationStartupFactoryOptionsTest {
	@Test
	void commandLineFileArgumentsRemainLocalOpenTargets() {
		HashMap<String, Object> options = ApplicationStartupFactory.extractOpts(
			new String[] { "plan.mpo", "baseline.pod" });

		assertEquals(List.of("plan.mpo", "baseline.pod"), options.get("fileNames"));
	}

	@Test
	void retiredServerStartupFlagsAreRejectedEvenWithoutValues() {
		assertThrows(IllegalArgumentException.class,
			() -> ApplicationStartupFactory.extractOpts(new String[] { "--serverUrl" }));
		assertThrows(IllegalArgumentException.class,
			() -> ApplicationStartupFactory.extractOpts(new String[] { "https://example.test", "login", "user", "password" }));
		assertThrows(IllegalArgumentException.class,
			() -> ApplicationStartupFactory.extractOpts(new String[] { "--projectId", "42" }));
		assertThrows(IllegalArgumentException.class,
			() -> new ApplicationStartupFactory(new HashMap<>(java.util.Map.of("credentials", "secret"))));
	}
}

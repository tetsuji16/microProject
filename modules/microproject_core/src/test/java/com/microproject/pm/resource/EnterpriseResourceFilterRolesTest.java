package com.microproject.pm.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class EnterpriseResourceFilterRolesTest {
	@Test
	void filtersUnauthorizedRolesAndPreservesTheInactiveFallback() {
		EnterpriseResource resource = new EnterpriseResource((ResourcePool) null);
		resource.setAuthorizedRoles(Set.of(1));

		List<String> keys = new ArrayList<>(List.of("allowed", "denied"));
		List<Integer> values = List.of(1, 2);
		resource.filterRoles(keys, values);

		assertEquals(List.of("allowed"), keys);

		List<String> onlyInactive = new ArrayList<>(List.of("inactive"));
		resource.setAuthorizedRoles(Set.of());
		resource.filterRoles(onlyInactive, List.of(0));

		assertEquals(List.of("inactive"), onlyInactive);
	}
}

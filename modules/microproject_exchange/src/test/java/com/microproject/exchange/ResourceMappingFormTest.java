/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.exchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ResourceMappingFormTest {
	@Test
	void mergeFieldKeepsOnlyUniqueResourceMatchesInImportOrder() {
		Resource first = new Resource("Development");
		Resource duplicateA = new Resource("Shared");
		Resource duplicateB = new Resource("Shared");
		Resource unassigned = new Resource("Unassigned");
		ResourceMappingForm form = new ResourceMappingForm() {
			@Override
			public boolean execute() {
				return false;
			}
		};
		form.setResources(new ArrayList<>(List.of(first, duplicateA, duplicateB)));
		form.setImportedResources(List.of(new Resource("Shared"), new Resource("Development"), new Resource("Missing")));
		form.setUnassignedResource(unassigned);

		form.setMergeField(new ResourceMappingForm.MergeField("name", "name", "Name"));

		assertEquals(3, form.getSelectedResources().size());
		assertSame(unassigned, form.getSelectedResources().get(0));
		assertSame(first, form.getSelectedResources().get(1));
		assertSame(unassigned, form.getSelectedResources().get(2));
	}

	public static final class Resource {
		private final String name;

		private Resource(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}

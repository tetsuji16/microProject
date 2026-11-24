/*******************************************************************************
 * MIT License
 *
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
	void mapsImportedResourcesBySimpleJavaBeanPropertyAndLeavesMissingMatchesUnassigned() {
		TestResource existing = new TestResource("existing@example.com");
		Object unassigned = new Object();
		ResourceMappingForm form = new ResourceMappingForm() {
			@Override public boolean execute() { return true; }
		};
		form.setResources(new ArrayList<>(List.of(existing)));
		form.setImportedResources(List.<Object>of(new ImportedResource("existing@example.com"),
			new ImportedResource("missing@example.com")));
		form.setUnassignedResource(unassigned);

		form.setMergeField(new ResourceMappingForm.MergeField("emailAddress", "emailAddress", "email"));

		assertEquals(2, form.getSelectedResources().size());
		assertSame(existing, form.getSelectedResources().get(0));
		assertSame(unassigned, form.getSelectedResources().get(1));
	}

	@Test
	void leavesDuplicateExistingPropertyValuesUnmapped() {
		Object unassigned = new Object();
		ResourceMappingForm form = new ResourceMappingForm() {
			@Override public boolean execute() { return true; }
		};
		form.setResources(new ArrayList<>(List.of(new TestResource("duplicate@example.com"),
			new TestResource("duplicate@example.com"))));
		form.setImportedResources(List.<Object>of(new ImportedResource("duplicate@example.com")));
		form.setUnassignedResource(unassigned);

		form.setMergeField(new ResourceMappingForm.MergeField("emailAddress", "emailAddress", "email"));

		assertSame(unassigned, form.getSelectedResources().get(0));
	}

	@Test
	void mapsAcronymPropertyUsingItsJavaBeansName() {
		TestResource existing = new TestResource("email", 17);
		Object unassigned = new Object();
		ResourceMappingForm form = new ResourceMappingForm() {
			@Override public boolean execute() { return true; }
		};
		form.setResources(new ArrayList<>(List.of(existing)));
		form.setImportedResources(List.<Object>of(new ImportedResource("email", 17)));
		form.setUnassignedResource(unassigned);

		form.setMergeField(new ResourceMappingForm.MergeField("uniqueID", "uniqueID", "id"));

		assertSame(existing, form.getSelectedResources().get(0),
			"The uniqueID acronym property must be resolved with its JavaBeans spelling");
	}

	public static final class TestResource {
		private final String emailAddress;
		private final int uniqueID;

		public TestResource(String emailAddress) { this(emailAddress, 0); }
		public TestResource(String emailAddress, int uniqueID) { this.emailAddress = emailAddress; this.uniqueID = uniqueID; }
		public String getEmailAddress() { return emailAddress; }
		public int getUniqueID() { return uniqueID; }
	}

	public static final class ImportedResource {
		private final String emailAddress;
		private final int uniqueID;

		public ImportedResource(String emailAddress) { this(emailAddress, 0); }
		public ImportedResource(String emailAddress, int uniqueID) { this.emailAddress = emailAddress; this.uniqueID = uniqueID; }
		public String getEmailAddress() { return emailAddress; }
		public int getUniqueID() { return uniqueID; }
	}
}

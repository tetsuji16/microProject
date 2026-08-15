package test.com.microproject.collaboration;

import java.io.File;
import java.nio.file.Files;

import junit.framework.TestCase;

import com.microproject.collaboration.CollaborationMetadataStore;

public class CollaborationMetadataStoreTest extends TestCase {
	public void testMetadataRoundTripsThroughJsonSidecar() throws Exception {
		File projectFile = File.createTempFile("projectlibre-collaboration", ".xlsx");
		projectFile.deleteOnExit();
		CollaborationMetadataStore store = new CollaborationMetadataStore(projectFile);

		store.mutate(metadata -> {
			CollaborationMetadataStore.UserRecord user = new CollaborationMetadataStore.UserRecord();
			user.setUserKey("alice");
			user.setDisplayName("Alice \"A\"\nTeam");
			user.setClientInstanceId("client-1");
			user.setLastSeenAt(123456789L);
			metadata.getUsers().put(user.getUserKey(), user);

			CollaborationMetadataStore.LockRecord lock = new CollaborationMetadataStore.LockRecord();
			lock.setTaskId(42L);
			lock.setOwnerKey("alice#client-1");
			lock.setUserKey("alice");
			lock.setDisplayName(user.getDisplayName());
			lock.setClientInstanceId("client-1");
			lock.setLeaseUntil(987654321L);
			lock.setUpdatedAt(987654000L);
			metadata.getLocks().put("42", lock);
		});

		CollaborationMetadataStore.Metadata metadata = store.load();
		assertNotNull(metadata);
		assertEquals(1, metadata.getUsers().size());
		assertEquals(1, metadata.getLocks().size());
		assertEquals("Alice \"A\"\nTeam", metadata.getUsers().get("alice").getDisplayName());
		assertEquals(42L, metadata.getLocks().get("42").getTaskId());

		String sidecar = Files.readString(store.getSidecarFile().toPath());
		assertTrue(sidecar.startsWith("{"));
		assertTrue(sidecar.contains("\"schemaVersion\""));
		assertTrue(sidecar.contains("\\n"));
	}
}

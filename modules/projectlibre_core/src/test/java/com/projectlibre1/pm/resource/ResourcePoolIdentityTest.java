package com.projectlibre1.pm.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.projectlibre1.undo.DataFactoryUndoController;

class ResourcePoolIdentityTest {
    @Test
    void generatedDisplayIdsContinueAfterImportedResources() {
        ResourcePool pool = ResourcePool.createRourcePool("test", new DataFactoryUndoController());
        ResourceImpl imported = new ResourceImpl(new EnterpriseResource(pool));
        imported.setId(12L);
        pool.add(imported);

        Resource created = pool.newResourceInstance();

        assertEquals(13L, created.getId());
    }

    @Test
    void uniqueIdIndexTracksAddAndRemove() {
        ResourcePool pool = ResourcePool.createRourcePool("test", new DataFactoryUndoController());
        ResourceImpl resource = new ResourceImpl(new EnterpriseResource(pool));
        resource.setUniqueId(8123L);

        pool.add(resource);
        assertSame(resource, pool.findById(8123L));

        pool.remove(resource);
        assertNull(pool.findById(8123L));
    }
}

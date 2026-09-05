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
package com.microproject.pm.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.microproject.undo.DataFactoryUndoController;

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

    @Test
    void changingPoolScopeUpdatesAlreadyCreatedOutlines() {
        ResourcePool pool = ResourcePool.createRourcePool("test", new DataFactoryUndoController());

        assertEquals(false, pool.getResourceOutline().isLocal());
        assertEquals(false, pool.getResourceOutline().isMaster());

        pool.setLocal(true);
        pool.setMaster(true);

        assertEquals(true, pool.getResourceOutline().isLocal());
        assertEquals(true, pool.getResourceOutline().isMaster());
    }
}

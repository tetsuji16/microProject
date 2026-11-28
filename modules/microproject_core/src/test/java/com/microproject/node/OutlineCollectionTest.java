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
package com.microproject.node;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;

import javax.swing.tree.TreeModel;

import org.junit.jupiter.api.Test;

import com.microproject.configuration.Configuration;
import com.microproject.node.event.HierarchyEvent;
import com.microproject.node.hierarchy.NodeHierarchy;
import com.microproject.node.model.NodeModel;
import com.microproject.persistence.ClassRelocations;

class OutlineCollectionTest {
	@Test
	void outlineContractIsSwingFreeAndImplementedByExistingModels() throws Exception {
		Class<?> outlineModel = Class.forName("com.microproject.node.OutlineModel");
		Class<?> hierarchy = Class.forName("com.microproject.node.OutlineModel$Hierarchy");

		assertTrue(outlineModel.isInterface());
		assertTrue(hierarchy.isInterface());
		assertTrue(outlineModel.isAssignableFrom(NodeModel.class));
		assertTrue(TreeModel.class.isAssignableFrom(NodeModel.class));
		assertTrue(TreeModel.class.isAssignableFrom(NodeHierarchy.class));
		assertTrue(hierarchy.isAssignableFrom(NodeHierarchy.class));
		assertFalse(Arrays.stream(OutlineCollection.class.getMethods())
				.filter(method -> method.getName().startsWith("getOutline"))
				.anyMatch(method -> method.getReturnType() == NodeModel.class));

		for (Method method : outlineModel.getMethods()) {
			assertSwingFree(method.getGenericReturnType());
			for (Type parameter : method.getGenericParameterTypes()) {
				assertSwingFree(parameter);
			}
		}
		for (Method method : hierarchy.getMethods()) {
			assertSwingFree(method.getGenericReturnType());
			for (Type parameter : method.getGenericParameterTypes()) {
				assertSwingFree(parameter);
			}
		}
	}

	@Test
	void hierarchyPersistenceAndConfigurationContractsRemainStable() throws Exception {
		assertEquals(4012304159814639765L, ObjectStreamClass.lookup(HierarchyEvent.class).getSerialVersionUID());
		for (String name : new String[] {"type", "nodes", "flag", "oldNodes", "previousParentChildren", "consumed"}) {
			HierarchyEvent.class.getDeclaredField(name);
		}
		assertEquals("com.microproject.node.event.HierarchyEvent", ClassRelocations.relocate(
				"com.microproject.grouping.core.event.HierarchyEvent"));
		assertNotNull(Configuration.getFieldFromId("Field.parentId"));
	}

	private static void assertSwingFree(Type type) {
		assertFalse(type.getTypeName().contains("javax.swing"), type.getTypeName());
		assertFalse(type.getTypeName().contains("NodeModel"), type.getTypeName());
		assertFalse(type.getTypeName().contains("TreeModel"), type.getTypeName());
	}
}

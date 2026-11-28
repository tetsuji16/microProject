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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.microproject.node.hierarchy.NodeHierarchy;
import com.microproject.node.model.NodeModel;

class OutlineCollectionTest {
    @Test
    void outlineContractIsSwingFreeAndImplementedByExistingModels() throws Exception {
        Class<?> outlineModel = Class.forName("com.microproject.node.OutlineModel");
        Class<?> hierarchy = Class.forName("com.microproject.node.OutlineModel$Hierarchy");

        assertTrue(outlineModel.isInterface());
        assertTrue(hierarchy.isInterface());
        assertTrue(outlineModel.isAssignableFrom(NodeModel.class));
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

    private static void assertSwingFree(Type type) {
        assertFalse(type.getTypeName().contains("javax.swing"), type.getTypeName());
        assertFalse(type.getTypeName().contains("NodeModel"), type.getTypeName());
        assertFalse(type.getTypeName().contains("TreeModel"), type.getTypeName());
    }
}

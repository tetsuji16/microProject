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
package com.microproject.reports.adapter;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.model.NodeModel;
import com.microproject.grouping.core.model.NodeModelDataFactory;

class DataSourceTest {
	@Test
	void returnsNodeModelDataFactoryWhenNodeModelProvidesOne() {
		NodeModelDataFactory expectedFactory = (NodeModelDataFactory) Proxy.newProxyInstance(
			NodeModelDataFactory.class.getClassLoader(),
			new Class<?>[] { NodeModelDataFactory.class },
			new DefaultHandler(null));
		NodeModel nodeModel = (NodeModel) Proxy.newProxyInstance(
			NodeModel.class.getClassLoader(),
			new Class<?>[] { NodeModel.class },
			new DefaultHandler(expectedFactory));
		DataSource dataSource = new DataSource();

		dataSource.setNodeModel(nodeModel);

		assertSame(expectedFactory, dataSource.getDataFactory());
	}

	private static final class DefaultHandler implements InvocationHandler {
		private final NodeModelDataFactory factory;

		private DefaultHandler(NodeModelDataFactory factory) {
			this.factory = factory;
		}

		public Object invoke(Object proxy, Method method, Object[] args) {
			if ("getDataFactory".equals(method.getName())) {
				return factory;
			}
			Class<?> type = method.getReturnType();
			if (type.equals(Boolean.TYPE)) {
				return Boolean.FALSE;
			}
			if (type.equals(Integer.TYPE)) {
				return Integer.valueOf(0);
			}
			if (type.equals(Long.TYPE)) {
				return Long.valueOf(0L);
			}
			if (type.equals(Double.TYPE)) {
				return Double.valueOf(0.0d);
			}
			return null;
		}
	}
}

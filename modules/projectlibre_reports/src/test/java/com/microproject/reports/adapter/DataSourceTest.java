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

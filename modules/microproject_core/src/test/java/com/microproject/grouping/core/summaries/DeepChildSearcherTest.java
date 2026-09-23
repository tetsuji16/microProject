package com.microproject.grouping.core.summaries;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Proxy;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;

class DeepChildSearcherTest {
	@Test
	void findsTheFirstMatchingImplementationInDepthFirstOrder() {
		Object expected = new Object();
		Node first = node(expected);
		Node second = node(new Object());
		NodeModel nodeModel = (NodeModel) Proxy.newProxyInstance(NodeModel.class.getClassLoader(),
				new Class<?>[] { NodeModel.class }, (proxy, method, arguments) -> {
					if ("getChildren".equals(method.getName()))
						return arguments[0] == null ? List.of(first, second) : List.of();
					return null;
				});

		Object result = DeepChildSearcher.search(nodeModel, implementation -> implementation == expected);

		assertSame(expected, result);
	}

	private static Node node(Object implementation) {
		return (Node) Proxy.newProxyInstance(Node.class.getClassLoader(), new Class<?>[] { Node.class },
				(proxy, method, arguments) -> "getImpl".equals(method.getName()) ? implementation : null);
	}
}

package com.microproject.grouping.core.summaries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;

class DeepChildWalkerTest {
	@Test
	void recursivelyVisitsParentBeforeChildrenInListOrder() {
		Node grandchild = node();
		Node child = node();
		Node root = node();
		NodeModel nodeModel = (NodeModel) Proxy.newProxyInstance(NodeModel.class.getClassLoader(),
				new Class<?>[] { NodeModel.class }, (proxy, method, arguments) -> {
					if ("getChildren".equals(method.getName())) {
						if (arguments[0] == root) return List.of(child);
						if (arguments[0] == child) return List.of(grandchild);
						return List.of();
					}
					return null;
				});
		List<Object> visited = new ArrayList<>();
		DeepChildWalker walker = new DeepChildWalker(visited::add, false);
		walker.setNodeModel(nodeModel);

		walker.accept(root);

		assertEquals(3, visited.size());
		assertSame(root, visited.get(0));
		assertSame(child, visited.get(1));
		assertSame(grandchild, visited.get(2));
	}

	private static Node node() {
		return (Node) Proxy.newProxyInstance(Node.class.getClassLoader(), new Class<?>[] { Node.class },
				(proxy, method, arguments) -> null);
	}
}

package com.microproject.grouping.core.hierarchy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;

class HierarchyUtilsTest {
	@Test
	void extractsOnlyTopmostSelectedNodesAndPreservesInputOrder() {
		Node unselectedParent = node(null);
		Node selectedChild = node(unselectedParent);
		Node selectedGrandchild = node(selectedChild);
		Node independentRoot = node(null);
		List<Node> selection = List.of(selectedChild, selectedGrandchild, independentRoot);
		List<Node> roots = new ArrayList<>();

		HierarchyUtils.extractParents(selection, roots);

		assertEquals(2, roots.size());
		assertSame(selectedChild, roots.get(0));
		assertSame(independentRoot, roots.get(1));
	}

	private static Node node(Node parent) {
		return (Node) Proxy.newProxyInstance(Node.class.getClassLoader(), new Class<?>[] { Node.class },
				(proxy, method, arguments) -> switch (method.getName()) {
					case "getParent" -> parent;
					case "equals" -> proxy == arguments[0];
					case "hashCode" -> System.identityHashCode(proxy);
					default -> null;
				});
	}
}

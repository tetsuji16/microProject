package com.microproject.grouping.core.summaries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;

class ChildWalkerTest {
	@Test
	void leafWalkerVisitsOnlyTerminalNodesInChildOrder() {
		Node root = node();
		Node firstChild = node();
		Node leaf = node();
		Node secondChild = node();
		NodeModel model = nodeModel(root, List.of(firstChild, secondChild), firstChild, List.of(leaf));
		List<Object> visited = new ArrayList<>();
		LeafWalker walker = new LeafWalker(visited::add);
		walker.setNodeModel(model);

		walker.accept(root);

		assertEquals(2, visited.size());
		assertSame(leaf, visited.get(0));
		assertSame(secondChild, visited.get(1));
	}

	@Test
	void shallowWalkerVisitsOnlyImmediateChildrenInListOrder() {
		Node root = node();
		Node firstChild = node();
		Node secondChild = node();
		NodeModel model = nodeModel(root, List.of(firstChild, secondChild));
		RecordingVisitor visitor = new RecordingVisitor();
		ShallowChildWalker walker = new ShallowChildWalker(visitor);
		walker.setNodeModel(model);

		walker.accept(root);

		assertEquals(List.of(firstChild, secondChild), visitor.visited);
	}

	private static NodeModel nodeModel(Node root, List<Node> rootChildren, Object... additionalParentChildren) {
		return (NodeModel) Proxy.newProxyInstance(NodeModel.class.getClassLoader(), new Class<?>[] { NodeModel.class },
				(proxy, method, arguments) -> {
					if (!"getChildren".equals(method.getName())) return null;
					Node parent = (Node) arguments[0];
					if (parent == root) return rootChildren;
					for (int i = 0; i < additionalParentChildren.length; i += 2)
						if (parent == additionalParentChildren[i]) return additionalParentChildren[i + 1];
					return List.of();
				});
	}

	private static Node node() {
		return (Node) Proxy.newProxyInstance(Node.class.getClassLoader(), new Class<?>[] { Node.class },
				(proxy, method, arguments) -> null);
	}

	private static final class RecordingVisitor extends SummaryVisitor {
		private final List<Object> visited = new ArrayList<>();

		@Override
		public void reset() {
			visited.clear();
		}

		@Override
		public void accept(Object node) {
			visited.add(node);
		}

		@Override
		public Object getSummary() {
			return visited;
		}

		@Override
		public void addToSummary(Object value) {
			visited.add(value);
		}
	}
}

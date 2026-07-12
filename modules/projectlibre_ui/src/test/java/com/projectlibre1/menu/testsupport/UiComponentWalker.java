package com.projectlibre1.menu.testsupport;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

public final class UiComponentWalker {
	private UiComponentWalker() {
	}

	public static List<Component> flatten(Component root) {
		List<Component> components = new ArrayList<>();
		visit(root, components);
		return components;
	}

	private static void visit(Component component, List<Component> components) {
		if (component == null) {
			return;
		}
		components.add(component);
		if (component instanceof Container container) {
			for (Component child : container.getComponents()) {
				visit(child, components);
			}
		}
	}
}

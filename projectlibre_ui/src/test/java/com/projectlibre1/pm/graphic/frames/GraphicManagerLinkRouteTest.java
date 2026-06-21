package com.projectlibre1.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

class GraphicManagerLinkRouteTest {
	@Test
	void linkAndUnlinkActionsUseActionRouteGuards() {
		List<String> routedActions = new ArrayList<>();
		GraphicManager graphicManager = new GraphicManager(new JPanel()) {
			@Override
			protected boolean beforeActionRoute(String actionId) {
				routedActions.add(actionId);
				return true;
			}

			@Override
			public boolean isDocumentActive() {
				return true;
			}
		};

		graphicManager.new LinkAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "link"));
		graphicManager.new UnlinkAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "unlink"));

		assertEquals(List.of("link", "unlink"), routedActions);
		assertFalse(routedActions.isEmpty());
	}
}

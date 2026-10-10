package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.event.ActionEvent;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.microproject.util.Environment;
import org.junit.jupiter.api.Test;

class LocalOpenRoutingTest {
	@Test
	void openActionAlwaysUsesLocalChooserWhenStandAloneFlagIsFalse() throws Exception {
		boolean wasStandAlone = Environment.getStandAlone();
		try {
			Environment.setStandAlone(false);
			final int[] opens = {0};
			SwingUtilities.invokeAndWait(() -> {
				GraphicManager manager = new GraphicManager(new JPanel()) {
					@Override public void openLocalProject() { opens[0]++; }
				};
				manager.new OpenProjectAction().actionPerformed(new ActionEvent(manager, ActionEvent.ACTION_PERFORMED, "open"));
				manager.cleanUp();
			});
			assertEquals(1, opens[0]);
		} finally {
			Environment.setStandAlone(wasStandAlone);
		}
	}
}

package com.microproject.ui.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.MouseEvent;

import javax.swing.JButton;

import org.junit.jupiter.api.Test;

class PopupTriggerControllerTest {
	@Test
	void popupTriggerIsDispatchedOnceForPressAndReleasePlatform() {
		PopupTriggerController controller = new PopupTriggerController();
		JButton source = new JButton();

		assertTrue(controller.mousePressed(mouseEvent(source, MouseEvent.MOUSE_PRESSED, true)));
		assertEquals(PopupTriggerController.ReleaseOutcome.SUPPRESSED,
			controller.mouseReleased(mouseEvent(source, MouseEvent.MOUSE_RELEASED, true)));
		assertFalse(controller.wasTriggeredOnPress());
	}

	@Test
	void releaseOnlyPlatformShowsPopupOnRelease() {
		PopupTriggerController controller = new PopupTriggerController();
		JButton source = new JButton();

		assertFalse(controller.mousePressed(mouseEvent(source, MouseEvent.MOUSE_PRESSED, false)));
		assertEquals(PopupTriggerController.ReleaseOutcome.SHOW,
			controller.mouseReleased(mouseEvent(source, MouseEvent.MOUSE_RELEASED, true)));
	}

	@Test
	void ordinaryClickDoesNotShowPopupAndResetsGestureState() {
		PopupTriggerController controller = new PopupTriggerController();
		JButton source = new JButton();

		assertFalse(controller.mousePressed(mouseEvent(source, MouseEvent.MOUSE_PRESSED, false)));
		assertEquals(PopupTriggerController.ReleaseOutcome.NONE,
			controller.mouseReleased(mouseEvent(source, MouseEvent.MOUSE_RELEASED, false)));
		assertFalse(controller.wasTriggeredOnPress());
	}

	private static MouseEvent mouseEvent(JButton source, int id, boolean popupTrigger) {
		return new MouseEvent(source, id, 1L, 0, 1, 1, 1, popupTrigger, MouseEvent.BUTTON3);
	}
}

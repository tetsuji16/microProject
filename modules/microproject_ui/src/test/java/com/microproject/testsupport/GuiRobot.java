/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.testsupport;

import java.awt.AWTException;
import java.awt.Robot;

/** Robot input boundary that refuses to send input after desktop contention is observed. */
public final class GuiRobot extends Robot {
	public GuiRobot() throws AWTException {
		super();
	}

	@Override
	public void mouseMove(int x, int y) {
		verifyDesktopOwnership();
		super.mouseMove(x, y);
	}

	@Override
	public void mousePress(int buttons) {
		verifyDesktopOwnership();
		super.mousePress(buttons);
	}

	@Override
	public void mouseRelease(int buttons) {
		verifyDesktopOwnership();
		super.mouseRelease(buttons);
	}

	@Override
	public void mouseWheel(int wheelAmt) {
		verifyDesktopOwnership();
		super.mouseWheel(wheelAmt);
	}

	@Override
	public void keyPress(int keycode) {
		verifyDesktopOwnership();
		super.keyPress(keycode);
	}

	@Override
	public void keyRelease(int keycode) {
		verifyDesktopOwnership();
		super.keyRelease(keycode);
	}

	private static void verifyDesktopOwnership() {
		GuiDesktopSessionCoordinator.verifyDesktopBeforeRobotInput();
	}
}

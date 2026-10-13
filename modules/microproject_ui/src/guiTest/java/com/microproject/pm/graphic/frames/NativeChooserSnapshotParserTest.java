/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class NativeChooserSnapshotParserTest {
	private static final long TARGET_PID = 6616;
	private static final String BASE = """
		foreground pid=6616 hwnd=0x100 class=#32770 title=Open bounds=10,10,900,600
		ownedDialog pid=6616 hwnd=0x100 owner=0x80 ownerPid=6616 class=#32770 visible=true enabled=true bounds=10,10,900,600
		cancelControl pid=6616 hwnd=0x102 dialog=0x100 dialogClass=#32770 dialogOwnedByTarget=true dialogVisible=true dialogEnabled=true controlId=2 class=Button visible=true enabled=true bounds=800,550,80,30 dialogBounds=10,10,900,600
		nativeChooserReady=True
		""";

	@Test
	void acceptsForegroundOwnedDialogAndDirectCancelControl() {
		var parsed = RibbonExternalCommandGuiAcceptanceTest.parseNativeCancelControl(BASE, "synthetic", TARGET_PID);
		assertEquals("0x100", parsed.dialogHandle);
	}

	@Test
	void acceptsForegroundNativeChildOfOwnedDialog() {
		String snapshot = BASE.replace("hwnd=0x100 class=#32770", "hwnd=0x103 class=Button")
			+ "nativeChild pid=6616 hwnd=0x103 dialog=0x100 controlId=2 class=Button visible=true enabled=true bounds=800,550,80,30\n";
		assertEquals("0x100", RibbonExternalCommandGuiAcceptanceTest
			.parseNativeCancelControl(snapshot, "synthetic-child", TARGET_PID).dialogHandle);
	}

	@Test
	void rejectsDialogThatIsNotTheForegroundWindow() {
		String snapshot = BASE.replace("foreground pid=6616 hwnd=0x100", "foreground pid=6616 hwnd=0x200");
		assertThrows(AssertionError.class,
			() -> RibbonExternalCommandGuiAcceptanceTest.parseNativeCancelControl(snapshot, "behind", TARGET_PID));
	}

	@Test
	void rejectsHiddenDialog() {
		String snapshot = BASE.replace("visible=true enabled=true bounds=10,10,900,600\n", "visible=false enabled=true bounds=10,10,900,600\n");
		assertThrows(AssertionError.class,
			() -> RibbonExternalCommandGuiAcceptanceTest.parseNativeCancelControl(snapshot, "hidden", TARGET_PID));
	}

	@Test
	void rejectsMultipleOwnedDialogsAndInvalidCancelControl() {
		String multiple = BASE + BASE.substring(BASE.indexOf("ownedDialog "));
		assertThrows(AssertionError.class,
			() -> RibbonExternalCommandGuiAcceptanceTest.parseNativeCancelControl(multiple, "multiple", TARGET_PID));
		String invalidControl = BASE.replace("controlId=2 class=Button", "controlId=1 class=Button");
		assertThrows(AssertionError.class,
			() -> RibbonExternalCommandGuiAcceptanceTest.parseNativeCancelControl(invalidControl, "invalid", TARGET_PID));
	}
}

package com.microproject.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Issue #165: extractTip() threw StringIndexOutOfBoundsException for HTML
 * tooltips shorter than the internal prefix template and for tooltips with no
 * '<' after the prefix.
 */
class HyperLinkToolTipTest {

	@Test
	void extractTipReturnsShortHtmlTipUnchanged() {
		String tip = "<html>Hello</html>";
		assertEquals(tip, HyperLinkToolTip.extractTip(tip));
	}

	@Test
	void extractTipReturnsPlainTextTipUnchanged() {
		String tip = "plain tooltip";
		assertEquals(tip, HyperLinkToolTip.extractTip(tip));
	}

	@Test
	void extractTipStripsTemplateTags() {
		String tip = "<html><font face=\"Dialog\" size=\"2\">My Tip</font></html>";
		assertEquals("My Tip", HyperLinkToolTip.extractTip(tip));
	}

	@Test
	void extractTipHandlesMissingClosingTag() {
		String tip = "<html><font face=\"Dialog\" size=\"2\">No closing tag here";
		assertEquals("No closing tag here", HyperLinkToolTip.extractTip(tip));
	}
}

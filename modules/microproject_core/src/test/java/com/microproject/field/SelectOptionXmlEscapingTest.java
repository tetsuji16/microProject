/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.field;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SelectOptionXmlEscapingTest {
	@Test
	void configurationXmlKeepsLang2EscapingForAttributes() {
		assertEquals("<option key=\"caf&#233; &amp; &lt;&gt;&quot;&apos;\" value=\"&#8364;\"/>",
				SelectOption.toConfigurationXML("café & <>\"'", "€"));
	}
}

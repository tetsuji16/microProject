/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.association;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

import org.junit.jupiter.api.Test;

import com.microproject.configuration.Settings;

class AssociationListFormatTest {
	private final AssociationListFormat format = AssociationListFormat.getInstance(new TokenFormat());

	@Test
	void parsesFromCurrentPositionAndAdvancesToEnd() {
		String input = "prefix" + Settings.LIST_SEPARATOR + "Alpha" + Settings.LIST_SEPARATOR + "Beta";
		ParsePosition position = new ParsePosition("prefix".length() + Settings.LIST_SEPARATOR.length());

		AssociationList parsed = assertInstanceOf(AssociationList.class, format.parseObject(input, position));

		assertEquals(2, parsed.list.size());
		assertEquals("Alpha", parsed.list.get(0).getLeft());
		assertEquals("Beta", parsed.list.get(1).getLeft());
		assertEquals(input.length(), position.getIndex());
		assertEquals(-1, position.getErrorIndex());
	}

	@Test
	void reportsParseFailureWithoutAdvancingPosition() {
		String input = "prefix" + Settings.LIST_SEPARATOR + "Alpha" + Settings.LIST_SEPARATOR + "bad";
		int start = "prefix".length() + Settings.LIST_SEPARATOR.length();
		ParsePosition position = new ParsePosition(start);

		assertNull(format.parseObject(input, position));

		assertEquals(start, position.getIndex());
		assertEquals(input.indexOf("bad"), position.getErrorIndex());
	}

	@Test
	void formatsOnlyNonDefaultAssociationsWithoutTrailingSeparator() {
		AssociationList associations = new AssociationList();
		associations.add(new TestAssociation("Alpha", false));
		associations.add(new TestAssociation("Default", true));
		associations.add(new TestAssociation("Beta", false));
		associations.add(new TestAssociation("Trailing default", true));

		String formatted = format.format(associations, new StringBuffer(), new FieldPosition(0)).toString();

		assertEquals("Alpha" + Settings.LIST_SEPARATOR + "Beta", formatted);
	}

	private static final class TokenFormat extends Format {
		@Override
		public StringBuffer format(Object value, StringBuffer target, FieldPosition position) {
			return target.append(((Association) value).getLeft());
		}

		@Override
		public Object parseObject(String source, ParsePosition position) {
			String token = source.substring(position.getIndex());
			if (token.equals("bad")) {
				position.setErrorIndex(position.getIndex());
				return null;
			}
			position.setIndex(source.length());
			return new TestAssociation(token, false);
		}
	}

	private record TestAssociation(String id, boolean defaultAssociation) implements Association {
		@Override
		public Object getLeft() {
			return id;
		}

		@Override
		public Object getRight() {
			return id + "-right";
		}

		@Override
		public void testValid(boolean allowDuplicate) {
		}

		@Override
		public void copyPrincipalFieldsFrom(Association from) {
		}

		@Override
		public void doAddService(Object eventSource) {
		}

		@Override
		public void doRemoveService(Object eventSource) {
		}

		@Override
		public void doUpdateService(Object eventSource) {
		}

		@Override
		public boolean isDefault() {
			return defaultAssociation;
		}

		@Override
		public void replace(Object newOne, boolean leftObject) {
		}
	}
}

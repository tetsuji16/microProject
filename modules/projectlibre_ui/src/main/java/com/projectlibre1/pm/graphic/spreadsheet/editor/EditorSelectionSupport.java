/*******************************************************************************
 * The contents of this file are subject to the Common Public Attribution License 
 * Version 1.0 (the "License"); you may not use this file except in compliance with 
 * the License. You may obtain a copy of the License at 
 * http://www.projectlibre.com/license . The License is based on the Mozilla Public 
 * License Version 1.1 but Sections 14 and 15 have been added to cover use of 
 * software over a computer network and provide for limited attribution for the 
 * Original Developer. In addition, Exhibit A has been modified to be consistent 
 * with Exhibit B. 
 *******************************************************************************/
package com.projectlibre1.pm.graphic.spreadsheet.editor;

import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

final class EditorSelectionSupport {
	private EditorSelectionSupport() {
	}

	static void selectAllWithOptionalRefocus(final JTextComponent text, boolean keyboard) {
		text.selectAll();
		if (!keyboard) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					if (text.isFocusOwner())
						text.selectAll();
				}
			});
		}
	}
}

/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.pm.graphic;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.KeyStroke;

/**
 * Lightweight text field with dirty tracking only.
 */
public class ChangeAwareTextField extends JTextField implements DocumentListener, ChangeAwareComponent {
	private static final long serialVersionUID = -1961714277621662190L;
	public static final String NAME_HIERARCHY_COLLAPSE_ACTION_PROPERTY = "projectlibre.nameHierarchyCollapseAction";
	public static final String NAME_HIERARCHY_EXPAND_ACTION_PROPERTY = "projectlibre.nameHierarchyExpandAction";
	public static final String NAME_HIERARCHY_PREVIOUS_ACTION_PROPERTY = "projectlibre.nameHierarchyPreviousAction";
	public static final String NAME_HIERARCHY_NEXT_ACTION_PROPERTY = "projectlibre.nameHierarchyNextAction";

	protected boolean changed = false;

	public ChangeAwareTextField() {
		super();
		getDocument().addDocumentListener(this);
		installHierarchyKeyBindings();
	}

	private void installHierarchyKeyBindings() {
		InputMap inputMap = getInputMap(JComponent.WHEN_FOCUSED);
		ActionMap actionMap = getActionMap();
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_DOWN_MASK), NAME_HIERARCHY_COLLAPSE_ACTION_PROPERTY);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_DOWN_MASK), NAME_HIERARCHY_EXPAND_ACTION_PROPERTY);
		actionMap.put(NAME_HIERARCHY_COLLAPSE_ACTION_PROPERTY, new AbstractAction() {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				performHierarchyAction(NAME_HIERARCHY_COLLAPSE_ACTION_PROPERTY, e);
			}
		});
		actionMap.put(NAME_HIERARCHY_EXPAND_ACTION_PROPERTY, new AbstractAction() {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				performHierarchyAction(NAME_HIERARCHY_EXPAND_ACTION_PROPERTY, e);
			}
		});
	}

	private void performHierarchyAction(String property, ActionEvent event) {
		Action action = (Action)getClientProperty(property);
		if (action != null) {
			action.actionPerformed(event);
		}
	}

	public boolean hasChanged() {
		return changed;
	}

	public void resetChange() {
		changed = false;
	}

	@Override
	public void markChanged() {
		changed = true;
	}

	public void changedUpdate(DocumentEvent e) {
		changed = true;
	}

	public void insertUpdate(DocumentEvent e) {
		changed = true;
	}

	public void removeUpdate(DocumentEvent e) {
		changed = true;
	}

	@Override
	protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
		return super.processKeyBinding(ks, e, condition, pressed);
	}
}


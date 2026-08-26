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
package com.microproject.dialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JList;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.ListModel;

/**
 * A Jlist that responds to dbl clicks and enter key taken from
 * http://www.rgagnon.com/javadetails/java-0201.html
 */
public class ActionJList extends JList {
	private static final long serialVersionUID = 1L;
	private void init() {
		var activateSelection = new AbstractAction() {
			@Override public void actionPerformed(ActionEvent event) {
				if (al == null) return;
				List<?> selectedValues = getSelectedValuesList();
				if (selectedValues.size() == 1)
					al.actionPerformed(new ActionEvent(ActionJList.this,
							ActionEvent.ACTION_PERFORMED, selectedValues.get(0).toString()));
			}
		};
		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent me) {
				if (me.getClickCount() == 2) {
					activateSelection.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "activate"));
					me.consume();
				}
			}
		});
		getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "activateSelection");
		getActionMap().put("activateSelection", activateSelection);
		this.setSelectedIndex(0);
	}
	
	/**
	 * @param arg0
	 */
	public ActionJList(ListModel arg0) {
		super(arg0);
		init();
	}

	/**
	 * @param arg0
	 */
	public ActionJList(Object[] arg0) {
		super(arg0);
		init();
	}

	/**
	 * @param arg0
	 */
	public ActionJList(List<?> items) {
		super(items == null ? null : items.toArray());
		init();
	}

	/**
	 *  
	 */
	public ActionJList() {
		super();
		init();
	}

	/*
	 * * sends ACTION_PERFORMED event for double-click * and ENTER key
	 */
	ActionListener al;

	public ActionJList(String[] it) {
		super(it);
		init();
	}

	public void addActionListener(ActionListener al) {
		this.al = al;
	}
}


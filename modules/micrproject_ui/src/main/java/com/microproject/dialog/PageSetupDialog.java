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

import java.awt.Frame;

import javax.swing.JComponent;

import com.microproject.help.HelpUtil;
import com.microproject.print.PageSetup;
import com.microproject.strings.Messages;
/**
 *
 */
public class PageSetupDialog extends AbstractDialog {
	private static final long serialVersionUID = 1L;

	PageSetup setup;

	public static PageSetupDialog getInstance(Frame owner,PageSetup setup) {
		return new PageSetupDialog(owner,setup);
	}
	private PageSetupDialog(Frame owner,PageSetup setup) {
		super(owner, Messages.getString("PageSetupDialog.Title"),false);
		this.setup=setup;
		HelpUtil.addDocHelp(this,"Print_Preview"); // Keep the help topic aligned with the print preview flow.
	}
	public JComponent createContentPanel() {
		return setup.createContentPanel(false);
	}
	protected void onCancel() {
		setVisible(false);
	}
}


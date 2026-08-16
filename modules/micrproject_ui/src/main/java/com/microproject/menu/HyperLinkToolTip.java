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
package com.microproject.menu;

import java.awt.BorderLayout;

import javax.swing.JEditorPane;
import javax.swing.JToolTip;
import javax.swing.LookAndFeel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.plaf.ToolTipUI;

import com.microproject.help.HelpUtil;
import com.microproject.configuration.Settings;
import com.microproject.strings.Messages;
import com.microproject.util.BrowserControl;
 
/**
 * A tooltip that contains hyperlinks that are clickable.
 * Modified from a comment in this thread http://forum.java.sun.com/thread.jspa?threadID=592163&messageID=3095280
 * Note that a tooltip disappears when the mouse leaves the button. You must make sure to align the tooltip
 * directly touching the button to avoid this.
 * 
 */
public class HyperLinkToolTip extends JToolTip {
	private static final long serialVersionUID = 1L;
	private JEditorPane theEditorPane;
 
	public HyperLinkToolTip() {
		setLayout(new BorderLayout());
		LookAndFeel.installBorder(this, "ToolTip.border"); //$NON-NLS-1$
		LookAndFeel.installColors(this, "ToolTip.background", "ToolTip.foreground"); //$NON-NLS-1$ //$NON-NLS-2$
		theEditorPane = new JEditorPane();
		theEditorPane.setContentType("text/html"); //$NON-NLS-1$
		theEditorPane.setEditable(false);
		theEditorPane.addHyperlinkListener(new HyperlinkListener() {
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					BrowserControl.displayURL(e.getURL().toExternalForm());				}
			}
		});
		add(theEditorPane);
	}
 
	public void setTipText(String tipText) {
		theEditorPane.setText(tipText);
	}
 
	public void updateUI() {
		setUI(new ToolTipUI() {});
	}
	
	private static final String htmlPrefix = "<html><font face=\"Dialog\" size=\"2\">";//$NON-NLS-1$
	
	public static String helpTipText(String text,String helpLink, String demoLink, String docLink) {
		StringBuilder result = new StringBuilder();
		result.append(htmlPrefix); 
		result.append(text);
//		if (Settings.SHOW_HELP_LINKS && helpLink != null) {
//			result.append("<br><a href=\""); //$NON-NLS-1$
//			result.append(helpLink);
//			result.append(Messages.getString("HyperLinkToolTip.SeeOnlineHelp")); //$NON-NLS-1$
//		}
//		if (demoLink != null) {
//			result.append("<br><a href=\""); //$NON-NLS-1$
//			result.append(demoLink);
//			result.append(Messages.getString("HyperLinkToolTip.SeeOnlineDemo")); //$NON-NLS-1$
//		}
		if (docLink != null)
			result.append("<br>").append(HelpUtil.helpTipImg);
		result.append("</font></html>"); //$NON-NLS-1$
		return result.toString();	
		
	}
	
	public static String extractTip(String htmlTip) {
		if (!htmlTip.startsWith("<html>"))
			return htmlTip;
		if (htmlTip.length() < htmlPrefix.length()) // tooltip shorter than our prefix template: not ours, return as-is
			return htmlTip;
		String t = htmlTip.substring(htmlPrefix.length());
		int tagStart = t.indexOf('<');
		if (tagStart < 0) // no closing tag: plain text after prefix
			return t;
		return t.substring(0, tagStart);
		
	}
 
}



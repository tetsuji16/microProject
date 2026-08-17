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
package com.microproject.pm.graphic.frames;

import java.awt.FlowLayout;
import java.text.MessageFormat;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.microproject.strings.Messages;

/**
 * Slim status bar at the bottom of a document frame, mirroring the essential
 * MS Project status bar: the current timescale zoom level and the number of
 * selected tasks.
 */
public class DocumentStatusBar extends JPanel {
	private static final long serialVersionUID = 1L;

	private final JLabel zoomLabel = new JLabel();
	private final JLabel selectionLabel = new JLabel();
	private final JLabel modeLabel = new JLabel();

	public DocumentStatusBar() {
		setLayout(new FlowLayout(FlowLayout.LEFT, 16, 2));
		setBorder(new EmptyBorder(1, 6, 1, 6));
		add(zoomLabel);
		add(selectionLabel);
		add(modeLabel);
		setZoom(1, 1);
		setSelectedCount(0);
		setMode("StatusBar.Ready");
	}

	public void setMode(String modeKey) {
		modeLabel.setText(Messages.getString(modeKey));
	}

	public void setZoom(int scaleIndex, int scaleCount) {
		zoomLabel.setText(formatZoom(scaleIndex, scaleCount));
	}

	public void setSelectedCount(int count) {
		selectionLabel.setText(formatSelection(count));
	}

	static String formatZoom(int scaleIndex, int scaleCount) {
		int clampedIndex = Math.max(0, scaleIndex);
		int clampedCount = Math.max(1, scaleCount);
		return MessageFormat.format(Messages.getString("StatusBar.Zoom"), clampedIndex + 1, clampedCount);
	}

	static String formatSelection(int count) {
		return MessageFormat.format(Messages.getString("StatusBar.SelectedTasks"), Math.max(0, count));
	}
}

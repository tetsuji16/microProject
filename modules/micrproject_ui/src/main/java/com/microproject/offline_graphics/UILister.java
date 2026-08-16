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
package com.microproject.offline_graphics;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;

import com.microproject.util.FlatLafSupport;

public class UILister {
	private static final Logger logger = Logger.getLogger(UILister.class.getName());
	public static void main(String[] args) {
		FlatLafSupport.ensureInitialized();
		try {
			FlatLafSupport.initialize();
			Set defaults = UIManager.getLookAndFeelDefaults().entrySet();
			TreeSet ts = new TreeSet(new Comparator() {
				public int compare(Object a, Object b) {
					Map.Entry ea = (Map.Entry) a;
					Map.Entry eb = (Map.Entry) b;
					return ((String) ea.getKey()).compareTo(((String)
							eb.getKey()));
				}
			});
			ts.addAll(defaults);
			Object[][] kvPairs = new Object[defaults.size()][2];
			Object[] columnNames = new Object[] { "Key", "Value" };
			int row = 0;
			for (Iterator i = ts.iterator(); i.hasNext();) {
				Object o = i.next();
				Map.Entry entry = (Map.Entry) o;
				kvPairs[row][0] = entry.getKey();
				kvPairs[row][1] = entry.getValue();
				row++;
			}

			JTable table = new JTable(kvPairs, columnNames);
			JScrollPane tableScroll = new JScrollPane(table);

			JButton closeButton = new JButton("Close");
			closeButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					System.exit(0);
				}
			});

			JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER,
					6, 6));
			buttons.add(closeButton, null);

			JPanel main = new JPanel(new BorderLayout());
			main.add(tableScroll, BorderLayout.CENTER);
			main.add(buttons, BorderLayout.SOUTH);

			JFrame frame = new JFrame("UI Properties");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.getContentPane().add(main);
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		} catch (Exception ex) {
			logger.log(Level.WARNING, "Failed to display UI properties", ex);
		}
	}
}


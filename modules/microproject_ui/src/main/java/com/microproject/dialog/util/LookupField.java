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
package com.microproject.dialog.util;


import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.microproject.dialog.LookupDialog;
import com.microproject.pm.graphic.IconManager;
import com.microproject.field.Field;
import com.microproject.strings.Messages;

public class LookupField extends JPanel {
	JLabel display;
	JButton button;
	String value;
	public LookupField(Field field,Object value) {
		super();
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		
		this.value = (String)value;
		add(button = createLookupButton(field));
		button.setAlignmentY(Component.CENTER_ALIGNMENT);
		button.setAlignmentX(Component.LEFT_ALIGNMENT);

//		button.setHorizontalAlignment(SwingConstants.LEFT);
		add(display = new JLabel());
		setText(this.value);

	}
	public void setText(String value) {
		if (value == null)
			display.setText(""); //$NON-NLS-1$
		else {
			int index = value.indexOf("\\") + 1; //$NON-NLS-1$
			String label = value.substring(index); // gets the part after the slash, or all if no slash
			display.setText(label);
		}
	}

  	private JButton createLookupButton(final Field f) {
  		JButton lookup= new JButton();
  		lookup.setToolTipText(Messages.getString("LookupField.LookupAValue")); //$NON-NLS-1$
		ImageIcon icon = IconManager.getIcon("menu.find"); //$NON-NLS-1$
		lookup.setIcon(icon);
		lookup.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				//route message to main frame
				String keyAndValuePair[]=LookupDialog.getKeyAndValue(f);
				if (keyAndValuePair == null)
					return; // no change
				if (keyAndValuePair[0] == null)
					value = null;
				else
					value = keyAndValuePair[0] + "\\" + keyAndValuePair[1]; //$NON-NLS-1$
				setText(value);
				fire();
				
			}});
  		return lookup;
  	}
	public String getValue() {
		return value;
	}
	public String getText() {
		return value;
	}
	public JLabel getDisplay() {
		return display;
	}
	public void setDisplay(JLabel display) {
		this.display = display;
	}
    protected javax.swing.event.EventListenerList listenerList =
        new javax.swing.event.EventListenerList();

    // This methods allows classes to register for ObjectEvents
    public void addChangeListener(ChangeListener listener) {
        listenerList.add(ChangeListener.class, listener);
    }

    // This methods allows classes to unregister for ObjectEvents
    public void removeChangeListener(ChangeListener listener) {
        listenerList.remove(ChangeListener.class, listener);
    }
    private void fire() {
    	ChangeEvent evt = new ChangeEvent(this);
        Object[] listeners = listenerList.getListenerList();
        for (int i=0; i<listeners.length; i+=2) {
            if (listeners[i]==ChangeListener.class) {
                ((ChangeListener)listeners[i+1]).stateChanged(evt);
            }
        }
    }
	
}


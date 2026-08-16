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
package com.microproject.pm.graphic.frames.workspace;

import java.util.function.Consumer;

import java.awt.Container;
import java.awt.HeadlessException;

import javax.swing.ImageIcon;
import javax.swing.JPanel;


public abstract class NamedFrame extends JPanel {
	private boolean showTitleBar = true;
	private String tabTitle = null;
	private String id = null;
	private boolean active;
	public NamedFrame(String id, ImageIcon icon) throws HeadlessException {
		this.id = id;
		this.setBorder(null);
	}
	public Container getContentPane() {
		return this;
	}
	public final boolean isShowTitleBar() {
		return showTitleBar;
	}
	public final void setShowTitleBar(boolean showTitleBar) {
		this.showTitleBar = showTitleBar;
	}
	
	public void setTabTitle(String tabTitle) {
		this.tabTitle = tabTitle;
		if (manager != null)
			manager.setTabTitle(this, tabTitle);
	}
	
	public String getTitle() {
		return tabTitle;
	}
	
    protected javax.swing.event.EventListenerList listenerList =
        new javax.swing.event.EventListenerList();
	private FrameManager manager;


    // This methods allows classes to unregister for ObjectEvents
    public void removeNamedFrameListener(NamedFrameListener listener) {
        listenerList.remove(NamedFrameListener.class, listener);
    }
	
	public void addNamedFrameListener(NamedFrameListener listener) {
        listenerList.add(NamedFrameListener.class, listener);
	}
	
	public void fireNamedFrameActivated(final NamedFrameEvent evt) {
		fire(evt,new Consumer<Object>() { public void accept(Object arg0) {
				((NamedFrameListener)arg0).namedFrameActivated(evt);
			}
		});
	}

	public void fireNamedFrameTabShown(final NamedFrameEvent evt) {
		fire(evt,new Consumer<Object>() { public void accept(Object arg0) {
				((NamedFrameListener)arg0).namedFrameShown(evt);
			}
		});
	}

	private void fire(NamedFrameEvent evt, Consumer<Object> closure) {    	
        Object[] listeners = listenerList.getListenerList();
        // Each listener occupies two elements - the first is the listener class
        // and the second is the listener instance
        for (int i=0; i<listeners.length; i+=2) {
            if (listeners[i]==NamedFrameListener.class) {
            	closure.accept(((NamedFrameListener)listeners[i+1]));
            }
        }
    }

	
	final String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public final boolean isActive() {
		return active;
	}
	public void setActive(boolean active) {
		this.active = active;
	}
	public void setManager(FrameManager manager) {
		this.manager = manager;
		
	}
	public String toString() {
		return tabTitle;
	}
}


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

import java.util.function.Consumer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JRootPane;
import javax.swing.JToolTip;
import javax.swing.KeyStroke;
import javax.swing.plaf.ButtonUI;
import javax.swing.plaf.basic.BasicButtonUI;


import com.microproject.help.HelpUtil;
import com.microproject.menu.HyperLinkToolTip;
import com.microproject.menu.MenuManager;
import com.microproject.pm.graphic.IconManager;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.ReferenceNodeModelCache;
import com.microproject.configuration.FieldDictionary;
import com.microproject.configuration.Settings;
import com.microproject.strings.Messages;
import com.microproject.util.BrowserControl;
import com.microproject.util.FlatUiSupport;

/**
 *
 */
public abstract class AbstractDialog extends JDialog {
	private static final Logger logger = Logger.getLogger(AbstractDialog.class.getName());
	protected JButton ok;

	protected JButton cancel;
	protected JComponent help;

	protected Frame owner;
    private int dialogResult = JOptionPane.CANCEL_OPTION;

    protected JComponent contentPanel = null;
    protected ButtonPanel buttonPanel = null;
    private String helpAddress = null;

	public AbstractDialog() {
		super();
	}

	public AbstractDialog(Frame owner/*, MainFrame main*/, String title, boolean modal) {
		super(owner, title, modal);
		createRootPane();
		setLocationRelativeTo(null);
		this.owner = owner;
		FlatUiSupport.styleDialogRoot(getRootPane());
	}

	protected JRootPane createRootPane() {
		ActionListener escapeListener = new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				onCancel();

			}
		};
		ActionListener enterListener = new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				onOk();

			}
		};

		ActionListener helpListener = new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				onHelp();

			}


		};
		JRootPane rootPane = new JRootPane();
		KeyStroke escapeStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		rootPane.registerKeyboardAction(escapeListener, escapeStroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
		KeyStroke enterStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		rootPane.registerKeyboardAction(enterListener, enterStroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
		KeyStroke f1Stroke = KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0);
		rootPane.registerKeyboardAction(helpListener, f1Stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
		return rootPane;
	}

	protected boolean hasHelp() {
		return helpAddress !=null;
	}
	protected boolean hasHelpButton() {
		return hasHelp();
	}

	protected void onHelp() {
		if (helpAddress != null)
			BrowserControl.displayURL(HelpUtil.getHelpURL(helpAddress));
		else
			logger.info("no help available");
	}
	protected void onCancel() {
		setVisible(false);
		setDialogResult(JOptionPane.CANCEL_OPTION);
	}

	public void onOk() {
		if (!bind(false))
			return;
		setDialogResult(JOptionPane.OK_OPTION);
		setVisible(false);
	}

	public abstract JComponent createContentPanel();

	public void setVisible(boolean b) {
		if (b && !listenersActivated)
			activateListeners();
		else if (!b && listenersActivated)
			desactivateListeners();
		super.setVisible(b);
	}

	protected boolean listenersActivated = true;

	protected void activateListeners() {
		listenersActivated = true;
	}

	protected void desactivateListeners() {
		listenersActivated = false;
	}

	protected boolean bind(boolean get) {
		return true;
	}

	protected void initComponents() {
		contentPanel = createContentPanel();
		buttonPanel = createButtonPanel();
        getContentPane().setLayout(new BorderLayout());
		if (getContentPane() instanceof JComponent content) {
			FlatUiSupport.styleDialogContent(content);
		}
		if (contentPanel != null)
			getContentPane().add(contentPanel, BorderLayout.CENTER);
		if (buttonPanel != null)
			getContentPane().add(buttonPanel, BorderLayout.AFTER_LAST_LINE);

	}
	protected void clearComponents() {
		if (contentPanel != null)
			getContentPane().remove(contentPanel);
		if (buttonPanel != null)
			getContentPane().remove(buttonPanel);
		
	}
    public void pack() {
       	initComponents();
        super.pack();
        lockMinimumSizeToCurrentPack();
    }

    protected void lockMinimumSizeToCurrentPack() {
        if (getWidth() > 0 && getHeight() > 0 && getMinimumSize().equals(new Dimension())) {
            FlatUiSupport.applyMinimumSize(this, getSize());
        }
    }

	protected void createOkCancelButtons(String okText,String cancelText) {
		ok = new JButton(okText);
		ok.setEnabled(initialOkEnabledState());
		FlatUiSupport.styleDialogButton(ok, true);
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				AbstractDialog.this.onOk();
			}
		});
		cancel = new JButton(cancelText);
		FlatUiSupport.styleDialogButton(cancel, false);
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				AbstractDialog.this.onCancel();
			}
		});
    }
    
		protected JComponent getHelpButton() {
	    	if (help  == null) {
				help= new JButton(MenuManager.getMenuString("Help.text"));//,IconManager.getIcon("menu24.help"));
				FlatUiSupport.styleDialogButton((JButton) help, false);

				help.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent arg0) {
					AbstractDialog.this.onHelp();
			}});
    	}
    	return help;
    }
    protected void createOkCancelButtons() {
    	createOkCancelButtons(Messages.getString("ButtonText.OK"), Messages.getString("ButtonText.Cancel"));
    }

	protected void createCloseButton() {
		ok = new JButton(Messages.getString("ButtonText.Close"));
		FlatUiSupport.styleDialogButton(ok, true);
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				AbstractDialog.this.onOk();
			}
		});
	}

	public ButtonPanel createButtonPanel() {
		if (!hasOkAndCancelButtons() && !hasCloseButton())
			return null;
		if (hasCloseButton())
			createCloseButton();
		else
			createOkCancelButtons();
		ButtonPanel buttonPanel = new ButtonPanel();
		buttonPanel.addButton(ok);
		if (hasOkAndCancelButtons())
			buttonPanel.addButton(cancel);
		if (hasHelpButton())
			buttonPanel.add(getHelpButton());
		return buttonPanel;
	}

	public JComponent createBannerPanel() {
		return null;
	}

	public boolean doModal() {
		pack();
		setLocationRelativeTo(getParent());// to center on parent
		setVisible(true);
		return (getDialogResult() != JOptionPane.CANCEL_OPTION);
	}

	public Object getBean() {
		return null;
	}


	public int execute(Consumer<Object> setter, Consumer<Object> getter) {
		pack();
		setter.accept(getBean());
		bind(true);
		setLocationRelativeTo(null);// to center on screen
		setVisible(true);
		if (getDialogResult() != JOptionPane.CANCEL_OPTION) {
			if (getter != null)
				getter.accept(getBean());
		}
		return getDialogResult();
	}

	protected boolean initialOkEnabledState() {
		return true;
	}

	@SuppressWarnings("unchecked")
	public static ComboBoxModel<Object> getComboBoxModel(String fieldId) {
		Object[] options = FieldDictionary.getInstance().getFieldFromId(fieldId).getOptions(null);
		return new DefaultComboBoxModel<Object>(options);
	}

	public ReferenceNodeModelCache getReferenceCache(boolean task) {
		DocumentFrame df = GraphicManager.getInstance(this).getCurrentFrame();
		return df.getReferenceCache(task);
	}

	public NodeModelCache createCache(boolean task, String viewName) {
		DocumentFrame df = GraphicManager.getInstance(this).getCurrentFrame();
		return df.createCache(task, viewName);
	}

	protected boolean hasOkAndCancelButtons() {
		return !hasCloseButton();
	}

	protected boolean hasCloseButton() {
		return false;
	}

	public static JDialog containedInDialog(Object object) {
		if (!(object instanceof Component))
			return null;
		Component c = (Component) object;
		while (c != null) {
			if (c instanceof JDialog)
				return (JDialog) c;
			c = c.getParent();
		}
		return null;
	}

	public class DoubleClickRadio extends JRadioButton implements MouseListener {
		private static final long serialVersionUID = 1L;
		public DoubleClickRadio(String label, String tooltip) {
			super(label);
			this.setToolTipText(tooltip);
			addMouseListener(this);
		}
		public Point getToolTipLocation(MouseEvent event) { // the tip MUST be touching the button if html because you can click on links
			return new Point(getWidth()-2, -20);
		}

		public JToolTip createToolTip() {
				JToolTip tip = new HyperLinkToolTip();
				tip.setComponent(this);
				return tip;
		}

		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {
				bind(false);
				((JRadioButton) e.getSource()).setSelected(true);
				AbstractDialog.this.onOk();
			}
		}

		public void mousePressed(MouseEvent e) {
		}

		public void mouseReleased(MouseEvent e) {
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}
	}

	public int getDialogResult() {
		return dialogResult;
	}

	public void setDialogResult(int dialogResult) {
		this.dialogResult = dialogResult;
	}

	public ButtonPanel getButtonPanel() {
		return buttonPanel;
	}

	public void setButtonPanel(ButtonPanel buttonPanel) {
		this.buttonPanel = buttonPanel;
	}

	public JComponent getContentPanel() {
		return contentPanel;
	}

	public void setContentPanel(JComponent contentPanel) {
		this.contentPanel = contentPanel;
	}
	public void addDocHelp(String helpAddress) {
		HelpUtil.addDocHelp(this,helpAddress);
		this.helpAddress = helpAddress;
	}
}


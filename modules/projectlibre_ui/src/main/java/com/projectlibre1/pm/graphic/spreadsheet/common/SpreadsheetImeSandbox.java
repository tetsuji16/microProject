package com.projectlibre1.pm.graphic.spreadsheet.common;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextHitInfo;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.KeyStroke;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;

import com.projectlibre1.util.FlatLafSupport;

/**
 * Small Swing sandbox for trying spreadsheet-style editing before touching the
 * real task table.
 */
public final class SpreadsheetImeSandbox {
    private SpreadsheetImeSandbox() {
    }

    public static void main(String[] args) {
        FlatLafSupport.ensureInitialized();
        EventQueue.invokeLater(SpreadsheetImeSandbox::showWindow);
    }

    private static void showWindow() {
        JFrame frame = new JFrame("Spreadsheet IME Sandbox");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        LogSink logSink = new LogSink();
        SandboxTreeTableModel model = new SandboxTreeTableModel();
        SandboxTable table = new SandboxTable(model, logSink);
        logSink.bind(table);

        JLabel help = new JLabel("<html><b>IME sandbox</b>: click the Name column and type Japanese with an IME. "
                + "Ctrl+Left/Right toggles expand/collapse on the selected row when not editing. "
                + "F2 starts edit on the selected row. Arrow keys should keep normal table navigation.</html>");
        help.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JTextArea logArea = new JTextArea(10, 80);
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setBackground(new Color(0xFA, 0xFA, 0xFA));
        logSink.bind(logArea);

        JScrollPane tablePane = new JScrollPane(table);
        tablePane.setBorder(BorderFactory.createEmptyBorder());

        JScrollPane logPane = new JScrollPane(logArea);
        logPane.setBorder(BorderFactory.createTitledBorder("Event log"));

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        JButton focusButton = new JButton(new AbstractAction("Focus table") {
            @Override
            public void actionPerformed(ActionEvent e) {
                table.requestFocusInWindow();
                if (table.getRowCount() > 0) {
                    table.changeSelection(0, 1, false, false);
                }
                table.logState("focus button pressed");
            }
        });
        JButton resetButton = new JButton(new AbstractAction("Reset data") {
            @Override
            public void actionPerformed(ActionEvent e) {
                model.resetSampleData();
                table.requestFocusInWindow();
                table.logState("sample data reset");
            }
        });
        JButton replayButton = new JButton(new AbstractAction("Replay received text") {
            @Override
            public void actionPerformed(ActionEvent e) {
                runReceivedTextReplay(frame, table, logSink, "テスト", 3);
            }
        });
        JCheckBox allowImmediateTyping = new JCheckBox("Start edit on typed key", true);
        allowImmediateTyping.addActionListener(e -> {
            table.setImmediateTypingEnabled(allowImmediateTyping.isSelected());
            table.logState("immediate typing = " + allowImmediateTyping.isSelected());
        });
        JCheckBox allowImeSupport = new JCheckBox("Enable IME support", true);
        allowImeSupport.addActionListener(e -> {
            table.setImeSupportEnabled(allowImeSupport.isSelected());
            table.logState("ime support = " + allowImeSupport.isSelected());
        });
        toolbar.add(focusButton);
        toolbar.add(resetButton);
        toolbar.add(replayButton);
        toolbar.addSeparator();
        toolbar.add(allowImmediateTyping);
        toolbar.add(allowImeSupport);

        JPanel content = new JPanel(new BorderLayout());
        content.add(help, BorderLayout.NORTH);
        content.add(tablePane, BorderLayout.CENTER);
        content.add(toolbar, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, content, logPane);
        splitPane.setResizeWeight(0.72);

        frame.setLayout(new BorderLayout());
        frame.add(splitPane, BorderLayout.CENTER);
        frame.setSize(new Dimension(1000, 720));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        if (table.getRowCount() > 0) {
            table.changeSelection(0, 1, false, false);
        }
        table.requestFocusInWindow();
        table.logState("sandbox ready");
    }

    private enum StartKind {
        NONE,
        TYPED_CHAR,
        INPUT_METHOD,
        F2
    }

    private interface Sink {
        void append(String line);
    }

    private static void runReceivedTextReplay(JFrame frame, SandboxTable table, LogSink logSink, String text, int loops) {
        new Thread(() -> {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    frame.toFront();
                    frame.requestFocus();
                    table.requestFocusInWindow();
                    if (table.getRowCount() > 0) {
                        table.changeSelection(1, 1, false, false);
                    }
                    table.logState("auto replay start text='" + text + "'");
                });
                for (int i = 0; i < loops; i++) {
                    SwingUtilities.invokeAndWait(() -> table.insertReceivedText(text));
                }
                SwingUtilities.invokeLater(() -> table.logState("auto replay done"));
            } catch (Exception ex) {
                logSink.append("[auto] replay failed: " + ex.getMessage());
            }
        }, "sandbox-replay").start();
    }

    private static final class LogSink {
        private Sink sink = line -> { };
        private JTextArea area;

        void bind(JTextArea area) {
            this.area = area;
            this.sink = line -> SwingUtilities.invokeLater(() -> {
                if (this.area == null) {
                    return;
                }
                this.area.append(line + "\n");
                this.area.setCaretPosition(this.area.getDocument().getLength());
            });
        }

        void bind(SandboxTable table) {
            table.setLogSink(this::append);
        }

        void append(String line) {
            sink.append(line);
        }
    }

    private static final class SandboxTreeTableModel extends AbstractTableModel {
        private final List<DemoNode> roots = new ArrayList<>();
        private final List<DemoNode> visible = new ArrayList<>();

        SandboxTreeTableModel() {
            resetSampleData();
        }

        void resetSampleData() {
            roots.clear();

            DemoNode summary = node("1", "Three-story Office Building");
            summary.expanded = true;
            summary.add(node("1.1", "General Conditions"));
            summary.add(node("1.2", "Receive notice to proceed"));
            summary.add(node("1.3", "Submit bond and insurance"));
            summary.add(node("1.4", "Prepare and submit plans"));
            summary.add(node("1.5", "IME sample"));
            summary.add(node("1.6", "Obtain building permit"));
            summary.add(node("1.7", "Submit preliminary shop drawings"));

            DemoNode procurement = node("2", "Long Lead Procurement");
            procurement.expanded = true;
            procurement.add(node("2.1", "Submit monthly requisitions"));
            procurement.add(node("2.2", "Order steel package"));
            procurement.add(node("2.3", "Review vendor submittals"));

            DemoNode blank = node("3", "");
            blank.expanded = true;
            blank.add(node("3.1", ""));

            roots.add(summary);
            roots.add(procurement);
            roots.add(blank);
            rebuildVisibleRows();
        }

        private DemoNode node(String wbs, String name) {
            return new DemoNode(wbs, name);
        }

        private void rebuildVisibleRows() {
            visible.clear();
            for (DemoNode root : roots) {
                appendVisible(root);
            }
            fireTableDataChanged();
        }

        private void appendVisible(DemoNode node) {
            visible.add(node);
            if (!node.expanded) {
                return;
            }
            for (DemoNode child : node.children) {
                appendVisible(child);
            }
        }

        DemoNode getNodeAt(int row) {
            if (row < 0 || row >= visible.size()) {
                return null;
            }
            return visible.get(row);
        }

        boolean toggleExpanded(int row) {
            DemoNode node = getNodeAt(row);
            if (node == null || node.children.isEmpty()) {
                return false;
            }
            node.expanded = !node.expanded;
            rebuildVisibleRows();
            return true;
        }

        int indentRow(int row) {
            DemoNode node = getNodeAt(row);
            if (node == null || row <= 0) {
                return -1;
            }
            DemoNode newParent = getNodeAt(row - 1);
            if (newParent == null || newParent == node.parent) {
                return -1;
            }
            detach(node);
            newParent.expanded = true;
            newParent.add(node);
            rebuildVisibleRows();
            return visible.indexOf(node);
        }

        int outdentRow(int row) {
            DemoNode node = getNodeAt(row);
            if (node == null || node.parent == null) {
                return -1;
            }
            DemoNode parent = node.parent;
            DemoNode grandParent = parent.parent;
            detach(node);
            if (grandParent == null) {
                roots.add(indexAfterParent(parent), node);
            } else {
                grandParent.children.add(indexAfterParent(parent), node);
                node.parent = grandParent;
            }
            rebuildVisibleRows();
            return visible.indexOf(node);
        }

        private void detach(DemoNode node) {
            if (node.parent == null) {
                roots.remove(node);
                return;
            }
            node.parent.children.remove(node);
            node.parent = null;
        }

        private int indexAfterParent(DemoNode parent) {
            if (parent.parent == null) {
                return Math.min(roots.size(), roots.indexOf(parent) + 1);
            }
            return Math.min(parent.parent.children.size(), parent.parent.children.indexOf(parent) + 1);
        }

        boolean canEdit(int row, int column) {
            return column == 1 && getNodeAt(row) != null;
        }

        int findEditableColumn(int row, int preferredColumn) {
            if (canEdit(row, preferredColumn)) {
                return preferredColumn;
            }
            return canEdit(row, 1) ? 1 : -1;
        }

        @Override
        public int getRowCount() {
            return visible.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            return column == 0 ? "WBS" : "Name";
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            DemoNode node = getNodeAt(rowIndex);
            if (node == null) {
                return "";
            }
            return columnIndex == 0 ? node.wbs : node.name;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return canEdit(rowIndex, columnIndex);
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            DemoNode node = getNodeAt(rowIndex);
            if (node == null || columnIndex != 1) {
                return;
            }
            node.name = aValue == null ? "" : String.valueOf(aValue);
            fireTableCellUpdated(rowIndex, columnIndex);
        }

        private static final class DemoNode {
            private final String wbs;
            private String name;
            private final List<DemoNode> children = new ArrayList<>();
            private DemoNode parent;
            private boolean expanded;

            private DemoNode(String wbs, String name) {
                this.wbs = wbs;
                this.name = name == null ? "" : name;
            }

            private void add(DemoNode child) {
                child.parent = this;
                children.add(child);
            }
        }
    }

    private static final class SandboxTable extends JTable {
        private final SandboxTreeTableModel model;
        private final SandboxTextCellEditor editor;
        private Sink logSink = line -> { };
        private boolean immediateTypingEnabled = true;
        private boolean imeSupportEnabled = true;
        private StartKind currentStartKind = StartKind.NONE;

        SandboxTable(SandboxTreeTableModel model, LogSink logSink) {
            super(model);
            this.model = model;
            this.editor = new SandboxTextCellEditor(this::getCurrentStartKind, logSink::append);
            this.logSink = logSink::append;

            setAutoCreateColumnsFromModel(true);
            setCellSelectionEnabled(true);
            setRowSelectionAllowed(true);
            setColumnSelectionAllowed(true);
            setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
            setRowHeight(24);
            setShowGrid(true);
            setGridColor(new Color(0xD0, 0xD0, 0xD0));
            setFillsViewportHeight(true);
            enableInputMethods(true);
            setFocusTraversalKeysEnabled(false);
            putClientProperty("JTable.autoStartsEdit", Boolean.FALSE);
            putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

            setDefaultEditor(String.class, editor);
            setDefaultRenderer(String.class, new SandboxRenderer(this));
            setDefaultRenderer(Object.class, new SandboxRenderer(this));

            addInputMethodListener(new InputMethodListener() {
                @Override
                public void inputMethodTextChanged(InputMethodEvent event) {
                    logEvent("table input method", event);
                }

                @Override
                public void caretPositionChanged(InputMethodEvent event) {
                    logEvent("table input method caret", event);
                }
            });

            addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e) {
                }

                @Override
                public void keyReleased(KeyEvent e) {
                }

                @Override
                public void keyPressed(KeyEvent e) {
                    logEvent("table key pressed", e);
                }
            });
        }

        void setLogSink(Sink logSink) {
            this.logSink = Objects.requireNonNull(logSink);
            editor.setLogSink(logSink);
        }

        void setImmediateTypingEnabled(boolean enabled) {
            this.immediateTypingEnabled = enabled;
        }

        void setImeSupportEnabled(boolean enabled) {
            this.imeSupportEnabled = enabled;
            enableInputMethods(enabled);
            editor.setImeSupportEnabled(enabled);
        }

        void insertReceivedText(String text) {
            if (text == null || text.isEmpty()) {
                return;
            }
            boolean startNewEdit = !isEditing();
            if (!isEditing()) {
                startEditingAtSelection(StartKind.INPUT_METHOD, new EventObject(this), null);
            }
            SwingUtilities.invokeLater(() -> {
                Component editorComponent = editorComponent();
                if (!(editorComponent instanceof JTextField textField)) {
                    logSink.append("[table] no text field for received text");
                    return;
                }
                textField.requestFocusInWindow();
                if (startNewEdit) {
                    textField.setText(text);
                } else {
                    String existing = textField.getText();
                    textField.setText(existing + text);
                }
                textField.setCaretPosition(textField.getDocument().getLength());
                logSink.append("[table] inserted received text='" + text + "'");
            });
        }

        StartKind getCurrentStartKind() {
            return currentStartKind;
        }

        void logState(String message) {
            logSink.append("[table] " + message);
        }

        @Override
        public boolean editCellAt(int row, int column, EventObject e) {
            currentStartKind = detectStartKind(e);
            boolean started = super.editCellAt(row, column, e);
            logSink.append("[table] editCellAt row=" + row + " col=" + column
                    + " kind=" + currentStartKind + " started=" + started);
            if (!started) {
                currentStartKind = StartKind.NONE;
            }
            return started;
        }

        @Override
        protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
            logEvent("table processKeyBinding", e);
            if (e != null) {
                if (pressed && e.getKeyCode() == KeyEvent.VK_TAB) {
                    int selectedRow = getSelectedRow();
                    int newRow = e.isShiftDown() ? model.outdentRow(selectedRow) : model.indentRow(selectedRow);
                    logSink.append("[table] tab " + (e.isShiftDown() ? "outdent" : "indent") + " row=" + selectedRow + " newRow=" + newRow);
                    if (newRow >= 0) {
                        changeSelection(newRow, Math.max(1, getSelectedColumn()), false, false);
                    }
                    return true;
                }
                if (pressed && e.isControlDown()
                        && (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT)) {
                    boolean toggled = model.toggleExpanded(getSelectedRow());
                    logSink.append("[table] ctrl+arrow toggleExpanded=" + toggled);
                    return true;
                }
                if (pressed && e.getKeyCode() == KeyEvent.VK_F2) {
                    startEditingAtSelection(StartKind.F2, new EventObject(this), null);
                    return true;
                }
                if (immediateTypingEnabled && e.getID() == KeyEvent.KEY_TYPED && shouldTreatAsReceivedText(e)) {
                    insertReceivedText(String.valueOf(e.getKeyChar()));
                    return true;
                }
                if (!isEditing() && immediateTypingEnabled && e.getID() == KeyEvent.KEY_TYPED && shouldStartTypingEdit(e)) {
                    startEditingAtSelection(StartKind.TYPED_CHAR, e, null);
                    return true;
                }
            }
            return super.processKeyBinding(ks, e, condition, pressed);
        }

        @Override
        protected void processInputMethodEvent(InputMethodEvent e) {
            logEvent("table processInputMethodEvent", e);
            if (imeSupportEnabled && e != null && !isEditing() && e.getText() != null) {
                startEditingAtSelection(StartKind.INPUT_METHOD, new EventObject(this), e);
                e.consume();
                return;
            }
            super.processInputMethodEvent(e);
        }

        private void startEditingAtSelection(StartKind kind, EventObject eventObject, InputMethodEvent imeEvent) {
            currentStartKind = kind;
            int row = getSelectedRow();
            if (row < 0 && getRowCount() > 0) {
                row = 0;
            }
            int column = model.findEditableColumn(row, getSelectedColumn());
            if (row < 0 || column < 0) {
                logSink.append("[table] no editable cell available");
                return;
            }

            if (!editCellAt(row, column, eventObject)) {
                return;
            }

            SwingUtilities.invokeLater(() -> {
                Component editorComponent = editorComponent();
                if (editorComponent instanceof JTextField textField) {
                    textField.requestFocusInWindow();
                    if (kind == StartKind.TYPED_CHAR && eventObject instanceof KeyEvent keyEvent) {
                        textField.setText(String.valueOf(keyEvent.getKeyChar()));
                        textField.setCaretPosition(textField.getDocument().getLength());
                    }
                    if (kind == StartKind.INPUT_METHOD && imeEvent != null) {
                        InputMethodEvent copied = copyInputMethodEvent(imeEvent, textField);
                        if (copied != null) {
                            textField.dispatchEvent(copied);
                        }
                    }
                }
            });
        }

        private Component editorComponent() {
            TableCellEditor cellEditor = getCellEditor();
            if (cellEditor instanceof SandboxTextCellEditor sandboxEditor) {
                return sandboxEditor.getTextField();
            }
            return null;
        }

        private boolean shouldStartTypingEdit(KeyEvent e) {
            if (e == null) {
                return false;
            }
            if (e.isControlDown() || e.isAltDown() || e.isMetaDown()) {
                return false;
            }
            char c = e.getKeyChar();
            return c != KeyEvent.CHAR_UNDEFINED && !Character.isISOControl(c);
        }

        private boolean shouldTreatAsReceivedText(KeyEvent e) {
            if (e == null || e.getID() != KeyEvent.KEY_TYPED) {
                return false;
            }
            if (e.isControlDown() || e.isAltDown() || e.isMetaDown()) {
                return false;
            }
            char c = e.getKeyChar();
            return c != KeyEvent.CHAR_UNDEFINED && !Character.isISOControl(c) && c > 0x7f;
        }

        private StartKind detectStartKind(EventObject e) {
            if (e instanceof KeyEvent keyEvent && keyEvent.getID() == KeyEvent.KEY_TYPED) {
                return StartKind.TYPED_CHAR;
            }
            if (e instanceof InputMethodEvent) {
                return StartKind.INPUT_METHOD;
            }
            return StartKind.NONE;
        }

        private InputMethodEvent copyInputMethodEvent(InputMethodEvent event, Component target) {
            if (event == null || target == null) {
                return null;
            }
            AttributedCharacterIterator text = event.getText();
            return new InputMethodEvent(target, event.getID(), event.getWhen(), text,
                    event.getCommittedCharacterCount(), event.getCaret(), event.getVisiblePosition());
        }

        private void logEvent(String prefix, KeyEvent e) {
            if (e == null) {
                return;
            }
            logSink.append("[table] " + prefix + " id=" + e.getID()
                    + " keyCode=" + e.getKeyCode()
                    + " keyChar=" + printable(e.getKeyChar())
                    + " ctrl=" + e.isControlDown()
                    + " alt=" + e.isAltDown()
                    + " meta=" + e.isMetaDown()
                    + " editing=" + isEditing());
        }

        private void logEvent(String prefix, InputMethodEvent e) {
            if (e == null) {
                return;
            }
            logSink.append("[table] " + prefix + " committed=" + e.getCommittedCharacterCount()
                    + " text=" + eventText(e.getText())
                    + " editing=" + isEditing());
        }
    }

    private static final class SandboxTextCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JTextField textField = new JTextField();
        private final Supplier<StartKind> startKindSupplier;
        private Sink logSink = line -> { };
        private boolean imeSupportEnabled = true;

        SandboxTextCellEditor(Supplier<StartKind> startKindSupplier, Sink logSink) {
            this.startKindSupplier = startKindSupplier;
            this.logSink = logSink;
            textField.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
            textField.setBackground(UIManager.getColor("TextField.background"));
            textField.addInputMethodListener(new InputMethodListener() {
                @Override
                public void inputMethodTextChanged(InputMethodEvent event) {
                    logEvent("editor input method", event);
                }

                @Override
                public void caretPositionChanged(InputMethodEvent event) {
                    logEvent("editor input method caret", event);
                }
            });
            textField.addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e) {
                    logEvent("editor key typed", e);
                }

                @Override
                public void keyPressed(KeyEvent e) {
                    logEvent("editor key pressed", e);
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    logEvent("editor key released", e);
                }
            });
            textField.enableInputMethods(true);
            textField.setFocusTraversalKeysEnabled(false);
        }

        void setImeSupportEnabled(boolean enabled) {
            this.imeSupportEnabled = enabled;
            textField.enableInputMethods(enabled);
        }

        void setLogSink(Sink logSink) {
            this.logSink = logSink == null ? line -> { } : logSink;
        }

        JTextField getTextField() {
            return textField;
        }

        @Override
        public Object getCellEditorValue() {
            return textField.getText();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            String text = value == null ? "" : String.valueOf(value);
            textField.setText(text);
            if (startKindSupplier.get() == StartKind.INPUT_METHOD) {
                textField.setCaretPosition(Math.min(textField.getDocument().getLength(), textField.getText().length()));
            } else {
                textField.selectAll();
            }
            logSink.append("[editor] start row=" + row + " col=" + column
                    + " startKind=" + startKindSupplier.get()
                    + " text='" + text + "'");
            return textField;
        }

        @Override
        public boolean stopCellEditing() {
            logSink.append("[editor] stopCellEditing text='" + textField.getText() + "'");
            return super.stopCellEditing();
        }

        @Override
        public void cancelCellEditing() {
            logSink.append("[editor] cancelCellEditing text='" + textField.getText() + "'");
            super.cancelCellEditing();
        }

        private void logEvent(String prefix, KeyEvent e) {
            logSink.append("[editor] " + prefix + " id=" + e.getID()
                    + " keyCode=" + e.getKeyCode()
                    + " keyChar=" + printable(e.getKeyChar())
                    + " ctrl=" + e.isControlDown()
                    + " editingText='" + textField.getText() + "'");
        }

        private void logEvent(String prefix, InputMethodEvent e) {
            logSink.append("[editor] " + prefix + " committed=" + e.getCommittedCharacterCount()
                    + " text=" + eventText(e.getText())
                    + " editingText='" + textField.getText() + "'");
        }
    }

    private static final class SandboxRenderer extends DefaultTableCellRenderer {
        private final SandboxTable table;

        SandboxRenderer(SandboxTable table) {
            this.table = table;
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable tableComponent, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(tableComponent, value, isSelected, hasFocus, row, column);
            if (column == 1 && table.model.getNodeAt(row) != null) {
                SandboxTreeTableModel.DemoNode node = table.model.getNodeAt(row);
                setText(indent(node) + node.name);
            } else {
                setText(value == null ? "" : String.valueOf(value));
            }
            if (column == 0) {
                setHorizontalAlignment(SwingConstants.CENTER);
            } else {
                setHorizontalAlignment(SwingConstants.LEFT);
            }
            if (isSelected) {
                setBackground(tableComponent.getSelectionBackground());
                setForeground(tableComponent.getSelectionForeground());
            } else {
                setBackground(tableComponent.getBackground());
                setForeground(tableComponent.getForeground());
            }
            return this;
        }

        private String indent(SandboxTreeTableModel.DemoNode node) {
            StringBuilder sb = new StringBuilder();
            for (SandboxTreeTableModel.DemoNode current = node; current.parent != null; current = current.parent) {
                sb.append("  ");
            }
            if (!node.children.isEmpty()) {
                sb.append(node.expanded ? "[-] " : "[+] ");
            } else {
                sb.append("  ");
            }
            return sb.toString();
        }
    }

    private static String printable(char c) {
        if (c == KeyEvent.CHAR_UNDEFINED) {
            return "<undef>";
        }
        if (Character.isISOControl(c)) {
            return String.format("\\u%04x", (int) c);
        }
        return "'" + c + "'";
    }

    private static String eventText(AttributedCharacterIterator text) {
        if (text == null) {
            return "<null>";
        }
        StringBuilder sb = new StringBuilder();
        for (char c = text.first(); c != AttributedCharacterIterator.DONE; c = text.next()) {
            sb.append(c);
        }
        return "'" + sb + "'";
    }
}

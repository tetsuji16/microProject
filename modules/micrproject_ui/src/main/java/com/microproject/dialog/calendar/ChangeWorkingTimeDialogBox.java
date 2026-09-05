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
package com.microproject.dialog.calendar;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Reader;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.undo.UndoableEditSupport;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.contrib.calendar.ContribIntervals;
import com.microproject.dialog.AbstractDialog;
import com.microproject.dialog.ButtonPanel;
import com.microproject.dialog.options.CalendarDialogBox;
import com.microproject.pm.graphic.IconManager;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.configuration.Settings;
import com.microproject.options.CalendarOption;
import com.microproject.pm.calendar.CalendarExceptionImporter;
import com.microproject.pm.calendar.CalendarService;
import com.microproject.pm.calendar.DayDescriptor;
import com.microproject.pm.calendar.InvalidCalendarException;
import com.microproject.pm.calendar.WorkRangeException;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.calendar.WorkingHours;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.pm.time.HasStartAndEnd;
import com.microproject.strings.Messages;
import com.microproject.undo.CalendarEdit;
import com.microproject.undo.UndoController;
import com.microproject.util.Alert;
import com.microproject.util.DateTime;
import com.microproject.util.FlatUiSupport;


/**
 *
 */
public class ChangeWorkingTimeDialogBox extends AbstractDialog{
	private static final Logger logger = Logger.getLogger(ChangeWorkingTimeDialogBox.class.getName());
	private static final long serialVersionUID = 1L;
	public static class Form {
        protected WorkingCalendar calendar;
        public WorkingCalendar getCalendar() {
            return calendar;
        }
        public void setCalendar(WorkingCalendar calendar) {
            this.calendar = calendar;
        }
    }
    private UndoController undoController;
    private Form form;
    WorkingHours defaultWorkingHours = WorkingHours.getDefault();
    JComboBox calendarType;
    CalendarView sdCalendar;
    JRadioButton unknownWorkingTime;
    JRadioButton defaultWorkingTime;
    JRadioButton nonWorking;
    JRadioButton working;
    ButtonGroup datesSetting;
    JTextField[] timeStart;
    JTextField[] timeEnd;
    JLabel notEditable;
    JLabel caution;
    boolean dirtyWorkingHours = false;
    ContribIntervals lastSelection=new ContribIntervals();
    boolean lastWeekSelection[] = new boolean[7];
    JCheckBox test;
    JComponent cal;
    JButton newCalendar;
    JButton options;
    JButton importNonWorkingDays;
    SimpleDateFormat hourFormat= DateTime.dateFormatInstance("H:mm"); //$NON-NLS-1$
    JLabel basedOnText;
    List<WorkingCalendar> documentCalendars;
    List<WorkingCalendar> projectCalendars;
    WorkingCalendar editedCalendar;
    boolean restrict;
	private Project project;
    /**
     * True while the scratch copy ({@code form}) holds edits not yet committed back to
     * {@code editedCalendar}. Per the MS Project "Change Working Time" spec (issue #353)
     * every in-dialog edit marks this; OK commits everything and Cancel discards it all.
     */
    boolean calendarEdited = false;
    /** Set once {@link #saveCalendar()} commits the scratch copy (test hook). */
    private boolean committed;


    private void setEditable(boolean editable) {
    	notEditable.setVisible(!editable);
    	caution.setVisible(editable);
    	sdCalendar.setEnabled(editable);
    	unknownWorkingTime.setEnabled(editable);
    	defaultWorkingTime.setEnabled(editable);
    	nonWorking.setEnabled(editable);
    	working.setEnabled(editable);
    	for (int i = 0; i < timeStart.length; i++) {
    		timeStart[i].setEditable(editable);
    		timeEnd[i].setEditable(editable);
    		timeStart[i].setEnabled(editable);
    		timeEnd[i].setEnabled(editable);
    	}
		if (importNonWorkingDays != null)
			importNonWorkingDays.setEnabled(editable);
    }
	public static ChangeWorkingTimeDialogBox getInstance(Frame owner, Project project, WorkingCalendar cal, List<WorkingCalendar> documentCalendars, boolean restrict, UndoController undoController) {
		return new ChangeWorkingTimeDialogBox(owner, project,cal,documentCalendars, restrict,undoController);
	}

	private ChangeWorkingTimeDialogBox(Frame owner, Project project, WorkingCalendar cal, List<WorkingCalendar> documentCalendars, boolean restrict,UndoController undoController)  {
		super(owner, Messages.getString("ChangeWorkingTimeDialogBox.ChangeWorkingTime"), true); //$NON-NLS-1$
		this.documentCalendars = documentCalendars;
		this.project=project;
		this.restrict = restrict;
		this.undoController = undoController;
//		ProjectFactory projectFactory = ((MainFrame)owner).getProjectFactory();
		ProjectFactory projectFactory = GraphicManager.getInstance(this).getProjectFactory();
		ArrayList<WorkingCalendar> projCals = projectFactory.getPortfolio().extractCalendars();
		projectCalendars = new ArrayList<>();
		Iterator<WorkingCalendar> i = projCals.iterator();
		WorkingCalendar current;
		while (i.hasNext()) { // add all non base cals that are project cals
			current =(WorkingCalendar)i.next();
			if (!current.isBaseCalendar())
				projectCalendars.add(current);
		}

	    newCalendar = new JButton(Messages.getString("ChangeWorkingTimeDialogBox.New")); //$NON-NLS-1$
		addDocHelp("Change_Working_Time_Dialog");

		form = new Form();
		form.setCalendar(cal);
	}

	private void setCal(WorkingCalendar cal) {
		editedCalendar = cal;
		form.setCalendar(CalendarService.getInstance().makeScratchCopy(cal));
		calendarType.setSelectedItem(editedCalendar);


	}
	class ListRenderer extends DefaultListCellRenderer {
		private Icon resourceIcon = IconManager.getIcon("man");	 //$NON-NLS-1$
		private Icon greenCircle = IconManager.getIcon("greenCircle");	 //$NON-NLS-1$

		public Component getListCellRendererComponent(JList arg0, Object arg1,
				int arg2, boolean arg3, boolean arg4) {
			Component c = super.getListCellRendererComponent(arg0, arg1, arg2, arg3,
					arg4);
			if (documentCalendars != null && documentCalendars.contains(arg1))
				setIcon(resourceIcon);
			else if (projectCalendars.contains(arg1))
				setIcon(greenCircle);
			return c;
		}
	}
	private void fillInCalendarNames() {
		ArrayList<WorkingCalendar> all = new ArrayList<>();
		CalendarService service = CalendarService.getInstance();
		all.addAll(service.getBaseCalendars());
		all.addAll(projectCalendars);

		if (documentCalendars != null)
			all.addAll(documentCalendars);
		ComboBoxModel calModel = new DefaultComboBoxModel(all.toArray());
		calendarType.setModel(calModel);
	}

	private void clearLastSelection() {
		lastSelection.clear();
		for (int i=0; i < 7; i++)
			lastWeekSelection[i] = false;
	}
	protected void initControls() {
	    calendarType = new JComboBox() ;
	    calendarType.setRenderer( new ListRenderer());
	    fillInCalendarNames();
	    basedOnText =  new JLabel();

	    sdCalendar=new CalendarView();

	    unknownWorkingTime= new JRadioButton();
	    notEditable = new JLabel(Messages.getString("ChangeWorkingTimeDialogBox.NotEdiableMessage")); // html provides word wrap //$NON-NLS-1$
	    caution = new JLabel(Messages.getString("ChangeWorkingTimeDialogBox.ModificationMessage")); // html provides word wrap //$NON-NLS-1$

	    // Calendar persistence still needs a dedicated save path here.
	    notEditable.setVisible(false);
	    caution.setVisible(false);

	    defaultWorkingTime= new JRadioButton(Messages.getString("ChangeWorkingTimeDialogBox.UseDefault")); //$NON-NLS-1$
	    nonWorking= new JRadioButton(Messages.getString("ChangeWorkingTimeDialogBox.NonWorkingTime")); //$NON-NLS-1$
	    working= new JRadioButton(Messages.getString("ChangeWorkingTimeDialogBox.NonDefaultWorkingTime")); //$NON-NLS-1$
	    datesSetting= new ButtonGroup();
	    datesSetting.add(unknownWorkingTime);
	    datesSetting.add(defaultWorkingTime);
	    datesSetting.add(nonWorking);
	    datesSetting.add(working);

	    timeStart=new JTextField[Settings.CALENDAR_INTERVALS];
	    timeEnd=new JTextField[Settings.CALENDAR_INTERVALS];
	    DocumentListener makeDirtyListener = new DocumentListener(){
            public void changedUpdate(DocumentEvent e) {
		           markCalendarEdited();
	            }
	            public void insertUpdate(DocumentEvent e) {
	                markCalendarEdited();
	            }
	            public void removeUpdate(DocumentEvent e) {
	                markCalendarEdited();
	            }
	    	};

	    for (int i=0;i<timeStart.length;i++){
		    timeStart[i]=new JTextField(""); //$NON-NLS-1$
		    timeStart[i].setEnabled(false);
		    timeStart[i].getDocument().addDocumentListener(makeDirtyListener);
		    timeEnd[i]=new JTextField(""); //$NON-NLS-1$
		    timeEnd[i].setEnabled(false);
		    timeEnd[i].getDocument().addDocumentListener(makeDirtyListener);
	    }


	    defaultWorkingTime.addActionListener(new ActionListener(){
		    public void actionPerformed(ActionEvent e){
		        setWorkingHours(null);
			    CalendarService service=CalendarService.getInstance();
			    WorkingCalendar wc=form.getCalendar();
			    service.makeDefaultDays(wc,sdCalendar.getSelectedFixedIntervals(), sdCalendar.getSelectedWeekDays());
			    markCalendarEdited();
			    updateWorkingHours();
			    updateView();
			    clearLastSelection();

		    }});

	    nonWorking.addActionListener(new ActionListener(){
		    public void actionPerformed(ActionEvent e){
			    Intervals selectedIntervals = sdCalendar.getSelectedFixedIntervals();
			    boolean[] selectedWeekDays = sdCalendar.getSelectedWeekDays().clone();
		        setWorkingHours(null);
			    CalendarService service=CalendarService.getInstance();
			    WorkingCalendar wc=form.getCalendar();
			    WorkingCalendar copy = wc.makeScratchCopy();
			    try {
			    	// try on copy first
					service.setDaysNonWorking(copy,selectedIntervals, selectedWeekDays);
				    service.setDaysNonWorking(wc,selectedIntervals, selectedWeekDays);
				} catch (InvalidCalendarException e1) {
					Alert.error(e1.getMessage(),ChangeWorkingTimeDialogBox.this);
					return;
				}
			    markCalendarEdited();
			    updateWorkingHours();
			    updateView();
	            clearLastSelection();
		    }});

	    working.addActionListener(new ActionListener(){
		    public void actionPerformed(ActionEvent e){
			    CalendarService service=CalendarService.getInstance();
			    WorkingCalendar wc=form.getCalendar();

		        setWorkingHours(defaultWorkingHours);
			    WorkingCalendar copy = wc.makeScratchCopy();

			    try {
                    service.setDaysWorkingHours(copy,sdCalendar.getSelectedFixedIntervals(), sdCalendar.getSelectedWeekDays(),defaultWorkingHours);
                    service.setDaysWorkingHours(wc,sdCalendar.getSelectedFixedIntervals(), sdCalendar.getSelectedWeekDays(),defaultWorkingHours);
    			    markCalendarEdited();
    			    updateWorkingHours();
                    updateView();
                } catch (WorkRangeException e1) {
                    logger.log(Level.WARNING, "Failed to update working hours", e1);
                } catch (InvalidCalendarException e2) {
                	Alert.error(e2.getMessage(),ChangeWorkingTimeDialogBox.this);
                	return;
				}
                clearLastSelection();
		    }});
	    sdCalendar.addPropertyChangeListener(new PropertyChangeListener(){
	        final CalendarService service=CalendarService.getInstance();
	        public void propertyChange(PropertyChangeEvent e){
//	        	System.out.println("propery change");
	            String property=e.getPropertyName();
	            if ("lastDisplayedDate".equals(property)||"firstDisplayedDate".equals(property)){ //$NON-NLS-1$ //$NON-NLS-2$
	            	updateView();
	            }else if ("selectedDates".equals(property)){ //$NON-NLS-1$
	            	updateWorkingHours();
	            }
	        }
	    });
		setCal(form.getCalendar());
		// add listener at end so above setCal won't trigger update
		calendarType.addActionListener(new ActionListener(){
		    public void actionPerformed(ActionEvent e){
		    	WorkingCalendar cal = (WorkingCalendar)calendarType.getSelectedItem();
		    	if (cal != form.getCalendar()) {
		    		setNewCalendar(cal);
			    	setEditable(isCalEditable(cal));
		    	}
		    }});

		calendarType.setEnabled(!restrict);
		setEditable(isCalEditable(form.getCalendar()));
		newCalendar.setVisible(!restrict);
	}

	private boolean isCalEditable(WorkingCalendar cal) {
	    // form.getCalendar() is a scratch copy, so compare the original selected
	    // calendar as well; otherwise project calendars become falsely read-only.
	    boolean editable = projectCalendars.contains(cal) || projectCalendars.contains(editedCalendar);
    	if (GraphicManager.getInstance().isEditingMasterProject()) // always editable if master project
    		editable = true;
    	return editable;

	}
	private void setNewCalendar(WorkingCalendar cal) {
		saveIfNeeded();
        setCal(cal);
		updateView();

	}
	private void saveWorkingHoursChanges(boolean saveCalendar){
	    try {
	        WorkingHours hours=new WorkingHours();
	        String startS,endS;
	        for (int i=0;i<timeStart.length;i++){
	            startS=timeStart[i].getText();
	            endS=timeEnd[i].getText();
	            if (startS!=null&&endS!=null&&startS.length()>0&&endS.length()>0){
	                hours.setInterval(i,parseTime(startS),parseTime(endS));
	            } else{
	                if (startS.length()==0&&endS.length()==0)
	                    break;
	                else{
	                    Alert.warn(Messages.getString("Message.badTimeFormat"),this); //$NON-NLS-1$
	                    return;
	                }
	            }
	        }
		    CalendarService service=CalendarService.getInstance();
		    WorkingCalendar wc=form.getCalendar();
		    WorkingCalendar copy = wc.makeScratchCopy();
		    service.setDaysWorkingHours(copy,lastSelection,lastWeekSelection,hours);
		    service.setDaysWorkingHours(wc,lastSelection,lastWeekSelection,hours);

		    if (saveCalendar) {
		    	saveCalendar();
		    } else {
		    	markCalendarEdited();
		    }
		    //System.out.println("Saved "+lastSelection);
	    } catch (WorkRangeException e) {
	        Alert.warn(Messages.getString("Message.badTimeIntervals"),this); //$NON-NLS-1$
	    } catch (ParseException e) {
	        Alert.warn(Messages.getString("Message.badTimeFormat"),this); //$NON-NLS-1$
	    } catch (InvalidCalendarException e) {
	    	Alert.warn(e.getMessage(),this);
	    	return;
		}
	    updateView();
	}

	private void saveCalendar() {
		CalendarService service=CalendarService.getInstance();
		WorkingCalendar wc=form.getCalendar();
		UndoableEditSupport undoableEditSupport=undoController.getEditSupport();
		if (undoableEditSupport!=null){
			undoableEditSupport.postEdit(new CalendarEdit(editedCalendar,wc));
		}

		service.assignCalendar(editedCalendar,wc);
		service.saveAndUpdate(editedCalendar);
		committed = true;

	}

	private void importNonWorkingDays() {
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle(Messages.getString("ChangeWorkingTimeDialogBox.ImportNonWorkingDays")); //$NON-NLS-1$
		if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
			return;
		try (Reader reader = Files.newBufferedReader(chooser.getSelectedFile().toPath())) {
			int imported = CalendarExceptionImporter.applyNonWorkingDates(form.getCalendar(),
				CalendarExceptionImporter.readNonWorkingDates(reader), ZoneId.systemDefault());
			if (imported > 0) {
				markCalendarEdited();
				updateView();
			}
		} catch (Exception error) {
			Alert.error(error.getMessage(), this);
		}
	}

	/**
	 * Marks the scratch calendar as edited so {@link #onOk()} commits it (issue #353).
	 */
	void markCalendarEdited() {
		calendarEdited = true;
	}

	/**
	 * Test/verification hook: true once the scratch copy has been committed back
	 * to {@code editedCalendar} via {@link #saveCalendar()}.
	 */
	boolean isCalendarCommitted() {
		return committed;
	}

	/** Test hook: name of the scratch calendar currently being edited. */
	String getFormCalendarName() {
		return form.getCalendar().getName();
	}

	public void saveIfNeeded() {
		if (dirtyWorkingHours)
			saveWorkingHoursChanges(true);
		else if (calendarEdited)
			saveCalendar();
	}

	private Calendar _calendar=DateTime.calendarInstance();
	public long getTimeInMillis(int h,int m){
        _calendar.setTimeInMillis(0);
        _calendar.set(Calendar.HOUR_OF_DAY,h);
        _calendar.set(Calendar.MINUTE,m);
        return _calendar.getTimeInMillis();
	}


	private String formatTime(long time) {
		GregorianCalendar cal = DateTime.calendarInstance();;
		cal.setTimeInMillis(time);
	//	cal.roll(GregorianCalendar.HOUR_OF_DAY,false);
		return hourFormat.format(cal.getTime());
	}
	private long parseTime(String s) throws ParseException {
		GregorianCalendar cal = DateTime.calendarInstance();;
		cal.setTime(hourFormat.parse(s));
	//	cal.roll(GregorianCalendar.HOUR_OF_DAY,true);
		return cal.getTimeInMillis();
	}
	private void setWorkingHours(WorkingHours hours){
//	    System.out.println("setting working hours" + hours);
		// if not working treat as empty
		if (hours != null && hours.getDuration() == 0)
			hours = null;
	    for (int i=0;i<timeStart.length;i++){
	         timeStart[i].setEnabled(hours!=null);
	         timeEnd[i].setEnabled(hours!=null);
	         if (hours==null){
		         timeStart[i].setText(""); //$NON-NLS-1$
		         timeEnd[i].setText(""); //$NON-NLS-1$
	         }
	    }

	    if (hours!=null){
            HasStartAndEnd interval;
        //    int j=0;
            int intervals = hours.getIntervals().size();
            for (int j = 0; j < Settings.CALENDAR_INTERVALS; j++) {
//            for (Iterator i=hours.getIntervals().iterator();i.hasNext();j++){
                interval=(HasStartAndEnd)hours.getInterval(j);
//				i.next();
                if (interval!=null){
                    String startS=formatTime(interval.getStart());
                    timeStart[j].setText(startS);
                    timeEnd[j].setText(formatTime(interval.getEnd()));
                } else {
                	timeStart[j].setText(""); //$NON-NLS-1$
                	timeEnd[j].setText(""); //$NON-NLS-1$
                }
            }
            clearLastSelection();
            lastSelection.addAll(sdCalendar.getSelectedFixedIntervals());
            for (int i =0; i <7; i++)
            	lastWeekSelection[i] = sdCalendar.getSelectedWeekDays()[i];
	    }

	}


	protected void updateView(){

	    CalendarService service=CalendarService.getInstance();
	    WorkingCalendar wc=form.getCalendar();
	    if (wc.isBaseCalendar()) {
	    	basedOnText.setText(" "); // a space.  need a space for vertical spacing //$NON-NLS-1$
	    } else {
		basedOnText.setText(Messages.format("Format.join",
				Messages.getString("ChangeWorkingTimeDialogBox.BasedOn"), wc.getBaseCalendar().getName())); //$NON-NLS-1$
	    }

	    long first=sdCalendar.getFirstDisplayedDate();
	    long last=sdCalendar.getLastDisplayedDate();
	    Calendar calendar=DateTime.calendarInstance();
	    calendar.setTimeInMillis(first);

	    sdCalendar.setFlaggedDates(null);
        sdCalendar.setColorDates(null);

	    DayDescriptor day;
	    ArrayList<Long> flaggedDates = new ArrayList<>();
	    ArrayList<Long> colorDates = new ArrayList<>();
	    while(calendar.getTimeInMillis()<=last){
	        day=service.getDay(wc,calendar.getTimeInMillis());
	        if (day.isModified())
	            flaggedDates.add(Long.valueOf(calendar.getTimeInMillis()));
	        if (!day.isWorking())
	            colorDates.add(Long.valueOf(calendar.getTimeInMillis()));
	        calendar.add(Calendar.DATE,1);
	    }


	    if (flaggedDates.size()>0)
	    	sdCalendar.setFlaggedDates(toLongArray(flaggedDates));
	    if (colorDates.size()>0)
	    	sdCalendar.setColorDates(toLongArray(colorDates));

	    boolean colorWeekDates[] = new boolean[7];
	    boolean flaggedWeekDates[] = new boolean[7];
	    for (int i =0; i < 7; i++) {
	    	day = service.getWeekDay(wc,i+1);
	    	if (day.isModified())
    			flaggedWeekDates[i] = true;
	    	if (!day.isWorking())
    			colorWeekDates[i] = true;
	    }
	    sdCalendar.setColorWeekDates(colorWeekDates);
	    sdCalendar.setFlaggedWeekDates(flaggedWeekDates);
	   // updateWorkingHours();
	    //System.out.println(service.dump(wc));
	}

	//stupid jdnc calendar use long[]
	public long[] toLongArray(ArrayList<Long> list){
	    //if (list.size()==0) return null;
	    long[] array=new long[list.size()];
	    int j=0;
	    for (Iterator<Long> i=list.iterator();i.hasNext();j++) array[j]=i.next().longValue();
	    return array;
	}


	private void updateWorkingHours() {
//		System.out.println("updating working hours");
        final CalendarService service=CalendarService.getInstance();

		 if (dirtyWorkingHours){
	        saveWorkingHoursChanges(false);
	    }

        DayDescriptor day=service.getDay(form.getCalendar(),sdCalendar.getSelectedFixedIntervals(),sdCalendar.getSelectedWeekDays());
        if (day==null){
//            System.out.println("none");
            ChangeWorkingTimeDialogBox.this.datesSetting.setSelected(ChangeWorkingTimeDialogBox.this.unknownWorkingTime.getModel(),true);
            setWorkingHours(null);
        }else if (!day.isModified()){
//            System.out.println("default");
            ChangeWorkingTimeDialogBox.this.datesSetting.setSelected(ChangeWorkingTimeDialogBox.this.defaultWorkingTime.getModel(),true);
            setWorkingHours(day.getWorkingHours());
        }else if (!day.isWorking()){
//            System.out.println("non working");
            ChangeWorkingTimeDialogBox.this.datesSetting.setSelected(ChangeWorkingTimeDialogBox.this.nonWorking.getModel(),true);
            setWorkingHours(null);
        }else{
//            System.out.println("working");
            ChangeWorkingTimeDialogBox.this.datesSetting.setSelected(ChangeWorkingTimeDialogBox.this.working.getModel(),true);
            setWorkingHours(day.getWorkingHours());
        }
        // Re-arm the text-field dirty flag: setWorkingHours() programmatically updates the
        // fields and the DocumentListener then marks calendarEdited. Do not clear
        // calendarEdited here: radio-button actions have already changed the scratch
        // calendar and must remain eligible for saveIfNeeded() (issue #430).
        dirtyWorkingHours = false;
	}

	private JComponent createSettingsPanel() {
		FormLayout settingsLayout = new FormLayout("100dlu,3dlu,100dlu", //$NON-NLS-1$
		"pref,0dlu,p,3dlu,p,3dlu,p,3dlu,p,0dlu,p,0dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p"); //$NON-NLS-1$
		DefaultFormBuilder settingBuilder = new DefaultFormBuilder(settingsLayout);

		settingBuilder.addLabel(Messages.getString("ChangeWorkingTimeDialogBox.For")); //$NON-NLS-1$
		settingBuilder.nextLine(2);
		settingBuilder.add(calendarType);
		settingBuilder.nextLine(2);
		settingBuilder.add(basedOnText);
		settingBuilder.nextLine(2);
		settingBuilder.add(notEditable);
		settingBuilder.add(caution);
		settingBuilder.nextLine(2);
		settingBuilder.add(defaultWorkingTime);
		settingBuilder.nextLine(2);
		settingBuilder.add(nonWorking);
		settingBuilder.nextLine(2);
		settingBuilder.add(working);
		settingBuilder.nextLine(2);

		JPanel time = new JPanel();
		time.setLayout(new GridLayout(1,2));
		time.add(new JLabel(Messages.getString("ChangeWorkingTimeDialogBox.From"))); //$NON-NLS-1$
		time.add(new JLabel(Messages.getString("ChangeWorkingTimeDialogBox.To"))); //$NON-NLS-1$
		settingBuilder.add(time);


		for (int i=0;i<timeStart.length;i++){
			JPanel timePanel = new JPanel();
			timePanel.setLayout(new GridLayout(1,2));
			timePanel.add(timeStart[i]);
			timePanel.add(timeEnd[i]);
			settingBuilder.nextLine(2);
			settingBuilder.add(timePanel);
		}
		return settingBuilder.getPanel();
}

	public JComponent createContentPanel() {

		initControls();

		FormLayout layout = new FormLayout(
		        "300dlu:grow", //$NON-NLS-1$
			"pref,pref,fill:260dlu:grow"); //$NON-NLS-1$

		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();



//	    JSplitPane settingsPanel=new JSplitPane(JSplitPane.VERTICAL_SPLIT);
//	    settingsPanel.setTopComponent(createSettingsPanel());
//	    settingsPanel.setBottomComponent(new JPanel());
//	    settingsPanel.setDividerSize(0);
	    JSplitPane panel=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
//		panel.setLeftComponent(settingsPanel);
		panel.setLeftComponent(createSettingsPanel());

		panel.setRightComponent(sdCalendar);

		JPanel buttonPanel=new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		JButton backButton=new JButton(IconManager.getIcon("calendar.back")); //$NON-NLS-1$
		FlatUiSupport.styleToolBarButton(backButton);
		backButton.addActionListener(new ActionListener(){
	        public void actionPerformed(ActionEvent e) {
	            long first=sdCalendar.getFirstDisplayedDate();
	            Calendar calendar=DateTime.calendarInstance();
	    	    calendar.setTimeInMillis(sdCalendar.getLastDisplayedDate());
	    	    int nbMonth=0;
	    	    while(calendar.getTimeInMillis()>first){
	    	        calendar.add(Calendar.MONTH,-1);
	    	        nbMonth++;
	    	    }
	    	    calendar.setTimeInMillis(first);
	    	    calendar.add(Calendar.MONTH,-nbMonth);
	    	    sdCalendar.setFirstDisplayedDate(calendar.getTimeInMillis());
	        }
        });
		JButton todayButton=new JButton(IconManager.getIcon("calendar.today")); //$NON-NLS-1$
		FlatUiSupport.styleToolBarButton(todayButton);
		todayButton.addActionListener(new ActionListener(){
	        public void actionPerformed(ActionEvent e) {
	            Calendar calendar=DateTime.calendarInstance();
	    	    calendar.setTimeInMillis(System.currentTimeMillis());
	    	    calendar.set(Calendar.DATE,1);
	    	    sdCalendar.setFirstDisplayedDate(calendar.getTimeInMillis());
	        }
        });
		JButton forwardButton=new JButton(IconManager.getIcon("calendar.forward")); //$NON-NLS-1$
		FlatUiSupport.styleToolBarButton(forwardButton);
		forwardButton.addActionListener(new ActionListener(){
	        public void actionPerformed(ActionEvent e) {
	            Calendar calendar=DateTime.calendarInstance();
	    	    calendar.setTimeInMillis(sdCalendar.getLastDisplayedDate());
	    	    calendar.add(Calendar.DATE,1);
	    	    sdCalendar.setFirstDisplayedDate(calendar.getTimeInMillis());
	        }
        });
		buttonPanel.add(backButton);
		buttonPanel.add(todayButton);
		buttonPanel.add(forwardButton);
		builder.nextLine();
		builder.append(buttonPanel);
		builder.nextLine();
		builder.append(panel);

//		builder.append(newCalendar);


		return builder.getPanel();
	}


	public Object getBean(){
		return form;
	}

	public ButtonPanel createButtonPanel() {
		createOkCancelButtons();
	    newCalendar.addActionListener(new ActionListener(){
			    public void actionPerformed(ActionEvent e){
			    	NewBaseCalendarDialog dialog = NewBaseCalendarDialog.getInstance(owner,null);
			    	if (dialog.doModal()) {
			    		fillInCalendarNames();
			    		WorkingCalendar cal = dialog.getNewCalendar();
			    		setNewCalendar(cal);
			    	}
	            }
			 });

	    options = new JButton(Messages.getString("ChangeWorkingTimeDialogBox.Options")); //$NON-NLS-1$
	    options.addActionListener(new ActionListener(){
			    public void actionPerformed(ActionEvent e){
			    	CalendarOption option = project.getCalendarOption();
			    	if (option == null)
			    		option = CalendarOption.getInstance();
			    	CalendarDialogBox dialog = CalendarDialogBox.getInstance((Frame) ChangeWorkingTimeDialogBox.this.getOwner(),option);
			    	if (dialog.doModal()) {
			    		option = CalendarOption.getNewInstance();
			    		dialog.getForm().copyToOption(option);
			    		CalendarOption.setInstance(option);
			    		project.setCalendarOption(option);
			    	}
			 }
		 });
		importNonWorkingDays = new JButton(Messages.getString("ChangeWorkingTimeDialogBox.ImportNonWorkingDays")); //$NON-NLS-1$
		importNonWorkingDays.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					importNonWorkingDays();
				}
			});

		ButtonPanel buttonPanel = new ButtonPanel();
		buttonPanel.addButton(newCalendar);
		buttonPanel.addButton(options);
		buttonPanel.addButton(importNonWorkingDays);
		buttonPanel.addButton(ok);
		buttonPanel.addButton(cancel);
		buttonPanel.add(getHelpButton());
		return buttonPanel;
	}
	public void onOk() {
		saveIfNeeded();
		super.onOk();
	}
}

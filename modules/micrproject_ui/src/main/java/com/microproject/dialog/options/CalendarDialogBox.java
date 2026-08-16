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
package com.microproject.dialog.options;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.dialog.AbstractDialog;
import com.microproject.configuration.Settings;
import com.microproject.options.CalendarOption;
import com.microproject.strings.Messages;
import com.microproject.util.TimeInputParser;

/**
 *
 */
public class CalendarDialogBox extends AbstractDialog{
	private static final long serialVersionUID = -6887419605301434923L;

	public static class Form {
        Double hoursPerDay;
        Double hoursPerWeek;
        Double daysPerMonth;
        String startTime;
        String endTime;
        String weekStart;
        String fiscalYearStart;
        Boolean showTimeInDates;
        Boolean useStartingYear;
        Boolean setAsDefault;
        
        Form(CalendarOption option) {
        	hoursPerDay = Double.valueOf(option.getHoursPerDay());
        	hoursPerWeek = Double.valueOf(option.getHoursPerWeek());
        	daysPerMonth = Double.valueOf(option.getDaysPerMonth());
        	startTime = option.getDefaultStartHour() +""; //$NON-NLS-1$
        	endTime = option.getDefaultEndHour()+""; //$NON-NLS-1$
        	showTimeInDates = Boolean.valueOf(option.isShowTimeInDates());
        	
        }
        public void copyToOption(CalendarOption option) {
        	option.setHoursPerDay(hoursPerDay.doubleValue());
        	option.setHoursPerWeek(hoursPerWeek.doubleValue());
        	option.setDaysPerMonth(daysPerMonth.doubleValue());
        	option.setShowTimeInDates(showTimeInDates.booleanValue());
        	option.setDefaultStartHour(parseHour(startTime, option.getDefaultStartHour()));
        	option.setDefaultEndHour(parseHour(endTime, option.getDefaultEndHour()));
        	
        }
        public Double getDaysPerMonth() {
            return daysPerMonth;
        }
        public void setDaysPerMonth(Double daysPerMonth) {
            this.daysPerMonth = daysPerMonth;
        }
        public String getEndTime() {
            return endTime;
        }
        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }
        public String getFiscalYearStart() {
            return fiscalYearStart;
        }
        public void setFiscalYearStart(String fiscalYearStart) {
            this.fiscalYearStart = fiscalYearStart;
        }
        public Double getHoursPerDay() {
            return hoursPerDay;
        }
        public void setHoursPerDay(Double hoursPerDay) {
            this.hoursPerDay = hoursPerDay;
        }
        public Double getHoursPerWeek() {
            return hoursPerWeek;
        }
        public void setHoursPerWeek(Double hoursPerWeek) {
            this.hoursPerWeek = hoursPerWeek;
        }
        public Boolean getSetAsDefault() {
            return setAsDefault;
        }
        public void setSetAsDefault(Boolean setAsDefault) {
            this.setAsDefault = setAsDefault;
        }
        public Boolean getShowTimeInDates() {
            return showTimeInDates;
        }
        public void setShowTimeInDates(Boolean showTimeInDates) {
            this.showTimeInDates = showTimeInDates;
        }
        public String getStartTime() {
            return startTime;
        }
        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }
        public Boolean getUseStartingYear() {
            return useStartingYear;
        }
        public void setUseStartingYear(Boolean useStartingYear) {
            this.useStartingYear = useStartingYear;
        }
        public String getWeekStart() {
            return weekStart;
        }
        public void setWeekStart(String weekStart) {
            this.weekStart = weekStart;
        }

        private int parseHour(String value, int fallback) {
        	return TimeInputParser.parseHour(value, fallback);
        }
    }	
        
        private Form form;
       
        JSpinner hoursPerDay;
        JSpinner hoursPerWeek;
        JSpinner daysPerMonth;
        JTextField startTime;
        JTextField endTime;
        JComboBox weekStart;
        JComboBox fiscalYearStart;
        JCheckBox showTimeInDates;
        JCheckBox useStartingYear;
        JButton setAsDefault;

        
    	public static CalendarDialogBox getInstance(Frame owner, CalendarOption option) {
    		return new CalendarDialogBox(owner, option);
    	}

    	private CalendarDialogBox(Frame owner, CalendarOption option) {
    		super(owner, Messages.getString("CalendarDialogBox.DurationSettings"), true); //$NON-NLS-1$
   			this.form = new Form(option);
   			addDocHelp("Calendar_Options");
    	}
    	
    	protected void initControls() {
    	    
    	    String[] week =new String [] {Messages.getString("CalendarDialogBox.Monday"),Messages.getString("CalendarDialogBox.Tuesday"),Messages.getString("CalendarDialogBox.Wednesday"),Messages.getString("CalendarDialogBox.Thursday"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    	            Messages.getString("CalendarDialogBox.Friday"),Messages.getString("CalendarDialogBox.Saturday"),Messages.getString("CalendarDialogBox.Sunday") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    	    };
    	    String[] year =new String [] {Messages.getString("CalendarDialogBox.January"),Messages.getString("CalendarDialogBox.February"),Messages.getString("CalendarDialogBox.March"),Messages.getString("CalendarDialogBox.April"),Messages.getString("CalendarDialogBox.May"),Messages.getString("CalendarDialogBox.June"),Messages.getString("CalendarDialogBox.July"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
    	            Messages.getString("CalendarDialogBox.August"),Messages.getString("CalendarDialogBox.September"),Messages.getString("CalendarDialogBox.October"),Messages.getString("CalendarDialogBox.November"),Messages.getString("CalendarDialogBox.December") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
    	    };
    	    weekStart=new JComboBox(week);
    	    fiscalYearStart=new JComboBox(year);   
    	    showTimeInDates = new JCheckBox(Messages.getString("CalendarDialogBox.ShowTimeInDates")); //$NON-NLS-1$
    	    useStartingYear=new JCheckBox(Messages.getString("CalendarDialogBox.UserStartingYearForFVNumbering")); //$NON-NLS-1$
    	    useStartingYear.setEnabled(false);

            startTime= new JTextField (Messages.getString("CalendarDialogBox.EightAM")); //$NON-NLS-1$
            endTime= new JTextField (Messages.getString("CalendarDialogBox.SixPM")); //$NON-NLS-1$
    	              
    		hoursPerDay = new JSpinner(new SpinnerNumberModel(form.getHoursPerDay().doubleValue(),0,24.0,0.5));
    		JSpinner.NumberEditor editor1;
    		editor1 = new JSpinner.NumberEditor(hoursPerDay,"##.##"); //$NON-NLS-1$
    		hoursPerDay.setEditor(editor1);
    		
    		hoursPerWeek = new JSpinner(new SpinnerNumberModel(form.getHoursPerWeek().doubleValue(),0,168.0,0.5));
    		JSpinner.NumberEditor editor2;
    		editor2 = new JSpinner.NumberEditor(hoursPerWeek,"##.##"); //$NON-NLS-1$
    		hoursPerWeek.setEditor(editor2);
    		
    		daysPerMonth = new JSpinner(new SpinnerNumberModel(form.getDaysPerMonth().doubleValue(),0,31.0,1.0));
    		JSpinner.NumberEditor editor3;
    		editor3 = new JSpinner.NumberEditor(daysPerMonth,"##.##"); //$NON-NLS-1$
    		daysPerMonth.setEditor(editor3);
    		
            setAsDefault= new JButton(Messages.getString("CalendarDialogBox.SetAsDefault")); //$NON-NLS-1$
            setAsDefault.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    if (!bind(false)) {
                        return;
                    }
                    applyFormToDefault(form);
                }
            });

    		fiscalYearStart.addActionListener(new ActionListener(){
    		    public void actionPerformed(ActionEvent e){
    	    	    if (fiscalYearStart.getSelectedItem().equals(Messages.getString("CalendarDialogBox.January"))){ //$NON-NLS-1$
    	    	        useStartingYear.setEnabled(false);
    	    	    }else{
    	    	        useStartingYear.setEnabled(true);
    	    	    }
    		    }});
    		

    		
    	}

	static void applyFormToDefault(CalendarDialogBox.Form form) {
		form.copyToOption(CalendarOption.getDefaultInstance());
	}
 
    
    	protected boolean bind(boolean get) {
    		if (form == null)
    			return false;
    		if (get) {
    		    weekStart.setSelectedItem(form.getWeekStart());
    		    fiscalYearStart.setSelectedItem(form.getFiscalYearStart()); 		    
    		    showTimeInDates.setSelected((form.getShowTimeInDates()).booleanValue());
    		    useStartingYear.setSelected((form.getUseStartingYear()).booleanValue());
    		    startTime.setText(/*form.getStartTime()*/Messages.getString("CalendarDialogBox.Eight")); //$NON-NLS-1$
    		    endTime.setText(/*form.getEndTime()*/Messages.getString("CalendarDialogBox.Seventeen")); //$NON-NLS-1$
    		    hoursPerDay.setValue(form.getHoursPerDay());
    		    hoursPerWeek.setValue(form.getHoursPerWeek());
    		    daysPerMonth.setValue(form.getDaysPerMonth());    		    
    		    setAsDefault.setSelected((form.getSetAsDefault()).booleanValue());
    		    
 
 
    		} else {
    			form.setWeekStart((String)weekStart.getSelectedItem());
    			form.setFiscalYearStart((String)fiscalYearStart.getSelectedItem());  		    
    			form.setShowTimeInDates(Boolean.valueOf(showTimeInDates.isSelected()));
    			Boolean b1=Boolean.valueOf(useStartingYear.isSelected());
    			form.setUseStartingYear(b1);
    			form.setStartTime(startTime.getText());
    			form.setEndTime(endTime.getText());
    			form.setHoursPerDay((Double)hoursPerDay.getValue());
    			form.setHoursPerWeek((Double)hoursPerWeek.getValue());
    			form.setDaysPerMonth((Double)daysPerMonth.getValue());    			
    			Boolean b2=Boolean.valueOf(setAsDefault.isSelected());
    			form.setSetAsDefault(b2);

    		}
    		return true;
    	}
    	
    	public JComponent createContentPanel() {
    	
    		initControls();
    		
    		FormLayout layout = new FormLayout(
    		        "p,3dlu,p,p:grow", //$NON-NLS-1$
    	    		  "p,3dlu,p,3dlu,p,3dlu,p"); //$NON-NLS-1$

    		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
    		builder.setDefaultDialogBorder();
    		CellConstraints cc = new CellConstraints();
			builder.add(new JLabel(Messages.getString("CalendarDialogBox.TheseSettingsOnlyApplyToDuration")),cc.xyw(builder.getColumn(), builder //$NON-NLS-1$
					.getRow(), 4));
    		builder.nextLine(2);
    		builder.append(Messages.getString("CalendarDialogBox.HoursPerday"),hoursPerDay); //$NON-NLS-1$
    		builder.nextLine(2);
    		builder.append(Messages.getString("CalendarDialogBox.HoursPerWeek"),hoursPerWeek); //$NON-NLS-1$
    		builder.nextLine(2);
    		builder.append(Messages.getString("CalendarDialogBox.DaysPerMonth"),daysPerMonth); //$NON-NLS-1$
    		builder.nextLine(2);
    		builder.append(setAsDefault);
    		builder.nextLine(2);
    		builder.append(showTimeInDates);
  

    		return builder.getPanel();
    	}
//    	public JComponent createContentPanel() {
//        	
//    		initControls();
//    		
//    		FormLayout layout = new FormLayout(
//    		        "left:80dlu,3dlu,50dlu, 3dlu,130dlu,3dlu",
//    	    		  "p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,p,3dlu,fill:10dlu:grow");
//
//    		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
//    		builder.setDefaultDialogBorder();
//    		CellConstraints cc = new CellConstraints();
//    		
//    		builder.addSeparator("Local calendar options");
//    		builder.nextLine(4);
//    		builder.append("Week starts on :",weekStart);
//    		builder.nextLine(2);
//    		builder.append("Fiscal year starts in :",fiscalYearStart);
//    		builder.nextLine(2);
//    		builder.nextColumn(4);
//    		builder.append(useStartingYear);
//    		builder.nextLine(4);
//    		builder.addSeparator("");
//    		builder.nextLine(4);
//    		builder.append("Default start time :",startTime);
//    		builder.nextLine(2);
//    		builder.append("Default end time :",endTime);
//    		builder.nextLine(4);
//    		builder.addSeparator("");
//    		builder.nextLine(4);
//    		builder.append("Hours per day :",hoursPerDay);
//    		builder.nextLine(2);
//    		builder.append("Hours per week :",hoursPerWeek);
//    		builder.nextLine(2);
//    		builder.append("Days per month :",daysPerMonth);
//    		builder.nextLine(4);
//    		builder.append(setAsDefault);
//    		builder.nextLine(2);
//  
//
//    		return builder.getPanel();
//    	}
    	 	
    	public Object getBean(){
    		return form;
    	}

		public Form getForm() {
			return form;
		}
}


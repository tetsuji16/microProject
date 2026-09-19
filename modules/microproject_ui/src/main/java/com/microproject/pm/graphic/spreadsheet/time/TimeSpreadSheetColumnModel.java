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
package com.microproject.pm.graphic.spreadsheet.time;



import java.awt.Dimension;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.timescale.TimeInterval;
import com.microproject.timescale.TimeIterator;
import com.microproject.timescale.TimeScaleEvent;
import com.microproject.timescale.TimeScaleListener;
/**
 *
 */
public class TimeSpreadSheetColumnModel extends DefaultTableColumnModel implements TimeScaleListener {
	protected CoordinatesConverter coord;
	protected TimeSpreadSheet spreadSheet;

	public TimeSpreadSheetColumnModel(TimeSpreadSheet spreadSheet) {
		super();
		this.spreadSheet=spreadSheet;
	}
	
	/*public void addColumn(TableColumn tc){
		super.addColumn(tc);
	}
	public void removeColumn(TableColumn tc){
		super.removeColumn(tc);
	}*/
	
	
	
	public void updateColumns(){
        while (getColumnCount() > 0){
            removeColumn(getColumn(0));
            ((TimeSpreadSheetModel)spreadSheet.getModel()).decrementColumnCount();
        }
        
        
        if (coord==null) return;
		TimeSpreadSheetModel model=(TimeSpreadSheetModel)spreadSheet.getModel();
		TimeIterator iterator=coord.getProjectTimeIterator();
		TimeInterval interval;
		model.resetTimeIntervals();
		//int totalW=0;
		for (int i=1;iterator.hasNext();i++){
			interval=iterator.next();
			//System.out.println("interval#"+i+"="+interval);
			int w=(int)Math.round(coord.toW(interval.getEnd1()-interval.getStart1()));
			TableColumn col=new TableColumn(i,w);
			col.setMinWidth(w);
			col.setMaxWidth(w);
			addColumn(col);
			model.incrementColumnCount();
			
			model.addTimeInterval(interval);
			//totalW+=w;
		}
		//spreadSheet.setPreferredSize(new Dimension(totalW,spreadSheet.getPreferredSize().height));
//		int totalWidth=getTotalColumnWidth();
//		spreadSheet.setPreferredSize(new Dimension(totalWidth,spreadSheet.getPreferredSize().height));
		//System.out.println("updateColumns coord="+CalendarUtil.toString(coord.getEnd()));
	}
	
	
	
    
    public CoordinatesConverter getCoord() {
        return coord;
    }
    public void setCoord(CoordinatesConverter coord) {
        if (this.coord!=null) this.coord.removeTimeScaleListener(this);
        this.coord = coord;
		coord.addTimeScaleListener(this);
    }

    

	public void timeScaleChanged(TimeScaleEvent e) {
		updateColumns();
		
		//dynamic time spreadsheets don't update themselves for a stange reason
		//fix here
//   		Dimension d=spreadSheet.getPreferredSize();
//   		d.setSize(coord.toW(coord.getEnd()-coord.getOrigin()),spreadSheet.getPreferredSize().getHeight());
   		spreadSheet.setPreferredSize(new Dimension((int)coord.toW(coord.getEnd()-coord.getOrigin()),spreadSheet.getPreferredSize().height));
		spreadSheet.revalidate();

	}
}


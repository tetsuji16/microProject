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
package com.microproject.pm.snapshot;

import java.util.function.Consumer;

import java.io.Serializable;


import com.microproject.configuration.Settings;
import com.microproject.pm.task.Task;
import com.microproject.pm.task.TaskSnapshot;
import com.microproject.strings.Messages;

/**
 *
 */
public class SnapshottableImpl implements Snapshottable, Serializable {
	DataSnapshot snapshots[] = null;
	
	/**
	 * 
	 */
	public SnapshottableImpl(int number) {
		snapshots = new DataSnapshot[number];
	}

	public DataSnapshot getSnapshot(Object snapshotId) {
		return snapshots[((Integer)snapshotId).intValue()];
	}

	public void setSnapshot(Object snapshotId, DataSnapshot snapshot) {
		snapshots[((Integer)snapshotId).intValue()] = cloneSnapshot(snapshot);
	}
	
	// functor this guy
	public void saveCurrentToSnapshot(Object snapshotId) {
		setSnapshot(snapshotId,snapshots[CURRENT.intValue()]);
	}	
	
	public DataSnapshot getCurrentSnapshot() {
		return snapshots[CURRENT.intValue()];
	}

	public void setCurrentSnapshot(DataSnapshot snapshot) {
		snapshots[CURRENT.intValue()] = snapshot;
	}	
	
	public void clearSnapshot(Object snapshotId) {
		snapshots[((Integer)snapshotId).intValue()] = null;
	}
	
	public static class SaveCurrentToSnapshotClosure implements Consumer<Object> {
		Object snapshotId;
		public SaveCurrentToSnapshotClosure(Object snapshotId) {
			this.snapshotId = snapshotId;
		}
		public void accept(Object arg0) {
			if (arg0 instanceof Snapshottable)
				((Snapshottable) arg0).saveCurrentToSnapshot(snapshotId);
		}
	}

	public static class ClearSnapshotClosure implements Consumer<Object> {
		Object snapshotId;
		public ClearSnapshotClosure(Object snapshotId) {
			this.snapshotId = snapshotId;
		}
		public void accept(Object arg0) {
			if (arg0 instanceof Snapshottable)			
				((Snapshottable) arg0).clearSnapshot(snapshotId);
		}
	}

	public DataSnapshot cloneSnapshot(DataSnapshot snapshot) {
		return snapshot; //not a clone at all.  This should be overridden
	}

	public static String snapshotName(int baselineNumber) {
		String text = Messages.getString("Text.Baseline");
		if (baselineNumber > 0)
			text += " " + baselineNumber;
		return text;
	}
	
	private static String[] snapshotNames = null;
	public static String[] getSnapshotNames() {
		if (snapshotNames == null) {
			snapshotNames = new String[Settings.NUM_ARRAY_BASELINES];
			for (int i=0; i < Settings.NUM_ARRAY_BASELINES; i++)
				snapshotNames[i] = snapshotName(i);
		}
		return snapshotNames;
	}
	
	
	public Object clone(){ //Handle wbs outside
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}
	public Object cloneWithTask(Task task){ //Handle wbs outside
			SnapshottableImpl s=(SnapshottableImpl)clone();
			s.snapshots=new DataSnapshot[snapshots.length];
			for (int i=0;i<snapshots.length;i++){
				s.snapshots[i]=(snapshots[i]==null)?null:(DataSnapshot)((TaskSnapshot)snapshots[i]).deepCloneWithTask(task);
			}
			return s;
	}

}

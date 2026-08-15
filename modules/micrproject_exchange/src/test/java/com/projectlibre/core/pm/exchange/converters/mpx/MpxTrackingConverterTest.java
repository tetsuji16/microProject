package com.projectlibre.core.pm.exchange.converters.mpx;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.projectlibre.pm.calendar.DefaultWorkCalendar;
import com.projectlibre.core.pm.exchange.converters.type.DateUTCConverter;
import com.projectlibre.pm.resources.Resource;
import com.projectlibre.pm.resources.ResourcePool;
import com.projectlibre.pm.tasks.Assignment;

import net.sf.mpxj.Duration;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.ResourceAssignment;
import net.sf.mpxj.TimeUnit;
import net.sf.mpxj.TaskMode;
import net.sf.mpxj.mspdi.schema.TimephasedDataType;
import junit.framework.TestCase;

public class MpxTrackingConverterTest extends TestCase {
	public void testTaskConverterPreservesMicrosoftTrackingFields() {
		ProjectFile file = new ProjectFile();
		net.sf.mpxj.Task source = file.addTask();
		Date actualStart = new Date(1_700_000_000_000L);
		Date actualFinish = new Date(1_700_028_800_000L);
		source.setPercentageComplete(40);
		source.setPercentageWorkComplete(25);
		source.setPhysicalPercentComplete(30);
		source.setActualStart(actualStart);
		source.setActualFinish(actualFinish);
		source.setActualDuration(Duration.getInstance(2, TimeUnit.DAYS));
		source.setRemainingDuration(Duration.getInstance(3, TimeUnit.DAYS));
		source.setWork(Duration.getInstance(40, TimeUnit.HOURS));
		source.setActualWork(Duration.getInstance(10, TimeUnit.HOURS));
		source.setRemainingWork(Duration.getInstance(30, TimeUnit.HOURS));
		source.setTaskMode(TaskMode.MANUALLY_SCHEDULED);
		source.setActive(false);

		com.projectlibre.pm.tasks.Task target = new com.projectlibre.pm.tasks.Task();
		MpxImportState state = new MpxImportState();
		state.setProjectBaseCalendar(new DefaultWorkCalendar());

		new MpxTaskConverter().from(source, target, state);

		assertEquals(0.40d, (Double) target.getPropertyValue("percentComplete"), 0.00001d);
		assertEquals(0.25d, (Double) target.getPropertyValue("percentWorkComplete"), 0.00001d);
		assertEquals(0.30d, (Double) target.getPropertyValue("physicalPercentComplete"), 0.00001d);
		assertEquals(convertDate(actualStart), target.getPropertyValue("actualStart"));
		assertEquals(convertDate(actualFinish), target.getPropertyValue("actualFinish"));
		assertEquals(2.0d, durationValue(target, "actualDuration"), 0.00001d);
		assertEquals(3.0d, durationValue(target, "remainingDuration"), 0.00001d);
		assertEquals(40.0d, durationValue(target, "work"), 0.00001d);
		assertEquals(10.0d, durationValue(target, "actualWork"), 0.00001d);
		assertEquals(30.0d, durationValue(target, "remainingWork"), 0.00001d);
		assertEquals(Boolean.TRUE, target.getPropertyValue("manuallyScheduled"));
		assertEquals(Boolean.TRUE, target.getPropertyValue("inactiveTask"));
	}

	public void testAssignmentConverterPreservesMicrosoftTrackingFields() {
		ProjectFile file = new ProjectFile();
		net.sf.mpxj.Task sourceTask = file.addTask();
		net.sf.mpxj.Resource sourceResource = file.addResource();
		ResourceAssignment source = sourceTask.addResourceAssignment(sourceResource);
		Date actualStart = new Date(1_700_000_000_000L);
		Date actualFinish = new Date(1_700_028_800_000L);
		source.setUnits(100);
		source.setWork(Duration.getInstance(16, TimeUnit.HOURS));
		source.setPercentageWorkComplete(50);
		source.setActualStart(actualStart);
		source.setActualFinish(actualFinish);
		source.setActualWork(Duration.getInstance(8, TimeUnit.HOURS));
		source.setRemainingWork(Duration.getInstance(8, TimeUnit.HOURS));

		MpxImportState state = new MpxImportState();
		ResourcePool resourcePool = new ResourcePool();
		Resource targetResource = new Resource();
		resourcePool.addResource(targetResource);
		state.setResourcePool(resourcePool);
		state.mapResource(sourceResource, targetResource);
		state.setMpxTimephasedMap(new HashMap<ResourceAssignment, List<TimephasedDataType>>());
		Assignment target = new Assignment();
		target.setTask(new com.projectlibre.pm.tasks.Task());

		new MpxAssignmentConverter().from(source, target, state, -1);

		assertEquals(0.50d, (Double) target.getPropertyValue("percentWorkComplete"), 0.00001d);
		assertEquals(convertDate(actualStart), target.getPropertyValue("actualStart"));
		assertEquals(convertDate(actualFinish), target.getPropertyValue("actualFinish"));
		assertEquals(16.0d, durationValue(target, "work"), 0.00001d);
		assertEquals(8.0d, durationValue(target, "actualWork"), 0.00001d);
		assertEquals(8.0d, durationValue(target, "remainingWork"), 0.00001d);
	}

	private double durationValue(com.projectlibre.core.fields.HasFields target, String property) {
		return ((com.projectlibre.core.time.Duration) target.getPropertyValue(property)).getValue();
	}

	private Date convertDate(Date value) {
		return (Date) new DateUTCConverter().from(value);
	}
}

/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.assignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.microproject.datatype.Rate;
import com.microproject.pm.assignment.contour.AbstractContour;
import com.microproject.pm.assignment.contour.AbstractContourBucket;
import com.microproject.pm.assignment.contour.ContourFactory;
import com.microproject.pm.assignment.contour.ContourTypes;
import com.microproject.pm.assignment.contour.PersonalContourBucket;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.criticalpath.TaskSchedule;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.task.Task;

/** Immutable value view of a detached saved-baseline assignment profile. */
public final class BaselineAssignmentProfile {
	public record Bucket(long duration, double units) {
		public Bucket {
			if (duration < 0L || !Double.isFinite(units))
				throw new IllegalArgumentException("Invalid baseline contour bucket");
		}
	}

	public record Contour(int type, List<Bucket> buckets) {
		public Contour {
			ContourTypes.Kind kind = ContourTypes.Kind.fromCode(type);
			buckets = List.copyOf(Objects.requireNonNull(buckets, "buckets"));
			if ((kind == ContourTypes.Kind.CONTOURED) != !buckets.isEmpty())
				throw new IllegalArgumentException("Baseline contour type and buckets do not match");
		}
	}

	private final long profileId;
	private final long resourceUniqueId;
	private final long start;
	private final long finish;
	private final long duration;
	private final double units;
	private final double rate;
	private final int rateUnit;
	private final int requestDemandType;
	private final int costRateIndex;
	private final long delay;
	private final long levelingDelay;
	private final long overtimeWork;
	private final Contour workContour;
	private final Contour costContour;
	private final WorkingCalendar calendar;

	public BaselineAssignmentProfile(long profileId, long resourceUniqueId, long start, long finish,
		long duration, double units, double rate, int rateUnit, int requestDemandType, int costRateIndex,
		long delay, long levelingDelay, long overtimeWork, Contour workContour, Contour costContour,
		WorkingCalendar calendar) {
		if (profileId == 0L || resourceUniqueId == 0L || finish < start || duration < 0L
				|| !Double.isFinite(units) || !Double.isFinite(rate))
			throw new IllegalArgumentException("Invalid baseline assignment profile: profile=" + profileId
				+ " resource=" + resourceUniqueId + " start=" + start + " finish=" + finish + " duration=" + duration
				+ " units=" + units + " rate=" + rate);
		this.profileId = profileId;
		this.resourceUniqueId = resourceUniqueId;
		this.start = start;
		this.finish = finish;
		this.duration = duration;
		this.units = units;
		this.rate = rate;
		this.rateUnit = rateUnit;
		this.requestDemandType = requestDemandType;
		this.costRateIndex = costRateIndex;
		this.delay = delay;
		this.levelingDelay = levelingDelay;
		this.overtimeWork = overtimeWork;
		this.workContour = Objects.requireNonNull(workContour, "workContour");
		this.costContour = Objects.requireNonNull(costContour, "costContour");
		this.calendar = Objects.requireNonNull(calendar, "calendar").copyForBaselineSnapshot();
	}

	public static BaselineAssignmentProfile capture(Assignment assignment) {
		Objects.requireNonNull(assignment, "assignment");
		if (!assignment.isBaselineAssignment())
			throw new IllegalArgumentException("Only saved baseline assignments can be captured");
		AssignmentDetail detail = assignment.detail;
		return new BaselineAssignmentProfile(assignment.getUniqueId(), assignment.getResource().getUniqueId(),
			assignment.getStart(), assignment.getFinish(), detail.getDuration(), detail.getUnits(),
			detail.getRate().getValue(), detail.getRate().getTimeUnit(), detail.getRequestDemandType(),
			detail.getCostRateIndex(), detail.getDelay(), detail.getLevelingDelay(), detail.getOvertimeWork(),
			contour(detail.getWorkContour()), contour(detail.getCostContour()), detail.getBaselineCalendar());
	}

	private static Contour contour(AbstractContour contour) {
		List<Bucket> buckets = new ArrayList<>();
		if (contour.isPersonal()) {
			for (AbstractContourBucket bucket : contour.getContourBuckets())
				buckets.add(new Bucket(bucket.getBucketDuration(0L), bucket.getUnits()));
		}
		return new Contour(contour.getType(), buckets);
	}

	public long profileId() { return profileId; }
	public long resourceUniqueId() { return resourceUniqueId; }
	public long start() { return start; }
	public long finish() { return finish; }
	public long duration() { return duration; }
	public double units() { return units; }
	public double rate() { return rate; }
	public int rateUnit() { return rateUnit; }
	public int requestDemandType() { return requestDemandType; }
	public int costRateIndex() { return costRateIndex; }
	public long delay() { return delay; }
	public long levelingDelay() { return levelingDelay; }
	public long overtimeWork() { return overtimeWork; }
	public Contour workContour() { return workContour; }
	public Contour costContour() { return costContour; }
	public WorkingCalendar calendarCopy() { return calendar.copyForBaselineSnapshot(); }

	public Assignment restore(Task task, Resource resource) {
		Objects.requireNonNull(task, "task");
		Objects.requireNonNull(resource, "resource");
		if (resource.getUniqueId() != resourceUniqueId)
			throw new IllegalArgumentException("Baseline profile resource identity does not match");
		AssignmentDetail detail = new AssignmentDetail(task, resource, units, requestDemandType, delay);
		detail.setRate(new Rate(rate, rateUnit));
		detail.setDuration(duration);
		detail.setLevelingDelay(levelingDelay);
		detail.setOvertimeWork(overtimeWork);
		detail.setCostRateIndex(costRateIndex);
		detail.setWorkContour(restoreContour(workContour));
		if (costContour.type() == ContourTypes.Kind.CONTOURED.code())
			detail.setContour(HasTimeDistributedData.COST, buckets(costContour));
		else
			detail.setContour(HasTimeDistributedData.COST,
				ContourFactory.getInstance(ContourTypes.Kind.fromCode(costContour.type())));
		TaskSchedule schedule = new TaskSchedule(task, TaskSchedule.Kind.CURRENT);
		schedule.setStart(start);
		schedule.setEnd(finish);
		detail.setTaskSchedule(schedule);
		detail.convertToBaselineAssignment(false);
		detail.setBaselineCalendarSnapshot(calendar);
		Assignment result = new Assignment(detail);
		result.setUniqueId(profileId);
		return result;
	}

	private static AbstractContour restoreContour(Contour contour) {
		ContourTypes.Kind kind = ContourTypes.Kind.fromCode(contour.type());
		return kind == ContourTypes.Kind.CONTOURED
			? com.microproject.pm.assignment.contour.PersonalContour.getInstance(buckets(contour))
			: ContourFactory.getInstance(kind);
	}

	private static AbstractContourBucket[] buckets(Contour contour) {
		AbstractContourBucket[] result = new AbstractContourBucket[contour.buckets().size()];
		for (int index = 0; index < result.length; index++) {
			Bucket bucket = contour.buckets().get(index);
			result[index] = PersonalContourBucket.getInstance(bucket.duration(), bucket.units());
		}
		return result;
	}
}

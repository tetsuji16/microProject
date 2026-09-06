package com.microproject.pm.costing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.microproject.pm.assignment.RequestDemandType;
import com.microproject.pm.assignment.timesheet.TimesheetStatus;
import com.microproject.pm.resource.BookingType;
import com.microproject.pm.resource.ResourceType;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.scheduling.ConstraintType;
import com.microproject.pm.scheduling.SchedulingType;
import com.microproject.pm.task.AccessControlPolicy;
import com.microproject.pm.task.ProjectStatus;
import com.microproject.pm.task.ProjectType;

class PersistedKindCompatibilityTest {
	@Test
	void costingKindsRoundTripTheirPersistedCodes() {
		assertEquals(ExpenseType.Kind.DIRECT, ExpenseType.Kind.fromCode(ExpenseType.DIRECT));
		assertEquals(Accrual.Kind.PRORATED, Accrual.Kind.fromCode(Accrual.PRORATED));
		assertEquals(EarnedValueMethodType.Kind.PHYSICAL_PERCENT_COMPLETE,
				EarnedValueMethodType.Kind.fromCode(EarnedValueMethodType.PHYSICAL_PERCENT_COMPLETE));
		assertEquals(SchedulingType.Kind.FIXED_WORK, SchedulingType.Kind.fromCode(SchedulingType.FIXED_WORK));
		assertEquals(RequestDemandType.Kind.DEMAND, RequestDemandType.Kind.fromCode(RequestDemandType.DEMAND));
		assertEquals(BookingType.Kind.COMMITTED, BookingType.Kind.fromCode(BookingType.COMMITTED));
		assertEquals(ProjectType.Kind.IT, ProjectType.Kind.fromCode(ProjectType.IT));
		assertEquals(ProjectStatus.Kind.ON_HOLD, ProjectStatus.Kind.fromCode(ProjectStatus.ON_HOLD));
		assertEquals(AccessControlPolicy.Kind.RESTRICTED,
				AccessControlPolicy.Kind.fromCode(AccessControlPolicy.RESTRICTED));
		assertEquals(TimesheetStatus.Kind.MIXED,
				TimesheetStatus.Kind.fromCode(TimesheetStatus.MIXED));
		assertEquals(ResourceType.Kind.MATERIAL, ResourceType.Kind.fromCode(ResourceType.MATERIAL));
		assertEquals(DependencyType.Kind.FS, DependencyType.Kind.fromCode(DependencyType.FS));
		assertEquals(ConstraintType.Kind.FNLT, ConstraintType.Kind.fromCode(ConstraintType.FNLT));
	}

	@Test
	void costingKindsRejectUnknownCodes() {
		assertThrows(IllegalArgumentException.class, () -> ExpenseType.Kind.fromCode(99));
		assertThrows(IllegalArgumentException.class, () -> Accrual.Kind.fromCode(-1));
		assertThrows(IllegalArgumentException.class, () -> EarnedValueMethodType.Kind.fromCode(99));
		assertThrows(IllegalArgumentException.class, () -> SchedulingType.Kind.fromCode(99));
		assertThrows(IllegalArgumentException.class, () -> RequestDemandType.Kind.fromCode(99));
		assertThrows(IllegalArgumentException.class, () -> BookingType.Kind.fromCode(99));
		assertThrows(IllegalArgumentException.class, () -> ProjectType.Kind.fromCode(99));
		assertThrows(IllegalArgumentException.class, () -> ProjectStatus.Kind.fromCode(99));
		assertThrows(IllegalArgumentException.class, () -> AccessControlPolicy.Kind.fromCode(99));
		assertThrows(IllegalArgumentException.class, () -> TimesheetStatus.Kind.fromCode(99));
		assertThrows(IllegalArgumentException.class, () -> ResourceType.Kind.fromCode(99));
		assertThrows(IllegalArgumentException.class, () -> DependencyType.Kind.fromCode(99));
		assertThrows(IllegalArgumentException.class, () -> ConstraintType.Kind.fromCode(99));
	}

	@Test
	void expenseTypeOwnerExposesTypeSafeCompatibilityMethods() {
		int[] stored = { ExpenseType.Kind.NONE.code() };
		HasExpenseType owner = new HasExpenseType() {
			@Override public int getExpenseType() { return stored[0]; }
			@Override public void setExpenseType(int expenseType) { stored[0] = expenseType; }
			@Override public int getEffectiveExpenseType() { return stored[0]; }
		};

		owner.setExpenseKind(ExpenseType.Kind.INDIRECT);
		assertEquals(ExpenseType.Kind.INDIRECT, owner.getExpenseKind());
		assertEquals(ExpenseType.Kind.INDIRECT.code(), owner.getExpenseType());
	}
}

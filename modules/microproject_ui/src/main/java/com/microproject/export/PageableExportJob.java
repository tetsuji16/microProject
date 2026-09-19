/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.export;

import java.awt.Component;
import com.microproject.job.Job;
import com.microproject.job.JobQueue;
import com.microproject.job.JobRunnable;
import com.microproject.print.GraphPageable;
import com.microproject.session.SessionFactory;
import com.microproject.strings.Messages;

final class PageableExportJob {
	private PageableExportJob() { }
	static void schedule(GraphPageable pageable, Component parentComponent, ExportTarget target, String jobName) {
		JobQueue jobQueue = SessionFactory.getInstance().getJobQueue();
		Job job = new Job(jobQueue, jobName, Messages.getString("LocalFileImporter.Exporting"), true, parentComponent);
		job.addRunnable(new JobRunnable(jobName, 1.0f) {
			@Override public Object run() throws Exception {
				if (target.format() == ExportFormat.PDF) new PageablePdfWriter().write(pageable, target.file(), progress -> setProgress((float) progress));
				else new PageablePngWriter().write(pageable, target.file(), progress -> setProgress((float) progress));
				setProgress(1.0f);
				return null;
			}
		});
		jobQueue.schedule(job);
	}
}

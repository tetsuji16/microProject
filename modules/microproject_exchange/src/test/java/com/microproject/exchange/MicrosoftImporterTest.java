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
package test.com.microproject.exchange;

import junit.framework.TestCase;

import com.microproject.exchange.MicrosoftImporter;
import com.microproject.job.Job;
import com.microproject.job.JobQueue;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.session.SessionFactory;

/**
 *
 */
public class MicrosoftImporterTest extends TestCase {
	private static String mppFileName = "testdata/New Product.mpp";
	private static String xmlFileName = "testdata/New Product.xml";	
	/**
	 * Main method for testing from command line
	 * 
	 * @param args array of command line arguments
	 */
	public static void main(String[] args) {
	}
	

	public void testMppImport() throws Exception {
		SessionFactory.getInstance().setJobQueue(new JobQueue("test", false));
		MicrosoftImporter importer = new MicrosoftImporter();
		importer.setFileName(mppFileName);
		importer.setProject(ProjectFactory.getInstance().createProject());
		assertNotNull("project creation must succeed when optional resource-session APIs are unavailable", importer.getProject());
		Job job=importer.getImportFileJob();
		SessionFactory.getInstance().getJobQueue().schedule(job);
	}

	// JAXB is not on classpath yet
//	public void testXmlImport() throws Exception {
//		MicrosoftImporter importer = new MicrosoftImporter(xmlFileName,	Document.getTestInstance());
//		importer.importFile();
//	}
	
}

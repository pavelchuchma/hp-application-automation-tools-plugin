/*
 * Certain versions of software and/or documents ("Material") accessible here may contain branding from
 * Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 * the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 * and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 * marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * (c) Copyright 2012-2019 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors ("Micro Focus") are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.results.parser;

import java.io.InputStream;
import java.util.List;

import com.microfocus.application.automation.tools.results.parser.mavensurefire.MavenSureFireReportParserImpl;
import com.microfocus.application.automation.tools.results.service.almentities.AlmTestSet;
import org.junit.Assert;


public class TestMavenSureFireReportParserImpl {

	//@Test
	public void testParseTestSets()throws Exception {
		InputStream in = TestMavenSureFireReportParserImpl.class.getResourceAsStream("MAVENTEST-com.demoapp.demo.AppTest.xml");
		MavenSureFireReportParserImpl parser = new MavenSureFireReportParserImpl();
		List<AlmTestSet> testsets = parser.parseTestSets(in, "JUnit", "Maven");
		assert (testsets.size () == 1);
		AlmTestSet testset = testsets.get(0);
		Assert.assertEquals("com.demoapp.demo.AppTest", testset.getName());
	}

}

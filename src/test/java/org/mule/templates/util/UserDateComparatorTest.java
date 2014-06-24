/**
 * Mule Anypoint Template
 * Copyright (c) MuleSoft, Inc.
 * All rights reserved.  http://www.mulesoft.com
 */

package org.mule.templates.util;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.mule.api.transformer.TransformerException;
import org.mule.templates.util.UserDateComparator;

public class UserDateComparatorTest {
	@Test(expected = IllegalArgumentException.class)
	public void nullUserA() {
		Map<String, String> userA = null;

		Map<String, String> userB = new HashMap<String, String>();
		userB.put("Id", "I000032300ESE");
		userB.put("LastModifiedDate", "2013-12-09T22:15:33.001Z");

		UserDateComparator.isAfter(userA, userB);
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullUserB() {
		Map<String, String> userA = new HashMap<String, String>();
		userA.put("Id", "I000032300ESE");
		userA.put("LastModifiedDate", "2013-12-09T22:15:33.001Z");

		Map<String, String> userB = null;

		UserDateComparator.isAfter(userA, userB);
	}

	@Test(expected = IllegalArgumentException.class)
	public void malFormedUserA() throws TransformerException {

		Map<String, String> userA = new HashMap<String, String>();
		userA.put("Id", "I0000323AE754F");

		Map<String, String> userB = new HashMap<String, String>();
		userB.put("Id", "I000032300ESE");
		userB.put("LastModifiedDate", "2013-12-09T22:15:33.001Z");

		UserDateComparator.isAfter(userA, userB);
	}

	@Test(expected = IllegalArgumentException.class)
	public void malFormedUserB() throws TransformerException {

		Map<String, String> userA = new HashMap<String, String>();
		userA.put("Id", "I0000323AE754F");
		userA.put("LastModifiedDate", "2013-12-09T22:15:33.001Z");

		Map<String, String> userB = new HashMap<String, String>();
		userB.put("Id", "I000032300ESE");

		UserDateComparator.isAfter(userA, userB);
	}

	@Test
	public void userAIsAfterUserB() throws TransformerException {

		Map<String, String> userA = new HashMap<String, String>();
		userA.put("Id", "I0000323AE754F");
		userA.put("LastModifiedDate", "2013-12-10T22:15:33.001Z");

		Map<String, String> userB = new HashMap<String, String>();
		userB.put("Id", "I000032300ESE");
		userB.put("LastModifiedDate", "2013-12-09T22:15:33.001Z");

		Assert.assertTrue("The user A should be after the user B", UserDateComparator.isAfter(userA, userB));
	}

	@Test
	public void userAIsNotAfterUserB() throws TransformerException {

		Map<String, String> userA = new HashMap<String, String>();
		userA.put("Id", "I0000323AE754F");
		userA.put("LastModifiedDate", "2013-12-08T22:15:33.001Z");

		Map<String, String> userB = new HashMap<String, String>();
		userB.put("Id", "I000032300ESE");
		userB.put("LastModifiedDate", "2013-12-09T22:15:33.001Z");

		Assert.assertFalse("The user A should not be after the user B", UserDateComparator.isAfter(userA, userB));
	}

	@Test
	public void userAIsTheSameThatUserB() throws TransformerException {

		Map<String, String> userA = new HashMap<String, String>();
		userA.put("Id", "I0000323AE754F");
		userA.put("LastModifiedDate", "2013-12-09T22:15:33.001Z");

		Map<String, String> userB = new HashMap<String, String>();
		userB.put("Id", "I000032300ESE");
		userB.put("LastModifiedDate", "2013-12-09T22:15:33.001Z");

		Assert.assertFalse("The user A should not be after the user B", UserDateComparator.isAfter(userA, userB));
	}

}

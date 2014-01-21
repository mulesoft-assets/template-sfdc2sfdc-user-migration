package org.mule.kicks.util;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.mule.api.transformer.TransformerException;

public class ContactDateComparatorTest {
	@Test(expected = IllegalArgumentException.class)
	public void nullConcactA() {
		Map<String, String> contactA = null;

		Map<String, String> contactB = new HashMap<String, String>();
		contactB.put("Id", "I000032300ESE");
		contactB.put("LastModifiedDate", "2013-12-09T22:15:33.001Z");

		ContactDateComparator.isAfter(contactA, contactB);
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullConcactB() {
		Map<String, String> contactA = new HashMap<String, String>();
		contactA.put("Id", "I000032300ESE");
		contactA.put("LastModifiedDate", "2013-12-09T22:15:33.001Z");

		Map<String, String> contactB = null;

		ContactDateComparator.isAfter(contactA, contactB);
	}

	@Test(expected = IllegalArgumentException.class)
	public void malFormedContactA() throws TransformerException {

		Map<String, String> contactA = new HashMap<String, String>();
		contactA.put("Id", "I0000323AE754F");

		Map<String, String> contactB = new HashMap<String, String>();
		contactB.put("Id", "I000032300ESE");
		contactB.put("LastModifiedDate", "2013-12-09T22:15:33.001Z");

		ContactDateComparator.isAfter(contactA, contactB);
	}

	@Test(expected = IllegalArgumentException.class)
	public void malFormedContactB() throws TransformerException {

		Map<String, String> contactA = new HashMap<String, String>();
		contactA.put("Id", "I0000323AE754F");
		contactA.put("LastModifiedDate", "2013-12-09T22:15:33.001Z");

		Map<String, String> contactB = new HashMap<String, String>();
		contactB.put("Id", "I000032300ESE");

		ContactDateComparator.isAfter(contactA, contactB);
	}

	@Test
	public void contactAIsAfterContactB() throws TransformerException {

		Map<String, String> contactA = new HashMap<String, String>();
		contactA.put("Id", "I0000323AE754F");
		contactA.put("LastModifiedDate", "2013-12-10T22:15:33.001Z");

		Map<String, String> contactB = new HashMap<String, String>();
		contactB.put("Id", "I000032300ESE");
		contactB.put("LastModifiedDate", "2013-12-09T22:15:33.001Z");

		Assert.assertTrue("The contact A should be after the contact B", ContactDateComparator.isAfter(contactA, contactB));
	}

	@Test
	public void contactAIsNotAfterContactB() throws TransformerException {

		Map<String, String> contactA = new HashMap<String, String>();
		contactA.put("Id", "I0000323AE754F");
		contactA.put("LastModifiedDate", "2013-12-08T22:15:33.001Z");

		Map<String, String> contactB = new HashMap<String, String>();
		contactB.put("Id", "I000032300ESE");
		contactB.put("LastModifiedDate", "2013-12-09T22:15:33.001Z");

		Assert.assertFalse("The contact A should not be after the contact B", ContactDateComparator.isAfter(contactA, contactB));
	}

	@Test
	public void contactAIsTheSameThatContactB() throws TransformerException {

		Map<String, String> contactA = new HashMap<String, String>();
		contactA.put("Id", "I0000323AE754F");
		contactA.put("LastModifiedDate", "2013-12-09T22:15:33.001Z");

		Map<String, String> contactB = new HashMap<String, String>();
		contactB.put("Id", "I000032300ESE");
		contactB.put("LastModifiedDate", "2013-12-09T22:15:33.001Z");

		Assert.assertFalse("The contact A should not be after the contact B", ContactDateComparator.isAfter(contactA, contactB));
	}

}

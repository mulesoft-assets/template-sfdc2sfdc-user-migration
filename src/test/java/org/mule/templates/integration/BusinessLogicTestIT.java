/**
 * Mule Anypoint Template
 * Copyright (c) MuleSoft, Inc.
 * All rights reserved.  http://www.mulesoft.com
 */

package org.mule.templates.integration;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.mule.templates.builders.SfdcObjectBuilder.aUser;

import java.io.FileInputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.modules.salesforce.bulk.EnrichedUpsertResult;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.templates.builders.SfdcObjectBuilder;

import com.mulesoft.module.batch.BatchTestHelper;

/**
 * The objective of this class is to validate the correct behavior of the Mule
 * Kick that make calls to external systems.
 * 
 * The test will invoke the batch process and afterwards check that the users
 * had been correctly created and that the ones that should be filtered are not
 * in the destination sand box.
 * 
 */
public class BusinessLogicTestIT extends AbstractTemplateTestCase {

	private static final String KICK_NAME = "sfdc2sfdc-users-migration";
	private static final String PATH_TO_TEST_PROPERTIES = "./src/test/resources/mule.test.properties";

	// TODO - Replace this ProfileId with one of your own org
	private static String DEFAULT_PROFILE_ID;

	protected static final int TIMEOUT_SEC = 60;

	private static SubflowInterceptingChainLifecycleWrapper retrieveUserFlow;
	private static List<Map<String, Object>> createdUsers = new ArrayList<Map<String, Object>>();

	private BatchTestHelper helper;

	@Before
	public void setUp() throws Exception {
		final Properties props = new Properties();
		try {
			props.load(new FileInputStream(PATH_TO_TEST_PROPERTIES));
		} catch (Exception e) {
			logger.error("Error occured while reading mule.test.properties", e);
		}
		DEFAULT_PROFILE_ID = props.getProperty("sfdc.a.profile.id");
		
		helper = new BatchTestHelper(muleContext);

		retrieveUserFlow = getSubFlow("retrieveUserFlow");
		retrieveUserFlow.initialise();

		createTestDataInSandBox();
	}

	@After
	public void tearDown() throws Exception {
		// failed = null;
		deleteTestDataFromSandBox();
	}

	@Test
	public void testMainFlow() throws Exception {

		runFlow("mainFlow");

		// Wait for the batch job executed by the poll flow to finish
		helper.awaitJobTermination(TIMEOUT_SEC * 1000, 500);
		helper.assertJobWasSuccessful();

		assertNull("The user should not have been sync",
				invokeRetrieveFlow(retrieveUserFlow, createdUsers.get(0)));

		Map<String, Object> payload = invokeRetrieveFlow(retrieveUserFlow,
				createdUsers.get(1));
		assertEquals("The user should have been sync",
				createdUsers.get(1).get("Username") + ".target", payload.get("Username"));
	}

	@SuppressWarnings("unchecked")
	private void createTestDataInSandBox() throws MuleException, Exception {
		SubflowInterceptingChainLifecycleWrapper createUserFlow = getSubFlow("createUserFlow");
		createUserFlow.initialise();

		SfdcObjectBuilder baseUser = aUser().with("TimeZoneSidKey", "GMT")
				.with("LocaleSidKey", "en_US")
				.with("EmailEncodingKey", "ISO-8859-1")
				.with("LanguageLocaleKey", "en_US")
				.with("ProfileId", DEFAULT_PROFILE_ID);

		// This user should not be sync
		createdUsers.add(baseUser.with("FirstName", "FirstName_0")
				.with("LastName", "LastName_0").with("Alias", "Alias_0")
				.with("IsActive", false)
				.with("Username", generateUnique("some.email.0@fakemail.com"))
				.with("Email", "some.email.0@fakemail.com").build());

		// This user should BE sync
		createdUsers.add(baseUser
				//
				.with("FirstName", "FirstName_1")
				.with("LastName", "LastName_1").with("Alias", "Alias_" + 1)
				.with("IsActive", true)
				.with("Username", generateUnique("some.email.1@fakemail.com"))
				.with("Email", "some.email.1@fakemail.com").build());

		MuleEvent event = createUserFlow.process(getTestEvent(createdUsers,
				MessageExchangePattern.REQUEST_RESPONSE));
		List<EnrichedUpsertResult> results = (List<org.mule.modules.salesforce.bulk.EnrichedUpsertResult>) event
				.getMessage().getPayload();
		for (int i = 0; i < results.size(); i++) {
			logger.info("upsert result: " + results.get(i));
			createdUsers.get(i).put("Id", results.get(i).getId());
		}
	}

	private void deleteTestDataFromSandBox() throws MuleException, Exception {
		// Delete the created users in A
		SubflowInterceptingChainLifecycleWrapper deleteUserFromAFlow = getSubFlow("deleteUserFromAFlow");
		deleteUserFromAFlow.initialise();

		List<String> idList = new ArrayList<String>();
		for (Map<String, Object> c : createdUsers) {
			idList.add((String) c.get("Id"));
		}
		deleteUserFromAFlow.process(getTestEvent(idList,
				MessageExchangePattern.REQUEST_RESPONSE));

		// Delete the created users in B
		SubflowInterceptingChainLifecycleWrapper deleteUserFromBFlow = getSubFlow("deleteUserFromBFlow");
		deleteUserFromBFlow.initialise();

		idList.clear();
		for (Map<String, Object> c : createdUsers) {
			Map<String, Object> user = invokeRetrieveFlow(retrieveUserFlow, c);
			if (user != null) {
				idList.add((String) user.get("Id"));
			}
		}
		deleteUserFromBFlow.process(getTestEvent(idList,
				MessageExchangePattern.REQUEST_RESPONSE));
	}

	private String generateUnique(String string) {
		return MessageFormat.format("{0}-{1}-{2}", KICK_NAME,
				System.currentTimeMillis(), string).replace(",", "");
	}

}

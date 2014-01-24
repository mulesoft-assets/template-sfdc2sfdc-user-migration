package org.mule.kicks.integration;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.notification.ServerNotification;
import org.mule.construct.Flow;
import org.mule.modules.salesforce.bulk.EnrichedUpsertResult;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.transport.NullPayload;

import com.mulesoft.module.batch.api.BatchJobInstance;
import com.mulesoft.module.batch.api.notification.BatchNotification;
import com.mulesoft.module.batch.api.notification.BatchNotificationListener;
import com.mulesoft.module.batch.engine.BatchJobInstanceAdapter;
import com.mulesoft.module.batch.engine.BatchJobInstanceStore;

/**
 * The objective of this class is to validate the correct behavior of the Mule
 * Kick that make calls to external systems.
 * 
 * The test will invoke the batch process and afterwards check that the users
 * had been correctly created and that the ones that should be filtered are not
 * in the destination sand box.
 * 
 */
public class BusinessLogicTestIT extends AbstractKickTestCase {
	private static final String DEFAULT_PROFILE_ID = "00e80000001CDZBAA4";

	protected static final int TIMEOUT = 60;

	private Prober prober;
	protected Boolean failed;
	protected BatchJobInstanceStore jobInstanceStore;

	private static SubflowInterceptingChainLifecycleWrapper checkUserflow;
	private static List<Map<String, Object>> createdUsers = new ArrayList<Map<String, Object>>();

	protected class BatchWaitListener implements BatchNotificationListener {

		public synchronized void onNotification(ServerNotification notification) {
			final int action = notification.getAction();

			if (action == BatchNotification.JOB_SUCCESSFUL || action == BatchNotification.JOB_STOPPED) {
				failed = false;
			} else if (action == BatchNotification.JOB_PROCESS_RECORDS_FAILED || action == BatchNotification.LOAD_PHASE_FAILED || action == BatchNotification.INPUT_PHASE_FAILED
					|| action == BatchNotification.ON_COMPLETE_FAILED) {

				failed = true;
			}
		}
	}

	@Before
	public void setUp() throws Exception {
		failed = null;
		jobInstanceStore = muleContext.getRegistry().lookupObject(BatchJobInstanceStore.class);
		muleContext.registerListener(new BatchWaitListener());

		checkUserflow = getSubFlow("retrieveUserFlow");
		checkUserflow.initialise();

		createTestDataInSandBox();
	}

	@After
	public void tearDown() throws Exception {
		failed = null;
		deleteTestDataFromSandBox();
	}

	@Test
	public void testMainFlow() throws Exception {
		Flow flow = getFlow("mainFlow");
		MuleEvent event = flow.process(getTestEvent("", MessageExchangePattern.REQUEST_RESPONSE));
		BatchJobInstance batchJobInstance = (BatchJobInstance) event.getMessage().getPayload();

		this.awaitJobTermination();

		assertTrue(this.wasJobSuccessful());

		batchJobInstance = this.getUpdatedInstance(batchJobInstance);

		assertNull("The user should not have been sync", invokeRetrieveUserFlow(checkUserflow, createdUsers.get(0)));

		Map<String, String> payload = invokeRetrieveUserFlow(checkUserflow, createdUsers.get(1));
		assertEquals("The user should have been sync", createdUsers.get(1).get("Email"), payload.get("Email"));
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> invokeRetrieveUserFlow(SubflowInterceptingChainLifecycleWrapper flow, Map<String, Object> user) throws Exception {
		Map<String, String> userMap = new HashMap<String, String>();

		userMap.put("Email", (String) user.get("Email"));
		userMap.put("FirstName", (String) user.get("FirstName"));
		userMap.put("LastName", (String) user.get("LastName"));

		MuleEvent event = flow.process(getTestEvent(userMap, MessageExchangePattern.REQUEST_RESPONSE));
		Object payload = event.getMessage().getPayload();
		if (payload instanceof NullPayload) {
			return null;
		} else {
			return (Map<String, String>) payload;
		}
	}

	protected void awaitJobTermination() throws Exception {
		this.awaitJobTermination(TIMEOUT);
	}

	protected void awaitJobTermination(long timeoutSecs) throws Exception {
		this.prober = new PollingProber(timeoutSecs * 1000, 500);
		this.prober.check(new Probe() {

			@Override
			public boolean isSatisfied() {
				return failed != null;
			}

			@Override
			public String describeFailure() {
				return "batch job timed out";
			}
		});
	}

	protected boolean wasJobSuccessful() {
		return this.failed != null ? !this.failed : false;
	}

	protected BatchJobInstanceAdapter getUpdatedInstance(BatchJobInstance jobInstance) {
		return this.jobInstanceStore.getJobInstance(jobInstance.getOwnerJobName(), jobInstance.getId());
	}

	@SuppressWarnings("unchecked")
	private void createTestDataInSandBox() throws MuleException, Exception {
		SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("createUserFlow");
		flow.initialise();

		// This user should not be sync
		Map<String, Object> user = createUser("A", 0);
		user.put("IsActive", false);
		createdUsers.add(user);

		// This user should BE sync
		user = createUser("A", 1);
		createdUsers.add(user);
		
		MuleEvent event = flow.process(getTestEvent(createdUsers, MessageExchangePattern.REQUEST_RESPONSE));
		List<EnrichedUpsertResult> results = (List<org.mule.modules.salesforce.bulk.EnrichedUpsertResult>) event.getMessage().getPayload();
		for (int i = 0; i < results.size(); i++) {
			createdUsers.get(i).put("Id", results.get(i).getId());
		}
	}

	private void deleteTestDataFromSandBox() throws MuleException, Exception {
		// Delete the created users in A
		SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("deleteUserFromAFlow");
		flow.initialise();

		List<String> idList = new ArrayList<String>();
		for (Map<String, Object> c : createdUsers) {
			idList.add((String) c.get("Id"));
		}
		flow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));

		// Delete the created users in B
		flow = getSubFlow("deleteUserFromBFlow");
		flow.initialise();
		idList.clear();
		for (Map<String, Object> c : createdUsers) {
			Map<String, String> user = invokeRetrieveUserFlow(checkUserflow, c);
			if (user != null) {
				idList.add(user.get("Id"));
			}
		}
		flow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));
	}

	private Map<String, Object> createUser(String orgId, int sequence) {
		Map<String, Object> user = new HashMap<String, Object>();

		user.put("FirstName", "FirstName_" + sequence);
		user.put("LastName", "LastName_" + sequence);
		user.put("Alias", "Alias_" + sequence);
		user.put("TimeZoneSidKey", "GMT");
		user.put("LocaleSidKey", "en_US");
		user.put("EmailEncodingKey", "ISO-8859-1");
		
		// TODO - Replace this ProfileId with one of your own org
		user.put("ProfileId", DEFAULT_PROFILE_ID);
		
		user.put("LanguageLocaleKey", "en_US");
		user.put("IsActive", true);
		user.put("Username", "some.email." + sequence + "@fakemail.com");
		user.put("Email", "some.email." + sequence + "@fakemail.com");

		return user;
	}
}

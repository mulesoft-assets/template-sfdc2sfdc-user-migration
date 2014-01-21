package org.mule.kicks.integration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.notification.ServerNotification;
import org.mule.construct.Flow;
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
import com.sforce.soap.partner.SaveResult;

/**
 * The objective of this class is to validate the correct behavior of the Mule
 * Kick that make calls to external systems.
 * 
 * The test will invoke the batch process and afterwards check that the contacts
 * had been correctly created and that the ones that should be filtered are not
 * in the destination sand box.
 * 
 * @author damiansima
 */
public class BusinessLogicTestIT extends AbstractKickTestCase {
	protected static final int TIMEOUT = 60;

	private Prober prober;
	protected Boolean failed;
	protected BatchJobInstanceStore jobInstanceStore;

	private static SubflowInterceptingChainLifecycleWrapper checkContactflow;
	private static List<Map<String, String>> createdContacts = new ArrayList<Map<String, String>>();

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

		checkContactflow = getSubFlow("retrieveContactFlow");
		checkContactflow.initialise();

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

		Assert.assertTrue(this.wasJobSuccessful());

		batchJobInstance = this.getUpdatedInstance(batchJobInstance);

		Assert.assertEquals("The contact should not have been sync", null, invokeRetrieveContactFlow(checkContactflow, createdContacts.get(0)));

		Assert.assertEquals("The contact should not have been sync", null, invokeRetrieveContactFlow(checkContactflow, createdContacts.get(1)));

		Map<String, String> payload = invokeRetrieveContactFlow(checkContactflow, createdContacts.get(2));
		Assert.assertEquals("The contact should have been sync", createdContacts.get(2).get("Email"), payload.get("Email"));
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> invokeRetrieveContactFlow(SubflowInterceptingChainLifecycleWrapper flow, Map<String, String> contact) throws Exception {
		Map<String, String> contactMap = new HashMap<String, String>();

		contactMap.put("Email", contact.get("Email"));
		contactMap.put("FirstName", contact.get("FirstName"));
		contactMap.put("LastName", contact.get("LastName"));

		MuleEvent event = flow.process(getTestEvent(contactMap, MessageExchangePattern.REQUEST_RESPONSE));
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
		SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("createContactFlow");
		flow.initialise();

		// This contact should not be sync
		Map<String, String> contact = createContact("A", 0);
		contact.put("Email", "");
		createdContacts.add(contact);

		// This contact should not be sync
		contact = createContact("A", 1);
		contact.put("MailingCountry", "ARG");
		createdContacts.add(contact);

		// This contact should BE sync
		contact = createContact("A", 2);
		createdContacts.add(contact);

		MuleEvent event = flow.process(getTestEvent(createdContacts, MessageExchangePattern.REQUEST_RESPONSE));
		List<SaveResult> results = (List<SaveResult>) event.getMessage().getPayload();
		for (int i = 0; i < results.size(); i++) {
			createdContacts.get(i).put("Id", results.get(i).getId());
		}
	}

	private void deleteTestDataFromSandBox() throws MuleException, Exception {
		// Delete the created contacts in A
		SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("deleteContactFromAFlow");
		flow.initialise();

		List<String> idList = new ArrayList<String>();
		for (Map<String, String> c : createdContacts) {
			idList.add(c.get("Id"));
		}
		flow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));

		// Delete the created contacts in B
		flow = getSubFlow("deleteContactFromBFlow");
		flow.initialise();
		idList.clear();
		for (Map<String, String> c : createdContacts) {
			Map<String, String> contact = invokeRetrieveContactFlow(checkContactflow, c);
			if (contact != null) {
				idList.add(contact.get("Id"));
			}
		}
		flow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));
	}

	private Map<String, String> createContact(String orgId, int sequence) {
		Map<String, String> contact = new HashMap<String, String>();

		contact.put("FirstName", "FirstName_" + sequence);
		contact.put("LastName", "LastName_" + sequence);
		contact.put("Email", "some.email." + sequence + "@fakemail.com");
		contact.put("Description", "Some fake description");
		contact.put("MailingCity", "Denver");
		contact.put("MailingCountry", "USA");
		contact.put("MobilePhone", "123456789");
		contact.put("Department", "department_" + sequence + "_" + orgId);
		contact.put("Phone", "123456789");
		contact.put("Title", "Dr");

		return contact;
	}
}

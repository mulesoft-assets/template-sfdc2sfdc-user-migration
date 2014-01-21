package org.mule.kicks.util;

import java.util.Map;

import org.apache.commons.lang.Validate;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * The function of this class is to establish a relation happens before between
 * two maps representing SFDC contacts.
 * 
 * It's assumed that these maps are well formed maps from SFDC thus they both
 * contain an entry with the expected key. Never the less validations are being
 * done.
 * 
 * @author damiansima
 */
public class ContactDateComparator {
	private static final String LAST_MODIFIED_DATE = "LastModifiedDate";

	/**
	 * Validate which contact has the latest last modification date.
	 * 
	 * @param contactA
	 *            SFDC contact map
	 * @param contactB
	 *            SFDC contact map
	 * @return true if the last modified date from contactA is after the one
	 *         from contact B
	 */
	public static boolean isAfter(Map<String, String> contactA, Map<String, String> contactB) {
		Validate.notNull(contactA, "The contact A should not be null");
		Validate.notNull(contactB, "The contact B should not be null");

		Validate.isTrue(contactA.containsKey(LAST_MODIFIED_DATE), "The contact A map should containt the key " + LAST_MODIFIED_DATE);
		Validate.isTrue(contactB.containsKey(LAST_MODIFIED_DATE), "The contact B map should containt the key " + LAST_MODIFIED_DATE);

		DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		DateTime lastModifiedDateOfA = formatter.parseDateTime(contactA.get(LAST_MODIFIED_DATE));
		DateTime lastModifiedDateOfB = formatter.parseDateTime(contactB.get(LAST_MODIFIED_DATE));

		return lastModifiedDateOfA.isAfter(lastModifiedDateOfB);
	}
}

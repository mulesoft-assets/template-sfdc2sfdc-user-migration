package org.mule.kicks.builders;

import java.util.HashMap;
import java.util.Map;

public class UserBuilder {

	private Map<String, Object> fields;

	public UserBuilder() {
		this.fields = new HashMap<String, Object>();
	}
	
	public static UserBuilder aUser() {
		return new UserBuilder();
	}
	
	public UserBuilder with(String field, Object value) {
		UserBuilder copy = new UserBuilder();
		copy.fields.putAll(this.fields);
		copy.fields.put(field, value);
		return copy;
	}
	
	public Map<String, Object> build() {
		return fields;
	}
}

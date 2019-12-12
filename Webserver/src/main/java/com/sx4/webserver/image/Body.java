package com.sx4.webserver.image;

import java.util.List;
import java.util.HashMap;

public class Body extends HashMap<String, Object> {

	private static final long serialVersionUID = 1L;
	
	public Body() {}
	
	public Body(String key, Object value) {
		super.put(key, value);
	}
	
	public Body put(String key, Object value) {
		super.put(key, value);
		
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public <Type> Type get(Object key, Type defaultValue) {
		Object value = super.get(key);
		
		return value == null ? defaultValue : (Type) value;
	}
	
	public <Type> Type get(Object key, Class<Type> clazz) {
		return clazz.cast(super.get(key));
	}
	
	public String getString(Object key) {
		return (String) super.get(key);
	}
	
	public Long getLong(Object key) {
		return (Long) super.get(key);
	}
	
	public Integer getInteger(Object key) {
		return (Integer) super.get(key);
	}
	
	public Boolean getBoolean(Object key) {
		return (Boolean) super.get(key);
	}
	
	@SuppressWarnings("unchecked")
	public <Type> List<Type> getList(Object key, Class<Type> clazz) {
		return (List<Type>) super.get(key);
	}
	
	@SuppressWarnings("unchecked")
	public <Type> List<Type> getList(Object key, Class<Type> clazz, List<Type> defaultValue) {
		Object value = super.get(key);
		
		return value == null ? defaultValue : (List<Type>) value;
	}

}

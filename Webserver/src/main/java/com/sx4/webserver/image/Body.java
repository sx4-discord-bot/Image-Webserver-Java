package com.sx4.webserver.image;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Body implements Map<String, Object> {
	
	private final Map<String, Object> map;
	
	public Body() {
		this.map = new HashMap<>();
	}
	
	public Body(Map<String, Object> map) {
		this.map = Map.copyOf(map);
	}
	
	public Body(String key, Object value) {
		this.map = new HashMap<>();
		
		this.map.put(key, value);
	}
	
	public Body put(String key, Object value) {
		this.map.put(key, value);
		
		return this;
	}
	
	public Body putAll(Body body) {
		this.map.putAll(body);
		
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public <Type> Type get(String key, Type defaultValue) {
		Object value = this.map.get(key);
		
		return value == null ? defaultValue : (Type) value;
	}
	
	public <Type> Type get(String key, Class<Type> clazz) {
		return clazz.cast(this.map.get(key));
	}
	
	public String getString(String key) {
		return (String) this.map.get(key);
	}
	
	public Long getLong(String key) {
		return (Long) this.map.get(key);
	}
	
	public Integer getInteger(String key) {
		return (Integer) this.map.get(key);
	}
	
	public Boolean getBoolean(String key) {
		return (Boolean) this.map.get(key);
	}
	
	@SuppressWarnings("unchecked")
	public <Type> List<Type> getList(String key, Class<Type> clazz) {
		return (List<Type>) this.map.get(key);
	}
	
	@SuppressWarnings("unchecked")
	public <Type> List<Type> getList(String key, Class<Type> clazz, List<Type> defaultValue) {
		Object value = this.map.get(key);
		
		return value == null ? defaultValue : (List<Type>) value;
	}

	public int size() {
		return this.map.size();
	}

	public boolean isEmpty() {
		return this.map.isEmpty();
	}

	public boolean containsKey(Object key) {
		return this.map.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return this.map.containsValue(value);
	}

	public Object get(Object key) {
		return this.map.get(key);
	}

	public Object remove(Object key) {
		return this.map.remove(key);
	}

	public void putAll(Map<? extends String, ? extends Object> m) {
		this.map.putAll(m);
	}

	public void clear() {
		this.map.clear();
	}

	public Set<String> keySet() {
		return this.map.keySet();
	}

	public Collection<Object> values() {
		return this.map.values();
	}

	public Set<Entry<String, Object>> entrySet() {
		return this.map.entrySet();
	}

}

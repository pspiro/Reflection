package org.json.simple;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** JsonObject that maintains the insertion order. Use sparingly, it hasn't been well-tested */
public class OrderedJson extends JsonObject {

	private static final long serialVersionUID = 1L;

	// LinkedList to keep track of the keys in insertion order
	private final LinkedList<String> m_list;

	public OrderedJson() {
		m_list = new LinkedList<>();
	}

	public OrderedJson(Map<String, ? extends Object> base) {
		m_list = new LinkedList<>();
		putAll(base);  // This will ensure keys are added to keyOrder
	}

	@Override public Object put(String key, Object value) {
		if (!containsKey(key)) {
			m_list.add(key);  // Add new key to keyOrder
		}
		return super.put(key, value);  // Delegate to base class
	}

	@Override public Object remove(Object key) {
		m_list.remove(key);   // Remove key from keyOrder
		return super.remove(key);      // Delegate to base class
	}

	@Override public void clear() {
		super.clear();   // Clear the HashMap
		m_list.clear();       // Clear the keyOrder list
	}

	@Override public void putAll(Map<? extends String, ? extends Object> m) {
		for (Map.Entry<? extends String, ? extends Object> entry : m.entrySet()) {
			put(entry.getKey(), entry.getValue());  // Use overridden put()
		}
	}

	@Override public Set<String> keySet() {
		return new AbstractSet<String>() {
			@Override public Iterator<String> iterator() {
				return m_list.iterator();  // Return iterator over keys in order
			}

			@Override public int size() {
				return m_list.size();
			}

			@Override public boolean contains(Object o) {
				return m_list.contains(o);
			}

			@Override public void clear() {
				OrderedJson.this.clear();
			}

			@Override public boolean remove(Object o) {
				throw new RuntimeException( "not implemented");
			}
		};
	}

	@Override public Set<Map.Entry<String, Object>> entrySet() {
		return new AbstractSet<Map.Entry<String, Object>>() {
			@Override public Iterator<Map.Entry<String, Object>> iterator() {
				return new Iterator<Map.Entry<String, Object>>() {
					private final Iterator<String> keyIterator = m_list.iterator();
					private String currentKey = null;

					@Override  public boolean hasNext() {
						return keyIterator.hasNext();
					}

					@Override  public Map.Entry<String, Object> next() {
						currentKey = keyIterator.next();
						return new MapEntry(currentKey);
					}

					@Override  public void remove() {
						if (currentKey == null) {
							throw new IllegalStateException("next() has not been called yet");
						}
						keyIterator.remove();      // Remove from keyOrder
						OrderedJson.this.remove(currentKey);  // Remove from HashMap
						currentKey = null;
					}
				};
			}

			@Override public int size() {
				return m_list.size();
			}

			@Override public void clear() {
				OrderedJson.this.clear();
			}
		};
	}

	// Inner class to represent a map entry
	private class MapEntry implements Map.Entry<String, Object> {
		private final String key;

		MapEntry(String key) {
			this.key = key;
		}

		@Override public String getKey() {
			return key;
		}

		@Override public Object getValue() {
			return OrderedJson.this.get(key);
		}

		@Override public Object setValue(Object value) {
			return OrderedJson.this.put(key, value);
		}

		@Override public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof Map.Entry))
				return false;
			Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
			return Objects.equals(key, e.getKey()) && Objects.equals(getValue(), e.getValue());
		}

		@Override public int hashCode() {
			Object v = getValue();
			return (key == null ? 0 : key.hashCode()) ^ (v == null ? 0 : v.hashCode());
		}
	}
}

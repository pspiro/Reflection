package testcase;

import static org.junit.Assert.assertNotEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.simple.JsonObject;
import org.json.simple.OrderedJson;

import junit.framework.TestCase;

public class TestJson extends TestCase {
	public void test() throws Exception {
		String text = """
			{
			"a": 1,
			"b": 1,
			"c": 1,
			"3": 1,
			"2": 1,
			"1": 1,
			}""";
		
		String solution = """
				{"a":1,"b":1,"c":1,"3":1,"2":1,"1":1}""";
		
		// regular json parse fails
		assertNotEquals( solution, JsonObject.parse( text).toString() );
		
		// ordered json parse succeeds
		assertEquals( solution, JsonObject.parseOrdered( text).toString() );

		// order is maintained even when inside a non-ordered object
		JsonObject o = new JsonObject();
		o.put( "a", JsonObject.parseOrdered( text) );
		o.put( "b", JsonObject.parseOrdered( text) );
		
		String sol2 = """
				{"a":{"a":1,"b":1,"c":1,"3":1,"2":1,"1":1},"b":{"a":1,"b":1,"c":1,"3":1,"2":1,"1":1}}""";
		assertEquals( sol2, o.toString() );
		
	}

    private OrderedJson orderedJson = new OrderedJson();;

    public void testInsertionOrder() {
        orderedJson.put("apple", 1);
        orderedJson.put("banana", 2);
        orderedJson.put("cherry", 3);

        List<String> expectedKeys = Arrays.asList("apple", "banana", "cherry");
        List<String> actualKeys = new ArrayList<>(orderedJson.keySet());

        assertEquals("Keys should be in insertion order", expectedKeys, actualKeys);
    }

    
    public void testOverwriteKey() {
        orderedJson.put("apple", 1);
        orderedJson.put("banana", 2);
        orderedJson.put("apple", 3);  // Overwrite value for "apple"

        assertEquals("Value for 'apple' should be updated", 3, orderedJson.get("apple"));
        List<String> expectedKeys = Arrays.asList("apple", "banana");
        List<String> actualKeys = new ArrayList<>(orderedJson.keySet());

        assertEquals("Keys should not have duplicates", expectedKeys, actualKeys);
    }

    
    public void testRemoveKey() {
        orderedJson.put("apple", 1);
        orderedJson.put("banana", 2);
        orderedJson.put("cherry", 3);

        orderedJson.remove("banana");

        assertFalse("Map should not contain 'banana'", orderedJson.containsKey("banana"));

        List<String> expectedKeys = Arrays.asList("apple", "cherry");
        List<String> actualKeys = new ArrayList<>(orderedJson.keySet());

        assertEquals("Keys should reflect removal", expectedKeys, actualKeys);
    }

    
    public void testClear() {
        orderedJson.put("apple", 1);
        orderedJson.put("banana", 2);

        orderedJson.clear();

        assertTrue("Map should be empty after clear", orderedJson.isEmpty());
        assertTrue("keyOrder should be empty after clear", orderedJson.keySet().isEmpty());
    }

    
    public void testPutAll() {
        Map<String, Object> map = new HashMap<>();
        map.put("date", 4);
        map.put("elderberry", 5);

        orderedJson.put("apple", 1);
        orderedJson.putAll(map);

        List<String> expectedKeys = Arrays.asList("apple", "date", "elderberry");
        List<String> actualKeys = new ArrayList<>(orderedJson.keySet());

        assertEquals("Keys should include those from putAll in order", expectedKeys, actualKeys);
    }

    
    public void testKeySetIteration() {
        orderedJson.put("apple", 1);
        orderedJson.put("banana", 2);
        orderedJson.put("cherry", 3);

        Iterator<String> keyIterator = orderedJson.keySet().iterator();
        List<String> iteratedKeys = new ArrayList<>();

        while (keyIterator.hasNext()) {
            iteratedKeys.add(keyIterator.next());
        }

        List<String> expectedKeys = Arrays.asList("apple", "banana", "cherry");

        assertEquals("Iterated keys should be in insertion order", expectedKeys, iteratedKeys);
    }

    
    public void testEntrySetIteration() {
        orderedJson.put("apple", 1);
        orderedJson.put("banana", 2);
        orderedJson.put("cherry", 3);

        Iterator<Map.Entry<String, Object>> entryIterator = orderedJson.entrySet().iterator();
        List<String> iteratedKeys = new ArrayList<>();
        List<Object> iteratedValues = new ArrayList<>();

        while (entryIterator.hasNext()) {
            Map.Entry<String, Object> entry = entryIterator.next();
            iteratedKeys.add(entry.getKey());
            iteratedValues.add(entry.getValue());
        }

        List<String> expectedKeys = Arrays.asList("apple", "banana", "cherry");
        List<Object> expectedValues = Arrays.asList(1, 2, 3);

        assertEquals("Iterated keys should be in insertion order", expectedKeys, iteratedKeys);
        assertEquals("Iterated values should match inserted values", expectedValues, iteratedValues);
    }

    
    public void testIteratorRemove() {
        orderedJson.put("apple", 1);
        orderedJson.put("banana", 2);
        orderedJson.put("cherry", 3);

        Iterator<String> keyIterator = orderedJson.keySet().iterator();

        while (keyIterator.hasNext()) {
            String key = keyIterator.next();
            if ("banana".equals(key)) {
                keyIterator.remove();  // Should remove "banana"
            }
        }

//        assertFalse("Map should not contain 'banana'", orderedJson.containsKey("banana"));

        List<String> expectedKeys = Arrays.asList("apple", "cherry");
        List<String> actualKeys = new ArrayList<>(orderedJson.keySet());

        assertEquals("Keys should reflect removal via iterator", expectedKeys, actualKeys);
    }

    
    public void testInheritedMethods() {
        orderedJson.put("apple", "Red");
        orderedJson.put("banana", "Yellow");
        orderedJson.put("cherry", "Red");

        // Test getString from JsonObject
        assertEquals("Should retrieve value using getString", "Red", orderedJson.getString("apple"));

        // Test getLowerString from JsonObject
        assertEquals("Should retrieve lowercased value using getLowerString", "yellow", orderedJson.getLowerString("banana"));

        // Test toJSONString from JsonObject
        String jsonString = orderedJson.toJSONString();
        String expectedJsonString = "{\"apple\":\"Red\",\"banana\":\"Yellow\",\"cherry\":\"Red\"}";

        assertEquals("JSON string should represent the map", expectedJsonString, jsonString);
    }

    
    public void testSynchronizationBetweenMapAndKeyOrder() {
        orderedJson.put("apple", 1);
        orderedJson.put("banana", 2);

        orderedJson.remove("apple");

        assertFalse("keyOrder should not contain 'apple'", orderedJson.keySet().contains("apple"));
        assertFalse("Map should not contain 'apple'", orderedJson.containsKey("apple"));

        orderedJson.put("cherry", 3);

        List<String> expectedKeys = Arrays.asList("banana", "cherry");
        List<String> actualKeys = new ArrayList<>(orderedJson.keySet());

        assertEquals("keyOrder should be synchronized with map operations", expectedKeys, actualKeys);
    }

    
    public void testPutNullValue() {
        orderedJson.put("apple", null);
        orderedJson.put("banana", 2);

        assertTrue("Map should contain 'apple' key", orderedJson.containsKey("apple"));
        assertNull("Value for 'apple' should be null", orderedJson.get("apple"));

        List<String> expectedKeys = Arrays.asList("apple", "banana");
        List<String> actualKeys = new ArrayList<>(orderedJson.keySet());

        assertEquals("keyOrder should include keys with null values", expectedKeys, actualKeys);
    }

    
    public void testContainsMethods() {
        orderedJson.put("apple", 1);
        orderedJson.put("banana", 2);

        assertTrue("Map should contain key 'apple'", orderedJson.containsKey("apple"));
        assertTrue("Map should contain value 2", orderedJson.containsValue(2));

        assertFalse("Map should not contain key 'cherry'", orderedJson.containsKey("cherry"));
        assertFalse("Map should not contain value 3", orderedJson.containsValue(3));
    }

    
    public void testSizeAndIsEmpty() {
        assertTrue("New map should be empty", orderedJson.isEmpty());
        assertEquals("Size should be 0", 0, orderedJson.size());

        orderedJson.put("apple", 1);

        assertFalse("Map should not be empty after insertion", orderedJson.isEmpty());
        assertEquals("Size should be 1", 1, orderedJson.size());
    }

    
    public void testToString() {
        orderedJson.put("apple", 1);
        orderedJson.put("banana", 2);

        String expectedString = "{\"apple\":1,\"banana\":2}";

        assertEquals("toString should output JSON representation", expectedString, orderedJson.toString());
    }

    
    public void testSerialization() throws IOException, ClassNotFoundException {
        orderedJson.put("apple", 1);
        orderedJson.put("banana", 2);

        // Serialize to byte array
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream outStream = new ObjectOutputStream(byteOut);
        outStream.writeObject(orderedJson);

        // Deserialize from byte array
        ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
        ObjectInputStream inStream = new ObjectInputStream(byteIn);
        OrderedJson deserializedJson = (OrderedJson) inStream.readObject();

        assertEquals("Deserialized map should be equal to original", orderedJson, deserializedJson);

        List<String> expectedKeys = Arrays.asList("apple", "banana");
        List<String> actualKeys = new ArrayList<>(deserializedJson.keySet());

        assertEquals("Deserialized map should maintain insertion order", expectedKeys, actualKeys);
    }

    
    public void testModifyValue() {
        orderedJson.put("apple", 1);
        orderedJson.put("banana", 2);

        // Modify value for existing key
        orderedJson.put("apple", 10);

        assertEquals("Value for 'apple' should be updated", 10, orderedJson.get("apple"));
        List<String> expectedKeys = Arrays.asList("apple", "banana");
        List<String> actualKeys = new ArrayList<>(orderedJson.keySet());

        assertEquals("Keys should remain in the same order after modification", expectedKeys, actualKeys);
    }

    
    public void testEntrySetRemove() {
        orderedJson.put("apple", 1);
        orderedJson.put("banana", 2);

        Iterator<Map.Entry<String, Object>> entryIterator = orderedJson.entrySet().iterator();

        while (entryIterator.hasNext()) {
            Map.Entry<String, Object> entry = entryIterator.next();
            if ("apple".equals(entry.getKey())) {
                entryIterator.remove();  // Should remove "apple"
            }
        }

        assertFalse("Map should not contain 'apple' after removal via entrySet", orderedJson.containsKey("apple"));
        assertFalse("keyOrder should not contain 'apple' after removal via entrySet", orderedJson.keySet().contains("apple"));

        List<String> expectedKeys = Arrays.asList("banana");
        List<String> actualKeys = new ArrayList<>(orderedJson.keySet());

        assertEquals("Keys should reflect removal via entrySet", expectedKeys, actualKeys);
    }
}

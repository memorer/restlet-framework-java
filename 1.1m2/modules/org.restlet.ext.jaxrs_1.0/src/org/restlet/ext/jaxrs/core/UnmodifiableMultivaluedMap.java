package org.restlet.ext.jaxrs.core;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

import org.restlet.data.Form;
import org.restlet.data.Parameter;

/**
 * An unmodifiable {@link MultivaluedMap}.
 * 
 * @author Stephan Koops
 * 
 * @param <K>
 * @param <V>
 */
public class UnmodifiableMultivaluedMap<K, V> implements MultivaluedMap<K, V> {

    /**
     * Creates an UnmodifiableMultivaluedMap&lt;String, String;&gt; from the
     * given Form.
     * 
     * @param form
     * @param caseSensitive
     * @return
     */
    public static UnmodifiableMultivaluedMap<String, String> getFromForm(
            Form form, boolean caseSensitive) {
        return new UnmodifiableMultivaluedMap<String, String>(copyForm(form,
                !caseSensitive), caseSensitive);
    }

    private MultivaluedMapImpl<K, V> map;

    private boolean caseInsensitive;

    /**
     * Creates a new unmodifiable {@link MultivaluedMap}.
     * 
     * @param mmap
     * @param caseSensitive
     */
    private UnmodifiableMultivaluedMap(MultivaluedMapImpl<K, V> mmap,
            boolean caseSensitive) {
        this.map = mmap;
        this.caseInsensitive = !caseSensitive;
    }

    @Deprecated
    @SuppressWarnings("unused")
    public void add(K key, V value) {
        throw throwUnmodifiable();
    }

    @Deprecated
    @SuppressWarnings("unused")
    public void clear() throws UnsupportedOperationException {
        throw throwUnmodifiable();
    }

    public boolean containsKey(Object key) {
        if (caseInsensitive && key != null)
            map.containsKey(caseInsensitive(key.toString()));
        return map.containsKey(key);
    }

    private Object caseInsensitive(Object key) {
        if (caseInsensitive && key != null)
            key = key.toString().toLowerCase();
        return key;
    }

    public boolean containsValue(Object value) {
        if (value instanceof List) {
            return map.containsValue(value);
        }
        for (List<V> vList : map.values())
            if (vList.contains(value))
                return true;
        return false;
    }

    public Set<java.util.Map.Entry<K, List<V>>> entrySet() {
        return Collections.unmodifiableSet(map.entrySet());
    }

    public List<V> get(Object key) {
        return Collections.unmodifiableList(map.get(caseInsensitive(key)));
    }

    @SuppressWarnings("unchecked")
    public V getFirst(K key) {
        if (caseInsensitive && key instanceof String)
            key = (K) key.toString().toLowerCase();
        return map.getFirst(key);
    }

    /**
     * Returns the last element for the given key.
     * 
     * @param key
     * @return Returns the last element for the given key.
     */
    @SuppressWarnings("unchecked")
    public V getLast(K key) {
        if (caseInsensitive && key instanceof String)
            key = (K) key.toString().toLowerCase();
        return map.getLast(key);
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public Set<K> keySet() {
        return Collections.unmodifiableSet(map.keySet());
    }

    @Deprecated
    @SuppressWarnings("unused")
    public List<V> put(K key, List<V> value)
            throws UnsupportedOperationException {
        throw throwUnmodifiable();
    }

    @Deprecated
    @SuppressWarnings("unused")
    public void putAll(Map<? extends K, ? extends List<V>> t)
            throws UnsupportedOperationException {
        throw throwUnmodifiable();
    }

    @Deprecated
    @SuppressWarnings("unused")
    public void putSingle(K key, V value) throws UnsupportedOperationException {
        throw throwUnmodifiable();
    }

    @Deprecated
    @SuppressWarnings("unused")
    public List<V> remove(Object key) throws UnsupportedOperationException {
        throw throwUnmodifiable();
    }

    public int size() {
        int size = 0;
        for (List<V> l : map.values())
            size += l.size();
        return size;
    }

    /**
     * @throws UnsupportedOperationException
     */
    private UnsupportedOperationException throwUnmodifiable()
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException(
                "The HTTP headers are immutable");
    }

    /**
     * Returns an UnmodifiableMultivaluedMap, that contains the content of the
     * given {@link MultivaluedMap}. 
     * 
     * @param mmap
     * @param caseSensitive
     * @return
     */
    public static UnmodifiableMultivaluedMap<String, String> get(
            MultivaluedMap<String, String> mmap, boolean caseSensitive) {
        if (mmap instanceof UnmodifiableMultivaluedMap)
            return (UnmodifiableMultivaluedMap<String, String>) mmap;
        if (mmap instanceof MultivaluedMapImpl)
            return new UnmodifiableMultivaluedMap<String, String>(
                    (MultivaluedMapImpl<String, String>) mmap, caseSensitive);
        return new UnmodifiableMultivaluedMap<String, String>(
                new MultivaluedMapImpl<String, String>(mmap), caseSensitive);
    }

    /**
     * Returns an UnmodifiableMultivaluedMap, that contains the content of the
     * given {@link MultivaluedMap}. 
     * 
     * @param mmap
     * @return
     */
    public static UnmodifiableMultivaluedMap<String, String> get(
            MultivaluedMap<String, String> mmap) {
        return get(mmap, true);
    }
    
    public Collection<List<V>> values() {
        return Collections.unmodifiableCollection(map.values());
    }

    /**
     * Creates a MultiValuedMap of unmodifiable Lists.
     */
    private static MultivaluedMapImpl<String, String> copyForm(Form form,
            boolean caseInsensitive) {
        MultivaluedMapImpl<String, String> mmap = new MultivaluedMapImpl<String, String>();
        for (Parameter param : form) {
            String key = caseInsensitive ? param.getName().toLowerCase()
                    : param.getName();
            mmap.add(key, param.getValue());
        }
        for (Map.Entry<String, List<String>> entry : mmap.entrySet()) {
            List<String> unmodifiable = Collections.unmodifiableList(entry
                    .getValue());
            mmap.put(entry.getKey(), unmodifiable);
        }
        return mmap;
    }
}
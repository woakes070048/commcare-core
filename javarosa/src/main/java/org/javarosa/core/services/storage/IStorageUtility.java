package org.javarosa.core.services.storage;

import org.javarosa.core.util.externalizable.Externalizable;

import java.util.Vector;

/**
 * IStorageUtility
 *
 * Implementations of this interface provide persistent records-based storage in which records are stored
 * and retrieved using record IDs.
 *
 * IStorageUtility can be used in two flavors: you manage the IDs, or the utility manages the IDs:
 *
 * If you manage the IDs, the objects you are storing must implement Persistable, which provides the ID from the object
 * itself. You then use the functions read(), write(), and remove() when dealing with storage.
 *
 * If the utility manages the IDs, your objects need only implement Externalizable. You use the functions read(), add(),
 * update(), and remove(). add() will return a new ID for the record, which you then explicitly provide to all subsequent
 * calls to update().
 *
 * These two schemes should not be mixed within the same StorageUtility.
 */
public interface IStorageUtility<E extends Externalizable> {

    /**
     * Read and return the record corresponding to 'id'.
     *
     * @param id id of the object
     * @return object for 'id'. null if no object is stored under that ID
     */
    E read(int id);

    /**
     * Read and return the raw bytes for the record corresponding to 'id'.
     *
     * @param id id of the object
     * @return raw bytes for the record. null if no record is stored under that ID
     */
    byte[] readBytes(int id);

    /**
     * Write an object to the store. Will either add a new record, or update the existing record (if one exists) for the
     * object's ID. This function should never be used in conjunction with add() and update() within the same StorageUtility
     *
     * @param p object to store
     */
    void write(Persistable p);

    /**
     * Add a new record to the store. This function always adds a new record; it never updates an existing record. The
     * record ID under which this record is added is allocated by the StorageUtility. If this StorageUtility stores
     * Persistables, you should almost certainly use write() instead.
     *
     * @param e object to add
     * @return record ID for newly added object
     */
    int add(E e);

    /**
     * Update a record in the store. The record must have previously been added to the store using add(). If this
     * StorageUtility stores Persistables, you should almost certainly use write() instead.
     *
     * @param id ID of record to update
     * @param e  updated object
     * @throws IllegalArgumentException if no record exists for ID
     */
    void update(int id, E e);

    /**
     * Remove record with the given ID from the store.
     *
     * @param id ID of record to remove
     * @throws IllegalArgumentException if no record with that ID exists
     */
    void remove(int id);

    /**
     * Remove object from the store
     *
     * @param p object to remove
     * @throws IllegalArgumentException if object is not in the store
     */
    void remove(Persistable p);

    void removeAll();

    Vector<Integer> removeAll(EntityFilter ef);

    /**
     * Return the number of records in the store
     *
     * @return number of records
     */
    int getNumRecords();

    /**
     * Return whether the store is empty
     *
     * @return true if there are no records in the store
     */
    boolean isEmpty();

    /**
     * Return whether a record exists in the store
     *
     * @param id record ID
     * @return true if a record exists for that ID in the store
     */
    boolean exists(int id);

    /**
     * Return an iterator to iterate through all records in this store
     *
     * @return record iterator
     */
    IStorageIterator<E> iterate();

    /**
     * Close all resources associated with this StorageUtility. Any attempt to use this StorageUtility after this call will result
     * in error. Though not strictly necessary, it is a good idea to call this when you are done with the StorageUtility, as closing
     * may trigger clean-up in the underlying device storage (reclaiming unused space, etc.).
     */
    void close();

    /**
     * Fetch the object that acts as the synchronization lock for this StorageUtility
     *
     * @return lock object
     */
    Object getAccessLock();
}


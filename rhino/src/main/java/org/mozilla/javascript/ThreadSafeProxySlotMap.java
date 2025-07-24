package org.mozilla.javascript;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

public class ThreadSafeProxySlotMap extends ProxySlotMap {

    private SlotMap realMap;

    public ThreadSafeProxySlotMap(SlotMapOwner owner, SlotMap realMap) {
        super(owner);
        this.realMap = realMap;
    }

    @Override
    public void add(SlotMapOwner owner, Slot newSlot) {
        ((LockAwareSlotMap) realMap).addWithLock(owner, newSlot);
        touched = true;
    }

    @Override
    public <S extends Slot> S compute(
            SlotMapOwner owner, Object key, int index, SlotComputer<S> compute) {
        var res = ((LockAwareSlotMap) realMap).computeWithLock(owner, this, key, index, compute);
        touched = true;
        return res;
    }

    @Override
    public <S extends Slot> S compute(
            SlotMapOwner owner,
            ProxySlotMap mutableMap,
            Object key,
            int index,
            SlotComputer<S> compute) {
        touched = false;
        try {
            return ((LockAwareSlotMap) realMap).computeWithLock(owner, this, key, index, compute);
        } finally {
            touched = true;
        }
    }

    @Override
    public int dirtySize() {
        return realMap.dirtySize();
    }

    @Override
    public boolean isEmpty() {
        return ((LockAwareSlotMap) realMap).isEmptyWithLock();
    }

    @Override
    public Slot modify(SlotMapOwner owner, Object key, int index, int attributes) {
        var res = ((LockAwareSlotMap) realMap).modifyWithLock(owner, key, index, attributes);
        touched = true;
        return res;
    }

    @Override
    public Slot query(Object key, int index) {
        return ((LockAwareSlotMap) realMap).queryWithLock(key, index);
    }

    @Override
    public long readLock() {
        return 0;
    }

    @Override
    public int size() {
        return ((LockAwareSlotMap) realMap).sizeWithLock();
    }

    @Override
    public void unlockRead(long stamp) {
        // What should happen here? Error?
    }

    @Override
    public void forEach(Consumer<? super Slot> action) {
        ((LockAwareSlotMap) realMap).forEach(action);
    }

    @Override
    public Iterator<Slot> iterator() {
        return ((LockAwareSlotMap) realMap).iterator();
    }

    @Override
    public Spliterator<Slot> spliterator() {
        return ((LockAwareSlotMap) realMap).spliterator();
    }

    @Override
    public void close() {
        //If we claimed the lock we need to release the lock.
    }
}

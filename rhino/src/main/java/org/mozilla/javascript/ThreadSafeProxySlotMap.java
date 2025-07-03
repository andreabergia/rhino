package org.mozilla.javascript;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

public class ThreadSafeProxySlotMap extends ProxySlotMap {

    public ThreadSafeProxySlotMap(SlotMap realMap) {
        super(realMap);
    }

    @Override
    public void add(SlotMapOwner owner, Slot newSlot) {
        ((LockAwareSlotMap) realMap).addWithLock(owner, newSlot);
    }

    @Override
    public <S extends Slot> S compute(
            SlotMapOwner owner, Object key, int index, SlotComputer<S> compute) {
        touched = true;
        return ((LockAwareSlotMap) realMap).computeWithLock(owner, key, index, compute);
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
        touched = true;
        return ((LockAwareSlotMap) realMap).modifyWithLock(owner, key, index, attributes);
    }

    @Override
    public Slot query(Object key, int index) {
        touched = true;
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
}

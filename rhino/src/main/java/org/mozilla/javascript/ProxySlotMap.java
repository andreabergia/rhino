package org.mozilla.javascript;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

public class ProxySlotMap implements SlotMap {
    protected final SlotMap realMap;
    boolean touched = false;

    public ProxySlotMap(SlotMap realMap) {
        this.realMap = realMap;
    }

    @Override
    public void add(SlotMapOwner owner, Slot newSlot) {
        realMap.add(owner, newSlot);
    }

    @Override
    public <S extends Slot> S compute(
            SlotMapOwner owner, Object key, int index, SlotComputer<S> compute) {
        touched = true;
        return realMap.compute(owner, key, index, compute);
    }

    @Override
    public int dirtySize() {
        return SlotMap.super.dirtySize();
    }

    @Override
    public boolean isEmpty() {
        return realMap.isEmpty();
    }

    @Override
    public Slot modify(SlotMapOwner owner, Object key, int index, int attributes) {
        touched = true;
        return realMap.modify(owner, key, index, attributes);
    }

    @Override
    public Slot query(Object key, int index) {
        touched = true;
        return realMap.query(key, index);
    }

    @Override
    public long readLock() {
        return realMap.readLock();
    }

    @Override
    public int size() {
        return realMap.size();
    }

    @Override
    public void unlockRead(long stamp) {
        realMap.unlockRead(stamp);
    }

    @Override
    public void forEach(Consumer<? super Slot> action) {
        realMap.forEach(action);
    }

    @Override
    public Iterator<Slot> iterator() {
        return realMap.iterator();
    }

    @Override
    public Spliterator<Slot> spliterator() {
        return realMap.spliterator();
    }
}

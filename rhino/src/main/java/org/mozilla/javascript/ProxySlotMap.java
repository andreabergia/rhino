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

    public boolean isTouched() {
        return touched;
    }

    @Override
    public void add(SlotMapOwner owner, Slot newSlot) {
        realMap.add(owner, newSlot);
        touched = true;
    }

    @Override
    public <S extends Slot> S compute(
            SlotMapOwner owner, Object key, int index, SlotComputer<S> compute) {
        var res = realMap.compute(owner, this, key, index, compute);
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
        if (this != mutableMap) {
            throw new Error();
        }
        var res = realMap.compute(owner, this, key, index, compute);
        touched = true;
        return res;
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
        var res = realMap.modify(owner, key, index, attributes);
        touched = true;
        return res;
    }

    @Override
    public Slot query(Object key, int index) {
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

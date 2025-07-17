package org.mozilla.javascript;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

// Should be named something like compound operations map.
//
// Should only be created by slot map owner, too easy to pass in the
// wrong owner.
//
// Should be auto-closable so the creator can let the map claim and
// release locks internally.
//
// Might be able to use this to refactor better lock promotion
// eventually - i.e. we only need to claim a readlock most of the
// time, and could promote that iff we actually need to make a
// modification as part of a compute operation or similar.
public class ProxySlotMap implements SlotMap, AutoCloseable {
    protected final SlotMapOwner owner;
    boolean touched = false;

    public ProxySlotMap(SlotMapOwner owner) {
        this.owner = owner;
    }

    public boolean isTouched() {
        return touched;
    }

    @Override
    public void add(SlotMapOwner owner, Slot newSlot) {
        owner.getMap().add(owner, newSlot);
        touched = true;
    }

    @Override
    public <S extends Slot> S compute(
            SlotMapOwner owner, Object key, int index, SlotComputer<S> compute) {
        var res = owner.getMap().compute(owner, this, key, index, compute);
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
        var res = owner.getMap().compute(owner, this, key, index, compute);
        touched = true;
        return res;
    }

    @Override
    public int dirtySize() {
        return owner.getMap().dirtySize();
    }

    @Override
    public boolean isEmpty() {
        return owner.getMap().isEmpty();
    }

    @Override
    public Slot modify(SlotMapOwner owner, Object key, int index, int attributes) {
        var res = owner.getMap().modify(owner, key, index, attributes);
        touched = true;
        return res;
    }

    @Override
    public Slot query(Object key, int index) {
        return owner.getMap().query(key, index);
    }

    @Override
    public long readLock() {
        return owner.getMap().readLock();
    }

    @Override
    public int size() {
        return owner.getMap().size();
    }

    @Override
    public void unlockRead(long stamp) {
        owner.getMap().unlockRead(stamp);
    }

    @Override
    public void forEach(Consumer<? super Slot> action) {
        owner.getMap().forEach(action);
    }

    @Override
    public Iterator<Slot> iterator() {
        return owner.getMap().iterator();
    }

    @Override
    public Spliterator<Slot> spliterator() {
        return owner.getMap().spliterator();
    }

    @Override
    public void close() throws Exception {
        // This version doesn't need to do anything on clean up.
    }
}

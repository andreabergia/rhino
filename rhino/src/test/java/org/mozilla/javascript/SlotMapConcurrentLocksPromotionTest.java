package org.mozilla.javascript;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

class SlotMapConcurrentLocksPromotionTest {
	@Test
	public void singleThreadPromotionInCompute_emptyToOne() {
		ScriptableObject obj = new TestScriptableObject();
		obj.setMap(SlotMapOwner.THREAD_SAFE_EMPTY_SLOT_MAP);

		obj.getMap().compute(
				obj,
				"foo",
				0,
				(key, index, existing, mutableMap, owner) -> {
					assertSame(owner, obj);

					mutableMap.add(owner, new Slot("a", 1, 0));

					return null;
				});

		assertArrayEquals(new Object[]{"a"}, obj.getIds());
	}

	@Test
	public void singleThreadPromotionInCompute_emptyToTwo() {
		ScriptableObject obj = new TestScriptableObject();
		obj.setMap(SlotMapOwner.THREAD_SAFE_EMPTY_SLOT_MAP);

		obj.getMap().compute(
				obj,
				"foo",
				0,
				(key, index, existing, mutableMap, owner) -> {
					assertSame(owner, obj);

					mutableMap.add(owner, new Slot("a", 1, 0));
					mutableMap.add(owner, new Slot("b", 2, 0));

					return null;
				});

		assertArrayEquals(new Object[]{"a", "b"}, obj.getIds());
	}

	@Test
	public void singleThreadPromotionInCompute_oneToTwo() {
		ScriptableObject obj = new TestScriptableObject();
		obj.setMap(new SlotMapOwner.SingleEntrySlotMap(new Slot("a", 1, 0)));

		obj.getMap().compute(
				obj,
				"foo",
				0,
				(key, index, existing, mutableMap, owner) -> {
					assertSame(owner, obj);

					mutableMap.add(owner, new Slot("b", 2, 0));

					return null;
				});

		assertArrayEquals(new Object[]{"a", "b"}, obj.getIds());
	}


	@Test
	public void multiThreadPromotion_emptyToTwo() throws InterruptedException {
		ScriptableObject obj = new TestScriptableObject();
		obj.setMap(SlotMapOwner.THREAD_SAFE_EMPTY_SLOT_MAP);

		AtomicBoolean interrupted = new AtomicBoolean(false);
		Semaphore semaphoreStartTwo = new Semaphore(0);
		Semaphore semaphoreResumeOne = new Semaphore(0);

		Thread thread2 = new Thread(() -> {
			obj.getMap().compute(
					obj, "foo", 0,
					(key, index, existing, mutableMap, owner) -> {
						try {
							semaphoreStartTwo.acquire();
							mutableMap.add(owner, new Slot("a", 1, 0));
							semaphoreResumeOne.release();
						} catch (InterruptedException e) {
							interrupted.set(true);
						}

						return null;
					});
		});
		thread2.start();

		obj.getMap().compute(
				obj, "foo", 0,
				(key, index, existing, mutableMap, owner) -> {
					try {
						semaphoreStartTwo.release(); // Unlock thread2
						semaphoreResumeOne.acquire(); // Wait for thread2 to be done with the computation
						mutableMap.add(owner, new Slot("b", 2, 0));
					} catch (InterruptedException e) {
						interrupted.set(true);
					}
					return null;
				});

		thread2.join();

		assertFalse(interrupted.get());
		assertArrayEquals(new Object[]{"a", "b"}, obj.getIds());
	}

	@Test
	public void multiThreadPromotion_emptyToMany() throws InterruptedException {
		ScriptableObject obj = new TestScriptableObject();
		obj.setMap(SlotMapOwner.THREAD_SAFE_EMPTY_SLOT_MAP);

		AtomicBoolean interrupted = new AtomicBoolean(false);
		Semaphore semaphoreStartTwo = new Semaphore(0);
		Semaphore semaphoreResumeOne = new Semaphore(0);

		int max = 10;

		Thread thread2 = new Thread(() -> {
			obj.getMap().compute(
					obj, "foo", 0,
					(key, index, existing, mutableMap, owner) -> {
						try {
							for (int i = 0;i < max; i++) {
								semaphoreStartTwo.acquire();
								mutableMap.add(owner, new Slot("two" + i, i, 0));
								semaphoreResumeOne.release();
							}
						} catch (InterruptedException e) {
							interrupted.set(true);
						}

						return null;
					});
		});
		thread2.start();

		obj.getMap().compute(
				obj, "foo", 0,
				(key, index, existing, mutableMap, owner) -> {
					try {
						for (int i = 0;i <  max; i++) {
							semaphoreStartTwo.release(); // Unlock thread2
							semaphoreResumeOne.acquire(); // Wait for thread2 to be done with the computation
							mutableMap.add(owner, new Slot("one" + i, max + i, 0));
						}
					} catch (InterruptedException e) {
						interrupted.set(true);
					}
					return null;
				});

		thread2.join();

		assertFalse(interrupted.get());
		assertEquals(max * 2, obj.getIds().length);
	}

	private static class TestScriptableObject extends ScriptableObject {
		public String getClassName() {
			return "foo";
		}
	}
}

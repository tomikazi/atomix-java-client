/*
 * Copyright 2019-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.client.counter;

import io.atomix.client.AbstractPrimitiveTest;

/**
 * Unit tests for {@link AtomicCounter}.
 */
public class AtomicCounterTest extends AbstractPrimitiveTest {
    /*@Test
    public void testBasicOperations() throws Throwable {
        AtomicCounter along = client().atomicCounterBuilder("test-counter-basic-operations").build();
        assertEquals(0, along.get());
        assertEquals(1, along.incrementAndGet());
        along.set(100);
        assertEquals(100, along.get());
        assertEquals(100, along.getAndAdd(10));
        assertEquals(110, along.get());
        assertFalse(along.compareAndSet(109, 111));
        assertTrue(along.compareAndSet(110, 111));
        assertEquals(100, along.addAndGet(-11));
        assertEquals(100, along.getAndIncrement());
        assertEquals(101, along.get());
        assertEquals(100, along.decrementAndGet());
        assertEquals(100, along.getAndDecrement());
        assertEquals(99, along.get());
    }*/
}

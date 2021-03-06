/*
 * Copyright 2010-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.gemfire;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.gemstone.gemfire.cache.CacheListener;
import com.gemstone.gemfire.cache.CacheLoader;
import com.gemstone.gemfire.cache.CacheLoaderException;
import com.gemstone.gemfire.cache.CacheWriter;
import com.gemstone.gemfire.cache.CacheWriterException;
import com.gemstone.gemfire.cache.CustomExpiry;
import com.gemstone.gemfire.cache.DataPolicy;
import com.gemstone.gemfire.cache.EntryEvent;
import com.gemstone.gemfire.cache.EvictionAction;
import com.gemstone.gemfire.cache.EvictionAlgorithm;
import com.gemstone.gemfire.cache.EvictionAttributes;
import com.gemstone.gemfire.cache.ExpirationAction;
import com.gemstone.gemfire.cache.ExpirationAttributes;
import com.gemstone.gemfire.cache.LoaderHelper;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.RegionEvent;
import com.gemstone.gemfire.cache.asyncqueue.AsyncEvent;
import com.gemstone.gemfire.cache.asyncqueue.AsyncEventListener;
import com.gemstone.gemfire.cache.util.CacheListenerAdapter;
import com.gemstone.gemfire.management.internal.cli.util.spring.StringUtils;

/**
 * The LookupRegionMutationIntegrationTest class is a test suite of test cases testing the contract and integrated
 * functionality between natively-defined GemFire Cache Regions and SDG's Region lookup functionality combined with
 * Region attribute(s) mutation.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.junit.runner.RunWith
 * @see org.springframework.data.gemfire.LookupRegionFactoryBean
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.junit4.SpringJUnit4ClassRunner
 * @since 1.7.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@SuppressWarnings("unused")
public class LookupRegionMutationIntegrationTest {

	@Resource(name = "Example")
	private Region<?, ?> example;

	protected void assertCacheListeners(CacheListener[] cacheListeners, Collection<String> expectedCacheListenerNames) {
		if (!expectedCacheListenerNames.isEmpty()) {
			assertNotNull("CacheListeners must not be null!", cacheListeners);
			assertEquals(expectedCacheListenerNames.size(), cacheListeners.length);
			assertTrue(toStrings(cacheListeners).containsAll(expectedCacheListenerNames));
		}
	}

	protected void assertEvictionAttributes(EvictionAttributes evictionAttributes, EvictionAction expectedAction, EvictionAlgorithm expectedAlgorithm, int expectedMaximum) {
		assertNotNull("EvictionAttributes must not be null!", evictionAttributes);
		assertEquals(expectedAction, evictionAttributes.getAction());
		assertEquals(expectedAlgorithm, evictionAttributes.getAlgorithm());
		assertEquals(expectedMaximum, evictionAttributes.getMaximum());
	}

	protected void assertExpirationAttributes(ExpirationAttributes expirationAttributes,
		String description, int expectedTimeout, ExpirationAction expectedAction) {
		assertNotNull(String.format("ExpirationAttributes for '%1$s' must not be null!", description), expirationAttributes);
		assertEquals(expectedAction, expirationAttributes.getAction());
		assertEquals(expectedTimeout, expirationAttributes.getTimeout());
	}

	protected void assertGemFireComponent(Object gemfireComponent, String expectedName) {
		assertNotNull("The GemFire component must not be null!", gemfireComponent);
		assertEquals(expectedName, gemfireComponent.toString());
	}

	protected Collection<String> toStrings(Object[] objects) {
		List<String> cacheListenerNames = new ArrayList<String>(objects.length);

		for (Object object : objects) {
			cacheListenerNames.add(object.toString());
		}

		return cacheListenerNames;
	}

	@Test
	public void testRegionConfiguration() {
		assertNotNull("'/Example' Region was not properly initialized!", example);
		assertEquals("Example", example.getName());
		assertEquals("/Example", example.getFullPath());
		assertNotNull(example.getAttributes());
		assertEquals(DataPolicy.REPLICATE, example.getAttributes().getDataPolicy());
		assertEquals(13, example.getAttributes().getInitialCapacity());
		assertEquals(0.85f, example.getAttributes().getLoadFactor(), 0.0f);
		assertCacheListeners(example.getAttributes().getCacheListeners(), Arrays.asList("A", "B"));
		assertGemFireComponent(example.getAttributes().getCacheLoader(), "C");
		assertGemFireComponent(example.getAttributes().getCacheWriter(), "D");
		assertEvictionAttributes(example.getAttributes().getEvictionAttributes(), EvictionAction.OVERFLOW_TO_DISK,
			EvictionAlgorithm.LRU_ENTRY, 1000);
		assertExpirationAttributes(example.getAttributes().getRegionTimeToLive(), "Region TTL",
			120, ExpirationAction.LOCAL_DESTROY);
		assertExpirationAttributes(example.getAttributes().getRegionIdleTimeout(), "Region TTI",
			60, ExpirationAction.INVALIDATE);
		assertExpirationAttributes(example.getAttributes().getEntryTimeToLive(), "Entry TTL",
			30, ExpirationAction.DESTROY);
		assertGemFireComponent(example.getAttributes().getCustomEntryIdleTimeout(), "E");
		assertNotNull(example.getAttributes().getGatewaySenderIds());
		assertEquals(1, example.getAttributes().getGatewaySenderIds().size());
		assertEquals("GWS", example.getAttributes().getGatewaySenderIds().iterator().next());
		assertNotNull(example.getAttributes().getAsyncEventQueueIds());
		assertEquals(1, example.getAttributes().getAsyncEventQueueIds().size());
		assertEquals("AEQ", example.getAttributes().getAsyncEventQueueIds().iterator().next());
	}

	protected static interface Nameable extends BeanNameAware {
		String getName();
		void setName(String name);
	}

	protected static abstract class AbstractNameable implements Nameable {

		private String name;

		public String getName() {
			return name;
		}

		public void setName(final String name) {
			this.name = name;
		}

		@Override
		public void setBeanName(final String name) {
			if (!StringUtils.hasText(this.name)) {
				setName(name);
			}
		}

		@Override
		public String toString() {
			return getName();
		}
	}

	public static final class TestAsyncEventListener extends AbstractNameable implements AsyncEventListener {

		@Override public boolean processEvents(List<AsyncEvent> events) {
			throw new UnsupportedOperationException("Not Implemented!");
		}

		@Override public void close() { }
	}

	public static final class TestCacheListener<K, V> extends CacheListenerAdapter<K, V> implements Nameable {

		private String name;

		public String getName() {
			return name;
		}

		public void setName(final String name) {
			this.name = name;
		}

		@Override
		public void setBeanName(final String name) {
			if (!StringUtils.hasText(this.name)) {
				setName(name);
			}
		}

		@Override
		public String toString() {
			return getName();
		}
	}

	public static final class TestCacheLoader<K, V> extends AbstractNameable implements CacheLoader<K, V> {

		@Override
		public V load(LoaderHelper<K, V> helper) throws CacheLoaderException {
			throw new UnsupportedOperationException("Not Implemented!");
		}

		@Override
		public void close() { }
	}

	public static final class TestCacheWriter<K, V> extends AbstractNameable implements CacheWriter<K, V> {

		@Override public void beforeUpdate(EntryEvent<K, V> event) throws CacheWriterException { }

		@Override public void beforeCreate(EntryEvent<K, V> event) throws CacheWriterException { }

		@Override public void beforeDestroy(EntryEvent<K, V> event) throws CacheWriterException { }

		@Override public void beforeRegionDestroy(RegionEvent<K, V> event) throws CacheWriterException { }

		@Override public void beforeRegionClear(RegionEvent<K, V> event) throws CacheWriterException { }

		@Override public void close() { }
	}

	public static final class TestCustomExpiry<K, V> extends AbstractNameable implements CustomExpiry<K, V> {

		@Override public ExpirationAttributes getExpiry(Region.Entry<K, V> entry) {
			throw new UnsupportedOperationException("Not Implemented!");
		}

		@Override public void close() { }
	}

}

/*
 * Copyright 2013 Phil Brown
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

package self.philbrown.droidQuery;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import android.util.Log;

/**
 * Used for caching Ajax Responses
 * @author Phil Brown
 * @since 8:25:44 AM Oct 4, 2013
 *
 */
public class AjaxCache
{
	/**
	 * Timeout to use in {@link AjaxOptions#cacheTimeout(long)} in order to specify that the cached
	 * response will only be timed out when {@link #clearCache()} is called.
	 */
	public static final int TIMEOUT_NEVER = -1;
	/**
	 * Timeout to use in {@link AjaxOptions#cacheTimeout(long)} in order to specify that the cached
	 * response will never be cleared unless {@link #removeEntry(AjaxOptions)} or {@link #removeEntry(String)}
	 * is specifically called.
	 */
	public static final int TIMEOUT_NEVER_CLEAR_FROM_CACHE = -2;
	
	/** singleton instance */
	private static AjaxCache self;
	
	/** Stores data */
	private static Map<String, ? super Object> data;
	/** Stores always-cached */
	private static Map<String, ? super Object> alwaysData;
	/** Stores dates */
	private static Map<String, Date> dates;
	/** {@code true} to show verbose output. Otherwise {@code false}. */
	private boolean verbose;
	
	/** Callback for cache-cleared events. */
	private OnCacheClearedListener onCacheClearedListener;
	
	/**
	 * Constructor.
	 */
	private AjaxCache()
	{
		data = new HashMap<String, Object>();
		dates = new HashMap<String, Date>();
		alwaysData = new HashMap<String, Object>();
	}
	
	/**
	 * Singleton accessor
	 * @return
	 */
	public static AjaxCache sharedCache()
	{
		if (self == null)
			self = new AjaxCache();
		return self;
	}
	
	/**
	 * Sets the callback that is invoked when the cache is cleared.
	 * @param listener the callback to invoke
	 */
	public void setOnCacheClearedListener(OnCacheClearedListener listener)
	{
		this.onCacheClearedListener = listener;
	}
	
	/**
	 * Enable or disable verbose logging
	 * @param verbose {@code true} to log more. Otherwise {@code false}
	 * @return this
	 */
	public AjaxCache verbose(boolean verbose)
	{
		this.verbose = verbose;
		return this;
	}
	
	/**
	 * Get the cached response for the given options
	 * @param options the options used to store the cache entry, or an options with the same data type, type, url, and data.
	 * @return the cached Object
	 */
	public Object getCachedResponse(AjaxOptions options)
	{
		String key = String.format(Locale.US, "%s::%s::%s::%s", options.dataType(), (options.type() == null ? "GET" : options.type()), options.url(), (options.data() == null ? "" : options.data().toString()));

		Object response;
		Date date;
		synchronized(data)
		{
			response = data.get(key);
			synchronized(dates)
			{
				date = dates.get(key);
			}
		}
		if (verbose)
		{
			Log.i("getCachedResponse", "Key = " + key);
			Log.i("getCachedResponse", "Response = " + (response == null ? "null" : response.toString()));
			Log.i("getCachedResponse", "Date = " + (date == null ? "null" : date.toString()));
		}
		
		if (response != null && date != null)
		{
			long cacheTime = date.getTime();
			long now = new Date().getTime();
			long cacheTimeout = options.cacheTimeout();
			if (cacheTimeout == TIMEOUT_NEVER || cacheTimeout == TIMEOUT_NEVER_CLEAR_FROM_CACHE || now < cacheTime + cacheTimeout)
			{
				if (verbose) Log.i("getCachedResponse", "Returning cached response");
				return response;
			}
			else
			{
				if (verbose) Log.i("getCachedResponse", "Returning null. Cache out of date.");
				synchronized(data)
				{
					data.remove(key);
					synchronized(dates)
					{
						dates.remove(key);
					}
				}
				
			}
		}
		
		return null;
	}
	
	/**
	 * Cache a response
	 * @param response the response value
	 * @param options the options used to get the value. This is used as the key.
	 * @return the key used to cache the response.
	 */
	public String cacheResponse(Object response, AjaxOptions options)
	{
		String key = String.format(Locale.US, "%s::%s::%s::%s", options.dataType(), (options.type() == null ? "GET" : options.type()), options.url(), (options.data() == null ? "" : options.data().toString()));
		if (verbose)
		{
			Log.i("cacheResponse", "Key = " + key);
			Log.i("cacheResponse", "Response = " + (response == null ? "null" : response.toString()));
		}
		synchronized(data)
		{
			data.put(key, response);
			synchronized(dates)
			{
				dates.put(key, new Date());
			}
		}
		if (options.cacheTimeout() == TIMEOUT_NEVER_CLEAR_FROM_CACHE)
		{
			synchronized(alwaysData)
			{
				alwaysData.put(key, response);
			}
		}
		return key;
	}
	
	/**
	 * Print the cache contents
	 */
	public void printCache()
	{
		if (data.size() == 0)
		{
			Log.i("printCache", "Cache is empty");
			return;
		}
		Map<String, Object> copy = new HashMap<String, Object>(data);
		for (Entry<String, ?> entry : copy.entrySet())
		{
			Log.i("printCache", String.format(Locale.US, "%s : %s", entry.getKey(), entry.getValue().toString()));
		}
	}
	
	/**
	 * Get a copy of the current cache to see what can be added or removed
	 * @return
	 * @see #removeEntry(String)
	 * @see #cacheResponse(Object, AjaxOptions)
	 */
	public Map<String, Object> getCache()
	{
		return new HashMap<String, Object>(data);
	}
	
	/**
	 * Remove the entry for the given String key
	 * @param key
	 * @see #printCache()
	 * @see #getCache()
	 */
	public void removeEntry(String key)
	{
		synchronized(data)
		{
			data.remove(key);
			synchronized(dates)
			{
				dates.remove(key);
			}
		}
		synchronized(alwaysData)
		{
			alwaysData.remove(key);
		}
	}
	
	/**
	 * Remove entry for the given AjaxOptions key
	 * @param options
	 */
	public void removeEntry(AjaxOptions options)
	{
		String key = String.format(Locale.US, "%s::%s::%s", options.dataType(), (options.type() == null ? "GET" : options.type()), options.url());
		synchronized(data)
		{
			data.remove(key);
			synchronized(dates)
			{
				dates.remove(key);
			}
		}
		synchronized(alwaysData)
		{
			alwaysData.remove(key);
		}
	}
	
	/**
	 * Clears all cache entries.
	 */
	public void clearCache()
	{
		synchronized(data)
		{
			data.clear();
			synchronized(dates)
			{
				dates.clear();
			}
			synchronized(alwaysData)
			{
				data.putAll(alwaysData);
			}
		}
		if (this.onCacheClearedListener != null)
			this.onCacheClearedListener.onCacheCleared();
	}
	
	/**
	 * Callback for clearing the Ajax Cache
	 * @author Phil Brown
	 * @since 1:01:24 PM Dec 3, 2013
	 *
	 */
	public interface OnCacheClearedListener
	{
		/** Called after the cache has been cleared. */
		public void onCacheCleared();
	}
	
}

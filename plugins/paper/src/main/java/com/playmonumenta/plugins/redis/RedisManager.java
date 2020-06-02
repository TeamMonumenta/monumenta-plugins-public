package com.playmonumenta.plugins.redis;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
/*
 * IMPORTANT: Do not try to access the Jedis functions directly as doing so incorrectly can cause
 * server crashes. If you need to use a Jedis function that does not yet exist in this class,
 * add it here following the same format used in the other functions.
 */

public class RedisManager {
	private static JedisPool mPool = null;
	private static Logger mLogger = null;


	public RedisManager(Logger logger) throws Exception {
		mPool = new JedisPool(new JedisPoolConfig(), "redis", 6379);
		mLogger = logger;
	}

	/*
	 * Do not call this outside Pulgin.java onDisable()
	 */
	public void closePool() {
		mPool.close();
	}

	public static void set(String key, String value) {
		Jedis j = null;
		try {
			j = mPool.getResource();
			j.set(key, value);
		} catch (Exception e) {
			mLogger.warning("Redis set failed: " + e.getMessage());
			e.printStackTrace();
		}
		if (j != null) {
			j.close();
		}
	}

	public static String get(String key) {
		String retVal = "";
		Jedis j = null;
		try {
			j = mPool.getResource();
			retVal = j.get(key);
		} catch (Exception e) {
			mLogger.warning("Redis get failed: " + e.getMessage());
			e.printStackTrace();
		}
		if (j != null) {
			j.close();
		}
		return retVal;
	}

	public static boolean hexists(String key, String field) {
		boolean retVal = false;
		Jedis j = null;
		try {
			j = mPool.getResource();
			retVal = j.hexists(key, field);
		} catch (Exception e) {
			mLogger.warning("Redis hexists failed: " + e.getMessage());
			e.printStackTrace();
		}
		if (j != null) {
			j.close();
		}
		return retVal;
	}

	public static String hget(String key, String field) {
		String retVal = "";
		Jedis j = null;
		try {
			j = mPool.getResource();
			retVal = j.hget(key, field);
		} catch (Exception e) {
			mLogger.warning("Redis hget failed: " + e.getMessage());
			e.printStackTrace();
		}
		if (j != null) {
			j.close();
		}
		return retVal;
	}

	public static Map<String, String> hgetAll(String key) {
		Map<String, String> retVal = new HashMap<>();
		Jedis j = null;
		try {
			j = mPool.getResource();
			retVal = j.hgetAll(key);
		} catch (Exception e) {
			mLogger.warning("Redis hgetAll failed: " + e.getMessage());
			e.printStackTrace();
		}
		if (j != null) {
			j.close();
		}
		return retVal;
	}

	public static void hset(String key, String field, String value) {
		Jedis j = null;
		try {
			j = mPool.getResource();
			j.hset(key, field, value);
		} catch (Exception e) {
			mLogger.warning("Redis hset failed: " + e.getMessage());
			e.printStackTrace();
		}
		if (j != null) {
			j.close();
		}
	}

	public static boolean exists(String key) {
		boolean retVal = false;
		Jedis j = null;
		try {
			j = mPool.getResource();
			j.exists(key);
		} catch (Exception e) {
			mLogger.warning("Redis exists failed: " + e.getMessage());
			e.printStackTrace();
		}
		if (j != null) {
			j.close();
		}
		return retVal;
	}

	public static void del(String key) {
		Jedis j = null;
		try {
			j = mPool.getResource();
			j.del(key);
		} catch (Exception e) {
			mLogger.warning("Redis del failed: " + e.getMessage());
			e.printStackTrace();
		}
		if (j != null) {
			j.close();
		}
	}
}

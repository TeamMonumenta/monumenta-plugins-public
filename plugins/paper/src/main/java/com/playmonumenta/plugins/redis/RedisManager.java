package com.playmonumenta.plugins.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisManager {
	public JedisPool mPool = new JedisPool(new JedisPoolConfig(), "redis", 6379);

	public void set(String key, String value) {
		Jedis j = null;
		try {
			j = mPool.getResource();
			j.set(key, value);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		if (j != null) {
			j.close();
		}
	}

	public String get(String key) {
		String retVal = "";
		Jedis j = null;
		try {
			j = mPool.getResource();
			retVal = j.get(key);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		if (j != null) {
			j.close();
		}
		return retVal;
	}
}

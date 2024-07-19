package com.playmonumenta.plugins.utils;

import java.sql.Time;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class Debugging {


	private static final HashMap<String, Trace> marks = new HashMap<>();

	public static Trace begin(String what) {
		var trace = new Trace(what);

		marks.put(what, trace);
		return trace;
	}


	public static String end(Trace what) {
		return marks.remove(what.mName).render();
	}

	public static String end(String what) {
		return marks.remove(what).render();
	}

	public static class Trace {

		private final String mName;
		private final List<String> mInfo;
		private final long mStartTime;


		Trace(String name) {
			this.mName = name;
			mInfo = new ArrayList<>();
			mStartTime = System.currentTimeMillis();
		}


		public String render() {
			return "-----TRACEBACK from " + Time.from(Instant.ofEpochMilli(mStartTime)) + " to " + Time.from(Instant.ofEpochMilli(System.currentTimeMillis())) + "  (" + (System.currentTimeMillis() - mStartTime) + "ms)----\n" + "-----" + mName + "-----\n" + (mInfo.isEmpty() ? "" : mInfo.stream().collect(Collectors.joining(" ", "-  ", "\n")));
		}

		public void addInfo(String info) {
			this.mInfo.add(info);
		}

	}

}

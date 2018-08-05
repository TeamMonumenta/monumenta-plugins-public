package com.playmonumenta.plugins.utils;

public class StringUtils {
	static public String ticksToTime(int ticks) {
		int minutes = ((ticks / 20) / 60);
		int seconds = ((ticks - ((minutes * 60) * 20))) / 20;
		
		String time = "";
		if (minutes > 0) {
			time = minutes + " minutes ";
		}
		
		time += seconds + " seconds";
		
		
		return time;
	}
}

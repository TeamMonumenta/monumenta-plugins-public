package pe.project.utils;

public class StringUtils {
	static public String ticksToTime(int ticks) {
		int minutes = (int)((ticks / 20) / 60);
		int seconds = ((ticks - ((minutes * 60) * 20))) / 20;
		
		String time = "";
		if (minutes > 0) {
			time = minutes + " minutes ";
		}
		
		time += seconds + " seconds";
		
		
		return time;
	}
}

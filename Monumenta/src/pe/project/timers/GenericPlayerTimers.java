package pe.project.timers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;

//	TODO - Expand this to take in a "Timer Type" id as well so we can store multiple different types of
//	timers for players in the future with this.
public class GenericPlayerTimers {
	public HashMap<UUID, Integer> mTimers = null;
	
	public GenericPlayerTimers() {
		mTimers = new HashMap<UUID, Integer>();
	}
	
	public void addTimer(UUID playerUUID, int time) {
		mTimers.put(playerUUID, time);
	}
	
	public Integer getTimer(UUID playerUUID) {
		if (mTimers.containsKey(playerUUID)) {
			return mTimers.get(playerUUID);
		}
		
		return 0;
	}
	
	public void update(Integer ticks) {
		Iterator<Entry<UUID, Integer>> iter = mTimers.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<UUID, Integer> player = iter.next();
			
			int timer = player.getValue() - ticks;
			if (timer > 0) {
				player.setValue(timer);
			} else {
				iter.remove();
			}
		}
	}
}

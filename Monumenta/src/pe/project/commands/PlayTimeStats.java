package pe.project.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import pe.project.Main;

public class PlayTimeStats implements CommandExecutor {
	private World mWorld;
	
	public PlayTimeStats(Main plugin, World world) {
		mWorld = world;
	}
	
	@Override
    public boolean onCommand(CommandSender arg0, Command arg1, String arg2, String[] arg3) {	
    	List<Player> players = new ArrayList<Player>();
    	
    	players.addAll(mWorld.getPlayers());
    	
    	Map<String, Integer> playTime = new HashMap<String, Integer>();
    	for (Player player : players) {
    		playTime.put(player.getDisplayName(), player.getStatistic(Statistic.PLAY_ONE_TICK));
    	}
    	
    	List<Entry<String, Integer>> playerList = new ArrayList<Entry<String, Integer>>(playTime.entrySet());
    	Collections.sort(playerList, new Comparator<Entry<String, Integer>>() {

			@Override
			public int compare(Entry<String, Integer> obj1, Entry<String, Integer> obj2) {
				return obj2.getValue().compareTo(obj1.getValue());
			}
    		
    	});
    	
    	for (int i = 0; i < playerList.size(); i++) {
    		Entry<String, Integer> entry = playerList.get(i);
    		
    		int grandTotal = (int)entry.getValue();
    		int totalTicks = grandTotal;
    		
    		int days = _ticksToDays(totalTicks);
    		totalTicks -= _daysToTicks(days);
    		
    		int hours = _ticksToHours(totalTicks);
    		totalTicks -= _hoursToTicks(hours);
    		
    		int minutes = _ticksToMinutes(totalTicks);
    		totalTicks -= _minutesToTicks(minutes);
    		
    		int seconds = _ticksToSeconds(totalTicks);
    		
    		arg0.sendMessage(ChatColor.GREEN + "[" + (i+1) + "] " + entry.getKey() + ": Total Play Time - " + days + " days " + hours + " hours " + minutes + " minutes " + seconds + " seconds" + " (Total Ticks: " + grandTotal + ")");
    	}
    	
    	return true;
    }
	
	int _ticksToDays(int ticks) {
		return ((((ticks / 20) / 60) / 60) / 24);
	}
	
	int _daysToTicks(int days) {
		return days * 24 * 60 * 60  * 20;
	}
    
    int _ticksToHours(int ticks) {
    	return (((ticks / 20) / 60) / 60);
    }
    
    int _hoursToTicks(int hours) {
    	return hours * 60 * 60  * 20;
    }
    
    int _ticksToMinutes(int ticks) {
    	return ((ticks / 20) / 60);
    }
    
    int _minutesToTicks(int minutes) {
    	return minutes * 60 * 20;
    }
    
    int _ticksToSeconds(int ticks) {
    	return (ticks / 20);
    }
}

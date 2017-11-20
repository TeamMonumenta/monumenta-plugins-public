package pe.project.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import pe.project.Plugin;

public class ProfilingCommand implements CommandExecutor {
	Plugin mPlugin;
	
	public ProfilingCommand(Plugin plugin) {
		mPlugin = plugin;
	}
	
	@Override
	public boolean onCommand(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		if (arg0 instanceof Player) {
			Player player = (Player)arg0;
			String param = arg3[0];
			if (param != null) {
				if (param.equals("entities")) {
					List<Entity> allEntities = Bukkit.getWorlds().get(0).getEntities();
					
					HashMap<EntityType, Integer> entityCount = new HashMap<EntityType, Integer>();
					for (Entity entity : allEntities) {
						EntityType type = entity.getType();
						if (!entityCount.containsKey(type)) {
							entityCount.put(entity.getType(), 1);
						} else {
							entityCount.put(type, entityCount.get(type) + 1);
						}
					}
					
					List<Entry<EntityType, Integer>> list = new ArrayList<Entry<EntityType, Integer>>(entityCount.entrySet());
					Collections.sort(list, new Comparator<Map.Entry<EntityType, Integer>>(){
			            public int compare(Map.Entry<EntityType, Integer> o1, Map.Entry<EntityType, Integer> o2) {
			                return (o2.getValue()).compareTo( o1.getValue() );
			            }
			        } );
					
					player.sendMessage("Total Entities: " + allEntities.size());
					
					for(Entry<EntityType, Integer> entity : list) {
						player.sendMessage(entity.getKey().toString().toLowerCase() + ": " + entity.getValue());
			        }
					
					return true;
				}
			}
		}
		
		return false;
	}

}

package com.playmonumenta.plugins.bosses.bosses.sirius;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.*;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class CopyBlightArena extends BossAbilityGroup {
	public static final String identityTag = "boss_copysiriusarena";
	private @Nullable Location mCornerOne;
	private @Nullable Location mCornerTwo;
	private static final EnumSet<Material> IGNORED_MATS = EnumSet.of(
		Material.COMMAND_BLOCK,
		Material.CHAIN_COMMAND_BLOCK,
		Material.REPEATING_COMMAND_BLOCK,
		Material.BEDROCK
	);


	public CopyBlightArena(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mCornerOne = null;
		mCornerTwo = null;
		//stops build sharders swapping the arena and makes sure it only works on build.
		//could remove shard check but want to be safe
		if (!com.playmonumenta.plugins.Plugin.IS_PLAY_SERVER && !ServerProperties.getShardName().equals("build")) {
			mCornerOne = mBoss.getLocation().add(43, 49, 58);
			mCornerTwo = mBoss.getLocation().subtract(75, 7, 60);
			Map<String, List<BlockData>> mStates = new HashMap<>();
			for (double x = mCornerTwo.getX(); x < mCornerOne.getX(); x++) {
				for (double z = mCornerTwo.getZ(); z < mCornerOne.getZ(); z++) {
					List<BlockData> blockData = new ArrayList<>();
					for (double y = mCornerTwo.getY(); y < mCornerOne.getY(); y++) {
						Location loc = new Location(mBoss.getWorld(), x, y, z);
						if (loc.getBlock().getType().equals(Material.ORANGE_WOOL)) {
							blockData.add(Bukkit.createBlockData(Material.AIR));
						} else if (!IGNORED_MATS.contains(loc.getBlock().getType())) {
							blockData.add(loc.getBlock().getBlockData());
						}
					}
					mStates.put("x" + (mCornerOne.getX() - x) + "z" + (mCornerOne.getZ() - z), blockData);
				}
			}
			JsonObject blight = new JsonObject();
			JsonArray blightArr = new JsonArray();
			blight.add("blight", blightArr);
			for (String key : mStates.keySet()) {
				JsonObject object = new JsonObject();
				StringBuilder output = new StringBuilder();
				List<BlockData> list = mStates.get(key);
				if (!list.isEmpty()) {
					for (BlockData data : list) {
						String s = data.toString();
						s = s.substring(15, s.length() - 1);
						output.append(s).append(", ");
					}
					output.deleteCharAt(output.length() - 1);
					output.deleteCharAt(output.length() - 1);
					object.addProperty(key, output.toString());
					blightArr.add(object);
				}
			}
			try {
				FileUtils.writeJson(mPlugin.getDataFolder() + "/SiriusBlightArena.json", blight);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			for (Player p : PlayerUtils.playersInRange(boss.getLocation(), 10, true, true)) {
				p.sendMessage("You do not have permission to use this bosstag");
			}
		}
	}

}

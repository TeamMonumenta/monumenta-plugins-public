package com.playmonumenta.plugins.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.server.properties.ServerProperties;

public class ChestUtils {

	private static class DelvesLootTableMappings {
		private final String mScoreboard;
		private final List<Map<String, String>> mPathMappings;

		public DelvesLootTableMappings(String scoreboard, List<Map<String, String>> pathMappings) {
			mScoreboard = scoreboard;
			mPathMappings = pathMappings;
		}
	}

	// Index 0 = mobs shard, everything else is the numeric wool dungeon
	private static final Map<String, DelvesLootTableMappings> DELVES_LOOT_TABLE_MAPPINGS = new HashMap<String, DelvesLootTableMappings>();

	// Excuse the god awful loot table mappings
	static {
		Map<String, String> whiteCursedLootTableMappings = new HashMap<String, String>();
		Map<String, String> whiteTwistedLootTableMappings = new HashMap<String, String>();
		whiteCursedLootTableMappings.put("r1/dungeons/1/level_2_chest", "r1/dungeons/delves/white/cursed_chest");
		whiteCursedLootTableMappings.put("r1/dungeons/1/level_3_chest", "r1/dungeons/delves/white/cursed_chest");
		whiteCursedLootTableMappings.put("r1/dungeons/1/final_chest", "r1/dungeons/delves/white/cursed_final");
		whiteTwistedLootTableMappings.put("r1/dungeons/1/level_2_chest", "r1/dungeons/delves/white/twisted_chest");
		whiteTwistedLootTableMappings.put("r1/dungeons/1/level_3_chest", "r1/dungeons/delves/white/twisted_chest");
		whiteTwistedLootTableMappings.put("r1/dungeons/1/final_chest", "r1/dungeons/delves/white/twisted_final");

		Map<String, String> orangeCursedLootTableMappings = new HashMap<String, String>();
		Map<String, String> orangeTwistedLootTableMappings = new HashMap<String, String>();
		orangeCursedLootTableMappings.put("r1/dungeons/2/level_2_chest", "r1/dungeons/delves/orange/cursed_chest");
		orangeCursedLootTableMappings.put("r1/dungeons/2/level_3_chest", "r1/dungeons/delves/orange/cursed_chest");
		orangeCursedLootTableMappings.put("r1/dungeons/2/level_4_chest", "r1/dungeons/delves/orange/cursed_chest");
		orangeCursedLootTableMappings.put("r1/dungeons/2/final_chest", "r1/dungeons/delves/orange/cursed_final");
		orangeTwistedLootTableMappings.put("r1/dungeons/2/level_2_chest", "r1/dungeons/delves/orange/twisted_chest");
		orangeTwistedLootTableMappings.put("r1/dungeons/2/level_3_chest", "r1/dungeons/delves/orange/twisted_chest");
		orangeTwistedLootTableMappings.put("r1/dungeons/2/level_4_chest", "r1/dungeons/delves/orange/twisted_chest");
		orangeTwistedLootTableMappings.put("r1/dungeons/2/final_chest", "r1/dungeons/delves/orange/twisted_final");

		Map<String, String> magentaCursedLootTableMappings = new HashMap<String, String>();
		Map<String, String> magentaTwistedLootTableMappings = new HashMap<String, String>();
		magentaCursedLootTableMappings.put("r1/dungeons/3/level_3_chest", "r1/dungeons/delves/magenta/cursed_chest");
		magentaCursedLootTableMappings.put("r1/dungeons/3/level_4_chest", "r1/dungeons/delves/magenta/cursed_chest");
		magentaCursedLootTableMappings.put("r1/dungeons/3/final_chest", "r1/dungeons/delves/magenta/cursed_final");
		magentaTwistedLootTableMappings.put("r1/dungeons/3/level_3_chest", "r1/dungeons/delves/magenta/twisted_chest");
		magentaTwistedLootTableMappings.put("r1/dungeons/3/level_4_chest", "r1/dungeons/delves/magenta/twisted_chest");
		magentaTwistedLootTableMappings.put("r1/dungeons/3/final_chest", "r1/dungeons/delves/magenta/twisted_final");

		Map<String, String> lightblueCursedLootTableMappings = new HashMap<String, String>();
		Map<String, String> lightblueTwistedLootTableMappings = new HashMap<String, String>();
		lightblueCursedLootTableMappings.put("r1/dungeons/4/level_3_chest_u", "r1/dungeons/delves/lightblue/cursed_academy");
		lightblueCursedLootTableMappings.put("r1/dungeons/4/level_4_chest_u", "r1/dungeons/delves/lightblue/cursed_academy");
		lightblueCursedLootTableMappings.put("r1/dungeons/4/level_3_chest_a", "r1/dungeons/delves/lightblue/cursed_bastion");
		lightblueCursedLootTableMappings.put("r1/dungeons/4/level_4_chest_a", "r1/dungeons/delves/lightblue/cursed_bastion");
		lightblueCursedLootTableMappings.put("r1/dungeons/4/final_chest", "r1/dungeons/delves/lightblue/cursed_final");
		lightblueTwistedLootTableMappings.put("r1/dungeons/4/level_3_chest_u", "r1/dungeons/delves/lightblue/twisted_academy");
		lightblueTwistedLootTableMappings.put("r1/dungeons/4/level_4_chest_u", "r1/dungeons/delves/lightblue/twisted_academy");
		lightblueTwistedLootTableMappings.put("r1/dungeons/4/level_3_chest_a", "r1/dungeons/delves/lightblue/twisted_bastion");
		lightblueTwistedLootTableMappings.put("r1/dungeons/4/level_4_chest_a", "r1/dungeons/delves/lightblue/twisted_bastion");
		lightblueTwistedLootTableMappings.put("r1/dungeons/4/final_chest", "r1/dungeons/delves/lightblue/twisted_final");

		Map<String, String> yellowCursedLootTableMappings = new HashMap<String, String>();
		Map<String, String> yellowTwistedLootTableMappings = new HashMap<String, String>();
		yellowCursedLootTableMappings.put("r1/dungeons/5/overgrown-man-1", "r1/dungeons/delves/yellow/cursed_overgrown");
		yellowCursedLootTableMappings.put("r1/dungeons/5/poison-man-1", "r1/dungeons/delves/yellow/cursed_poison");
		yellowCursedLootTableMappings.put("r1/dungeons/5/poison-man-2", "r1/dungeons/delves/yellow/cursed_poison");
		yellowCursedLootTableMappings.put("r1/dungeons/5/fish-1", "r1/dungeons/delves/yellow/cursed_fish");
		yellowCursedLootTableMappings.put("r1/dungeons/5/fish-2", "r1/dungeons/delves/yellow/cursed_fish");
		yellowCursedLootTableMappings.put("r1/dungeons/5/dragon-1", "r1/dungeons/delves/yellow/cursed_dragon");
		yellowCursedLootTableMappings.put("r1/dungeons/5/dragon-2", "r1/dungeons/delves/yellow/cursed_dragon");
		yellowCursedLootTableMappings.put("r1/dungeons/5/polar-1", "r1/dungeons/delves/yellow/cursed_polar");
		yellowCursedLootTableMappings.put("r1/dungeons/5/ender-1", "r1/dungeons/delves/yellow/cursed_ender");
		yellowCursedLootTableMappings.put("r1/dungeons/5/overgrown-man-2", "r1/dungeons/delves/yellow/cursed_final");
		yellowTwistedLootTableMappings.put("r1/dungeons/5/overgrown-man-1", "r1/dungeons/delves/yellow/twisted_overgrown");
		yellowTwistedLootTableMappings.put("r1/dungeons/5/poison-man-1", "r1/dungeons/delves/yellow/twisted_poison");
		yellowTwistedLootTableMappings.put("r1/dungeons/5/poison-man-2", "r1/dungeons/delves/yellow/twisted_poison");
		yellowTwistedLootTableMappings.put("r1/dungeons/5/fish-1", "r1/dungeons/delves/yellow/twisted_fish");
		yellowTwistedLootTableMappings.put("r1/dungeons/5/fish-2", "r1/dungeons/delves/yellow/twisted_fish");
		yellowTwistedLootTableMappings.put("r1/dungeons/5/dragon-1", "r1/dungeons/delves/yellow/twisted_dragon");
		yellowTwistedLootTableMappings.put("r1/dungeons/5/dragon-2", "r1/dungeons/delves/yellow/twisted_dragon");
		yellowTwistedLootTableMappings.put("r1/dungeons/5/polar-1", "r1/dungeons/delves/yellow/twisted_polar");
		yellowTwistedLootTableMappings.put("r1/dungeons/5/ender-1", "r1/dungeons/delves/yellow/twisted_ender");
		yellowTwistedLootTableMappings.put("r1/dungeons/5/overgrown-man-2", "r1/dungeons/delves/yellow/twisted_final");

		Map<String, String> willowsCursedLootTableMappings = new HashMap<String, String>();
		Map<String, String> willowsTwistedLootTableMappings = new HashMap<String, String>();
		willowsCursedLootTableMappings.put("r1/dungeons/bonus/level_4_chest", "r1/dungeons/delves/willows/cursed_chest");
		willowsCursedLootTableMappings.put("r1/dungeons/bonus/level_5_chest", "r1/dungeons/delves/willows/cursed_chest");
		willowsCursedLootTableMappings.put("r1/dungeons/bonus/final_chest", "r1/dungeons/delves/willows/cursed_final");
		willowsTwistedLootTableMappings.put("r1/dungeons/bonus/level_4_chest", "r1/dungeons/delves/willows/twisted_chest");
		willowsTwistedLootTableMappings.put("r1/dungeons/bonus/level_5_chest", "r1/dungeons/delves/willows/twisted_chest");
		willowsTwistedLootTableMappings.put("r1/dungeons/bonus/final_chest", "r1/dungeons/delves/willows/twisted_final");

		Map<String, String> limeCursedLootTableMappings = new HashMap<String, String>();
		Map<String, String> limeTwistedLootTableMappings = new HashMap<String, String>();
		limeCursedLootTableMappings.put("r2/dungeons/lime/swamplevel_2_chest", "r2/dungeons/delves/lime/cursed_swamp");
		limeCursedLootTableMappings.put("r2/dungeons/lime/citylevel_2_chest", "r2/dungeons/delves/lime/cursed_city");
		limeCursedLootTableMappings.put("r2/dungeons/lime/citylevel_3_chest", "r2/dungeons/delves/lime/cursed_city");
		limeCursedLootTableMappings.put("r2/dungeons/lime/librarylevel_2_chest", "r2/dungeons/delves/lime/cursed_library");
		limeCursedLootTableMappings.put("r2/dungeons/lime/librarylevel_3_chest", "r2/dungeons/delves/lime/cursed_library");
		limeCursedLootTableMappings.put("r2/dungeons/lime/final_chest", "r2/dungeons/delves/lime/cursed_final");
		limeTwistedLootTableMappings.put("r2/dungeons/lime/swamplevel_2_chest", "r2/dungeons/delves/lime/twisted_swamp");
		limeTwistedLootTableMappings.put("r2/dungeons/lime/citylevel_2_chest", "r2/dungeons/delves/lime/twisted_city");
		limeTwistedLootTableMappings.put("r2/dungeons/lime/citylevel_3_chest", "r2/dungeons/delves/lime/twisted_city");
		limeTwistedLootTableMappings.put("r2/dungeons/lime/librarylevel_2_chest", "r2/dungeons/delves/lime/twisted_library");
		limeTwistedLootTableMappings.put("r2/dungeons/lime/librarylevel_3_chest", "r2/dungeons/delves/lime/twisted_library");
		limeTwistedLootTableMappings.put("r2/dungeons/lime/final_chest", "r2/dungeons/delves/lime/twisted_final");

		Map<String, String> pinkCursedLootTableMappings = new HashMap<String, String>();
		Map<String, String> pinkTwistedLootTableMappings = new HashMap<String, String>();
		pinkCursedLootTableMappings.put("r2/dungeons/pink/spring_chest", "r2/dungeons/delves/pink/cursed_spring");
		pinkCursedLootTableMappings.put("r2/dungeons/pink/summer_chest", "r2/dungeons/delves/pink/cursed_summer");
		pinkCursedLootTableMappings.put("r2/dungeons/pink/autumn_chest", "r2/dungeons/delves/pink/cursed_autumn");
		pinkCursedLootTableMappings.put("r2/dungeons/pink/winter_chest", "r2/dungeons/delves/pink/cursed_winter");
		pinkCursedLootTableMappings.put("r2/dungeons/pink/temple_chest", "r2/dungeons/delves/pink/cursed_temple");
		pinkCursedLootTableMappings.put("r2/dungeons/pink/dissonant_chest", "r2/dungeons/delves/pink/cursed_dissonant");
		pinkCursedLootTableMappings.put("r2/dungeons/pink/final_chest", "r2/dungeons/delves/pink/cursed_final");
		pinkTwistedLootTableMappings.put("r2/dungeons/pink/spring_chest", "r2/dungeons/delves/pink/twisted_spring");
		pinkTwistedLootTableMappings.put("r2/dungeons/pink/summer_chest", "r2/dungeons/delves/pink/twisted_summer");
		pinkTwistedLootTableMappings.put("r2/dungeons/pink/autumn_chest", "r2/dungeons/delves/pink/twisted_autumn");
		pinkTwistedLootTableMappings.put("r2/dungeons/pink/winter_chest", "r2/dungeons/delves/pink/twisted_winter");
		pinkTwistedLootTableMappings.put("r2/dungeons/pink/temple_chest", "r2/dungeons/delves/pink/twisted_temple");
		pinkTwistedLootTableMappings.put("r2/dungeons/pink/dissonant_chest", "r2/dungeons/delves/pink/twisted_dissonant");
		pinkTwistedLootTableMappings.put("r2/dungeons/pink/final_chest", "r2/dungeons/delves/pink/twisted_final");

		Map<String, String> grayCursedLootTableMappings = new HashMap<String, String>();
		Map<String, String> grayTwistedLootTableMappings = new HashMap<String, String>();
		grayCursedLootTableMappings.put("r2/dungeons/gray/chest_outside", "r2/dungeons/delves/gray/cursed_outside");
		grayCursedLootTableMappings.put("r2/dungeons/gray/chest_library", "r2/dungeons/delves/gray/cursed_library");
		grayCursedLootTableMappings.put("r2/dungeons/gray/chest_labs", "r2/dungeons/delves/gray/cursed_labs");
		grayCursedLootTableMappings.put("r2/dungeons/gray/chest_final", "r2/dungeons/delves/gray/cursed_final");
		grayTwistedLootTableMappings.put("r2/dungeons/gray/chest_outside", "r2/dungeons/delves/gray/twisted_outside");
		grayTwistedLootTableMappings.put("r2/dungeons/gray/chest_library", "r2/dungeons/delves/gray/twisted_library");
		grayTwistedLootTableMappings.put("r2/dungeons/gray/chest_labs", "r2/dungeons/delves/gray/twisted_labs");
		grayTwistedLootTableMappings.put("r2/dungeons/gray/chest_final", "r2/dungeons/delves/gray/twisted_final");

		Map<String, String> lightgrayCursedLootTableMappings = new HashMap<String, String>();
		Map<String, String> lightgrayTwistedLootTableMappings = new HashMap<String, String>();
		lightgrayCursedLootTableMappings.put("r2/dungeons/lightgray/level_3_chestoutside", "r2/dungeons/delves/lightgray/cursed_outside");
		lightgrayCursedLootTableMappings.put("r2/dungeons/lightgray/level_4_chestoutside", "r2/dungeons/delves/lightgray/cursed_outside");
		lightgrayCursedLootTableMappings.put("r2/dungeons/lightgray/level_3_chestpalace", "r2/dungeons/delves/lightgray/cursed_palace");
		lightgrayCursedLootTableMappings.put("r2/dungeons/lightgray/level_4_chestpalace", "r2/dungeons/delves/lightgray/cursed_palace");
		lightgrayCursedLootTableMappings.put("r2/dungeons/lightgray/final_chest", "r2/dungeons/delves/lightgray/cursed_final");
		lightgrayTwistedLootTableMappings.put("r2/dungeons/lightgray/level_3_chestoutside", "r2/dungeons/delves/lightgray/twisted_outside");
		lightgrayTwistedLootTableMappings.put("r2/dungeons/lightgray/level_4_chestoutside", "r2/dungeons/delves/lightgray/twisted_outside");
		lightgrayTwistedLootTableMappings.put("r2/dungeons/lightgray/level_3_chestpalace", "r2/dungeons/delves/lightgray/twisted_palace");
		lightgrayTwistedLootTableMappings.put("r2/dungeons/lightgray/level_4_chestpalace", "r2/dungeons/delves/lightgray/twisted_palace");
		lightgrayTwistedLootTableMappings.put("r2/dungeons/lightgray/final_chest", "r2/dungeons/delves/lightgray/twisted_final");

		Map<String, String> cyanCursedLootTableMappings = new HashMap<String, String>();
		Map<String, String> cyanTwistedLootTableMappings = new HashMap<String, String>();
		cyanCursedLootTableMappings.put("r2/dungeons/cyan/level_3_chestmine", "r2/dungeons/delves/cyan/cursed_mine");
		cyanCursedLootTableMappings.put("r2/dungeons/cyan/level_4_chestmine", "r2/dungeons/delves/cyan/cursed_mine");
		cyanCursedLootTableMappings.put("r2/dungeons/cyan/level_3_chesttemple", "r2/dungeons/delves/cyan/cursed_temple");
		cyanCursedLootTableMappings.put("r2/dungeons/cyan/level_4_chesttemple", "r2/dungeons/delves/cyan/cursed_temple");
		cyanCursedLootTableMappings.put("r2/dungeons/cyan/final_chest", "r2/dungeons/delves/cyan/cursed_final");
		cyanTwistedLootTableMappings.put("r2/dungeons/cyan/level_3_chestmine", "r2/dungeons/delves/cyan/twisted_mine");
		cyanTwistedLootTableMappings.put("r2/dungeons/cyan/level_4_chestmine", "r2/dungeons/delves/cyan/twisted_mine");
		cyanTwistedLootTableMappings.put("r2/dungeons/cyan/level_3_chesttemple", "r2/dungeons/delves/cyan/twisted_temple");
		cyanTwistedLootTableMappings.put("r2/dungeons/cyan/level_4_chesttemple", "r2/dungeons/delves/cyan/twisted_temple");
		cyanTwistedLootTableMappings.put("r2/dungeons/cyan/final_chest", "r2/dungeons/delves/cyan/twisted_final");

		Map<String, String> purpleCursedLootTableMappings = new HashMap<String, String>();
		Map<String, String> purpleTwistedLootTableMappings = new HashMap<String, String>();
		purpleCursedLootTableMappings.put("r2/dungeons/purple/chestpirate", "r2/dungeons/delves/purple/cursed_pirate");
		purpleCursedLootTableMappings.put("r2/dungeons/purple/chesttemple", "r2/dungeons/delves/purple/cursed_temple");
		purpleCursedLootTableMappings.put("r2/dungeons/purple/final_chest", "r2/dungeons/delves/purple/cursed_final");
		purpleTwistedLootTableMappings.put("r2/dungeons/purple/chestpirate", "r2/dungeons/delves/purple/twisted_pirate");
		purpleTwistedLootTableMappings.put("r2/dungeons/purple/chesttemple", "r2/dungeons/delves/purple/twisted_temple");
		purpleTwistedLootTableMappings.put("r2/dungeons/purple/final_chest", "r2/dungeons/delves/purple/twisted_final");

		// Testing purposes
		Map<String, String> mobsCursedLootTableMappings = new HashMap<String, String>();
		Map<String, String> mobsTwistedLootTableMappings = new HashMap<String, String>();
		mobsCursedLootTableMappings.putAll(whiteCursedLootTableMappings);
		mobsCursedLootTableMappings.putAll(orangeCursedLootTableMappings);
		mobsCursedLootTableMappings.putAll(magentaCursedLootTableMappings);
		mobsCursedLootTableMappings.putAll(lightblueCursedLootTableMappings);
		mobsCursedLootTableMappings.putAll(yellowCursedLootTableMappings);
		mobsCursedLootTableMappings.putAll(limeCursedLootTableMappings);
		mobsCursedLootTableMappings.putAll(pinkCursedLootTableMappings);
		mobsCursedLootTableMappings.putAll(grayCursedLootTableMappings);
		mobsCursedLootTableMappings.putAll(lightgrayCursedLootTableMappings);
		mobsCursedLootTableMappings.putAll(cyanCursedLootTableMappings);
		mobsCursedLootTableMappings.putAll(purpleCursedLootTableMappings);
		mobsTwistedLootTableMappings.putAll(whiteTwistedLootTableMappings);
		mobsTwistedLootTableMappings.putAll(orangeTwistedLootTableMappings);
		mobsTwistedLootTableMappings.putAll(magentaTwistedLootTableMappings);
		mobsTwistedLootTableMappings.putAll(lightblueTwistedLootTableMappings);
		mobsTwistedLootTableMappings.putAll(yellowTwistedLootTableMappings);
		mobsTwistedLootTableMappings.putAll(limeTwistedLootTableMappings);
		mobsTwistedLootTableMappings.putAll(pinkTwistedLootTableMappings);
		mobsTwistedLootTableMappings.putAll(grayTwistedLootTableMappings);
		mobsTwistedLootTableMappings.putAll(lightgrayTwistedLootTableMappings);
		mobsTwistedLootTableMappings.putAll(cyanTwistedLootTableMappings);
		mobsTwistedLootTableMappings.putAll(purpleTwistedLootTableMappings);

		DELVES_LOOT_TABLE_MAPPINGS.put("white", new DelvesLootTableMappings("Delve1Challenge", Arrays.asList(whiteCursedLootTableMappings, whiteTwistedLootTableMappings)));
		DELVES_LOOT_TABLE_MAPPINGS.put("orange", new DelvesLootTableMappings("Delve2Challenge", Arrays.asList(orangeCursedLootTableMappings, orangeTwistedLootTableMappings)));
		DELVES_LOOT_TABLE_MAPPINGS.put("magenta", new DelvesLootTableMappings("Delve3Challenge", Arrays.asList(magentaCursedLootTableMappings, magentaTwistedLootTableMappings)));
		DELVES_LOOT_TABLE_MAPPINGS.put("lightblue", new DelvesLootTableMappings("Delve4Challenge", Arrays.asList(lightblueCursedLootTableMappings, lightblueTwistedLootTableMappings)));
		DELVES_LOOT_TABLE_MAPPINGS.put("yellow", new DelvesLootTableMappings("Delve5Challenge", Arrays.asList(yellowCursedLootTableMappings, yellowTwistedLootTableMappings)));
		DELVES_LOOT_TABLE_MAPPINGS.put("willows", new DelvesLootTableMappings("DelveWChallenge", Arrays.asList(willowsCursedLootTableMappings, willowsTwistedLootTableMappings)));
		DELVES_LOOT_TABLE_MAPPINGS.put("lime", new DelvesLootTableMappings("Delve6Challenge", Arrays.asList(limeCursedLootTableMappings, limeTwistedLootTableMappings)));
		DELVES_LOOT_TABLE_MAPPINGS.put("pink", new DelvesLootTableMappings("Delve7Challenge", Arrays.asList(pinkCursedLootTableMappings, pinkTwistedLootTableMappings)));
		DELVES_LOOT_TABLE_MAPPINGS.put("gray", new DelvesLootTableMappings("Delve8Challenge", Arrays.asList(grayCursedLootTableMappings, grayTwistedLootTableMappings)));
		DELVES_LOOT_TABLE_MAPPINGS.put("lightgray", new DelvesLootTableMappings("Delve9Challenge", Arrays.asList(lightgrayCursedLootTableMappings, lightgrayTwistedLootTableMappings)));
		DELVES_LOOT_TABLE_MAPPINGS.put("cyan", new DelvesLootTableMappings("Delve10Challenge", Arrays.asList(cyanCursedLootTableMappings, cyanTwistedLootTableMappings)));
		DELVES_LOOT_TABLE_MAPPINGS.put("purple", new DelvesLootTableMappings("Delve11Challenge", Arrays.asList(purpleCursedLootTableMappings, purpleTwistedLootTableMappings)));
		DELVES_LOOT_TABLE_MAPPINGS.put("dev1", new DelvesLootTableMappings("DelveChallenge", Arrays.asList(mobsCursedLootTableMappings, mobsTwistedLootTableMappings)));
		DELVES_LOOT_TABLE_MAPPINGS.put("dev2", new DelvesLootTableMappings("DelveChallenge", Arrays.asList(mobsCursedLootTableMappings, mobsTwistedLootTableMappings)));
		DELVES_LOOT_TABLE_MAPPINGS.put("mobs", new DelvesLootTableMappings("DelveChallenge", Arrays.asList(mobsCursedLootTableMappings, mobsTwistedLootTableMappings)));
	}

	private static final String DUNGEON_LOOT_TABLE_NAMESPACE = "epic";

	private static final int CHEST_LUCK_RADIUS = 128;
	private static final double[] BONUS_ITEMS = {
			0,		// Dummy value, this is a player count indexed array
			0.5,
			1.7,
			2.6,
			3.3,
			3.8,
			4.2,
			4.4,
			4.5
	};		

	public static void chestScalingLuck(Plugin plugin, Player player, Block block) {
		int chestLuck = ScoreboardUtils.getScoreboardValue(player, "ChestLuckToggle");
		if (chestLuck > 0) {
			int playerCount = PlayerUtils.playersInRange(player.getLocation(), CHEST_LUCK_RADIUS).size();
			double bonusItems = BONUS_ITEMS[Math.min(BONUS_ITEMS.length - 1, playerCount)];
			int luckLevel = (int) bonusItems;

			if (FastUtils.RANDOM.nextDouble() < bonusItems - luckLevel) {
				luckLevel++;
			}

			if (luckLevel > 0) {
				plugin.mPotionManager.addPotion(player, PotionID.SAFE_ZONE, new PotionEffect(PotionEffectType.LUCK,
				                                3, luckLevel - 1, true, false));
			}

			if (player.getEquipment() != null &&
			    player.getEquipment().getItemInMainHand() != null &&
			    player.getEquipment().getItemInMainHand().getType() == Material.COMPASS) {
				if (playerCount == 1) {
					MessagingUtils.sendActionBarMessage(plugin, player, playerCount + " player in range!");
				} else {
					MessagingUtils.sendActionBarMessage(plugin, player, playerCount + " players in range!");
				}
			}
		}
	}

	public static void replaceWithDelveLootTable(Player player, Block block) {
		// If the chest doesn't have a loot table, we don't need to worry about it
		if (block.getState() instanceof Chest && ((Chest) block.getState()).getLootTable() != null) {
			Chest chest = (Chest) block.getState();
			DelvesLootTableMappings mappings = DELVES_LOOT_TABLE_MAPPINGS.get(ServerProperties.getShardName());

			/*
			 * Check if the Delve score indicates a Delve is active, then
			 * determine if the Delve is Cursed or Twisted.
			 *
			 * Get the appropriate HashMap based on the difficulty, then get the
			 * replacement loot table with the current loot table's key as the
			 * map key
			 */
			if (mappings != null) {
				int delveType = ScoreboardUtils.getScoreboardValue(player, mappings.mScoreboard) / 10 - 1;
				if (delveType >= 0) {
					String path = chest.getLootTable().getKey().getKey();
					String newPath = mappings.mPathMappings.get(delveType).get(path);
					if (newPath != null) {
						chest.setLootTable(Bukkit.getLootTable(new NamespacedKey(DUNGEON_LOOT_TABLE_NAMESPACE, newPath)));
						chest.update();
					}
				}
			}
		}
	}

	public static boolean isEmpty(Block block) {
		return block.getState() instanceof Chest && isEmpty((Chest)block.getState());
	}

	public static boolean isEmpty(Chest chest) {
		for (ItemStack slot : chest.getInventory()) {
			if (slot != null) {
				return false;
			}
		}
		return true;
	}
}

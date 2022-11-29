package com.playmonumenta.plugins.delves;

import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.loot.LootTable;

public class DelveLootTableGroup {

	public static final Map<String, DelveLootTableGroup> DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS = new HashMap<>();

	static {
		new DelveLootTableGroup("r1/delves/white/base_chest", "r1/delves/white/dmat_chest", "r1/delves/white/cmat_chest", "r1/dungeons/1/level_2_chest", "r1/dungeons/1/level_3_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r1/delves/white/base_final", "r1/delves/white/dmat_final", "r1/delves/white/cmat_final", "r1/dungeons/1/final_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);

		new DelveLootTableGroup("r1/delves/orange/base_chest", "r1/delves/orange/dmat_chest", "r1/delves/orange/cmat_chest", "r1/dungeons/2/level_2_chest", "r1/dungeons/2/level_3_chest", "r1/dungeons/2/level_4_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r1/delves/orange/base_final", "r1/delves/orange/dmat_final", "r1/delves/orange/cmat_final", "r1/dungeons/2/final_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);

		new DelveLootTableGroup("r1/delves/magenta/base_chest", "r1/delves/magenta/dmat_chest", "r1/delves/magenta/cmat_chest", "r1/dungeons/3/level_3_chest", "r1/dungeons/3/level_4_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r1/delves/magenta/base_final", "r1/delves/magenta/dmat_final", "r1/delves/magenta/cmat_final", "r1/dungeons/3/final_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);

		new DelveLootTableGroup("r1/delves/lightblue/base_academy", "r1/delves/lightblue/dmat_academy", "r1/delves/lightblue/cmat_academy", "r1/dungeons/4/level_3_chest_u", "r1/dungeons/4/level_4_chest_u").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r1/delves/lightblue/base_bastion", "r1/delves/lightblue/dmat_bastion", "r1/delves/lightblue/cmat_bastion", "r1/dungeons/4/level_3_chest_a", "r1/dungeons/4/level_4_chest_a").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r1/delves/lightblue/base_final", "r1/delves/lightblue/dmat_final", "r1/delves/lightblue/cmat_final", "r1/dungeons/4/final_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);

		new DelveLootTableGroup("r1/delves/yellow/base_seasons", "r1/delves/yellow/dmat_seasons", "r1/delves/yellow/cmat_seasons", "r1/dungeons/5/overgrown-man-1", "r1/dungeons/5/poison-man-1", "r1/dungeons/5/poison-man-2", "r1/dungeons/5/fish-1", "r1/dungeons/5/fish-2", "r1/dungeons/5/dragon-1", "r1/dungeons/5/dragon-2", "r1/dungeons/5/polar-1").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r1/delves/yellow/base_ender", "r1/delves/yellow/dmat_ender", "r1/delves/yellow/cmat_ender", "r1/dungeons/5/ender-1").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r1/delves/yellow/base_final", "r1/delves/yellow/dmat_final", "r1/delves/yellow/cmat_final", "r1/dungeons/5/overgrown-man-2").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);

		new DelveLootTableGroup("r1/delves/willows/base_chest", "r1/delves/willows/dmat_chest", "r1/delves/willows/cmat_chest", "r1/dungeons/bonus/level_4_chest", "r1/dungeons/bonus/level_5_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r1/delves/willows/base_final", "r1/delves/willows/dmat_final", "r1/delves/willows/cmat_final", "r1/dungeons/bonus/final_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);

		new DelveLootTableGroup("r1/delves/reverie/base_chest", "r1/delves/reverie/dmat_chest", "r1/delves/reverie/cmat_chest", "r1/dungeons/reverie/chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);

		new DelveLootTableGroup("r2/delves/lime/base_chest", "r2/delves/lime/dmat_chest", "r2/delves/lime/cmat_chest", "r2/dungeons/lime/swamplevel_2_chest", "r2/dungeons/lime/citylevel_2_chest", "r2/dungeons/lime/citylevel_3_chest", "r2/dungeons/lime/librarylevel_2_chest", "r2/dungeons/lime/librarylevel_3_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r2/delves/lime/base_final", "r2/delves/lime/dmat_final", "r2/delves/lime/cmat_final", "r2/dungeons/lime/final_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);

		new DelveLootTableGroup("r2/delves/pink/base_temple", "r2/delves/pink/dmat_temple", "r2/delves/pink/cmat_temple", "r2/dungeons/pink/spring_chest", "r2/dungeons/pink/summer_chest", "r2/dungeons/pink/autumn_chest", "r2/dungeons/pink/winter_chest", "r2/dungeons/pink/temple_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r2/delves/pink/base_dissonant", "r2/delves/pink/dmat_dissonant", "r2/delves/pink/cmat_dissonant", "r2/dungeons/pink/dissonant_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r2/delves/pink/base_final", "r2/delves/pink/dmat_final", "r2/delves/pink/cmat_final", "r2/dungeons/pink/final_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);

		new DelveLootTableGroup("r2/delves/gray/base_chest", "r2/delves/gray/dmat_chest", "r2/delves/gray/cmat_chest", "r2/dungeons/gray/chest_outside", "r2/dungeons/gray/chest_library", "r2/dungeons/gray/chest_labs").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r2/delves/gray/base_final", "r2/delves/gray/dmat_final", "r2/delves/gray/cmat_final", "r2/dungeons/gray/chest_final").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);

		new DelveLootTableGroup("r2/delves/lightgray/base_chest", "r2/delves/lightgray/dmat_chest", "r2/delves/lightgray/cmat_chest", "r2/dungeons/lightgray/level_3_chestoutside", "r2/dungeons/lightgray/level_4_chestoutside", "r2/dungeons/lightgray/level_3_chestpalace", "r2/dungeons/lightgray/level_4_chestpalace").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r2/delves/lightgray/base_final", "r2/delves/lightgray/dmat_final", "r2/delves/lightgray/cmat_final", "r2/dungeons/lightgray/final_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);

		new DelveLootTableGroup("r2/delves/cyan/base_mine", "r2/delves/cyan/dmat_mine", "r2/delves/cyan/cmat_mine", "r2/dungeons/cyan/level_3_chestmine", "r2/dungeons/cyan/level_4_chestmine").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r2/delves/cyan/base_temple", "r2/delves/cyan/dmat_temple", "r2/delves/cyan/cmat_temple", "r2/dungeons/cyan/level_3_chesttemple", "r2/dungeons/cyan/level_4_chesttemple").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r2/delves/cyan/base_final", "r2/delves/cyan/dmat_final", "r2/delves/cyan/cmat_final", "r2/dungeons/cyan/final_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);

		new DelveLootTableGroup("r2/delves/purple/base_pirate", "r2/delves/purple/dmat_pirate", "r2/delves/purple/cmat_pirate", "r2/dungeons/purple/chestpirate").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r2/delves/purple/base_temple", "r2/delves/purple/dmat_temple", "r2/delves/purple/cmat_temple", "r2/dungeons/purple/chesttemple").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r2/delves/purple/base_final", "r2/delves/purple/dmat_final", "r2/delves/purple/cmat_final", "r2/dungeons/purple/final_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);

		new DelveLootTableGroup("r2/delves/teal/base_chest", "r2/delves/teal/dmat_chest", "r2/delves/teal/cmat_chest", "r2/dungeons/teal/ruined", "r2/dungeons/teal/eroded", "r2/dungeons/teal/pristine").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r2/delves/teal/base_colosseum", "r2/delves/teal/dmat_colosseum", "r2/delves/teal/cmat_colosseum", "r2/dungeons/teal/colosseum").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r2/delves/teal/base_escape", "r2/delves/teal/dmat_escape", "r2/delves/teal/cmat_escape", "r2/dungeons/teal/escape").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r2/delves/teal/base_final", "r2/delves/teal/dmat_final", "r2/delves/teal/cmat_final", "r2/dungeons/teal/final").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);

		new DelveLootTableGroup("r2/delves/forum/base_forum", "r2/delves/forum/dmat_forum", "r2/delves/forum/cmat_forum", "r2/dungeons/forum/forum").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r2/delves/forum/base_quarters", "r2/delves/forum/dmat_quarters", "r2/delves/forum/cmat_quarters", "r2/dungeons/forum/quarters").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r2/delves/forum/base_conservatory", "r2/delves/forum/dmat_conservatory", "r2/delves/forum/cmat_conservatory", "r2/dungeons/forum/conservatory").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r2/delves/forum/base_conscriptorium", "r2/delves/forum/dmat_conscriptorium", "r2/delves/forum/cmat_conscriptorium", "r2/dungeons/forum/conscriptorium").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);

		new DelveLootTableGroup("r2/delves/shiftingcity/base_chest", "r2/delves/shiftingcity/dmat_chest", "r2/delves/shiftingcity/cmat_chest", "r2/dungeons/fred/normal_city", "r2/dungeons/fred/objective_city", "r2/dungeons/fred/normal_lush", "r2/dungeons/fred/objective_lush", "r2/dungeons/fred/normal_water", "r2/dungeons/fred/objective_water").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r2/delves/shiftingcity/base_challenge", "r2/delves/shiftingcity/dmat_challenge", "r2/delves/shiftingcity/cmat_challenge", "r2/dungeons/fred/challenge").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r2/delves/shiftingcity/base_final", "r2/delves/shiftingcity/dmat_final", "r2/delves/shiftingcity/cmat_final", "r2/dungeons/fred/final_chest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);

		new DelveLootTableGroup("r3/delves/blue/base_air", "r3/delves/blue/dmat_air", "r3/delves/blue/cmat_air", "r3/dungeons/blue/air").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r3/delves/blue/base_earth", "r3/delves/blue/dmat_earth", "r3/delves/blue/cmat_earth", "r3/dungeons/blue/earth").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r3/delves/blue/base_fire", "r3/delves/blue/dmat_fire", "r3/delves/blue/cmat_fire", "r3/dungeons/blue/fire").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r3/delves/blue/base_water", "r3/delves/blue/dmat_water", "r3/delves/blue/cmat_water", "r3/dungeons/blue/water").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r3/delves/blue/base_lootroom_normal", "r3/delves/blue/dmat_lootroom_normal", "r3/delves/blue/cmat_lootroom_normal", "r3/dungeons/blue/lootroom_normal").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r3/delves/blue/base_lootroom_racewin", "r3/delves/blue/dmat_lootroom_racewin", "r3/delves/blue/cmat_lootroom_racewin", "r3/dungeons/blue/lootroom_racewin").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r3/delves/blue/base_lootroom_rare", "r3/delves/blue/dmat_lootroom_rare", "r3/delves/blue/cmat_lootroom_rare", "r3/dungeons/blue/lootroom_rare").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);

		new DelveLootTableGroup("r3/delves/brown/base_cogs", "r3/delves/brown/dmat_cogs", "r3/delves/brown/cmat_cogs", "r3/dungeons/brown/cogs").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r3/delves/brown/base_quartz", "r3/delves/brown/dmat_quartz", "r3/delves/brown/cmat_quartz", "r3/dungeons/brown/quartz").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r3/delves/brown/base_lootroom_boss", "r3/delves/brown/dmat_lootroom_boss", "r3/delves/brown/cmat_lootroom_boss", "r3/dungeons/brown/lootroom_boss").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r3/delves/brown/base_lootroom_rare", "r3/delves/brown/dmat_lootroom_rare", "r3/delves/brown/cmat_lootroom_rare", "r3/dungeons/brown/lootroom_rare").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);

		// R3 Overworld Delves
		// amanita
		new DelveLootTableGroup("r3/world/poi/amanita_colony/delves/base_chest", "r3/world/poi/amanita_colony/delves/dmat_chest", "r3/world/poi/amanita_colony/delves/cmat_chest", "r3/world/poi/amanita_colony/normalchest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r3/world/poi/amanita_colony/delves/base_final", "r3/world/poi/amanita_colony/delves/dmat_final", "r3/world/poi/amanita_colony/delves/cmat_final", "r3/world/poi/amanita_colony/endchest", "r3/world/poi/amanita_colony/endchest2").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		// arx
		new DelveLootTableGroup("r3/world/poi/arx_spirensis/delves/base_chest", "r3/world/poi/arx_spirensis/delves/dmat_chest", "r3/world/poi/arx_spirensis/delves/cmat_chest", "r3/world/poi/arx_spirensis/normalchest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r3/world/poi/arx_spirensis/delves/base_final", "r3/world/poi/arx_spirensis/delves/dmat_final", "r3/world/poi/arx_spirensis/delves/cmat_final", "r3/world/poi/arx_spirensis/endchest", "r3/world/poi/arx_spirensis/endchest2").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		// cathedral
		new DelveLootTableGroup("r3/world/poi/cathedral/delves/base_chest", "r3/world/poi/cathedral/delves/dmat_chest", "r3/world/poi/cathedral/delves/cmat_chest", "r3/world/poi/cathedral/normalchest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r3/world/poi/cathedral/delves/base_final", "r3/world/poi/cathedral/delves/dmat_final", "r3/world/poi/cathedral/delves/cmat_final", "r3/world/poi/cathedral/endchest", "r3/world/poi/cathedral/endchest2").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		// chanterelle
		new DelveLootTableGroup("r3/world/poi/chanterelle_village/delves/base_chest", "r3/world/poi/chanterelle_village/delves/dmat_chest", "r3/world/poi/chanterelle_village/delves/cmat_chest", "r3/world/poi/chanterelle_village/normalchest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r3/world/poi/chanterelle_village/delves/base_final", "r3/world/poi/chanterelle_village/delves/dmat_final", "r3/world/poi/chanterelle_village/delves/cmat_final", "r3/world/poi/chanterelle_village/endchest", "r3/world/poi/chanterelle_village/endchest2").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		// coven
		new DelveLootTableGroup("r3/world/poi/coven_fortress/delves/base_chest", "r3/world/poi/coven_fortress/delves/dmat_chest", "r3/world/poi/coven_fortress/delves/cmat_chest", "r3/world/poi/coven_fortress/normalchest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r3/world/poi/coven_fortress/delves/base_final", "r3/world/poi/coven_fortress/delves/dmat_final", "r3/world/poi/coven_fortress/delves/cmat_final", "r3/world/poi/coven_fortress/endchest", "r3/world/poi/coven_fortress/endchest2").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		// doomed
		new DelveLootTableGroup("r3/world/poi/doomed_encampment/delves/base_chest", "r3/world/poi/doomed_encampment/delves/dmat_chest", "r3/world/poi/doomed_encampment/delves/cmat_chest", "r3/world/poi/doomed_encampment/normalchest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r3/world/poi/doomed_encampment/delves/base_final", "r3/world/poi/doomed_encampment/delves/dmat_final", "r3/world/poi/doomed_encampment/delves/cmat_final", "r3/world/poi/doomed_encampment/endchest", "r3/world/poi/doomed_encampment/endchest2").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		// farm
		new DelveLootTableGroup("r3/world/poi/farm/delves/base_chest", "r3/world/poi/farm/delves/dmat_chest", "r3/world/poi/farm/delves/cmat_chest", "r3/world/poi/farm/normalchest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r3/world/poi/farm/delves/base_final", "r3/world/poi/farm/delves/dmat_final", "r3/world/poi/farm/delves/cmat_final", "r3/world/poi/farm/endchest", "r3/world/poi/farm/endchest2").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		// forsaken
		new DelveLootTableGroup("r3/world/poi/forsaken_manor/delves/base_chest", "r3/world/poi/forsaken_manor/delves/dmat_chest", "r3/world/poi/forsaken_manor/delves/cmat_chest", "r3/world/poi/forsaken_manor/normalchest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r3/world/poi/forsaken_manor/delves/base_final", "r3/world/poi/forsaken_manor/delves/dmat_final", "r3/world/poi/forsaken_manor/delves/cmat_final", "r3/world/poi/forsaken_manor/endchest", "r3/world/poi/forsaken_manor/endchest2").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		// locum
		new DelveLootTableGroup("r3/world/poi/locum_vernatia/delves/base_chest", "r3/world/poi/locum_vernatia/delves/dmat_chest", "r3/world/poi/locum_vernatia/delves/cmat_chest", "r3/world/poi/locum_vernatia/normalchest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r3/world/poi/locum_vernatia/delves/base_final", "r3/world/poi/locum_vernatia/delves/dmat_final", "r3/world/poi/locum_vernatia/delves/cmat_final", "r3/world/poi/locum_vernatia/endchest", "r3/world/poi/locum_vernatia/endchest2").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		// silverstrike
		new DelveLootTableGroup("r3/world/poi/silverstrike_bastille/delves/base_chest", "r3/world/poi/silverstrike_bastille/delves/dmat_chest", "r3/world/poi/silverstrike_bastille/delves/cmat_chest", "r3/world/poi/silverstrike_bastille/normalchest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r3/world/poi/silverstrike_bastille/delves/base_final", "r3/world/poi/silverstrike_bastille/delves/dmat_final", "r3/world/poi/silverstrike_bastille/delves/cmat_final", "r3/world/poi/silverstrike_bastille/endchest", "r3/world/poi/silverstrike_bastille/endchest2").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		// terracotta
		new DelveLootTableGroup("r3/world/poi/terracotta_mine/delves/base_chest", "r3/world/poi/terracotta_mine/delves/dmat_chest", "r3/world/poi/terracotta_mine/delves/cmat_chest", "r3/world/poi/terracotta_mine/normalchest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r3/world/poi/terracotta_mine/delves/base_final", "r3/world/poi/terracotta_mine/delves/dmat_final", "r3/world/poi/terracotta_mine/delves/cmat_final", "r3/world/poi/terracotta_mine/endchest", "r3/world/poi/terracotta_mine/endchest2").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		// vibrant
		new DelveLootTableGroup("r3/world/poi/vibrant_hollow/delves/base_chest", "r3/world/poi/vibrant_hollow/delves/dmat_chest", "r3/world/poi/vibrant_hollow/delves/cmat_chest", "r3/world/poi/vibrant_hollow/normalchest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r3/world/poi/vibrant_hollow/delves/base_final", "r3/world/poi/vibrant_hollow/delves/dmat_final", "r3/world/poi/vibrant_hollow/delves/cmat_final", "r3/world/poi/vibrant_hollow/endchest", "r3/world/poi/vibrant_hollow/endchest2").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		// waterfall
		new DelveLootTableGroup("r3/world/poi/waterfall_village/delves/base_chest", "r3/world/poi/waterfall_village/delves/dmat_chest", "r3/world/poi/waterfall_village/delves/cmat_chest", "r3/world/poi/waterfall_village/normalchest").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
		new DelveLootTableGroup("r3/world/poi/waterfall_village/delves/base_final", "r3/world/poi/waterfall_village/delves/dmat_final", "r3/world/poi/waterfall_village/delves/cmat_final", "r3/world/poi/waterfall_village/endchest", "r3/world/poi/waterfall_village/endchest2").mapDelveLootTables(DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS);
	}


	private final String mBaseTable;
	private final String mDelveMaterialTable;
	private final String mCosmeticMaterialTable;

	private final String[] mRegularTables;

	public DelveLootTableGroup(String baseTable, String delveMaterialTable, String cosmeticMaterialTable, String... regularTables) {
		mBaseTable = baseTable;
		mDelveMaterialTable = delveMaterialTable;
		mCosmeticMaterialTable = cosmeticMaterialTable;

		mRegularTables = regularTables;
	}

	public static void setDelveLootTable(int depthPoints, int playerCount, Chest chest) {
		LootTable lootTable = chest.getLootTable();
		if (lootTable != null) {
			String path = lootTable.getKey().getKey();

			DelveLootTableGroup group = DELVE_LOOT_TABLE_REPLACEMENT_MAPPINGS.get(path);
			if (group != null) {
				String newPath = group.getDelveLootTable(depthPoints, playerCount);
				if (newPath != null) {
					chest.setLootTable(Bukkit.getLootTable(NamespacedKeyUtils.fromString("epic:" + newPath)));
					chest.update();
				}
			}
		}
	}

	public static void setDelveLootTable(Player player, Block block) {
		BlockState blockState = block.getState();
		if (blockState instanceof Chest chest && chest.hasLootTable()) {
			List<Player> players = DelvesUtils.playerInRangeForDelves(player.getLocation());
			int playerTotalDelvePoint = DelvesUtils.getPartyDelvePoints(players);
			int playerCount = players.size();
			if (chest.getInventory() instanceof DoubleChestInventory doubleChestInventory) {
				setDelveLootTable(playerTotalDelvePoint, playerCount, (Chest) doubleChestInventory.getLeftSide().getHolder());
				setDelveLootTable(playerTotalDelvePoint, playerCount, (Chest) doubleChestInventory.getRightSide().getHolder());
			} else {
				setDelveLootTable(playerTotalDelvePoint, playerCount, chest);
			}
		}
	}

	public @Nullable String getDelveLootTable(int depthPoints, int playerCount) {
		if (depthPoints == 0) {
			return null;
		}

		if (FastUtils.RANDOM.nextDouble() < getDelveMaterialTableChance(depthPoints, playerCount)) {
			if (FastUtils.RANDOM.nextDouble() < getCosmeticMaterialTableChance(depthPoints)) {
				return mCosmeticMaterialTable;
			}

			return mDelveMaterialTable;
		}

		return mBaseTable;
	}

	public static double getDelveMaterialTableChance(int depthPoints, int players) {
		int edp = Math.min(DelvesUtils.getLootCapDepthPoints(players), depthPoints) - DelvesUtils.MINIMUM_DEPTH_POINTS;
		return edp < 0 ? 0 : (0.3 + 0.05 * edp - 0.00075 * edp * edp);
	}

	public static double getCosmeticMaterialTableChance(int depthPoints) {
		return Math.max(0, (double) (depthPoints - DelvesUtils.getLootCapDepthPoints(9001)) / (DelvesUtils.MAX_DEPTH_POINTS - DelvesUtils.getLootCapDepthPoints(9001)));
	}

	public void mapDelveLootTables(Map<String, DelveLootTableGroup> map) {
		for (String regularTable : mRegularTables) {
			map.put(regularTable, this);
		}
	}
}

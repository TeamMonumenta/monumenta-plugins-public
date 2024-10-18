package com.playmonumenta.plugins.depths.abilities.gifts;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class TreasureMap extends DepthsAbility {
	public static final String ABILITY_NAME = "Treasure Map";

	public static final DepthsAbilityInfo<TreasureMap> INFO =
		new DepthsAbilityInfo<>(TreasureMap.class, ABILITY_NAME, TreasureMap::new, DepthsTree.GIFT, DepthsTrigger.PASSIVE)
			.displayItem(Material.GLOW_ITEM_FRAME)
			.floors(floor -> floor == 2)
			.descriptions(TreasureMap::getDescription);

	public TreasureMap(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public static void playSounds(Player player) {
		Plugin plugin = Plugin.getInstance();
		player.playSound(player, Sound.ITEM_SHOVEL_FLATTEN, 1.0f, 1.0f);
		Bukkit.getScheduler().runTaskLater(plugin, () -> player.playSound(player, Sound.ITEM_SHOVEL_FLATTEN, 1.0f, 0.9f), 20);
		Bukkit.getScheduler().runTaskLater(plugin, () -> player.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f), 38);
		Bukkit.getScheduler().runTaskLater(plugin, () -> player.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.7f), 40);
		Bukkit.getScheduler().runTaskLater(plugin, () -> player.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 2.0f), 42);
	}

	private static Description<TreasureMap> getDescription() {
		return new DescriptionBuilder<TreasureMap>().add("Once your team selects one of each room type, gain a ")
			.add(DepthsTree.PRISMATIC.getNameComponent())
			.add(" ability selection.");
	}
}

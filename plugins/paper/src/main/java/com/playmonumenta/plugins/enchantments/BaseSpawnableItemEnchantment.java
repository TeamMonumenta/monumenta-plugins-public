package com.playmonumenta.plugins.enchantments;

import org.bukkit.entity.Item;

import com.playmonumenta.plugins.Plugin;



/*
 * Use this instead of BaseEnchantment if you wants to perform actions on
 * spawned Item entities that have this enchantment.
 */
public interface BaseSpawnableItemEnchantment extends BaseEnchantment {
	/*
	 * Called when an Item entity with this enchant spawns or gets loaded along
	 * with a chunk in the world.
	 *
	 * It may or may not have been dropped by a player.
	 *
	 * getItemLevel() is useful for getting the level of this enchant on the
	 * spawned item, since it isn't tied to a Player and ItemSlot.
	 */
	default void onSpawn(
		Plugin plugin,
		Item item,
		int level
	) {}
}

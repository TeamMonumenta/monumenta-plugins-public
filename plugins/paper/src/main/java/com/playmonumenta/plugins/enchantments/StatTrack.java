package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;

public class StatTrack implements BaseEnchantment {

	public static final String PROPERTY_NAME = ChatColor.GRAY + "Stat Track";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public boolean useEnchantLevels() {
		return false;
	}

	//For now only mainhand, but could be refactored into all slots eventually
	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND);
	}

	//Listeners to change given stats

	@Override
	public void onKill(Plugin plugin, Player player, int level, Entity target, EntityDeathEvent event) {
		ItemStack is = player.getInventory().getItemInMainHand();

		//We killed a mob, so increase the stat and update it
		StatTrackManager.incrementStat(is, player, StatTrackOptions.KILLS, 1);
	}

	@Override
	public void onBlockBreak(Plugin plugin, Player player, BlockBreakEvent event, ItemStack item, int level) {

		if (event.getBlock().getType() != Material.SPAWNER) {
			return;
		}

		ItemStack is = player.getInventory().getItemInMainHand();
		//We killed a spawner, so increase the stat
		StatTrackManager.incrementStat(is, player, StatTrackOptions.SPAWNERS_BROKEN, 1);
	}

//	@Override
//	public void onLaunchProjectile(Plugin plugin, Player player, int level, Projectile proj, ProjectileLaunchEvent event) {
//		if (event.getEntity() instanceof AbstractArrow && !ItemUtils.isShootableItem(player.getInventory().getItemInOffHand())) {
//
//			ItemStack is = player.getInventory().getItemInMainHand();
//			//We fired an arrow, so increase the stat
//			StatTrackManager.incrementStat(is, player, StatTrackOptions.ARROWS_SHOT, 1);
//		}
//	}

	//Enum for the given options

	public enum StatTrackOptions {
		KILLS("kills", "Mob Kills"), SPAWNERS_BROKEN("spawners", "Spawners Broken"), TIMES_CONSUMED("consumed", "Times Consumed");

		private final String mEnchantName;
		private final String mLabel;

		StatTrackOptions(String label, String enchant) {
			mEnchantName = enchant;
			mLabel = label;
		}

		public String getEnchantName() {
			return mEnchantName;
		}

		public String getLabel() {
			return mLabel;
		}

		public static StatTrackOptions getInfusionSelection(String label) {
			if (label == null) {
				return null;
			}
			for (StatTrackOptions selection : StatTrackOptions.values()) {
				if (selection.getLabel().equals(label)) {
					return selection;
				}
			}
			return null;
		}
	}
}

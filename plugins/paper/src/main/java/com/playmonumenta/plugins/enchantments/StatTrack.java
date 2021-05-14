package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.EntityUtils;

public class StatTrack implements BaseEnchantment {

	public static final String PROPERTY_NAME = ChatColor.GRAY + "Stat Track";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public boolean isMultiLevel() {
		return false;
	}

	//For now only mainhand, but could be refactored into all slots eventually
	@Override
	public EnumSet<ItemSlot> getValidSlots() {
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
	public void onAttack(Plugin plugin, Player player, int level, LivingEntity target, EntityDamageByEntityEvent event) {
		ItemStack is = player.getInventory().getItemInMainHand();

		//Melee damage counter
		if (!isTrainingDummy(target)) {
			StatTrackManager.incrementStat(is, player, StatTrackOptions.MELEE_DAMAGE, (int) event.getDamage());
		}
	}

	@Override
	public void onDamage(Plugin plugin, Player player, int level, LivingEntity target, EntityDamageByEntityEvent event) {
		//Track damage dealt to bosses
		if (EntityUtils.isBoss(target)) {
			ItemStack is = player.getInventory().getItemInMainHand();
			StatTrackManager.incrementStat(is, player, StatTrackOptions.BOSS_DAMAGE, (int) event.getDamage());
		}
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

	public boolean isTrainingDummy(LivingEntity e) {
		Set<String> tags = e.getScoreboardTags();
		return tags.contains("boss_training_dummy");
	}

	//Enum for the given options

	public enum StatTrackOptions {
		KILLS("kills", "Mob Kills"),
		SPAWNERS_BROKEN("spawners", "Spawners Broken"),
		TIMES_CONSUMED("consumed", "Times Consumed"),
		MELEE_DAMAGE("melee", "Melee Damage Dealt"),
		BOSS_DAMAGE("boss", "Boss Damage Dealt"),
		BLOCKS_PLACED("blocks", "Blocks Placed");

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

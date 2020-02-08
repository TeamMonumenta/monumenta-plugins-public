package com.playmonumenta.plugins.overrides;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

public class AnvilOverride extends BaseOverride {
	final static String REPAIR_OBJECTIVE = "RepairT";

	@Override
	public boolean rightClickBlockInteraction(Plugin plugin, Player player, Action action, ItemStack item,
	                                          Block block) {
		if (player == null || player.getGameMode() == GameMode.CREATIVE) {
			return true;
		} else if (player.getGameMode() == GameMode.ADVENTURE
		           && block.getLocation().subtract(0, 0.75, 0).getBlock().getType() != Material.DISPENSER) {
			return false;
		}

		/*
		 * Make sure to only repair the item in the player's main hand. Otherwise if you
		 * sneak+right click an anvil the "item" might be the offhand item
		 */
		item = player.getInventory().getItemInMainHand();

		if (item != null && item.getDurability() > 0 && !item.getType().isBlock()
		    && (!item.hasItemMeta() || !item.getItemMeta().hasLore()
		        || (!InventoryUtils.testForItemWithLore(item, "* Irreparable *")
		            && !InventoryUtils.testForItemWithLore(item, "Curse of Irreparability")))
		    && block.hasMetadata(Constants.ANVIL_CONFIRMATION_METAKEY)) {

			item.setDurability((short) 0);
			Location loc = block.getLocation().add(0.5, 0.5, 0.5);
			World world = loc.getWorld();
			world.playSound(loc, Sound.BLOCK_ANVIL_DESTROY, 1.0f, 1.0f);
			new BukkitRunnable() {
				int mTicks = 0;
				Location mParticleLoc = block.getLocation().add(0.5, 1.1, 0.5);
				@Override
				public void run() {
					mTicks++;
					if (mTicks >= 3) {
						this.cancel();
						world.spawnParticle(Particle.BLOCK_DUST, mParticleLoc.subtract(0, 0.6, 0), 60, 0.3, 0.3, 0.3, 1.2F, Material.ANVIL.createBlockData());
						world.playSound(mParticleLoc, Sound.BLOCK_STONE_BREAK, 1, 0.75f);
						world.playSound(mParticleLoc, Sound.BLOCK_STONE_BREAK, 1, 0.75f);
					} else {
						world.spawnParticle(Particle.BLOCK_DUST, mParticleLoc, 10, 0.15, 0.15, 0.15, 0.35F, Material.ANVIL.createBlockData());
					}
				}

			}.runTaskTimer(plugin, 0, 7);
			block.setType(Material.AIR);
			block.removeMetadata(Constants.ANVIL_CONFIRMATION_METAKEY, plugin);
			int repCount = ScoreboardUtils.getScoreboardValue(player, REPAIR_OBJECTIVE);
			ScoreboardUtils.setScoreboardValue(player, REPAIR_OBJECTIVE, repCount + 1);
		} else {
			player.sendMessage(ChatColor.GOLD + "Right click the anvil with the item you want to repair");
			if (!block.hasMetadata(Constants.ANVIL_CONFIRMATION_METAKEY)) {
				Location loc = block.getLocation().add(0.5, 1.2, 0.5);
				World world = loc.getWorld();
				new BukkitRunnable() {
					int mTicks = 0;
					@Override
					public void run() {
						world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 1, 0.2, 0, 0.2, 0);
						mTicks++;
						if (block.getType() == Material.AIR || !block.hasMetadata(Constants.ANVIL_CONFIRMATION_METAKEY) || block == null) {
							this.cancel();
						}
						if (mTicks >= 20 * 4) {
							this.cancel();
							block.removeMetadata(Constants.ANVIL_CONFIRMATION_METAKEY, plugin);
						}
					}

				}.runTaskTimer(plugin, 0, 2);
			}
			block.setMetadata(Constants.ANVIL_CONFIRMATION_METAKEY, new FixedMetadataValue(plugin, true));
		}
		return false;
	}
}

package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.infusions.Shattered;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;

public class AnvilOverride extends BaseOverride {
	private static final String REPAIR_OBJECTIVE = "RepairT";

	private CoreProtectAPI getCoreProtect() {
        Plugin plugin = getServer().getPluginManager().getPlugin("CoreProtect");

        // Check that CoreProtect is loaded
        if (plugin == null || !(plugin instanceof CoreProtect)) {
            return null;
        }

        // Check that the API is enabled
        CoreProtectAPI CoreProtect = ((CoreProtect) plugin).getAPI();
        if (CoreProtect.isEnabled() == false) {
            return null;
        }

        // Check that a compatible version of the API is loaded
        if (CoreProtect.APIVersion() < 9) {
            return null;
        }

        return CoreProtect;
	}
	
	private void logAnvilRemoved(Player p, Location loc){
		CoreProtectAPI api = getCoreProtect();
		String name = p.getName();
		api.logRemoval(name, loc, Material.ANVIL, null); // technically anvil has direction block data but i don't think coreprotect cares if you ignore it, the docs aren't super great
	}

	@Override
	public boolean rightClickBlockInteraction(Plugin plugin, Player player, Action action, @Nullable ItemStack item, Block block, PlayerInteractEvent event) {
		if (player == null || player.getGameMode() == GameMode.CREATIVE) {
			return true;
		} else if (player.getGameMode() == GameMode.ADVENTURE) {
			return false;
		}

		/*
		 * Make sure to only repair the item in the player's main hand. Otherwise if you
		 * sneak+right click an anvil the "item" might be the offhand item
		 */
		item = player.getInventory().getItemInMainHand();
		if (block.hasMetadata(Constants.ANVIL_CONFIRMATION_METAKEY)) {
			boolean repaired = Shattered.unshatterOneLevel(item);
			ItemMeta itemMeta = item.getItemMeta();
			if (!repaired
				&& itemMeta instanceof Damageable damageable
				&& damageable.getDamage() > 0
				&& !ItemStatUtils.hasEnchantment(item, EnchantmentType.CURSE_OF_IRREPARIBILITY)) {
				repaired = true;
				damageable.setDamage(0);
				item.setItemMeta(damageable);
			}

			if (repaired) {
				Location loc = block.getLocation().add(0.5, 0.5, 0.5);
				World world = loc.getWorld();
				world.playSound(loc, Sound.BLOCK_ANVIL_DESTROY, SoundCategory.BLOCKS, 1.0f, 1.0f);
				new BukkitRunnable() {
					int mTicks = 0;
					final Location mParticleLoc = block.getLocation().add(0.5, 1.1, 0.5);

					@Override
					public void run() {
						mTicks++;
						if (mTicks >= 3) {
							this.cancel();
							world.spawnParticle(Particle.BLOCK_DUST, mParticleLoc.subtract(0, 0.6, 0), 60, 0.3, 0.3, 0.3, 1.2F, Material.ANVIL.createBlockData());
							world.playSound(mParticleLoc, Sound.BLOCK_STONE_BREAK, SoundCategory.BLOCKS, 1, 0.75f);
							world.playSound(mParticleLoc, Sound.BLOCK_STONE_BREAK, SoundCategory.BLOCKS, 1, 0.75f);
						} else {
							world.spawnParticle(Particle.BLOCK_DUST, mParticleLoc, 10, 0.15, 0.15, 0.15, 0.35F, Material.ANVIL.createBlockData());
						}
					}

				}.runTaskTimer(plugin, 0, 7);
				block.setType(Material.AIR);
				logAnvilRemoved(player, block.getLocation());
				block.removeMetadata(Constants.ANVIL_CONFIRMATION_METAKEY, plugin);
				int repCount = ScoreboardUtils.getScoreboardValue(player, REPAIR_OBJECTIVE).orElse(0) + 1;
				ScoreboardUtils.setScoreboardValue(player, REPAIR_OBJECTIVE, repCount);

				NmsUtils.getVersionAdapter().runConsoleCommandSilently("execute as " + player.getName() + " run function monumenta:mechanisms/item_repair/grant_repair_advancement");
			}
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
						if (block.getType() == Material.AIR || !block.hasMetadata(Constants.ANVIL_CONFIRMATION_METAKEY)) {
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

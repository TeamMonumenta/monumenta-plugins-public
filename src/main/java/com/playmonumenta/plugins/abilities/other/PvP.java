package com.playmonumenta.plugins.abilities.other;

import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.PotionUtils;

/*
 * This is a utility "ability" that makes it easy to see whether a player has PvP enabled or not
 */
public class PvP extends Ability {

	public PvP(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
	}

	@Override
	public void PlayerDeathEvent(PlayerDeathEvent event) {
		Player player = mPlayer;
		if (player.getKiller() != null) {
			event.setReviveHealth(player.getMaxHealth());
			event.setCancelled(true);
			player.setGameMode(GameMode.SPECTATOR);
			player.sendTitle(ChatColor.RED + "Respawning...", ChatColor.GREEN + "In 3 seconds...", 10, 20 * 2, 10);
			PotionUtils.clearNegatives(mPlugin, player);
			ItemStack[] inv = player.getInventory().getContents();
			ItemStack[] armor = player.getInventory().getArmorContents();
			new BukkitRunnable() {
				Location loc = player.getLocation();
				@Override
				public void run() {
					player.teleport(loc);
					for (ItemStack item : inv) {
						if (item != null && item.getType().getMaxDurability() > 0) {
							item.setDurability((short) 0);
						}
					}
					for (ItemStack item : armor) {
						if (item != null && item.getType().getMaxDurability() > 0) {
							item.setDurability((short) 0);
						}
					}
					player.setGameMode(GameMode.SURVIVAL);
					player.getInventory().setContents(inv);
					player.getInventory().setArmorContents(armor);
					player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 3, 10));

				}

			}.runTaskLater(mPlugin, 20 * 2);
		}
	}

	@Override
	public boolean canUse(Player player) {
		return player.getScoreboardTags().contains("pvp");
	}
}

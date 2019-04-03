package com.playmonumenta.plugins.abilities.other;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SplashPotion;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.InventoryUtils;

/* TODO:
 * There is no reason this should be a player ability - it doesn't need a unique object per player
 *
 * It's still the most convenient place to put it for now, but it should eventually be refactored
 * into something like EntityListener
 */
public class CluckingPotions extends Ability {
	public CluckingPotions(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
	}

	@Override
	public boolean canUse(Player player) {
		return true;
	}

	@Override
	public boolean PlayerThrewSplashPotionEvent(SplashPotion potion) {
		if (InventoryUtils.testForItemWithName(potion.getItem(), "Jar of Clucks")) {
			mPlugin.mProjectileEffectTimers.addEntity(potion, Particle.CLOUD);
			potion.setMetadata("CluckingPotion", new FixedMetadataValue(mPlugin, 0));
		}
		return true;
	}

	@Override
	public boolean PlayerSplashPotionEvent(Collection<LivingEntity> affectedEntities, ThrownPotion potion, PotionSplashEvent event) {
		if (potion.hasMetadata("CluckingPotion")) {
			if (affectedEntities != null && !affectedEntities.isEmpty()) {
				for (LivingEntity entity : affectedEntities) {
					entity.getLocation().getWorld().spawnParticle(Particle.EXPLOSION_LARGE, entity.getLocation(), 1, 0, 0, 0, 0);
					if (entity.getType() == EntityType.PLAYER) {
						Player player = (Player) entity;
						ItemStack[] armor = player.getInventory().getArmorContents();
						List<ItemStack> cluckingCandidates = new ArrayList<>();
						for (int i = 0; i < armor.length; i++) {
							if (armor[i] != null) {
								cluckingCandidates.add(armor[i]);
							}
						}

						if (!cluckingCandidates.isEmpty()) {
							ItemStack item = cluckingCandidates.get(mRandom.nextInt(cluckingCandidates.size()));
							ItemMeta meta = item.getItemMeta();
							List<String> lore = meta.getLore();

							List<String> newLore = new ArrayList<>();
							boolean hasClucking = false;
							for (String loreEntry : lore) {
								if (loreEntry.contains(ChatColor.GRAY + "Clucking")) {
									hasClucking = true;
								} else {
									newLore.add(loreEntry);
								}
							}
							if (!hasClucking) {
								newLore.add(ChatColor.GRAY + "Clucking");
							}
							meta.setLore(newLore);
							item.setItemMeta(meta);
						}
					}
				}
			}
		}
		return true;
	}
}

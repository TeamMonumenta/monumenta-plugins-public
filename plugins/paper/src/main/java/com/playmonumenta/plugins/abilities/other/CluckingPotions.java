package com.playmonumenta.plugins.abilities.other;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/* TODO:
 * There is no reason this should be a player ability - it doesn't need a unique object per player
 *
 * It's still the most convenient place to put it for now, but it should eventually be refactored
 * into something like EntityListener
 */
public class CluckingPotions extends Ability {
	public CluckingPotions(Plugin plugin, @Nullable Player player) {
		super(plugin, player, null);
	}

	@Override
	public boolean canUse(Player player) {
		return true;
	}

	@Override
	public boolean playerThrewSplashPotionEvent(ThrownPotion potion) {
		if (InventoryUtils.testForItemWithName(potion.getItem(), "Jar of Clucks")) {
			mPlugin.mProjectileEffectTimers.addEntity(potion, Particle.CLOUD);
			potion.setMetadata("CluckingPotion", new FixedMetadataValue(mPlugin, 0));
		}
		return true;
	}

	@Override
	public boolean playerSplashPotionEvent(Collection<LivingEntity> affectedEntities, ThrownPotion potion, PotionSplashEvent event) {
		if (potion.hasMetadata("CluckingPotion")) {
			if (affectedEntities != null && !affectedEntities.isEmpty()) {
				for (LivingEntity entity : affectedEntities) {
					entity.getLocation().getWorld().spawnParticle(Particle.EXPLOSION_LARGE, entity.getLocation(), 1, 0, 0, 0, 0);
					if (entity instanceof Player player) {
						List<ItemStack> cluckingCandidates = new ArrayList<>();
						for (ItemStack armor : player.getInventory().getArmorContents()) {
							if (armor != null) {
								cluckingCandidates.add(armor);
							}
						}

						loop:
						while (!cluckingCandidates.isEmpty()) {
							int idx = FastUtils.RANDOM.nextInt(cluckingCandidates.size());
							ItemStack item = cluckingCandidates.get(idx);
							cluckingCandidates.remove(idx);

							ItemMeta meta = item.getItemMeta();
							List<Component> lore = meta.lore();
							List<String> plainLore = ItemUtils.getPlainLore(item);

							List<Component> newLore = new ArrayList<>();
							if (plainLore != null) {
								for (int i = 0; i < lore.size(); i++) {
									String plainEntry = plainLore.get(i);
									if (plainEntry.contains("Clucking")) {
										// Already has clucking, don't touch this item
										continue loop;
									}

									newLore.add(lore.get(i));
								}
							}

							// This is an item without clucking - success!
							newLore.add(Component.text("Clucking", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
							meta.lore(newLore);
							item.setItemMeta(meta);
							ItemUtils.setPlainLore(item);
							break;
						}
					}
				}
			}
		}
		return true;
	}
}

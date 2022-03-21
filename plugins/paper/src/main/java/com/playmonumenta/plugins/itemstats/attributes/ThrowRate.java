package com.playmonumenta.plugins.itemstats.attributes;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.itemstats.Attribute;
import com.playmonumenta.plugins.itemstats.enchantments.TwoHanded;
import com.playmonumenta.plugins.listeners.DamageListener;
import com.playmonumenta.plugins.utils.ItemStatUtils.AttributeType;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class ThrowRate implements Attribute {

	@Override
	public String getName() {
		return "Throw Rate";
	}

	@Override
	public AttributeType getAttributeType() {
		return AttributeType.THROW_RATE;
	}

	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, double value, ProjectileLaunchEvent event, Projectile proj) {
		if (proj instanceof Trident trident) {
			ItemStack item = trident.getItemStack();

			//Check for Two Handed Curse.
			if (plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.TWO_HANDED) > 0) {
				if (TwoHanded.checkForOffhand(plugin, player)) {
					return;
				}
			}

			// Only run Throw Rate if the Infinity enchantment is not on the trident
			if (item.getEnchantmentLevel(Enchantment.ARROW_INFINITE) <= 0 && value > 0) {
				event.setCancelled(true);

				// Make trident unpickupable, set cooldown, damage trident based on Unbreaking enchant
				player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1, 1);
				player.setCooldown(item.getType(), (int) (20 / value));
				new BukkitRunnable() {
					@Override
					public void run() {
						player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 2.0f, 1);
					}
				}.runTaskLater(plugin, (int) (20 / value));

				// Duplicate the entity, then cancel the throw event so the trident doesn't leave inventory
				Trident newProj = NmsUtils.getVersionAdapter().duplicateEntity(trident);

				// Set a bunch of stuff that isn't caught by the entity duplication
				newProj.setShooter(player);
				DamageListener.addProjectileItemStats(newProj, player);

				newProj.setPickupStatus(PickupStatus.CREATIVE_ONLY);
				trident.setPickupStatus(PickupStatus.CREATIVE_ONLY);

				if (player.getGameMode() != GameMode.CREATIVE) {
					ItemUtils.damageItemWithUnbreaking(plugin, player, player.getInventory().getItemInMainHand(), 1, true);
				}
				AbilityManager.getManager().playerShotArrowEvent(player, newProj);
			}
		} else if (proj instanceof Snowball) {
			if (value > 0) {
				Snowball snowball = (Snowball) player.getWorld().spawnEntity(proj.getLocation(), EntityType.SNOWBALL);
				snowball.setShooter(player);
				snowball.setVelocity(proj.getVelocity());
				DamageListener.addProjectileItemStats(snowball, player);
				player.playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS, 0.5f, 0.5f);

				player.setCooldown(Material.SNOWBALL, (int)(20 / value));
				event.setCancelled(true);
			}
		}
	}
}

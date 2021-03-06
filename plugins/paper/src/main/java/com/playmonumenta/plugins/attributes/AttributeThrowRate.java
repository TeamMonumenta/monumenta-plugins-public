package com.playmonumenta.plugins.attributes;

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
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.IceAspect;
import com.playmonumenta.plugins.enchantments.Inferno;
import com.playmonumenta.plugins.enchantments.Regicide;
import com.playmonumenta.plugins.enchantments.RegionScalingDamageDealt;
import com.playmonumenta.plugins.enchantments.ThunderAspect;
import com.playmonumenta.plugins.enchantments.curses.TwoHanded;
import com.playmonumenta.plugins.enchantments.infusions.Focus;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.NmsUtils;

public class AttributeThrowRate implements BaseAttribute {
	//Trident attribute only
	private static final String PROPERTY_NAME = "Throw Rate";
	public static final String FIRE_ASPECT_META = "FireAspectLevelMetakey";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, double value, Projectile proj, ProjectileLaunchEvent event) {
		/*
		 * TODO:
		 * Since we have a generic entity cloning method now, we can
		 * get rid of the item type checks. Not going to mess with it
		 * currently since we need to hotfix the trident dupe bug.
		 */
		if (proj instanceof Trident) {
			Trident trident = (Trident) proj;
			ItemStack item = trident.getItemStack();
			ItemStack mainhand = player.getInventory().getItemInMainHand();
			ItemStack offhand = player.getInventory().getItemInOffHand();

			/*
			 * TODO:
			 * This needs to be a generic check, preferably contained within
			 * the code for the enchantment itself.
			 */
			//Check for Two Handed Curse.
			if (InventoryUtils.testForItemWithLore(mainhand, TwoHanded.PROPERTY_NAME) || InventoryUtils.testForItemWithLore(offhand, TwoHanded.PROPERTY_NAME)) {
				if (TwoHanded.checkForOffhand(player)) {
					return;
				}
			}

			// Only run Throw Rate if the Infinity enchantment is not on the trident
			if (item.getEnchantmentLevel(Enchantment.ARROW_INFINITE) <= 0 && value > 0) {
				// Make trident unpickupable, set cooldown, damage trident based on Unbreaking enchant
				player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1, 1);
				player.setCooldown(item.getType(), (int)(20 / value));
				new BukkitRunnable() {
					@Override
					public void run() {
						player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 2.0f, 1);
					}
				}.runTaskLater(plugin, (int)(20 / value));

				if (player.getGameMode() != GameMode.CREATIVE) {
					ItemUtils.damageItemWithUnbreaking(mainhand, 1, false);
				}

				// Duplicate the entity, then cancel the throw event so the trident doesn't leave inventory
				Trident newProj = NmsUtils.duplicateEntity(trident);

				// This is super jank and should be made less so. Tried a few ways and had no success
				int fireAspectLevel = player.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.FIRE_ASPECT);
				if (fireAspectLevel > 0) {
					newProj.setMetadata(FIRE_ASPECT_META, new FixedMetadataValue(plugin, fireAspectLevel));
				}
				int iceAspectLevel = plugin.mTrackingManager.mPlayers.getPlayerCustomEnchantLevel(player, IceAspect.class);
				if (iceAspectLevel > 0) {
					newProj.setMetadata(IceAspect.LEVEL_METAKEY, new FixedMetadataValue(plugin, iceAspectLevel));
				}
				int thunderAspectLevel = plugin.mTrackingManager.mPlayers.getPlayerCustomEnchantLevel(player, ThunderAspect.class);
				if (thunderAspectLevel > 0) {
					newProj.setMetadata(ThunderAspect.LEVEL_METAKEY, new FixedMetadataValue(plugin, thunderAspectLevel));
				}
				int focusLevel = plugin.mTrackingManager.mPlayers.getPlayerCustomEnchantLevel(player, Focus.class);
				if (focusLevel > 0) {
					newProj.setMetadata(Focus.LEVEL_METAKEY, new FixedMetadataValue(plugin, focusLevel));
				}
				int infernoLevel = plugin.mTrackingManager.mPlayers.getPlayerCustomEnchantLevel(player, Inferno.class);
				if (infernoLevel > 0) {
					newProj.setMetadata(Inferno.LEVEL_METAKEY, new FixedMetadataValue(plugin, infernoLevel));
				}
				int regicideLevel = plugin.mTrackingManager.mPlayers.getPlayerCustomEnchantLevel(player, Regicide.class);
				if (regicideLevel > 0) {
					newProj.setMetadata(Regicide.LEVEL_METAKEY, new FixedMetadataValue(plugin, regicideLevel));
				}

				// Set a bunch of stuff that isn't caught by the entity duplication
				newProj.setShooter(player);
				if (proj.hasMetadata(RegionScalingDamageDealt.APPLY_MULTIPLIER_METAKEY)) {
					newProj.setMetadata(RegionScalingDamageDealt.APPLY_MULTIPLIER_METAKEY, new FixedMetadataValue(plugin, null));
				}
				if (proj.hasMetadata(AttributeProjectileDamage.DAMAGE_METAKEY)) {
					newProj.setMetadata(AttributeProjectileDamage.DAMAGE_METAKEY, new FixedMetadataValue(plugin, proj.getMetadata(AttributeProjectileDamage.DAMAGE_METAKEY).get(0).asDouble()));
				}

				newProj.setPickupStatus(PickupStatus.CREATIVE_ONLY);
				trident.setPickupStatus(PickupStatus.CREATIVE_ONLY);

				event.setCancelled(true);
			}
		} else if (proj instanceof Snowball) {
			if (value > 0) {
				Snowball snowball = (Snowball) player.getWorld().spawnEntity(proj.getLocation(), EntityType.SNOWBALL);
				snowball.setShooter(player);
				snowball.setVelocity(proj.getVelocity());
				player.playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS, 0.5f, 0.5f);
				// Set projectile attributes; don't need to do speed attribute since that's only used to calculate non-critical arrow damage
				if (proj.hasMetadata(AttributeProjectileDamage.DAMAGE_METAKEY)) {
					snowball.setMetadata(AttributeProjectileDamage.DAMAGE_METAKEY, new FixedMetadataValue(plugin, proj.getMetadata(AttributeProjectileDamage.DAMAGE_METAKEY).get(0).asDouble()));
				}

				player.setCooldown(Material.SNOWBALL, (int)(20 / value));
				event.setCancelled(true);
			}
		}
	}
}

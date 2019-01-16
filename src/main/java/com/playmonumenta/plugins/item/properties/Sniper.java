package com.playmonumenta.plugins.item.properties;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.item.properties.ItemPropertyManager.ItemSlot;

public class Sniper implements ItemProperty {
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Sniper";
	private static final int DISTANCE = 8;
	private static final int DAMAGE_PER_LEVEL = 2;
	private static final String LEVEL_METAKEY = "SniperLevelMetakey";
	private static final String LOCATION_METAKEY = "SniperLocationMetakey";
	private static final EnumSet<EntityType> ALLOWED_PROJECTILES = EnumSet.of(EntityType.ARROW, EntityType.TIPPED_ARROW, EntityType.SPECTRAL_ARROW);

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND);
	}

	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, int level, Projectile proj, ProjectileLaunchEvent event) {
		if (ALLOWED_PROJECTILES.contains(proj.getType())) {
			/*
			 * TODO: Check that player doesn't have two bows with this enchant in main and offhand
			 * If they do, subtract from level the level of the lower of the two bows
			 */
			proj.setMetadata(LEVEL_METAKEY, new FixedMetadataValue(plugin, level));
			proj.setMetadata(LOCATION_METAKEY, new FixedMetadataValue(plugin, player.getLocation()));
		}
	}

	/*
	 * TODO: This needs some kind of better registration than expecting it to be called directly
	 *
	 * IF YOU COPY THIS YOU MUST PUT A CORRESPONDING CALL IN EntityDamageByEntityEvent !
	 *
	 * This works this way because you might have the enchantment when you fire the arrow, but switch to a different item before it hits
	 */
	public static void onShootAttack(Plugin plugin, Projectile proj, LivingEntity target, EntityDamageByEntityEvent event) {
		if (proj.hasMetadata(LEVEL_METAKEY) && proj.hasMetadata(LOCATION_METAKEY)) {
			int level = proj.getMetadata(LEVEL_METAKEY).get(0).asInt();
			Location loc = (Location)proj.getMetadata(LOCATION_METAKEY).get(0).value();

			if (loc.distance(target.getLocation()) > DISTANCE) {
				event.setDamage(event.getDamage() + level * DAMAGE_PER_LEVEL);

				// TODO: Fix this shitty particle! Maybe add sound?
				target.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, target.getEyeLocation(), 1, 0.1, 0.1, 0.1, 0.001);
			}
		}
	}
}

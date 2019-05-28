package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;

public class Inferno implements BaseEnchantment {
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Inferno";
	private static final String LEVEL_METAKEY = "InfernoLevelMetakey";

	private static final Map<LivingEntity, Integer> sTaggedMobs = new HashMap<LivingEntity, Integer>();
	private static BukkitRunnable sRunnable = null;
	private static final EnumSet<EntityType> ALLOWED_PROJECTILES = EnumSet.of(EntityType.ARROW, EntityType.TIPPED_ARROW, EntityType.SPECTRAL_ARROW);

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR);
	}

	@Override
	public void onAttack(Plugin plugin, Player player, int level, LivingEntity target, EntityDamageByEntityEvent event) {
		infernoTagMob(plugin, target, level);
	}

	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, int level, Projectile proj, ProjectileLaunchEvent event) {
		if (ALLOWED_PROJECTILES.contains(proj.getType())) {
			int mainHandLevel = this.getLevelFromItem(player.getInventory().getItemInMainHand());
			int offHandLevel = this.getLevelFromItem(player.getInventory().getItemInOffHand());

			if (mainHandLevel > 0 && offHandLevel > 0
			    && player.getInventory().getItemInMainHand().getType().equals(Material.BOW)
			    && player.getInventory().getItemInOffHand().getType().equals(Material.BOW)) {
				/* If we're trying to cheat by dual-wielding this enchant, subtract the lower of the two levels */
				level -= mainHandLevel < offHandLevel ? mainHandLevel : offHandLevel;
			}

			proj.setMetadata(LEVEL_METAKEY, new FixedMetadataValue(plugin, level));
		}
	}

	private static void infernoTagMob(Plugin plugin, LivingEntity target, int level) {
		/* Record this mob as being inferno tagged */
		sTaggedMobs.put(target, level);

		/* Make sure the ticking task is running that periodically validates that a mob is still alive and burning */
		if (sRunnable == null) {
			sRunnable = new BukkitRunnable() {
				public void run() {
					Iterator<Entry<LivingEntity, Integer>> infernoMobsIter = sTaggedMobs.entrySet().iterator();
					while (infernoMobsIter.hasNext()) {
						Entry<LivingEntity, Integer> entry = infernoMobsIter.next();
						LivingEntity mob = entry.getKey();

						/* Inferno wears off when the mob despawns, dies, or is extinguished */
						if (!mob.isValid() || mob.getHealth() < 0 || mob.getFireTicks() <= 0) {
							infernoMobsIter.remove();
						}
					}

					/* Don't bother ticking this when there are no mobs on it - just cancel the task */
					if (sTaggedMobs.isEmpty()) {
						this.cancel();
						sRunnable = null;
					}
				}
			};

			sRunnable.runTaskTimer(plugin, 5, 5);
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
		if (proj.hasMetadata(LEVEL_METAKEY)) {
			int level = proj.getMetadata(LEVEL_METAKEY).get(0).asInt();

			infernoTagMob(plugin, target, level);
		}
	}


	/*
	 * TODO: This needs some kind of better registration than expecting it to be called directly
	 *
	 * IF YOU COPY THIS YOU MUST PUT A CORRESPONDING CALL IN EntityDamageEvent !
	 */
	public static void onFireTick(LivingEntity mob, EntityDamageEvent event) {
		if (!sTaggedMobs.isEmpty()) {
			Integer infernoValue = sTaggedMobs.get(mob);
			if (infernoValue != null) {
				mob.getWorld().spawnParticle(Particle.FLAME, mob.getLocation().add(0, 1, 0), 11, 0.4, 0.4, 0.4, 0.05);
				event.setDamage(event.getDamage() + infernoValue);
			}
		}
	}
}

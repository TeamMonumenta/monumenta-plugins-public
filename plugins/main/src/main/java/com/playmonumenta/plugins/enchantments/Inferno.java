package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;

public class Inferno implements BaseEnchantment {

	public static class InfernoMob {
		public Player triggeredBy;
		public int level;
		public double fireResistantDamage;

		public InfernoMob(Player triggeredBy, int level) {
			this.triggeredBy = triggeredBy;
			this.level = level;
			this.fireResistantDamage = (level + 1) / 2.0;
		}
	}

	public static final String SET_FIRE_TICK_METAKEY = "FireSetOnMobTick";
	public static final String FIRE_TICK_METAKEY = "FireDamagedMobTick";
	public static final String INFERNO_TAG_METAKEY = "InfernoTagMob";
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Inferno";
	private static final String LEVEL_METAKEY = "InfernoLevelMetakey";

	private static final Map<LivingEntity, InfernoMob> sTaggedMobs = new HashMap<LivingEntity, InfernoMob>();
	private static BukkitRunnable sRunnable = null;
	private static final EnumSet<EntityType> ALLOWED_PROJECTILES = EnumSet.of(EntityType.ARROW, EntityType.TIPPED_ARROW, EntityType.SPECTRAL_ARROW);

	private static final int FIRE_RESISTANT_INFERNO_TICKS = 80;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR);
	}

	@Override
	public void onDamage(Plugin plugin, Player player, int level, LivingEntity target, EntityDamageByEntityEvent event) {
		// If the player melee attacks with a fire aspect weapon OR if the mob has the metadata
		// applied by ability fire, add the mob to inferno tracking
		if (!target.hasMetadata(INFERNO_TAG_METAKEY)) {
			target.setMetadata(INFERNO_TAG_METAKEY, new FixedMetadataValue(plugin, true));
			if (player.getInventory().getItemInMainHand().containsEnchantment(Enchantment.FIRE_ASPECT)
				&& !MetadataUtils.happenedThisTick(plugin, player, EntityUtils.PLAYER_DEALT_CUSTOM_DAMAGE_METAKEY, 0)) {
				infernoTagMob(plugin, target, level, player);
				target.setMetadata(SET_FIRE_TICK_METAKEY, new FixedMetadataValue(plugin, target.getTicksLived()));
				target.setMetadata(FIRE_TICK_METAKEY, new FixedMetadataValue(plugin, target.getTicksLived()));
			} else if (target.hasMetadata(SET_FIRE_TICK_METAKEY)) {
				infernoTagMob(plugin, target, level, player);
			}
			target.removeMetadata(INFERNO_TAG_METAKEY, plugin);
		}
	}

	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, int level, Projectile proj, ProjectileLaunchEvent event) {
		if (ALLOWED_PROJECTILES.contains(proj.getType()) && proj.getFireTicks() > 0) {
			/*
			 * You can delete this comment after viewing, but there is no longer a check for "cheating" by dual
			 * wielding inferno because there now exist proper offhands with as high a level of inferno as mainhand
			 * inferno items, so there is little (if any) benefit to dual wielding mainhand inferno items.
			 */
			proj.setMetadata(LEVEL_METAKEY, new FixedMetadataValue(plugin, level));
		}
	}

	private static void infernoTagMob(Plugin plugin, LivingEntity target, int level, Player player) {
		/* Record this mob as being inferno tagged */
		sTaggedMobs.put(target, new InfernoMob(player, level));

		/* Make sure the ticking task is running that periodically validates that a mob is still alive and burning */
		if (sRunnable == null) {
			sRunnable = new BukkitRunnable() {
				public void run() {
					Iterator<Entry<LivingEntity, InfernoMob>> infernoMobsIter = sTaggedMobs.entrySet().iterator();
					while (infernoMobsIter.hasNext()) {
						Entry<LivingEntity, InfernoMob> entry = infernoMobsIter.next();
						LivingEntity mob = entry.getKey();
						InfernoMob value = entry.getValue();
						int ticksLived = mob.getTicksLived();

						/*
						 * Inferno wears off when the mob despawns, dies, is in water, is extinguished and NOT fire resistant,
						 * or is fire resistant and has "burned" for 4 seconds.
						 */
						// TODO: Better water hitbox registration (not a huge issue because fire resistant mobs near water is a fringe case)
						if (!mob.isValid() || mob.getHealth() <= 0) {
							infernoMobsIter.remove();
						} else if (EntityUtils.isFireResistant(mob)) {
							if (mob.getLocation().getBlock().getType() == Material.WATER
								|| ticksLived - mob.getMetadata(SET_FIRE_TICK_METAKEY).get(0).asInt() >= FIRE_RESISTANT_INFERNO_TICKS) {
								infernoMobsIter.remove();
							}
						} else if (mob.getFireTicks() < 20) {
							// 20 ticks is the default value for how long a mob can be on fire without burning
							infernoMobsIter.remove();
						}

						// If the mob hasn't taken a fire tick in the past second, then give it a manual damage tick
						// This is either caused by another DoT (wither 3, usually) eating iFrames, or the mob being fire resistant
						if (ticksLived - mob.getMetadata(FIRE_TICK_METAKEY).get(0).asInt() > 20) {
							double damage = EntityUtils.isFireResistant(mob) == true ? value.fireResistantDamage : value.level;
							mob.setNoDamageTicks(0);
							mob.getWorld().spawnParticle(Particle.FLAME, mob.getLocation().add(0, 1, 0), 11, 0.4, 0.4, 0.4, 0.05);
							Vector velocity = mob.getVelocity();
							EntityUtils.damageEntity(plugin, mob, damage, value.triggeredBy);
							mob.setVelocity(velocity);
							mob.setMetadata(FIRE_TICK_METAKEY, new FixedMetadataValue(plugin, mob.getTicksLived()));
						}
					}

					/* Don't bother ticking this when there are no mobs on it - just cancel the task */
					if (sTaggedMobs.isEmpty()) {
						this.cancel();
						sRunnable = null;
					}
				}
			};

			sRunnable.runTaskTimer(plugin, 1, 1);
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
			infernoTagMob(plugin, target, level, (Player) proj.getShooter());
			target.setMetadata(SET_FIRE_TICK_METAKEY, new FixedMetadataValue(plugin, target.getTicksLived()));
			target.setMetadata(FIRE_TICK_METAKEY, new FixedMetadataValue(plugin, target.getTicksLived()));
		}
	}


	/*
	 * TODO: This needs some kind of better registration than expecting it to be called directly
	 *
	 * IF YOU COPY THIS YOU MUST PUT A CORRESPONDING CALL IN EntityDamageEvent !
	 */
	public static void onFireTick(LivingEntity mob, EntityDamageEvent event) {
		if (sTaggedMobs.containsKey(mob)) {
			Integer infernoValue = sTaggedMobs.get(mob).level;
			if (infernoValue != null) {
				mob.setMetadata(FIRE_TICK_METAKEY, new FixedMetadataValue(Plugin.getInstance(), mob.getTicksLived()));
				mob.getWorld().spawnParticle(Particle.FLAME, mob.getLocation().add(0, 1, 0), 11, 0.4, 0.4, 0.4, 0.05);
				event.setDamage(event.getDamage() + infernoValue);
			}
		}
	}
}

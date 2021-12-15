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
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.EntityUtils;

public class Inferno implements BaseEnchantment {

	public static class InfernoMob {
		public Player mTriggeredBy;
		public int mLevel;
		public double mFireResistantDamage;
		public int mFireAspectLevel;

		public InfernoMob(Player triggeredBy, int level, int fireAspectLevel) {
			mTriggeredBy = triggeredBy;
			mLevel = level;
			mFireResistantDamage = (level + 1) / 2.0;
			mFireAspectLevel = fireAspectLevel;
		}
	}

	public static final String SET_FIRE_TICK_METAKEY = "FireSetOnMobTick";
	public static final String FIRE_TICK_METAKEY = "FireDamagedMobTick";
	public static final String INFERNO_TAG_METAKEY = "InfernoTagMob";
	public static final String OLD_FIRE_TICKS_METAKEY = "MobFireTicksPreAttack";
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Inferno";
	public static final String LEVEL_METAKEY = "InfernoLevelMetakey";

	public static final Map<LivingEntity, InfernoMob> sTaggedMobs = new HashMap<LivingEntity, InfernoMob>();
	private static @Nullable BukkitRunnable sRunnable = null;
	private static final Map<LivingEntity, InfernoMob> sPendingTagMobs = new HashMap<LivingEntity, InfernoMob>();
	private static final EnumSet<EntityType> ALLOWED_PROJECTILES = EnumSet.of(EntityType.ARROW, EntityType.SPECTRAL_ARROW);

	private static final int FIRE_RESISTANT_INFERNO_TICKS = 80;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR);
	}

	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, int level, Projectile proj, ProjectileLaunchEvent event) {
		if (ALLOWED_PROJECTILES.contains(proj.getType()) && proj.getFireTicks() > 0) {
			proj.setMetadata(LEVEL_METAKEY, new FixedMetadataValue(plugin, level));
		}
	}

	@Override
	public void onDamage(Plugin plugin, Player player, int level, LivingEntity target, EntityDamageByEntityEvent event) {
		// If the player melee attacks with a fire aspect weapon OR if the mob has the metadata
		// applied by ability fire, add the mob to inferno tracking

		if (event.getCause() == DamageCause.ENTITY_ATTACK
			&& player.getInventory().getItemInMainHand().containsEnchantment(Enchantment.FIRE_ASPECT)) {
			int fireAspectLevel = player.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.FIRE_ASPECT);
			InfernoMob mob = sTaggedMobs.get(target);
			if (mob == null || mob.mLevel <= level) {
				infernoTagMob(plugin, target, level, player, "melee", fireAspectLevel);
				target.setMetadata(SET_FIRE_TICK_METAKEY, new FixedMetadataValue(plugin, target.getTicksLived()));
				target.setMetadata(FIRE_TICK_METAKEY, new FixedMetadataValue(plugin, target.getTicksLived()));
			}
		} else if (event.getCause() == DamageCause.ENTITY_ATTACK) {
			// If player has inferno levels but no fire aspect, dont do anything special
		} else if (event.getCause() == DamageCause.PROJECTILE) {
			// Happens in onShootAttack()
		} else if (target.hasMetadata(SET_FIRE_TICK_METAKEY)) {
			// TODO fire ticks when using abilities may be mismanaged atm
			// Treat magic damage as fire aspect 1
			if (!sTaggedMobs.containsKey(target) || sTaggedMobs.get(target).mLevel < level) {
				infernoTagMob(plugin, target, level, player, "magichigh", 1);
			} else if (!sTaggedMobs.containsKey(target) || sTaggedMobs.get(target).mLevel == level) {
				infernoTagMob(plugin, target, level, player, "magic", 1);
			}
		}
	}

	/*
	 * TODO: This needs some kind of better registration than expecting it to be called directly
	 *
	 * IF YOU COPY THIS YOU MUST PUT A CORRESPONDING CALL IN EntityDamageByEntityEvent !
	 *
	 * This works this way because you might have the enchantment when you fire the arrow, but switch to a different item before it hits
	 * This applies after the arrow's OnDamage()
	 * This applies for all onShootAttacks, not just Inferno shots
	 * This has to manually manage the fire ticks because the fire ticks get overwritten before this function triggers
	 */
	public static void onShootAttack(Plugin plugin, Projectile proj, LivingEntity target, EntityDamageByEntityEvent event) {
		// Treat projectiles as fire aspect 1
		if (!sTaggedMobs.containsKey(target)) {
			// New inferno case
			if (proj.hasMetadata(LEVEL_METAKEY)) {
				int level = proj.getMetadata(LEVEL_METAKEY).get(0).asInt();
				infernoTagMob(plugin, target, level, (Player) proj.getShooter(), "projectile", 1);
				target.setMetadata(SET_FIRE_TICK_METAKEY, new FixedMetadataValue(plugin, target.getTicksLived()));
				target.setMetadata(FIRE_TICK_METAKEY, new FixedMetadataValue(plugin, target.getTicksLived()));
			} else if (target.hasMetadata(OLD_FIRE_TICKS_METAKEY) && target.getMetadata(OLD_FIRE_TICKS_METAKEY).get(0).asInt() > target.getFireTicks()) {
				// Normal flame behaviour
				target.setFireTicks(target.getMetadata(OLD_FIRE_TICKS_METAKEY).get(0).asInt());
			}
		} else if (proj.hasMetadata(LEVEL_METAKEY) && sTaggedMobs.get(target).mLevel < proj.getMetadata(LEVEL_METAKEY).get(0).asInt()) {
			// Higher inferno case
			int level = proj.getMetadata(LEVEL_METAKEY).get(0).asInt();
			infernoTagMob(plugin, target, level, (Player) proj.getShooter(), "projectile", 1);
			target.setMetadata(SET_FIRE_TICK_METAKEY, new FixedMetadataValue(plugin, target.getTicksLived()));
			target.setMetadata(FIRE_TICK_METAKEY, new FixedMetadataValue(plugin, target.getTicksLived()));
		} else if (proj.hasMetadata(LEVEL_METAKEY) && sTaggedMobs.get(target).mLevel == proj.getMetadata(LEVEL_METAKEY).get(0).asInt()) {
			// Same level inferno case
			int level = proj.getMetadata(LEVEL_METAKEY).get(0).asInt();
			infernoTagMob(plugin, target, level, (Player) proj.getShooter(), "projectile", 1);
			target.setMetadata(SET_FIRE_TICK_METAKEY, new FixedMetadataValue(plugin, target.getTicksLived()));
			target.setMetadata(FIRE_TICK_METAKEY, new FixedMetadataValue(plugin, target.getTicksLived()));
			if (target.hasMetadata(OLD_FIRE_TICKS_METAKEY) && target.getMetadata(OLD_FIRE_TICKS_METAKEY).get(0).asInt() > target.getFireTicks()) {
				target.setFireTicks(target.getMetadata(OLD_FIRE_TICKS_METAKEY).get(0).asInt());
			}
		} else {
			// Lower/no inferno flame
			// Analog to potion effects, dont override higher level inferno with lower level even when longer duration fire, so just cancel the fire in that case
			if (target.hasMetadata(OLD_FIRE_TICKS_METAKEY)) {
				target.setFireTicks(target.getMetadata(OLD_FIRE_TICKS_METAKEY).get(0).asInt());
			}
		}
		if (target.hasMetadata(OLD_FIRE_TICKS_METAKEY)) {
			target.removeMetadata(OLD_FIRE_TICKS_METAKEY, plugin);
		}
	}

	public static boolean mobHasInferno(Plugin plugin, LivingEntity mob) {
		return sTaggedMobs.containsKey(mob);
	}

	public static int getMobInfernoLevel(Plugin plugin, LivingEntity mob) {
		return sTaggedMobs.containsKey(mob) ? sTaggedMobs.get(mob).mLevel : 0;
	}

	private static void infernoTagMob(Plugin plugin, LivingEntity target, int level, Player player, String type, int fireAspectLevel) {
		/*
		 * Record this mob as being inferno tagged
		 * Add to a secondary map of pending additions to prevent ConcurrentModificationException's
		 */

		sPendingTagMobs.put(target, new InfernoMob(player, level, fireAspectLevel));
		// Analog to potion effects, only track duration of highest level inferno
		if (target.getFireTicks() > 0) {
			// Fire aspect applies fire ticks after onDamage and will set the fireticks to the right amount with this change.
			// Due to ordering this differs for projectiles, which have to be managed manually in onShootAttack()
			if (type.equals("melee") || type.equals("magichigh")) {
				target.setFireTicks(1);
			}
		}

		/* Make sure the ticking task is running that periodically validates that a mob is still alive and burning */
		if (sRunnable == null) {
			sRunnable = new BukkitRunnable() {
				@Override
				public void run() {
					/* Move all the pending mobs to the actual tagged mobs list */
					if (sPendingTagMobs.size() > 0) {
						sTaggedMobs.putAll(sPendingTagMobs);
						sPendingTagMobs.clear();
					}

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
							continue;
						} else if (EntityUtils.isFireResistant(mob)) {
							if (mob.getLocation().getBlock().getType() == Material.WATER
								|| ticksLived - mob.getMetadata(SET_FIRE_TICK_METAKEY).get(0).asInt() >= FIRE_RESISTANT_INFERNO_TICKS * value.mFireAspectLevel) {
								infernoMobsIter.remove();
								continue;
							}
						} else if (mob.getFireTicks() < 20) {
							// 20 ticks is the default value for how long a mob can be on fire without burning
							infernoMobsIter.remove();
							continue;
						}

						// If the mob hasn't taken a fire tick in the past second, then give it a manual damage tick
						// This is usually caused by another DoT (wither 3, usually) eating iFrames, or the mob being fire resistant
						if (ticksLived - mob.getMetadata(FIRE_TICK_METAKEY).get(0).asInt() > 20) {
							double lastDamage = mob.getLastDamage();
							double damage = EntityUtils.isFireResistant(mob) == true ? value.mFireResistantDamage : value.mLevel;
							damage *= EntityUtils.vulnerabilityMult(mob);
							mob.setNoDamageTicks(0);
							mob.getWorld().spawnParticle(Particle.FLAME, mob.getLocation().add(0, 1, 0), 11, 0.4, 0.4, 0.4, 0.05);
							Vector velocity = mob.getVelocity();
							EntityUtils.damageEntity(plugin, mob, 1 + damage, value.mTriggeredBy, MagicType.FIRE);
							mob.setLastDamage(lastDamage + 1 + damage);
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
	 * IF YOU COPY THIS YOU MUST PUT A CORRESPONDING CALL IN EntityDamageEvent !
	 */
	public static void onFireTick(LivingEntity mob, EntityDamageEvent event) {
		if (sTaggedMobs.containsKey(mob)) {
			Integer infernoValue = sTaggedMobs.get(mob).mLevel;
			if (infernoValue != null) {
				mob.setMetadata(FIRE_TICK_METAKEY, new FixedMetadataValue(Plugin.getInstance(), mob.getTicksLived()));
				mob.getWorld().spawnParticle(Particle.FLAME, mob.getLocation().add(0, 1, 0), 11, 0.4, 0.4, 0.4, 0.05);
				event.setDamage((event.getDamage() + infernoValue) * EntityUtils.vulnerabilityMult(mob));
			}
		}
	}
}

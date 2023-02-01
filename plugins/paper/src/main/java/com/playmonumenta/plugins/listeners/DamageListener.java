package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.mage.ElementalArrows;
import com.playmonumenta.plugins.bosses.bosses.TrainingDummyBoss;
import com.playmonumenta.plugins.effects.ProjectileIframe;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager.PlayerItemStats;
import com.playmonumenta.plugins.player.activity.ActivityManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import java.util.Arrays;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.Nullable;

public class DamageListener implements Listener {

	private final Plugin mPlugin;

	private static final WeakHashMap<Projectile, PlayerItemStats> mPlayerItemStatsMap = new WeakHashMap<>();

	public DamageListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void entityDamageEvent(EntityDamageEvent event) {
		if (event instanceof EntityDamageByEntityEvent entityDamageByEntityEvent) {
			// don't allow dealing damage across worlds, no matter how (can e.g. happen via damage over time effects or delayed damage)
			if (event.getEntity().getWorld() != entityDamageByEntityEvent.getDamager().getWorld()
				    || (entityDamageByEntityEvent.getDamager() instanceof Projectile projectile
					        && projectile.getShooter() instanceof Entity shooter
					        && event.getEntity().getWorld() != shooter.getWorld())) {
				event.setCancelled(true);
				return;
			}

			if (event.getCause().equals(DamageCause.ENTITY_EXPLOSION)
				    && event.getEntity() instanceof LivingEntity le) {
				Entity damager = entityDamageByEntityEvent.getDamager();
				if (damager instanceof Creeper creeper) {
					event.setDamage(EntityUtils.calculateCreeperExplosionDamage(creeper, le, event.getDamage()));
				}
			}
			if (entityDamageByEntityEvent.getDamager() instanceof WitherSkull witherSkull
				    && witherSkull.getShooter() instanceof Wither wither) {
				event.setDamage(EntityUtils.getAttributeOrDefault(wither, Attribute.GENERIC_ATTACK_DAMAGE, event.getDamage()));
			}

			if (event.getEntity() instanceof LivingEntity mob) {
				event.setDamage(event.getDamage() * EntityUtils.vulnerabilityMult(mob));
			}

			if (entityDamageByEntityEvent.getDamager() instanceof Player player
				    && event.getCause() == DamageCause.ENTITY_SWEEP_ATTACK) {
				PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStats(player);
				double sweepingEdgeLevel = ItemStatUtils.getEnchantmentLevel(player.getInventory().getItemInMainHand(), ItemStatUtils.EnchantmentType.SWEEPING_EDGE);
				if (playerItemStats != null && sweepingEdgeLevel > 0) {
					double damage = (1 + playerItemStats.getItemStats().get(ItemStatUtils.AttributeType.ATTACK_DAMAGE_ADD.getItemStat()))
						                * playerItemStats.getItemStats().get(ItemStatUtils.AttributeType.ATTACK_DAMAGE_MULTIPLY.getItemStat());
					event.setDamage(1 + damage * (sweepingEdgeLevel / (sweepingEdgeLevel + 1)));
				} else {
					event.setDamage(1);
				}
			}
		}

		/*
		 * Puts the wrapper DamageEvent on EntityDamageEvents not caused by the
		 * plugin (DamageCause.CUSTOM), which should wrap events manually to
		 * set the correct DamageType.
		 */
		double originalDamage = event.getDamage();
		if (event.getEntity() instanceof LivingEntity le) {
			if (DamageUtils.nextEventMetadata != null) {
				DamageEvent.Metadata nextEventMetadata = DamageUtils.nextEventMetadata;
				DamageUtils.nextEventMetadata = null;
				Bukkit.getPluginManager().callEvent(new DamageEvent(event, le, nextEventMetadata));
			} else if (event.getCause() != DamageCause.CUSTOM) {
				Bukkit.getPluginManager().callEvent(new DamageEvent(event, le));
			}
		}
		// If the damage is blocked, revert to the initial damage to make sure the shield gets proper durability damage.
		// This also prevents knockback going through shields sometimes for some reason.
		// Needs to check for holding a shield since the mob's attack may have disabled it.
		if (event.getDamage(EntityDamageEvent.DamageModifier.BLOCKING) < 0
			    && event.getEntity() instanceof Player player
			    && player.getActiveItem() != null
			    && player.getActiveItem().getType() == Material.SHIELD) {
			event.setDamage(originalDamage);
		}

		// Negative damage fixes (negative damage can make mobs unkillable)
		if (event.getFinalDamage() < 0 && event.getFinalDamage() > -0.1) {
			// Small amount of negative damage - can happen as the Paper damage calculation mixes floats and doubles
			// Add the final damage to the base damage to make the calculation 0, while still damaging absorption
			// Uses Math.nextUp to prevent a small final damage value from not affecting the addition
			event.setDamage(EntityDamageEvent.DamageModifier.BASE, Math.nextUp(event.getDamage()) - event.getFinalDamage());
		}
		if (event.getDamage() < 0 || event.getFinalDamage() < 0) {
			// (Still) negative: log and fix
			mPlugin.getLogger().log(Level.WARNING,
					"Negative damage dealt! finalDamage=" + event.getFinalDamage() + ", "
							+ Arrays.stream(EntityDamageEvent.DamageModifier.values()).map(mod -> mod + "=" + event.getDamage(mod)).collect(Collectors.joining(", ")), new Exception());
			if (!(event.getEntity() instanceof Player)) { // the negative damage bug doesn't apply to players, and can cause issues with absorption making players invulnerable
				event.setDamage(0);
			}
		}

		if (!Double.isFinite(event.getDamage()) || !Double.isFinite(event.getFinalDamage())) {
			// NaN or infinite damage dealt: log and set damage to 0
			mPlugin.getLogger().log(Level.WARNING,
					"Non-finite damage dealt! finalDamage=" + event.getFinalDamage() + ", "
							+ Arrays.stream(EntityDamageEvent.DamageModifier.values()).map(mod -> mod + "=" + event.getDamage(mod)).collect(Collectors.joining(", ")), new Exception());
			event.setDamage(0);
		}

		// Damaging a dead entity can make it immortal, so prevent that
		if (!event.getEntity().isValid()) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void projectileLaunchEvent(ProjectileLaunchEvent event) {
		Projectile projectile = event.getEntity();
		ProjectileSource source = projectile.getShooter();
		if (source instanceof Player player) {
			addProjectileItemStats(projectile, player);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void damageEvent(DamageEvent event) {
		LivingEntity damagee = event.getDamagee();
		Entity damager = event.getDamager();
		LivingEntity source = event.getSource();

		// Player getting damaged
		if (damagee instanceof Player player) {
			mPlugin.mItemStatManager.onHurt(mPlugin, player, event, damager, source);
			mPlugin.mAbilityManager.onHurt(player, event, damager, source);

			if (event.getFinalDamage(true) >= player.getHealth()
				    && !event.isCancelled()) {
				mPlugin.mAbilityManager.onHurtFatal(player, event);
				mPlugin.mItemStatManager.onHurtFatal(mPlugin, player, event);
			}
		} else {
			if (source instanceof Player player) {
				// Check if projectile
				if (damager instanceof Projectile proj) {
					PlayerItemStats playerItemStats = mPlayerItemStatsMap.get(proj);
					if (playerItemStats != null) {
						mPlugin.mItemStatManager.onDamage(mPlugin, player, playerItemStats, event, damagee);
						mPlugin.mAbilityManager.onDamage(player, event, damagee);
					}
				} else {
					PlayerItemStats eventPlayerItemStats = event.getPlayerItemStats();
					if (eventPlayerItemStats != null) {
						mPlugin.mItemStatManager.onDamage(mPlugin, player, eventPlayerItemStats, event, damagee);
						mPlugin.mAbilityManager.onDamage(player, event, damagee);
					} else {
						mPlugin.mItemStatManager.onDamage(mPlugin, player, event, damagee);
						mPlugin.mAbilityManager.onDamage(player, event, damagee);
					}
				}
				// Check for activity purposes
				if (damagee.customName() != null && !damagee.getScoreboardTags().contains(TrainingDummyBoss.identityTag) && EntityUtils.isHostileMob(damagee)) {
					ActivityManager.getManager().addDamageDealt(player, Math.min(event.getDamage(), damagee.getHealth()));
				}
			}
		}
		if (!event.isLifelineCancel()) {
			mPlugin.mEffectManager.damageEvent(event);
		}

		// Reverb custom enchant needs to calculate final damage after effects are applied.
		if (source instanceof Player player) {
			PlayerItemStats eventPlayerItemStats = event.getPlayerItemStats();
			if (eventPlayerItemStats != null) {
				mPlugin.mItemStatManager.onDamageDelayed(mPlugin, player, eventPlayerItemStats, event, damagee);
			} else {
				mPlugin.mItemStatManager.onDamageDelayed(mPlugin, player, event, damagee);
			}
		}

		// Projectile Iframes rework. Need to be placed at the end in order to get final damage.
		if (source instanceof Player player
			    && (damager instanceof Projectile || ElementalArrows.isElementalArrowDamage(event))
			    && event.getType() != DamageEvent.DamageType.TRUE) {
			double damage = event.getDamage();
			// Now, set damage to 0.001 (to allow for knockback effects), and customly damage enemy using damage function.
			event.setDamage(0.001);
			ProjectileIframe projectileIframe = mPlugin.mEffectManager.getActiveEffect(damagee, ProjectileIframe.class);
			if (projectileIframe != null) {
				int duration = projectileIframe.getDuration();
				double magnitude = projectileIframe.getMagnitude();

				// If incoming damage is greater than magnitude, subtract and deal damage.
				// Otherwise, do nothing.
				if (damage > magnitude) {
					double extraDamage = damage - magnitude;
					DamageUtils.damage(player, damagee, DamageEvent.DamageType.TRUE, extraDamage, event.getAbility(), true, false);
					mPlugin.mEffectManager.addEffect(damagee, ProjectileIframe.SOURCE, new ProjectileIframe(duration, damage));
				}
			} else {
				DamageUtils.damage(player, damagee, DamageEvent.DamageType.TRUE, damage, event.getAbility(), true, false);
				mPlugin.mEffectManager.addEffect(damagee, ProjectileIframe.SOURCE, new ProjectileIframe(ProjectileIframe.IFRAME_DURATION, damage));
			}
		}
	}

	public static @Nullable PlayerItemStats getProjectileItemStats(Projectile proj) {
		return mPlayerItemStatsMap.get(proj);
	}

	public static void addProjectileItemStats(Projectile proj, Player player) {
		mPlayerItemStatsMap.put(proj, Plugin.getInstance().mItemStatManager.getPlayerItemStatsCopy(player));
	}

	public static void addProjectileItemStats(Projectile proj, PlayerItemStats playerItemStats) {
		mPlayerItemStatsMap.put(proj, playerItemStats);
	}

	public static PlayerItemStats removeProjectileItemStats(Projectile proj) {
		return mPlayerItemStatsMap.remove(proj);
	}
}

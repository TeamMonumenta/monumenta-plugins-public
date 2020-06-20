package com.playmonumenta.plugins.abilities.alchemist.harbinger;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class ScorchedEarth extends MultipleChargeAbility {

	private static final String SCORCHED_EARTH_POTION_METAKEY = "ScorchedEarthPotion";

	private static final int SCORCHED_EARTH_1_COOLDOWN = 20 * 30;
	private static final int SCORCHED_EARTH_2_COOLDOWN = 20 * 25;
	private static final int SCORCHED_EARTH_1_CHARGES = 1;
	private static final int SCORCHED_EARTH_2_CHARGES = 2;
	private static final int SCORCHED_EARTH_DURATION = 20 * 15;
	private static final int SCORCHED_EARTH_WEAKNESS_AMP = 0;
	private static final int SCORCHED_EARTH_BONUS_DAMAGE = 3;
	private static final double SCORCHED_EARTH_RADIUS = 5;
	private static final Color SCORCHED_EARTH_COLOR_LIGHT = Color.fromRGB(230, 134, 0);
	private static final Color SCORCHED_EARTH_COLOR_DARK = Color.fromRGB(140, 63, 0);

	/*
	 * I now hate myself for coming up with this skill, since jank is
	 * required unless we want to pollute the EntityListener with a
	 * bunch of zone checks and increase the event damage directly, but
	 * the basic idea here is to track mob health, and if mob health
	 * decreases and the mob in question is near a zone, then the mob
	 * must have taken damage and should be hit with an extra instance
	 * of damage.
	 *
	 * Problem comes when you have overlapping zones, zones from
	 * different players, etc. which is why we need a global tracker
	 * to resolve these issues.
	 */
	private static final Map<Location, Map.Entry<Player, Integer>> mZoneCenters = new HashMap<>();
	private static Map<LivingEntity, Double> mMobHealths = new HashMap<>();
	private static BukkitRunnable mMobHealthsTracker;

	public ScorchedEarth(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Scorched Earth", SCORCHED_EARTH_1_CHARGES, SCORCHED_EARTH_2_CHARGES);
		mInfo.mLinkedSpell = Spells.SCORCHED_EARTH;
		mInfo.mScoreboardId = "ScorchedEarth";
		mInfo.mShorthandName = "SE";
		mInfo.mDescriptions.add("Shift right click with an Alchemist Potion to deploy a 5 block radius zone that lasts 15 seconds where the potion lands. Mobs in this zone are afflicted with Weakness I and are dealt 3 extra damage whenever taking damage. Cooldown: 30s.");
		mInfo.mDescriptions.add("Cooldown reduced to 25s, and two charges of this ability can be stored at once.");
		mInfo.mCooldown = getAbilityScore() == 1 ? SCORCHED_EARTH_1_COOLDOWN : SCORCHED_EARTH_2_COOLDOWN;
		mInfo.mIgnoreCooldown = true;

		// Only one runnable ever exists for Scorched Earth - it is a global list, not tied to any individual players
		if (mMobHealthsTracker == null) {
			mMobHealthsTracker = new BukkitRunnable() {
				@Override
				public void run() {
					// Tick and remove expired zones
					Iterator<Map.Entry<Location, Map.Entry<Player, Integer>>> iter = mZoneCenters.entrySet().iterator();
					while (iter.hasNext()) {
						Map.Entry<Location, Map.Entry<Player, Integer>> entry = iter.next();
						Map.Entry<Player, Integer> timer = entry.getValue();
						if (timer.getValue() <= 0) {
							iter.remove();
						} else {
							timer.setValue(timer.getValue() - 1);

							Location loc = entry.getKey();

							mWorld.spawnParticle(Particle.SMOKE_LARGE, loc, 1, 2.1, 0.3, 2.1, 0);
							mWorld.spawnParticle(Particle.FLAME, loc, 1, 2, 0.1, 2, 0.1f);
							mWorld.spawnParticle(Particle.REDSTONE, loc, 2, 2.1, 0.3, 2.1, new Particle.DustOptions(SCORCHED_EARTH_COLOR_LIGHT, 1.5f));
							mWorld.spawnParticle(Particle.REDSTONE, loc, 2, 2.1, 0.3, 2.1, new Particle.DustOptions(SCORCHED_EARTH_COLOR_DARK, 1.5f));

							mWorld.spawnParticle(Particle.REDSTONE, loc.clone().add(5 * Math.sin(timer.getValue() % 40 / 20.0 * Math.PI), 0, 5 * Math.cos(timer.getValue() % 40 / 20.0 * Math.PI)), 1, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.25f));
							mWorld.spawnParticle(Particle.REDSTONE, loc.clone().add(5 * Math.sin((timer.getValue() % 40 / 20.0 - 1) * Math.PI), 0, 5 * Math.cos((timer.getValue() % 40 / 20.0 - 1) * Math.PI)), 1, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.25f));

							if (timer.getValue() % 4 == 0) {
								mWorld.spawnParticle(Particle.LAVA, loc, 1, 2.1, 0.1, 2.1, 0);
							}

							if (timer.getValue() % 120 == 0 && timer.getValue() > 1) {
								mWorld.playSound(loc, Sound.BLOCK_FIRE_AMBIENT, 1f, 0.5f);
							}
						}
					}

					// Get the new mob healths and record which ones need to be damaged (health decreased)
					Map<LivingEntity, Double> newMobHealths = new HashMap<>();
					Map<LivingEntity, Player> mobsToBeDamaged = new HashMap<>();
					for (Location loc : mZoneCenters.keySet()) {
						for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, SCORCHED_EARTH_RADIUS)) {
							Double oldHealth = mMobHealths.get(mob);
							if (oldHealth != null && mob.getHealth() < oldHealth) {
								// We need a way to determine "zone ownership" when dealing damage
								mobsToBeDamaged.put(mob, mZoneCenters.get(loc).getKey());
							} else {
								// We'll put the health of the mobs to be damaged in the new map after we damage them
								newMobHealths.put(mob, mob.getHealth());
							}

							PotionUtils.applyPotion(mPlayer, mob,
									new PotionEffect(PotionEffectType.WEAKNESS, 30, SCORCHED_EARTH_WEAKNESS_AMP, false, true));
						}
					}

					// Damage the mobs
					for (Map.Entry<LivingEntity, Player> entry : mobsToBeDamaged.entrySet()) {
						LivingEntity mob = entry.getKey();
						mWorld.spawnParticle(Particle.FLAME, mob.getLocation().clone().add(0, 1, 0), 5, 0.25, 0.5, 0.25, 0.05);
						mWorld.spawnParticle(Particle.REDSTONE, mob.getLocation().clone().add(0, 1, 0), 15, 0.35, 0.5, 0.35, new Particle.DustOptions(SCORCHED_EARTH_COLOR_DARK, 1.0f));
						mWorld.spawnParticle(Particle.LAVA, mob.getLocation().clone().add(0, 1, 0), 3, 0.25, 0.5, 0.25, 0);
						mob.setNoDamageTicks(0);
						Vector velocity = mob.getVelocity();
						EntityUtils.damageEntity(mPlugin, mob, SCORCHED_EARTH_BONUS_DAMAGE, entry.getValue(), MagicType.ALCHEMY, true, mInfo.mLinkedSpell);
						mob.setVelocity(velocity);
						newMobHealths.put(mob, mob.getHealth());
					}

					// Replace the old health map
					mMobHealths = newMobHealths;
				}
			};

			mMobHealthsTracker.runTaskTimer(mPlugin, 0, 1);
		}
	}

	@Override
	public boolean playerThrewSplashPotionEvent(ThrownPotion potion) {
		if (mPlayer.isSneaking() && InventoryUtils.testForItemWithName(mPlayer.getInventory().getItemInMainHand(), "Alchemist's Potion")) {
			if (consumeCharge()) {
				potion.setMetadata(SCORCHED_EARTH_POTION_METAKEY, new FixedMetadataValue(mPlugin, null));
			}
		}

		return true;
	}

	@Override
	public boolean playerSplashPotionEvent(Collection<LivingEntity> affectedEntities, ThrownPotion potion, PotionSplashEvent event) {
		if (potion.hasMetadata(SCORCHED_EARTH_POTION_METAKEY)) {
			Location loc = potion.getLocation();
			mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc, 50, 2.1, 0.5, 2.1, 0.1);
			mWorld.spawnParticle(Particle.SMOKE_LARGE, loc, 15, 2.1, 0.5, 2.1, 0);
			mWorld.spawnParticle(Particle.REDSTONE, loc, 20, 2.1, 0.5, 2.1, new Particle.DustOptions(SCORCHED_EARTH_COLOR_DARK, 2.0f));
			mWorld.spawnParticle(Particle.FLAME, loc, 30, 2.1, 0.5, 2.1, 0.1);
			mWorld.spawnParticle(Particle.LAVA, loc, 25, 1.5, 0.5, 1.5, 0);

			mWorld.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 1f, 0.5f);
			mWorld.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1f, 0.5f);
			mWorld.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.5f, 1.5f);

			mZoneCenters.put(loc, new AbstractMap.SimpleEntry<>(mPlayer, SCORCHED_EARTH_DURATION));
		}

		return true;
	}

}

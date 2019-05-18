package com.playmonumenta.plugins.abilities.mage;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

public class Spellshock extends Ability {
	public static class SpellShockedMob {
		public LivingEntity mob;
		public int ticksLeft;
		public Player initiator;
		public boolean triggered = false;

		public SpellShockedMob(LivingEntity inMob, int ticks, Player inInitiator) {
			mob = inMob;
			ticksLeft = ticks;
			initiator = inInitiator;
		}
	}

	private static final int SPELL_SHOCK_DURATION = 6 * 20;
	private static final int SPELL_SHOCK_TEST_PERIOD = 2;
	private static final int SPELL_SHOCK_DEATH_RADIUS = 3;
	private static final int SPELL_SHOCK_DEATH_DAMAGE = 4;
	private static final int SPELL_SHOCK_SPELL_RADIUS = 4;
	private static final int SPELL_SHOCK_SPELL_DAMAGE = 4;
	private static final int SPELL_SHOCK_SPEED_DURATION = 20 * 6;
	private static final int SPELL_SHOCK_SPEED_AMPLIFIER = 0;
	private static final int SPELL_SHOCK_STAGGER_DURATION = (int)(0.6 * 20);
	private static final int SPELL_SHOCK_VULN_DURATION = 4 * 20;
	private static final int SPELL_SHOCK_VULN_AMPLIFIER = 3; // 20%
	private static final String SPELL_SHOCK_SCOREBOARD = "SpellShock";
	private static final Particle.DustOptions SPELL_SHOCK_COLOR = new Particle.DustOptions(Color.fromRGB(220, 147, 249), 1.0f);

	private static final Map<UUID, SpellShockedMob>mSpellShockedMobs = new HashMap<UUID, SpellShockedMob>();
	private static BukkitRunnable mRunnable = null;

	public Spellshock(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = SPELL_SHOCK_SCOREBOARD;

		/*
		 * Only one runnable ever exists for spellshock - it is a global list, not tied to any individual players
		 * At least one player must be a mage for this to start running. Once started, it runs forever.
		 */
		if (mRunnable == null || mRunnable.isCancelled()) {
			// SpellShock task to process tagged mobs
			mRunnable = new BukkitRunnable() {
				/*
				 * If the player has level 2 spellshock (which chain hits), spellDamageMob() is called
				 * on nearby mobs. The problem is, that also triggers spellshock which can remove them
				 * from mSpellShockedMobs. This causes a concurrent modification exception, since that
				 * removes random other elements during iteration
				 *
				 * To work around this, instead of calling spellDamageMob() immediately while iterating,
				 * instead these mobs are put on this list and then it is iterated after processing
				 * the mSpellShockedMobs.
				 *
				 * Note: Mobs in mShockedPending can be both mobs with spellshock or mobs without it -
				 * it is a collection of mobs that were hit by the spell shock flare, which might or
				 * might not actually have static on them
				 *
				 * This mapping is UUID of mob hit by static discharging on nearby mobs, and the player
				 * that originally put that static there (to attribute the damage)
				 */
				private final Map<LivingEntity, Player>mShockedPending = new HashMap<LivingEntity, Player>();

				@Override
				public void run() {
					/* Iterators are needed here so you can remove during iteration if needed */
					Iterator<Map.Entry<UUID, SpellShockedMob>>it = mSpellShockedMobs.entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry<UUID, SpellShockedMob> entry = it.next();
						SpellShockedMob shocked = entry.getValue();

						Location loc = shocked.mob.getLocation().add(0, 1, 0);
						mWorld.spawnParticle(Particle.SPELL_WITCH, loc, 3, 0.2, 0.6, 0.2, 1);
						mWorld.spawnParticle(Particle.REDSTONE, loc, 4, 0.3, 0.6, 0.3, SPELL_SHOCK_COLOR);

						if (!shocked.triggered && (shocked.mob.isDead() || shocked.mob.getHealth() <= 0)) {
							// Mob has died - trigger effects
							int spellShock = ScoreboardUtils.getScoreboardValue(shocked.initiator, SPELL_SHOCK_SCOREBOARD);
							mWorld.spawnParticle(Particle.SPELL_WITCH, loc, 50, 1, 1, 1, 0.001);
							mWorld.spawnParticle(Particle.CRIT_MAGIC, loc, 100, 1, 1, 1, 0.25);
							world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 1.0f, 2.0f);
							for (LivingEntity nearbyMob : EntityUtils.getNearbyMobs(shocked.mob.getLocation(), SPELL_SHOCK_DEATH_RADIUS)) {
								if (spellShock > 1) {
									/*
									 * This might chain if calling spellDamageMob() directly - so defer that until after
									 * this iteration is complete
									 */
									mShockedPending.put(nearbyMob, shocked.initiator);
								} else {
									EntityUtils.damageEntity(plugin, nearbyMob, SPELL_SHOCK_SPELL_DAMAGE, shocked.initiator, MagicType.NONE, false);
								}

								nearbyMob.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, SPELL_SHOCK_VULN_DURATION,
								                                           SPELL_SHOCK_VULN_AMPLIFIER, false, true));
							}

							it.remove();
							continue;
						}

						shocked.ticksLeft -= SPELL_SHOCK_TEST_PERIOD;
						if (shocked.ticksLeft <= 0) {
							it.remove();
							continue;
						}
					}

					for (Map.Entry<LivingEntity, Player> pending : mShockedPending.entrySet()) {
						spellDamageMob(plugin, pending.getKey(), SPELL_SHOCK_DEATH_DAMAGE, pending.getValue(), null);
					}
					mShockedPending.clear();
				}
			};

			mRunnable.runTaskTimer(plugin, 1, SPELL_SHOCK_TEST_PERIOD);
		}
	}


	public static void addStaticToMob(LivingEntity mob, Player player) {
		mSpellShockedMobs.put(mob.getUniqueId(),
		                      new SpellShockedMob(mob, SPELL_SHOCK_DURATION, player));
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK && event.getEntity() instanceof LivingEntity) {
			addStaticToMob((LivingEntity)event.getEntity(), mPlayer);
		}
		return true;
	}

	@Override
	public boolean runCheck() {
		return InventoryUtils.isWandItem(mPlayer.getInventory().getItemInMainHand());
	}

	public static void spellDamageMob(Plugin plugin, LivingEntity mob, float dmg, Player player, MagicType type) {
		SpellShockedMob shocked = mSpellShockedMobs.get(mob.getUniqueId());
		if (shocked != null) {
			// Hit a shocked mob with a real spell - extra damage

			int spellShock = ScoreboardUtils.getScoreboardValue(player, SPELL_SHOCK_SCOREBOARD);
			if (spellShock > 1 && (!mob.isDead() || mob.getHealth() > 0)) {
				plugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF,
				                                new PotionEffect(PotionEffectType.SPEED,
				                                                 SPELL_SHOCK_SPEED_DURATION,
				                                                 SPELL_SHOCK_SPEED_AMPLIFIER, true, true));
			}

			// Consume the "charge"
			mSpellShockedMobs.remove(mob.getUniqueId());
			shocked.triggered = true;
			Location loc = shocked.mob.getLocation().add(0, 1, 0);
			World world = loc.getWorld();
			world.spawnParticle(Particle.SPELL_WITCH, loc, 100, 1, 1, 1, 0.001);
			world.spawnParticle(Particle.CRIT_MAGIC, loc, 75, 1, 1, 1, 0.25);
			world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 1.0f, 2.5f);
			world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 1.0f, 2.0f);
			world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 1.0f, 1.5f);
			for (LivingEntity nearbyMob : EntityUtils.getNearbyMobs(shocked.mob.getLocation(), SPELL_SHOCK_SPELL_RADIUS, player)) {
				// Only damage hostile mobs and specifically not the mob originally hit
				if (nearbyMob != mob) {
					if (spellShock > 1) {
						spellDamageMob(plugin, nearbyMob, SPELL_SHOCK_SPELL_DAMAGE, player, type);
					} else {
						EntityUtils.damageEntity(plugin, nearbyMob, SPELL_SHOCK_SPELL_DAMAGE, player, type, false);
					}

					PotionUtils.applyPotion(player, nearbyMob, new PotionEffect(PotionEffectType.UNLUCK, SPELL_SHOCK_VULN_DURATION,
					                                                            SPELL_SHOCK_VULN_AMPLIFIER, false, true));
				}
			}

			dmg += SPELL_SHOCK_SPELL_DAMAGE;

			// Make sure to apply vulnerability after damage
			if (!EntityUtils.isBoss(mob) && !(mob instanceof Player)) {
				EntityUtils.applyStun(plugin, SPELL_SHOCK_STAGGER_DURATION, mob);
			}
			PotionUtils.applyPotion(player, mob, new PotionEffect(PotionEffectType.UNLUCK, SPELL_SHOCK_VULN_DURATION,
			                                                      SPELL_SHOCK_VULN_AMPLIFIER, false, true));
		}

		// Apply damage to the hit mob all in one shot
		if (dmg > 0) {
			EntityUtils.damageEntity(plugin, mob, dmg, player, type);
		}
	}
}

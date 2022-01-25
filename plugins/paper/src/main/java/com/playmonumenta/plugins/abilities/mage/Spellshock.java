package com.playmonumenta.plugins.abilities.mage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NavigableSet;
import java.util.Set;

import javax.annotation.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.mage.elementalist.Blizzard;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.effects.SpellShockStatic;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.scriptedquests.utils.MetadataUtils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;



public class Spellshock extends Ability {

	public static final String NAME = "Spellshock";
	public static final ClassAbility ABILITY = ClassAbility.SPELLSHOCK;
	public static final String DAMAGED_THIS_TICK_METAKEY = "SpellShockDamagedThisTick";
	public static final String SPELL_SHOCK_STATIC_EFFECT_NAME = "SpellShockStaticEffect";
	public static final String PERCENT_SPEED_EFFECT_NAME = "SpellShockPercentSpeedEffect";
	public static final String PERCENT_SLOW_EFFECT_NAME = "SpellShockPercentSlowEffect";

	public static final float DAMAGE_1 = 0.25f;
	public static final float DAMAGE_2 = 0.35f;
	public static final float MELEE_BONUS = 0.15f;
	public static final int SIZE = 3;
	public static final double SPEED_MULTIPLIER = 0.2;
	public static final int DURATION_TICKS = 6 * 20;
	public static final int SLOW_DURATION = 10;
	public static final double SLOW_MULTIPLIER = -0.3;

	private final float mLevelDamage;

	public Spellshock(Plugin plugin, @Nullable Player player) {
		super(plugin, player, NAME);
		mInfo.mLinkedSpell = ABILITY;

		mInfo.mScoreboardId = "SpellShock";
		mInfo.mShorthandName = "SS";
		mInfo.mDescriptions.add("Hitting an enemy with a spell inflicts static for 6 seconds. If an enemy with static is hit by another spell, a spellshock centered on the enemy deals 25% of the triggering spell's damage to all mobs in a 3 block radius. Spellshock can cause a chain reaction on enemies with static. An enemy can only be hit by a spellshock once per tick. If a static mob is struck by a melee attack, it takes 15% more damage on the hit and is slowed by 30% for 0.5 seconds, clearing the static.");
		mInfo.mDescriptions.add("Damage is increased to 35% for spells and 20% for melee. Additionally, gain +20% speed for 2 seconds whenever a spellshock is triggered.");
		mDisplayItem = new ItemStack(Material.GLOWSTONE_DUST, 1);

		mLevelDamage = getAbilityScore() == 2 ? DAMAGE_2 : DAMAGE_1;
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity enemy) {
		if (mPlayer == null) {
			return;
		}

		if (event.getType() == DamageType.MELEE) {
			NavigableSet<Effect> effectGroupOriginal = mPlugin.mEffectManager.getEffects(enemy, SPELL_SHOCK_STATIC_EFFECT_NAME);
			if (effectGroupOriginal != null) {
				event.setDamage(event.getDamage() * (1 + MELEE_BONUS));
				mPlugin.mEffectManager.addEffect(enemy, PERCENT_SLOW_EFFECT_NAME, new PercentSpeed(SLOW_DURATION, SLOW_MULTIPLIER, PERCENT_SLOW_EFFECT_NAME));
				for (Effect e : effectGroupOriginal) {
					e.clearEffect();
				}
			}
		} else if (event.getAbility() != null && event.getAbility() != Blizzard.ABILITY && event.getAbility() != ArcaneStrike.ABILITY) {
			// Check if the mob has static, and trigger it if possible; otherwise, apply/refresh it
			NavigableSet<Effect> effectGroupOriginal = mPlugin.mEffectManager.getEffects(enemy, SPELL_SHOCK_STATIC_EFFECT_NAME);
			if (effectGroupOriginal != null) {
				SpellShockStatic effectOriginal = (SpellShockStatic) effectGroupOriginal.last();
				// We don't directly remove the effect, because we don't want mobs with static to immediately be re-applied with it, so set flags instead
				if (!effectOriginal.isTriggered()) {
					effectOriginal.trigger();

					if (getAbilityScore() > 1) {
						mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(DURATION_TICKS, SPEED_MULTIPLIER, PERCENT_SPEED_EFFECT_NAME));
					}

					Location loc = enemy.getLocation().add(0, 1, 0);
					World world = mPlayer.getWorld();
					world.spawnParticle(Particle.SPELL_WITCH, loc, 60, 1, 1, 1, 0.001);
					world.spawnParticle(Particle.CRIT_MAGIC, loc, 45, 1, 1, 1, 0.25);
					world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 0.75f, 2.5f);
					world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 0.75f, 2.0f);
					world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 0.75f, 1.5f);

					// Grab all realistically possible nearby mobs for simplicity, use Set for fast removal
					Set<LivingEntity> nearbyMobs = new HashSet<LivingEntity>(EntityUtils.getNearbyMobs(enemy.getLocation(), 32));
					List<LivingEntity> triggeredMobs = new ArrayList<LivingEntity>();
					triggeredMobs.add(enemy);

					/*
					 * Loop through triggeredMobs, and check distances to each in nearbyMobs. If in range,
					 * deal damage. If the mob can be triggered, trigger it, adding it before the iteration
					 * cursor for triggeredMobs. Remove the mob from nearbyMobs. When we finish iterating
					 * through nearbyMobs, remove the triggered mob from triggeredMobs.
					 *
					 * Once we reach the end of the iteration for triggeredMobs, create another iterator
					 * to catch any newly triggered mobs, and repeat until there are no more newly triggered
					 * mobs.
					 */
					while (!triggeredMobs.isEmpty()) {
						ListIterator<LivingEntity> triggeredMobsIter = triggeredMobs.listIterator();
						while (triggeredMobsIter.hasNext()) {
							LivingEntity triggeredMob = triggeredMobsIter.next();

							Iterator<LivingEntity> nearbyMobsIter = nearbyMobs.iterator();
							while (nearbyMobsIter.hasNext()) {
								LivingEntity nearbyMob = nearbyMobsIter.next();
								if (nearbyMob.getLocation().distanceSquared(triggeredMob.getLocation()) < (SIZE * SIZE)) {
									// Only damage a mob once per tick
									if (MetadataUtils.checkOnceThisTick(mPlugin, nearbyMob, DAMAGED_THIS_TICK_METAKEY)) {
										DamageUtils.damage(mPlayer, nearbyMob, DamageType.OTHER, event.getDamage() * mLevelDamage, mInfo.mLinkedSpell, true);
									}

									NavigableSet<Effect> effectGroup = mPlugin.mEffectManager.getEffects(enemy, SPELL_SHOCK_STATIC_EFFECT_NAME);
									if (effectGroup != null) {
										SpellShockStatic effect = (SpellShockStatic) effectGroup.last();
										if (!effect.isTriggered()) {
											effect.trigger();
											triggeredMobsIter.add(nearbyMob);
										}
									}

									nearbyMobsIter.remove();
								}
							}

							triggeredMobsIter.remove();
						}
					}
				}
			}
			if (event.getAbility() != null && event.getAbility() != mInfo.mLinkedSpell) {
				NavigableSet<Effect> effectGroup = mPlugin.mEffectManager.getEffects(enemy, SPELL_SHOCK_STATIC_EFFECT_NAME);
				if (effectGroup != null) {
					SpellShockStatic effect = (SpellShockStatic) effectGroup.last();
					if (!effect.isTriggered()) {
						effect.setDuration(DURATION_TICKS);
					}
				} else {
					mPlugin.mEffectManager.addEffect(enemy, SPELL_SHOCK_STATIC_EFFECT_NAME, new SpellShockStatic(DURATION_TICKS));
				}
			}
		}
	}

	@Override
	public boolean runCheck() {
		return mPlayer != null && ItemUtils.isWand(mPlayer.getInventory().getItemInMainHand());
	}
}

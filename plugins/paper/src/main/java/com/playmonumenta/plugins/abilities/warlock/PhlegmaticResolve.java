package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warlock.PhlegmaticResolveCS;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.gallery.GalleryManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.Nullable;

public class PhlegmaticResolve extends Ability {

	private static final String PERCENT_DAMAGE_RESIST_EFFECT_NAME = "PhlegmaticPercentDamageResistEffect";
	private static final String KNOCKBACK_RESIST_EFFECT_NAME = "PhlegmaticPercentKnockbackResistEffect";
	private static final double PERCENT_DAMAGE_RESIST_1 = -0.03;
	private static final double PERCENT_DAMAGE_RESIST_2 = -0.05;
	private static final double PERCENT_KNOCKBACK_RESIST = 0.1;
	private static final int ABILITY_CAP = 3;
	private static final int RADIUS = 7;
	private static final double ALLY_MODIFIER = 0.33;
	private static final double ENHANCEMENT_DAMAGE = 0.05;
	private static final int ENHANCE_RADIUS = 3;

	private final double mPercentDamageResist;
	private final double mKBR;
	private final int mAbilityCap;
	private final double mAllyModifier;
	private final double mRadius;
	private final double mEnhanceRadius;
	private final double[] mEnhancementDamageSpread = {0, 0, 0};
	private double mLastMaxDamage = 0;
	private double mLastPreMitigationDamage = 0;
	private int mLastPlayedSoundTick = 0;
	private boolean mDamagedLastWindow = false;
	private final PhlegmaticResolveCS mCosmetic;

	public static final String CHARM_RESIST = "Phlegmatic Resolve Resistance";
	public static final String CHARM_KBR = "Phlegmatic Resolve Knockback Resistance";
	public static final String CHARM_ABILITY_CAP = "Phlegmatic Resolve Ability Cap";
	public static final String CHARM_ALLY = "Phlegmatic Resolve Ally Modifier";
	public static final String CHARM_RANGE = "Phlegmatic Resolve Radius";
	public static final String CHARM_ENHANCE_DAMAGE = "Phlegmatic Resolve Enhancement Damage";
	public static final String CHARM_ENHANCE_RADIUS = "Phlegmatic Resolve Enhancement Radius";

	public static final AbilityInfo<PhlegmaticResolve> INFO =
		new AbilityInfo<>(PhlegmaticResolve.class, "Phlegmatic Resolve", PhlegmaticResolve::new)
			.scoreboardId("Phlegmatic")
			.shorthandName("PR")
			.descriptions(
				String.format("For each ability on cooldown, gain +%s%% Resistance and +%s Knockback Resistance, capped at %s abilities.",
					StringUtils.multiplierToPercentage(-PERCENT_DAMAGE_RESIST_1),
					StringUtils.formatDecimal(PERCENT_KNOCKBACK_RESIST * 10),
					StringUtils.formatDecimal(ABILITY_CAP)
				),
				String.format("Increase to +%s%% Resistance per ability on cooldown, and players within %s blocks are given %s%% of your bonuses. (Does not stack with multiple Warlocks.)",
					StringUtils.multiplierToPercentage(-PERCENT_DAMAGE_RESIST_2),
					StringUtils.formatDecimal(RADIUS),
					StringUtils.multiplierToPercentage(ALLY_MODIFIER)
				),
				String.format("All non-ailment damage taken is instead converted into a short Damage-over-Time effect. " +
						"A third of the damage stored is dealt every second for 3s. Each time this stored damage is dealt, deal %s%% of the initial damage to all mobs in a %s block radius.",
					StringUtils.multiplierToPercentage(ENHANCEMENT_DAMAGE),
					StringUtils.formatDecimal(ENHANCE_RADIUS)
				))
			.simpleDescription("Gain resistance and knockback resistance for each ability on cooldown.")
			.linkedSpell(ClassAbility.PHLEGMATIC_RESOLVE)
			.displayItem(Material.SHIELD);

	public PhlegmaticResolve(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mPercentDamageResist = (isLevelOne() ? PERCENT_DAMAGE_RESIST_1 : PERCENT_DAMAGE_RESIST_2) - CharmManager.getLevelPercentDecimal(player, CHARM_RESIST);
		mKBR = CharmManager.getLevel(player, CHARM_KBR) / 10 + PERCENT_KNOCKBACK_RESIST;
		mAbilityCap = ABILITY_CAP + (int) CharmManager.getLevel(player, CHARM_ABILITY_CAP);
		mAllyModifier = ALLY_MODIFIER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ALLY);
		mRadius = CharmManager.getRadius(player, CHARM_RANGE, RADIUS);
		mEnhanceRadius = CharmManager.getRadius(player, CHARM_ENHANCE_RADIUS, ENHANCE_RADIUS);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new PhlegmaticResolveCS());
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		//Triggers four times a second

		if (isEnhanced()) {
			if (twoHertz && mDamagedLastWindow) {
				mLastMaxDamage = 0;
			}

			if (oneSecond) {
				// This follows how reckless swing does it.
				if (mEnhancementDamageSpread[0] > 0.0) {
					if (mPlayer.getHealth() + mPlayer.getAbsorptionAmount() <= mEnhancementDamageSpread[0]) {
						mPlayer.damage(9001); // perish
					} else {
						if (mPlayer.getAbsorptionAmount() > 0) {
							double diff = mPlayer.getAbsorptionAmount() - mEnhancementDamageSpread[0];

							if (diff > 0) {
								mPlayer.setAbsorptionAmount(diff);
								mEnhancementDamageSpread[0] = 0;
							} else {
								mEnhancementDamageSpread[0] -= mPlayer.getAbsorptionAmount();
								mPlayer.setAbsorptionAmount(0);
							}
						}

						mPlayer.setHealth(Math.min(mPlayer.getHealth() - mEnhancementDamageSpread[0], EntityUtils.getMaxHealth(mPlayer)));
						mPlayer.damage(0);
						mDamagedLastWindow = true;
					}

					mCosmetic.enhanceDamageTick(mPlayer, mEnhanceRadius, mEnhancementDamageSpread);

					// AoE Effect
					double damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ENHANCE_DAMAGE, mLastPreMitigationDamage * ENHANCEMENT_DAMAGE);
					Hitbox hitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mPlayer), mEnhanceRadius);
					for (LivingEntity mob : hitbox.getHitMobs()) {
						DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.OTHER, damage, mInfo.getLinkedSpell(), true);
						mCosmetic.enhanceDamageMob(mPlayer, mob);
					}

					// Shift the array forward
					mEnhancementDamageSpread[0] = mEnhancementDamageSpread[1];
					mEnhancementDamageSpread[1] = mEnhancementDamageSpread[2];
					mEnhancementDamageSpread[2] = 0;
				} else {
					mDamagedLastWindow = false;
				}

				// If player has died, reset the array
				if (mPlayer.isDead() || !mPlayer.isOnline()) {
					mEnhancementDamageSpread[0] = 0;
					mEnhancementDamageSpread[1] = 0;
					mEnhancementDamageSpread[2] = 0;
				}
			}
		}

		int cooldowns = 0;
		for (Integer ability : mPlugin.mTimers.getCooldowns(mPlayer.getUniqueId())) {
			if (ability > 0) {
				cooldowns++;
			}
		}
		if (cooldowns == 0) {
			return;
		}
		cooldowns = Math.min(cooldowns, mAbilityCap);

		mCosmetic.periodicTrigger(mPlayer, mPlayer, cooldowns);
		mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_RESIST_EFFECT_NAME, new PercentDamageReceived(20, mPercentDamageResist * cooldowns).displaysTime(false));
		mPlugin.mEffectManager.addEffect(mPlayer, KNOCKBACK_RESIST_EFFECT_NAME, new PercentKnockbackResist(20, mKBR * cooldowns, KNOCKBACK_RESIST_EFFECT_NAME).displaysTime(false));
		if (isLevelTwo()) {
			for (Player p : PlayerUtils.otherPlayersInRange(mPlayer, mRadius, true)) {
				mCosmetic.periodicTrigger(mPlayer, p, cooldowns);
				mPlugin.mEffectManager.addEffect(p, PERCENT_DAMAGE_RESIST_EFFECT_NAME, new PercentDamageReceived(20, mPercentDamageResist * cooldowns * mAllyModifier).displaysTime(false));
				mPlugin.mEffectManager.addEffect(p, KNOCKBACK_RESIST_EFFECT_NAME, new PercentKnockbackResist(20, mKBR * cooldowns * mAllyModifier, KNOCKBACK_RESIST_EFFECT_NAME).displaysTime(false));
			}
		}
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (isEnhanced() &&
			    event.getType() != DamageEvent.DamageType.AILMENT &&
			    event.getType() != DamageEvent.DamageType.POISON &&
			    event.getType() != DamageEvent.DamageType.OTHER &&
			    event.getType() != DamageEvent.DamageType.TRUE &&
			    event.getType() != DamageEvent.DamageType.FALL &&
			    event.getCause() != EntityDamageEvent.DamageCause.FIRE_TICK &&
			    !event.isBlocked()) {

			// Need to apply Voodoo Bonds + Resistance effects here since the damage event will be cancelled before
			// they can apply, this is easier than rewriting whole effect manager.
			Effect voodooBondsEffect = mPlugin.mEffectManager.getActiveEffect(mPlayer, "VoodooBondsEffect");
			if (voodooBondsEffect != null) {
				voodooBondsEffect.onHurt(mPlayer, event);
			}

			if (mPlugin.mEffectManager.hasEffect(mPlayer, PercentDamageReceived.class)) {
				for (Effect priorityEffects : mPlugin.mEffectManager.getPriorityEffects(mPlayer).values()) {
					if (priorityEffects.getEffectID().equals(PercentDamageReceived.effectID)) {
						priorityEffects.onHurt(mPlayer, event);
					}
				}
			}

			// Hotfixed: Fix Gallery scaling not working with Phlegmatic Resolve.
			GalleryManager.onEntityDamageEvent(event);

			// If player isn't under invincibility ticks. (This means we can have attacks that bypass Iframes hopefully)
			if (event.getDamage() > mLastMaxDamage) {
				double damageSplit = (event.getDamage() - mLastMaxDamage) / 3.0;
				mLastMaxDamage = event.getDamage();
				mLastPreMitigationDamage = event.getOriginalDamage();

				// Only play sound to player once per second.
				if ((mLastPlayedSoundTick - Bukkit.getServer().getCurrentTick()) > 20) {
					mLastPlayedSoundTick = Bukkit.getServer().getCurrentTick();
					mCosmetic.enhanceHurtSound(mPlayer);
				}

				if (damageSplit > 0) {
					mEnhancementDamageSpread[0] += damageSplit;
					mEnhancementDamageSpread[1] += damageSplit;
					mEnhancementDamageSpread[2] += damageSplit;

				}
			}
			// Only set damage to 0 so that kb occurs.
			event.setDamage(0);
		}
	}
}

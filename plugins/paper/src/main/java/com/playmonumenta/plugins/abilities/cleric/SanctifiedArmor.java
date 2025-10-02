package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.bosses.bosses.HostileBoss;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.SanctifiedArmorCS;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.effects.SanctifiedArmorHeal;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.Nullable;


public class SanctifiedArmor extends Ability implements AbilityWithDuration {

	private static final int SANCTIFY_COOLDOWN = 25 * 20;
	private static final double RESISTANCE_AMPLIFIER_1 = 0.2;
	private static final double RESISTANCE_AMPLIFIER_2 = 0.3;
	private static final double KBR = 0.5;
	private static final int SANCTIFY_DURATION = 7 * 20;
	private static final double SLOWNESS_AMPLIFIER_1 = 0.15;
	private static final double SLOWNESS_AMPLIFIER_2 = 0.25;
	private static final int SLOWNESS_DURATION = 4 * 20;
	private static final float KNOCKBACK_SPEED = 0.4f;
	private static final int ELITE_DURATION = 2 * 20;
	private static final double ENHANCE_HEALING = 0.5;
	private static final String RESISTANCE_EFFECT_NAME = "SanctifiedArmorResistance";
	private static final String KBR_EFFECT_NAME = "SanctifiedArmorKBR";
	private static final String ENHANCEMENT_EFFECT_NAME = "SanctifiedArmorHealEffect";

	public static final String CHARM_COOLDOWN = "Sanctified Armor Cooldown";
	public static final String CHARM_RESISTANCE = "Sanctified Armor Resistance";
	public static final String CHARM_KBR = "Sanctified Armor Knockback Resistance";
	public static final String CHARM_DURATION = "Sanctified Armor Duration";
	public static final String CHARM_SLOW = "Sanctified Armor Slowness Amplifier";
	public static final String CHARM_SLOW_DURATION = "Sanctified Armor Slowness Duration";
	public static final String CHARM_ELITE_DURATION = "Sanctified Armor Elite Bonus Duration";
	public static final String CHARM_ENHANCE_HEAL = "Sanctified Armor Enhancement Healing";

	public static final AbilityInfo<SanctifiedArmor> INFO =
		new AbilityInfo<>(SanctifiedArmor.class, "Sanctified Armor", SanctifiedArmor::new)
			.linkedSpell(ClassAbility.SANCTIFIED_ARMOR)
			.scoreboardId("Sanctified")
			.shorthandName("Sa")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Taking large damage grants you temporary resistance and applies slowness to Heretics that hit you.")
			.quest216Message("-------h-------e-------")
			.displayItem(Material.IRON_CHESTPLATE)
			.cooldown(SANCTIFY_COOLDOWN, CHARM_COOLDOWN)
			.priorityAmount(5000); // after all damage modifiers, but before lifelines, to get the proper final damage

	private final double mResistance;
	private final double mKBR;
	private final int mDuration;
	private final double mSlowness;
	private final int mSlownessDuration;
	private final int mEliteDurationExtension;
	private final double mEnhanceHealing;

	private double mHealthLostCounter = 0;
	private int mDurationExtensionCounter = 0;

	private @Nullable UUID mLastAffectedMob = null;
	private double mLastDamage;
	@Nullable
	public DamageType mLastDamageType;

	private final SanctifiedArmorCS mCosmetic;

	public SanctifiedArmor(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mResistance = (isLevelOne() ? RESISTANCE_AMPLIFIER_1 : RESISTANCE_AMPLIFIER_2) + CharmManager.getLevelPercentDecimal(player, CHARM_RESISTANCE);
		mKBR = CharmManager.calculateFlatAndPercentValue(player, CHARM_KBR, KBR);
		mDuration = CharmManager.getDuration(player, CHARM_DURATION, SANCTIFY_DURATION);
		mSlowness = CharmManager.calculateFlatAndPercentValue(player, CHARM_SLOW, isLevelOne() ? SLOWNESS_AMPLIFIER_1 : SLOWNESS_AMPLIFIER_2);
		mSlownessDuration = CharmManager.getDuration(player, CHARM_SLOW_DURATION, SLOWNESS_DURATION);
		mEliteDurationExtension = CharmManager.getDuration(player, CHARM_ELITE_DURATION, ELITE_DURATION);
		mEnhanceHealing = CharmManager.calculateFlatAndPercentValue(player, CHARM_ENHANCE_HEAL, ENHANCE_HEALING);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new SanctifiedArmorCS());
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (source == null) {
			return;
		}

		if (!event.isBlocked()) {
			if (!isOnCooldown()) {
				double maxHealth = EntityUtils.getMaxHealth(mPlayer);
				double percent = event.getFinalDamage(true) / maxHealth;
				mHealthLostCounter += percent;
				Bukkit.getScheduler().runTaskLater(mPlugin, () ->
					mHealthLostCounter -= percent, 20);

				if ((mHealthLostCounter >= 0.4 || (mPlayer.getHealth() - event.getFinalDamage(false)) / maxHealth <= 0.4) && !isOnCooldown()) {
					mDurationExtensionCounter = 0;
					mPlugin.mEffectManager.addEffect(mPlayer, RESISTANCE_EFFECT_NAME, new PercentDamageReceived(mDuration, -mResistance) {
						@Override
						public void entityLoseEffect(Entity entity) {
							ClientModHandler.updateAbility(mPlayer, ClassAbility.SANCTIFIED_ARMOR);
						}
					}.deleteOnAbilityUpdate(true));
					mPlugin.mEffectManager.addEffect(mPlayer, KBR_EFFECT_NAME, new PercentKnockbackResist(mDuration, mKBR, KBR_EFFECT_NAME).deleteOnAbilityUpdate(true));
					mCosmetic.sanctOnTrigger(mPlayer.getWorld(), mPlayer, mPlayer.getLocation());
					putOnCooldown();
				}
			}

			if (mPlugin.mEffectManager.hasEffect(mPlayer, RESISTANCE_EFFECT_NAME)) {
				DamageType type = event.getType();
				if (type == DamageType.MELEE) {
					// Potential edge case that would get through is a mob with boss_hostile and a melee type spell
					// but better to have this than ignore boss_hostile mobs completely (since they do not do DamageCause.ENTITY_ATTACK)
					if (!(event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK || source.getScoreboardTags().contains(HostileBoss.identityTag))) {
						return;
					}
				} else if (type == DamageType.PROJECTILE) {
					if (!(damager instanceof Projectile)) {
						return;
					}
				} else {
					return;
				}

				MovementUtils.knockAway(mPlayer, source, KNOCKBACK_SPEED, KNOCKBACK_SPEED, true);
				EntityUtils.applySlow(mPlugin, mDuration, mSlowness, source);

				Location loc = source.getLocation();
				World world = mPlayer.getWorld();
				if (isLevelTwo()) {
					mCosmetic.sanctApply2(world, mPlayer, loc, source);
				} else {
					mCosmetic.sanctApply1(world, mPlayer, loc, source);
				}

				mLastAffectedMob = null;
				if (isEnhanced() && source.isValid()) {
					Optional<SanctifiedArmorHeal> existingEffect = mPlugin.mEffectManager.getEffects(source, SanctifiedArmorHeal.class).stream().findFirst();
					if (existingEffect.isPresent()) {
						existingEffect.get().addPlayer(mPlayer);
					} else {
						mPlugin.mEffectManager.addEffect(source, ENHANCEMENT_EFFECT_NAME,
							new SanctifiedArmorHeal(mPlayer.getUniqueId()).displaysTime(false)
								.deleteOnAbilityUpdate(true));
					}
					mLastDamage = event.getFinalDamage(false);
					mLastAffectedMob = source.getUniqueId();
					mLastDamageType = DamageType.THORNS;
				}
			}
		}
	}

	public void onMobHurt(LivingEntity entity, DamageType type) {
		if (isEnhanced() && mLastAffectedMob != null && mLastAffectedMob.equals(entity.getUniqueId()) && mPlugin.mEffectManager.hasEffect(mPlayer, RESISTANCE_EFFECT_NAME)) {
			mLastDamageType = type;
		}
	}

	public void onMobKilled(LivingEntity entity) {
		if (isEnhanced()
			&& mLastAffectedMob != null
			&& mLastAffectedMob.equals(entity.getUniqueId())
			&& mPlugin.mEffectManager.hasEffect(mPlayer, RESISTANCE_EFFECT_NAME)) {
			PlayerUtils.healPlayer(mPlugin, mPlayer, mLastDamage * mEnhanceHealing);
			mCosmetic.sanctOnHeal(mPlayer, mPlayer.getLocation(), entity);
		}
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (isLevelTwo() && EntityUtils.isElite(event.getEntity()) && Crusade.enemyTriggersAbilities(event.getEntity())) {
			Effect effect = mPlugin.mEffectManager.getActiveEffect(mPlayer, RESISTANCE_EFFECT_NAME);
			Effect effect2 = mPlugin.mEffectManager.getActiveEffect(mPlayer, KBR_EFFECT_NAME);
			if (effect != null && effect2 != null) {
				effect.setDuration(effect.getDuration() + mEliteDurationExtension);
				effect2.setDuration(effect2.getDuration() + mEliteDurationExtension);
				mDurationExtensionCounter += mEliteDurationExtension;
				ClientModHandler.updateAbility(mPlayer, ClassAbility.SANCTIFIED_ARMOR);
			}
		}
	}

	private static Description<SanctifiedArmor> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("When taking damage that leaves you at 40% of your max health or lower, or taking 40% of your max health or more damage within 1 second, sanctify your armor for the next ")
			.addDuration(a -> a.mDuration, SANCTIFY_DURATION)
			.add(" seconds. While sanctified, you gain ")
			.addPercent(a -> a.mResistance, RESISTANCE_AMPLIFIER_1, false, Ability::isLevelOne)
			.add(" resistance and ")
			.addPercent(a -> a.mKBR, KBR)
			.add(" knockback resistance, and you knock away and apply ")
			.addPercent(a -> a.mSlowness, SLOWNESS_AMPLIFIER_1, false, Ability::isLevelOne)
			.add(" slowness for ")
			.addDuration(a -> a.mSlownessDuration, SLOWNESS_DURATION)
			.add(" seconds to any Heretic that hits you with a melee or projectile attack in the duration.")
			.addCooldown(SANCTIFY_COOLDOWN);
	}

	private static Description<SanctifiedArmor> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Resistance is increased to ")
			.addPercent(a -> a.mResistance, RESISTANCE_AMPLIFIER_2, false, Ability::isLevelTwo)
			.add(", and slowness is increased to ")
			.addPercent(a -> a.mSlowness, SLOWNESS_AMPLIFIER_2, false, Ability::isLevelTwo)
			.add(". Killing an Elite while Sanctified Armor is active extends its duration by ")
			.addDuration(a -> a.mEliteDurationExtension, ELITE_DURATION)
			.add(" seconds.");
	}

	private static Description<SanctifiedArmor> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("If the most recently slowed mob is killed while active, regain half the health lost from the last damage taken.");
	}

	@Override
	public int getInitialAbilityDuration() {
		return mDuration + mDurationExtensionCounter;
	}

	@Override
	public int getRemainingAbilityDuration() {
		Effect effect = mPlugin.mEffectManager.getActiveEffect(mPlayer, RESISTANCE_EFFECT_NAME);
		if (effect != null) {
			return effect.getDuration();
		}
		return 0;
	}
}

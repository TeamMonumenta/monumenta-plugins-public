package com.playmonumenta.plugins.abilities.cleric;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker.KillTriggeredAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.HeavenlyBoonCS;
import com.playmonumenta.plugins.effects.HeavenlyBoonTracker;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.Collection;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;

public final class HeavenlyBoon extends Ability implements KillTriggeredAbility {
	private static final String BOON_EFFECT_NAME = "ClericHeavenlyBoonTracker";
	private static final int MOB_EFFECT_DURATION = (int) (Constants.TICKS_PER_SECOND * 1.5);

	/* Note: These values don't affect the skill's functionality and are used for the description/some charm logic.
	 * Changes must be made to the loot tables instead */
	private static final double HEAVENLY_BOON_HEAL = 0.2;
	private static final double HEAVENLY_BOON_STRENGTH = 0.1;
	private static final int HEAVENLY_BOON_REGEN = 0; // Actually 1 because of how effects work
	private static final double HEAVENLY_BOON_RESISTANCE = 0.1;
	private static final double HEAVENLY_BOON_SPEED = 0.2;
	private static final double HEAVENLY_BOON_ABSORPTION = 0.2;
	private static final int HEAVENLY_BOON_DURATION_1 = Constants.TICKS_PER_SECOND * 20;
	private static final int HEAVENLY_BOON_DURATION_2 = Constants.TICKS_PER_SECOND * 50;
	private static final int COOLDOWN_1 = Constants.TICKS_PER_SECOND * 8;
	private static final int COOLDOWN_2 = Constants.TICKS_PER_SECOND * 6;

	private static final double HEAVENLY_BOON_1_CHANCE = 0.2;
	private static final double HEAVENLY_BOON_2_CHANCE = 0.2;
	private static final double HEAVENLY_BOON_RADIUS = 12;
	private static final double HEAVENLY_BOON_TRIGGER_INTENSITY = 0;
	private static final double ENHANCEMENT_CDR = 0.05;
	private static final int ENHANCEMENT_CDR_CAP = 20;
	private static final int BOSS_DAMAGE_THRESHOLD_R1 = 100;
	private static final int BOSS_DAMAGE_THRESHOLD_R2 = 200;
	private static final int BOSS_DAMAGE_THRESHOLD_R3 = 300;

	public static final String CHARM_CHANCE = "Heavenly Boon Potion Chance";
	public static final String CHARM_DURATION = "Heavenly Boon Potion Duration";
	public static final String CHARM_RADIUS = "Heavenly Boon Radius";
	public static final String CHARM_COOLDOWN = "Heavenly Boon Cooldown";

	public static final String CHARM_HEAL_AMPLIFIER = "Heavenly Boon Healing";
	public static final String CHARM_REGEN_AMPLIFIER = "Heavenly Boon Regeneration Amplifier";
	public static final String CHARM_SPEED_AMPLIFIER = "Heavenly Boon Speed Amplifier";
	public static final String CHARM_STRENGTH_AMPLIFIER = "Heavenly Boon Strength Amplifier";
	public static final String CHARM_RESIST_AMPLIFIER = "Heavenly Boon Resistance Amplifier";
	public static final String CHARM_ABSORPTION_AMPLIFIER = "Heavenly Boon Absorption Amplifier";

	public static final String CHARM_ENHANCE_CDR = "Heavenly Boon Enhancement Cooldown Reduction";
	public static final String CHARM_ENHANCE_CDR_CAP = "Heavenly Boon Enhancement Cooldown Reduction Cap";

	public static final AbilityInfo<HeavenlyBoon> INFO =
		new AbilityInfo<>(HeavenlyBoon.class, "Heavenly Boon", HeavenlyBoon::new)
			.linkedSpell(ClassAbility.HEAVENLY_BOON)
			.scoreboardId("HeavenlyBoon")
			.shorthandName("HB")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Share all positive splash potion effects with nearby players and occasionally generate splash potions when killing Heretics.")
			.cooldown(COOLDOWN_1, COOLDOWN_2, CHARM_COOLDOWN)
			.displayItem(Material.SPLASH_POTION);

	private static final ImmutableSet<String> BOON_DROPS = ImmutableSet.of(
		"Regeneration Boon", "Speed Boon", "Strength Boon", "Absorption Boon", "Resistance Boon",
		"Regeneration Boon 2", "Speed Boon 2", "Strength Boon 2", "Absorption Boon 2", "Resistance Boon 2");
	private static final ImmutableList<NamespacedKey> LEVEL_1_POTIONS = ImmutableList.of(
		NamespacedKeyUtils.fromString("epic:items/potions/regeneration_boon"),
		NamespacedKeyUtils.fromString("epic:items/potions/absorption_boon"),
		NamespacedKeyUtils.fromString("epic:items/potions/speed_boon"),
		NamespacedKeyUtils.fromString("epic:items/potions/resistance_boon"),
		NamespacedKeyUtils.fromString("epic:items/potions/strength_boon")
	);
	private static final ImmutableList<NamespacedKey> LEVEL_2_POTIONS = ImmutableList.of(
		NamespacedKeyUtils.fromString("epic:items/potions/regeneration_boon_2"),
		NamespacedKeyUtils.fromString("epic:items/potions/absorption_boon_2"),
		NamespacedKeyUtils.fromString("epic:items/potions/speed_boon_2"),
		NamespacedKeyUtils.fromString("epic:items/potions/resistance_boon_2"),
		NamespacedKeyUtils.fromString("epic:items/potions/strength_boon_2")
	);

	private final KillTriggeredAbilityTracker mTracker;
	private final double mChance;
	private final int mDurationChange;
	private final ImmutableMap<String, Double> mPotStrengthChange;
	private final double mRadius;
	private final double mEnhanceCDR;
	private final int mEnhanceCDRCap;
	private final HeavenlyBoonCS mCosmetic;

	public HeavenlyBoon(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		mTracker = new KillTriggeredAbilityTracker(mPlayer, this, BOSS_DAMAGE_THRESHOLD_R1, BOSS_DAMAGE_THRESHOLD_R2, BOSS_DAMAGE_THRESHOLD_R3);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new HeavenlyBoonCS());

		mChance = CharmManager.getLevelPercentDecimal(mPlayer, CHARM_CHANCE) + (isLevelOne() ? HEAVENLY_BOON_1_CHANCE : HEAVENLY_BOON_2_CHANCE);
		mDurationChange = CharmManager.getDuration(mPlayer, CHARM_DURATION, 0);
		mPotStrengthChange = ImmutableMap.of(
			"InstantHealthPercent", HEAVENLY_BOON_HEAL * CharmManager.getLevelPercentDecimal(mPlayer, CHARM_HEAL_AMPLIFIER),
			"Regeneration", CharmManager.getLevelPercentDecimal(mPlayer, CHARM_REGEN_AMPLIFIER),
			"Speed", CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED_AMPLIFIER),
			"damage", CharmManager.getLevelPercentDecimal(mPlayer, CHARM_STRENGTH_AMPLIFIER),
			"Resistance", CharmManager.getLevelPercentDecimal(mPlayer, CHARM_RESIST_AMPLIFIER),
			"Absorption", CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ABSORPTION_AMPLIFIER)
		);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, HEAVENLY_BOON_RADIUS);
		mEnhanceCDR = ENHANCEMENT_CDR + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ENHANCE_CDR);
		mEnhanceCDRCap = CharmManager.getDuration(mPlayer, CHARM_ENHANCE_CDR_CAP, ENHANCEMENT_CDR_CAP);
	}

	/*
	 * When a cleric is hit by a splash potion, distribute full durations of any positive effects to all
	 * nearby players. If a player gets a full-duration effect in this way, they are removed from the
	 * affectedEntities list so they don't get potion effects applied to them twice
	 */
	@Override
	public boolean playerSplashedByPotionEvent(final Collection<LivingEntity> affectedEntities, final ThrownPotion potion, final PotionSplashEvent event) {
		if (!(potion.getShooter() instanceof Player)) {
			return true;
		}

		boolean isBoonPotion = false;
		for (final String boon : BOON_DROPS) {
			if (Objects.requireNonNull(potion.getItem().getItemMeta().displayName()).toString().contains(boon)) {
				isBoonPotion = true;
				break;
			}
		}

		final boolean hasPositiveEffects = PotionUtils.hasPositiveEffects(PotionUtils.getEffects(potion.getItem()));
		if ((PotionUtils.hasNegativeEffects(potion.getItem()) || ItemStatUtils.hasNegativeEffect(potion.getItem(), false)) && !hasPositiveEffects) {
			return true;
		}

		if (event.isCancelled()) {
			//Another cleric already spread this potion
			return false;
		}

		if (event.getIntensity(mPlayer) >= HEAVENLY_BOON_TRIGGER_INTENSITY) {
			/* If within range, apply full strength of all potion effects to all nearby players */

			for (final Player p : PlayerUtils.playersInRange(mPlayer.getLocation(), mRadius, true)) {
				// Don't buff players that have their class disabled
				if (p.getScoreboardTags().contains("disable_class")) {
					continue;
				}

				// Apply custom effects from potion
				if (isBoonPotion) {
					ItemStatUtils.applyCustomEffects(mPlugin, p, potion.getItem(), false, 1, mDurationChange, mPotStrengthChange);
				} else {
					ItemStatUtils.applyCustomEffects(mPlugin, p, potion.getItem(), false);
				}

				/* Remove this player from the "usual" application of potion effects */
				affectedEntities.remove(p);
			}
		}

		return true;
	}

	@Override
	public boolean onDamage(final DamageEvent event, final LivingEntity enemy) {
		if (event.getType().equals(DamageType.TRUE)) {
			return false; // don't count true damage
		}

		mTracker.updateDamageDealtToBosses(event);

		if (enemy.isValid() && !isOnCooldown()) {
			// Construct custom source for each player.
			final String source = BOON_EFFECT_NAME + mPlayer.getName();
			mPlugin.mEffectManager.addEffect(enemy, source, new HeavenlyBoonTracker(MOB_EFFECT_DURATION, mPlayer.getUniqueId()));
		}
		return false;
	}

	@Override
	public void triggerOnKill(final LivingEntity mob) {
		if (Crusade.enemyTriggersAbilities(mob) && !isOnCooldown() && FastUtils.RANDOM.nextDouble() < mChance) {
			final ImmutableList<NamespacedKey> lootTables = isLevelOne() ? LEVEL_1_POTIONS : LEVEL_2_POTIONS;
			final NamespacedKey lootTable = lootTables.get(FastUtils.RANDOM.nextInt(lootTables.size()));
			final ItemStack potion = InventoryUtils.getItemFromLootTable(mPlayer, lootTable);
			if (potion == null) {
				return;
			}

			putOnCooldown();
			final ThrownPotion splashPotion = EntityUtils.spawnSplashPotion(mPlayer, potion);
			PotionUtils.mimicSplashPotionEffect(mPlayer, splashPotion);
			final String name = ItemUtils.getRawDisplayNameAsString(potion);
			if (name.contains("Regeneration")) {
				mCosmetic.splashEffectRegeneration(mPlayer, mob);
			} else if (name.contains("Speed")) {
				mCosmetic.splashEffectSpeed(mPlayer, mob);
			} else if (name.contains("Strength")) {
				mCosmetic.splashEffectStrength(mPlayer, mob);
			} else if (name.contains("Resistance")) {
				mCosmetic.splashEffectResistance(mPlayer, mob);
			} else if (name.contains("Absorption")) {
				mCosmetic.splashEffectAbsorption(mPlayer, mob);
			}
			if (isEnhanced()) {
				for (final Player player : PlayerUtils.playersInRange(mPlayer.getLocation(), mRadius, true)) {
					for (final Ability ability : mPlugin.mAbilityManager.getPlayerAbilities(player).getAbilities()) {
						final ClassAbility linkedSpell = ability.getInfo().getLinkedSpell();
						if (linkedSpell == ClassAbility.HEAVENLY_BOON || linkedSpell == null) {
							continue;
						}
						final int reducedCD = Math.min((int) (ability.getModifiedCooldown() * mEnhanceCDR), mEnhanceCDRCap);
						mPlugin.mTimers.updateCooldown(player, linkedSpell, reducedCD);
					}

					mCosmetic.enhanceCDR(player);
				}
			}
		}
	}

	// This one is kind of terrible so I just didn't include a bunch of stuff
	private static Description<HeavenlyBoon> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Whenever you are hit with a positive splash potion, the effects are also given to other players within ")
			.add(a -> a.mRadius, HEAVENLY_BOON_RADIUS)
			.add(" blocks. In addition, whenever a Heretic you have hit within ")
			.addDuration(MOB_EFFECT_DURATION)
			.add(" seconds dies or you deal enough non-true damage to a boss (R1 " + BOSS_DAMAGE_THRESHOLD_R1 + "/R2 " + BOSS_DAMAGE_THRESHOLD_R2 + "/R3 " + BOSS_DAMAGE_THRESHOLD_R3 + "), you have a ")
			.addPercent(a -> a.mChance, HEAVENLY_BOON_1_CHANCE, false, Ability::isLevelOne)
			.add(" chance to be splashed with a ")
			.addPercent(HEAVENLY_BOON_HEAL)
			.add(" Instant Health potion, with an additional effect of either Regeneration ")
			.addPotionAmplifier(a -> HEAVENLY_BOON_REGEN, HEAVENLY_BOON_REGEN)
			.add(", ")
			.addPercent(HEAVENLY_BOON_STRENGTH)
			.add(" strength, ")
			.addPercent(HEAVENLY_BOON_RESISTANCE)
			.add(" resistance, ")
			.addPercent(HEAVENLY_BOON_SPEED)
			.add(" speed, or ")
			.addPercent(HEAVENLY_BOON_ABSORPTION)
			.add(" absorption with a ")
			.addDuration(HEAVENLY_BOON_DURATION_1)
			.add(" second duration.")
			.addCooldown(COOLDOWN_1, Ability::isLevelOne);
	}

	private static Description<HeavenlyBoon> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Boon generated potions now give ")
			.addDuration(HEAVENLY_BOON_DURATION_2)
			.add(" second effect duration.")
			.addCooldown(COOLDOWN_2, Ability::isLevelTwo);
	}

	private static Description<HeavenlyBoon> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("When a potion is created by this skill, decrease all other ability cooldowns of all players in the radius by ")
			.addPercent(a -> a.mEnhanceCDR, ENHANCEMENT_CDR)
			.add(" (max ")
			.addDuration(a -> a.mEnhanceCDRCap, ENHANCEMENT_CDR_CAP)
			.add(" seconds).");
	}
}

package com.playmonumenta.plugins.abilities.cleric;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker.KillTriggeredAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.HeavenlyBoonCS;
import com.playmonumenta.plugins.effects.HeavenlyBoonTracker;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.Collection;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;


public final class HeavenlyBoon extends Ability implements KillTriggeredAbility {

	private static final String BOON_EFFECT_NAME = "ClericHeavenlyBoonTracker";
	private static final int MOB_EFFECT_DURATION = 30;

	private static final double HEAVENLY_BOON_1_CHANCE = 0.1;
	private static final double HEAVENLY_BOON_2_CHANCE = 0.2;
	private static final double HEAVENLY_BOON_HEAL_1 = 0.2;
	private static final double HEAVENLY_BOON_HEAL_2 = 0.3;
	private static final double HEAVENLY_BOON_RADIUS = 12;
	private static final double HEAVENLY_BOON_TRIGGER_INTENSITY = 0;
	private static final double ENHANCEMENT_CDR = 0.1;
	private static final int ENHANCEMENT_CDR_CAP = 20;
	private static final int ENHANCEMENT_COOLDOWN = 8 * 20;

	private static final int BOSS_DAMAGE_THRESHOLD_R1 = 100;
	private static final int BOSS_DAMAGE_THRESHOLD_R2 = 200;
	private static final int BOSS_DAMAGE_THRESHOLD_R3 = 300;

	public static final String CHARM_CHANCE = "Heavenly Boon Potion Chance";
	public static final String CHARM_DURATION = "Heavenly Boon Potion Duration";
	public static final String CHARM_RADIUS = "Heavenly Boon Radius";

	public static final String CHARM_HEAL_AMPLIFIER = "Heavenly Boon Healing";
	public static final String CHARM_REGEN_AMPLIFIER = "Heavenly Boon Regeneration Amplifier";
	public static final String CHARM_SPEED_AMPLIFIER = "Heavenly Boon Speed Amplifier";
	public static final String CHARM_STRENGTH_AMPLIFIER = "Heavenly Boon Strength Amplifier";
	public static final String CHARM_RESIST_AMPLIFIER = "Heavenly Boon Resistance Amplifier";
	public static final String CHARM_ABSORPTION_AMPLIFIER = "Heavenly Boon Absorption Amplifier";

	public static final String CHARM_ENHANCE_COOLDOWN = "Heavenly Boon Enhancement Cooldown";
	public static final String CHARM_ENHANCE_CDR = "Heavenly Boon Enhancement Cooldown Reduction";
	public static final String CHARM_ENHANCE_CDR_CAP = "Heavenly Boon Enhancement Cooldown Reduction Cap";


	public static final AbilityInfo<HeavenlyBoon> INFO =
		new AbilityInfo<>(HeavenlyBoon.class, "Heavenly Boon", HeavenlyBoon::new)
			.linkedSpell(ClassAbility.HEAVENLY_BOON)
			.scoreboardId("HeavenlyBoon")
			.shorthandName("HB")
			.descriptions(
				"Whenever you are hit with a positive splash potion, the effects are also given to other players in a 12 block radius. In addition, whenever an undead mob you have hit within 1.5s dies or you deal non-true damage to a boss (R1 100/R2 200/R3 300), you have a 10% chance to be splashed with an 20% Instant Health potion, with an additional effect of either Regen I, +10% Strength, +10% Resistance, +20% Speed, or +20% Absorption with a 20 second duration.",
				"The chance to be splashed upon killing an undead mob is increased to 20%. The effect potions now give 40% Instant Health and the durations of each are increased to 50 seconds.",
				String.format(
					"When a potion is created by this skill, decrease all other ability cooldowns of all players in the radius by %s%% (max %ss). " +
						"This effect has a cooldown of %s seconds.",
					StringUtils.multiplierToPercentage(ENHANCEMENT_CDR),
					StringUtils.ticksToSeconds(ENHANCEMENT_CDR_CAP),
					StringUtils.ticksToSeconds(ENHANCEMENT_COOLDOWN)
				))
			.simpleDescription("Share all positive splash potion effects with nearby players and occasionally generate splash potions when killing Undead enemies.")
			.cooldown(0, 0, ENHANCEMENT_COOLDOWN, CHARM_ENHANCE_COOLDOWN)
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

	private @Nullable Crusade mCrusade;

	public HeavenlyBoon(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mTracker = new KillTriggeredAbilityTracker(player, this, BOSS_DAMAGE_THRESHOLD_R1, BOSS_DAMAGE_THRESHOLD_R2, BOSS_DAMAGE_THRESHOLD_R3);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new HeavenlyBoonCS());

		mChance = CharmManager.getLevelPercentDecimal(player, CHARM_CHANCE) + (isLevelOne() ? HEAVENLY_BOON_1_CHANCE : HEAVENLY_BOON_2_CHANCE);
		mDurationChange = CharmManager.getDuration(player, CHARM_DURATION, 0);
		mPotStrengthChange = ImmutableMap.of(
			"InstantHealthPercent", (isLevelOne() ? HEAVENLY_BOON_HEAL_1 : HEAVENLY_BOON_HEAL_2) * CharmManager.getLevelPercentDecimal(player, CHARM_HEAL_AMPLIFIER),
			"Regeneration", CharmManager.getLevelPercentDecimal(player, CHARM_REGEN_AMPLIFIER),
			"Speed", CharmManager.getLevelPercentDecimal(player, CHARM_SPEED_AMPLIFIER),
			"damage", CharmManager.getLevelPercentDecimal(player, CHARM_STRENGTH_AMPLIFIER),
			"Resistance", CharmManager.getLevelPercentDecimal(player, CHARM_RESIST_AMPLIFIER),
			"Absorption", CharmManager.getLevelPercentDecimal(player, CHARM_ABSORPTION_AMPLIFIER)
		);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, HEAVENLY_BOON_RADIUS);
		mEnhanceCDR = ENHANCEMENT_CDR + CharmManager.getLevelPercentDecimal(player, CHARM_ENHANCE_CDR);
		mEnhanceCDRCap = CharmManager.getDuration(player, CHARM_ENHANCE_CDR_CAP, ENHANCEMENT_CDR_CAP);

		Bukkit.getScheduler().runTask(plugin, () -> mCrusade = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, Crusade.class));
	}

	/*
	 * When a cleric is hit by a splash potion, distribute full durations of any positive effects to all
	 * nearby players. If a player gets a full-duration effect in this way, they are removed from the
	 * affectedEntities list so they don't get potion effects applied to them twice
	 */
	@Override
	public boolean playerSplashedByPotionEvent(Collection<LivingEntity> affectedEntities, ThrownPotion potion,
	                                           PotionSplashEvent event) {
		if (!(potion.getShooter() instanceof Player)) {
			return true;
		}

		boolean hasPositiveEffects = PotionUtils.hasPositiveEffects(PotionUtils.getEffects(potion.getItem()));
		if ((PotionUtils.hasNegativeEffects(potion.getItem()) || ItemStatUtils.hasNegativeEffect(potion.getItem())) && !hasPositiveEffects) {
			return true;
		}

		if (event.isCancelled()) {
			//Another cleric already spread this potion
			return false;
		}

		if (event.getIntensity(mPlayer) >= HEAVENLY_BOON_TRIGGER_INTENSITY) {
			/* If within range, apply full strength of all potion effects to all nearby players */

			for (Player p : PlayerUtils.playersInRange(mPlayer.getLocation(), mRadius, true)) {
				// Don't buff players that have their class disabled
				if (p.getScoreboardTags().contains("disable_class")) {
					continue;
				}

				boolean isBoonPotion = false;
				for (String boon : BOON_DROPS) {
					if (Objects.requireNonNull(potion.getItem().getItemMeta().displayName()).toString().contains(boon)) {
						isBoonPotion = true;
						break;
					}
				}

				// Apply custom effects from potion
				if (isBoonPotion) {
					ItemStatUtils.changeDurationAndStrengths(p, potion.getItem(), mDurationChange, mPotStrengthChange);
				} else {
					ItemStatUtils.applyCustomEffects(mPlugin, p, potion.getItem(), false);
				}

				/* Remove this player from the "usual" application of potion effects */
				affectedEntities.remove(p);
			}
			return false;
		}

		return true;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType().equals(DamageType.TRUE)) {
			return false; // don't count true damage
		}

		mTracker.updateDamageDealtToBosses(event);

		if (enemy.isValid()) {
			// Construct custom source for each player.
			String source = BOON_EFFECT_NAME + mPlayer.getName();
			mPlugin.mEffectManager.addEffect(enemy, source, new HeavenlyBoonTracker(MOB_EFFECT_DURATION, mPlayer.getUniqueId()));
		}
		return false;
	}

	@Override
	public void triggerOnKill(LivingEntity mob) {
		if (Crusade.enemyTriggersAbilities(mob, mCrusade)
			    && FastUtils.RANDOM.nextDouble() < mChance
			    && !ServerProperties.getShardName().equals("plots")
			    && !ServerProperties.getShardName().equals("playerplots")) {

			ImmutableList<NamespacedKey> lootTables = isLevelOne() ? LEVEL_1_POTIONS : LEVEL_2_POTIONS;
			NamespacedKey lootTable = lootTables.get(FastUtils.RANDOM.nextInt(lootTables.size()));
			ItemStack potion = InventoryUtils.getItemFromLootTable(mPlayer, lootTable);
			if (potion == null) {
				return;
			}

			ThrownPotion splashPotion = EntityUtils.spawnSplashPotion(mPlayer, potion);
			PotionUtils.mimicSplashPotionEffect(mPlayer, splashPotion);
			String name = ItemUtils.getRawDisplayNameAsString(potion);
			if (name.contains("Regeneration")) {
				mCosmetic.splashEffectRegeneration(mPlayer);
			} else if (name.contains("Speed")) {
				mCosmetic.splashEffectSpeed(mPlayer);
			} else if (name.contains("Strength")) {
				mCosmetic.splashEffectStrength(mPlayer);
			} else if (name.contains("Resistance")) {
				mCosmetic.splashEffectResistance(mPlayer);
			} else if (name.contains("Absorption")) {
				mCosmetic.splashEffectAbsorption(mPlayer);
			}

			if (isEnhanced() && !isOnCooldown()) {
				putOnCooldown();

				for (Player player : PlayerUtils.playersInRange(mPlayer.getLocation(), mRadius, true)) {
					for (Ability ability : mPlugin.mAbilityManager.getPlayerAbilities(player).getAbilities()) {
						ClassAbility linkedSpell = ability.getInfo().getLinkedSpell();
						if (ability == this || linkedSpell == null) {
							continue;
						}
						int reducedCD = Math.min((int) (ability.getModifiedCooldown() * mEnhanceCDR), mEnhanceCDRCap);
						mPlugin.mTimers.updateCooldown(mPlayer, linkedSpell, reducedCD);
					}

					mCosmetic.enhanceCDR(player);
				}
			}
		}
	}
}

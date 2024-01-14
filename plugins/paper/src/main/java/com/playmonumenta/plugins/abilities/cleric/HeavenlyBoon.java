package com.playmonumenta.plugins.abilities.cleric;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker.KillTriggeredAbility;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.HeavenlyBoonTracker;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.EffectType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.Collection;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.checkerframework.checker.nullness.qual.Nullable;


public final class HeavenlyBoon extends Ability implements KillTriggeredAbility {

	private static final String BOON_EFFECT_NAME = "ClericHeavenlyBoonTracker";
	private static final int MOB_EFFECT_DURATION = 30;

	private static final double HEAVENLY_BOON_1_CHANCE = 0.1;
	private static final double HEAVENLY_BOON_2_CHANCE = 0.2;
	private static final double HEAVENLY_BOON_HEAL_1 = 0.2;
	private static final double HEAVENLY_BOON_HEAL_2 = 0.4;
	private static final double HEAVENLY_BOON_RADIUS = 12;
	private static final double HEAVENLY_BOON_TRIGGER_INTENSITY = 0;
	private static final double ENHANCEMENT_POTION_EFFECT_BONUS = 0.2;
	private static final int ENHANCEMENT_POTION_EFFECT_MAX_DURATION = 2 * 60 * 60 * 20;
	private static final int MAX_EXTENSIONS = 10;

	private static final int ENHANCEMENT_COOLDOWN_TICKS = 20;

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


	public static final AbilityInfo<HeavenlyBoon> INFO =
		new AbilityInfo<>(HeavenlyBoon.class, "Heavenly Boon", HeavenlyBoon::new)
			.scoreboardId("HeavenlyBoon")
			.shorthandName("HB")
			.descriptions(
				"Whenever you are hit with a positive splash potion, the effects are also given to other players in a 12 block radius. In addition, whenever an undead mob you have hit within 1.5s dies or you deal damage to a boss (R1 100/R2 200/R3 300), you have a 10% chance to be splashed with an 20% Instant Health potion, with an additional effect of either Regen I, +10% Strength, +10% Resistance, +20% Speed, or +20% Absorption with a 20 second duration.",
				"The chance to be splashed upon killing an undead mob is increased to 20%. The effect potions now give 40% Instant Health and the durations of each are increased to 50 seconds.",
				String.format(
					"When a potion is created by this skill, also increase all current positive potion durations by %s%%" +
						" (up to a maximum of %s hours, and up to %s times per effect) on all players in the radius.",
					(int) (ENHANCEMENT_POTION_EFFECT_BONUS * 100),
					ENHANCEMENT_POTION_EFFECT_MAX_DURATION / (60 * 60 * 20),
					MAX_EXTENSIONS
				))
			.simpleDescription("Share all positive splash potion effects with nearby players and occasionally generate splash potions when killing Undead enemies.")
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
	private int mLastSuccessfulProcTick = 0;

	private @Nullable Crusade mCrusade;

	public HeavenlyBoon(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mTracker = new KillTriggeredAbilityTracker(player, this, BOSS_DAMAGE_THRESHOLD_R1, BOSS_DAMAGE_THRESHOLD_R2, BOSS_DAMAGE_THRESHOLD_R3);

		mChance = CharmManager.getLevelPercentDecimal(player, CHARM_CHANCE) + (isLevelOne() ? HEAVENLY_BOON_1_CHANCE : HEAVENLY_BOON_2_CHANCE);
		mDurationChange = CharmManager.getExtraDuration(player, CHARM_DURATION);
		mPotStrengthChange = ImmutableMap.of(
			"InstantHealthPercent", (isLevelOne() ? HEAVENLY_BOON_HEAL_1 : HEAVENLY_BOON_HEAL_2) * CharmManager.getLevelPercentDecimal(player, CHARM_HEAL_AMPLIFIER),
			"Regeneration", CharmManager.getLevelPercentDecimal(player, CHARM_REGEN_AMPLIFIER),
			"Speed", CharmManager.getLevelPercentDecimal(player, CHARM_SPEED_AMPLIFIER),
			"damage", CharmManager.getLevelPercentDecimal(player, CHARM_STRENGTH_AMPLIFIER),
			"Resistance", CharmManager.getLevelPercentDecimal(player, CHARM_RESIST_AMPLIFIER),
			"Absorption", CharmManager.getLevelPercentDecimal(player, CHARM_ABSORPTION_AMPLIFIER)
		);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, HEAVENLY_BOON_RADIUS);

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
					if (potion.getItem().getItemMeta().displayName().toString().contains(boon)) {
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
			PotionUtils.splashPotionParticlesAndSound(mPlayer, splashPotion.getPotionMeta().getColor());
			heavenlyBoonSound(mPlayer);

			if (isEnhanced() && Bukkit.getCurrentTick() > mLastSuccessfulProcTick + ENHANCEMENT_COOLDOWN_TICKS) {
				mLastSuccessfulProcTick = Bukkit.getCurrentTick();
				for (Player p : PlayerUtils.playersInRange(mPlayer.getLocation(), mRadius, true)) {
					mPlugin.mPotionManager.modifyPotionDuration(p,
						potionInfo -> {
							if (potionInfo.mDuration > ENHANCEMENT_POTION_EFFECT_MAX_DURATION
								    || potionInfo.mInfinite
								    || potionInfo.mType == null
								    || !PotionUtils.hasPositiveEffects(potionInfo.mType)
								    || potionInfo.mHeavenlyBoonExtensions >= MAX_EXTENSIONS) {
								return potionInfo.mDuration;
							}
							potionInfo.mHeavenlyBoonExtensions++;
							return Math.min(potionInfo.mDuration + (int) (potionInfo.mDuration * ENHANCEMENT_POTION_EFFECT_BONUS), ENHANCEMENT_POTION_EFFECT_MAX_DURATION);
						});
					List<Effect> effects = mPlugin.mEffectManager.getEffects(p);
					if (effects != null) {
						for (Effect e : effects) {
							if (e.isBuff()
								    && (e.getDuration() < ENHANCEMENT_POTION_EFFECT_MAX_DURATION
									        && e.getDuration() != PotionEffect.INFINITE_DURATION)
								    && EffectType.isEffectTypeAppliedEffect(mPlugin.mEffectManager.getSource(mPlayer, e))
								    && e.getHeavenlyBoonExtensions() < MAX_EXTENSIONS) {
								e.incrementHeavenlyBoonExtensions();
								e.setDuration(Math.min(e.getDuration() + (int) (e.getDuration() * ENHANCEMENT_POTION_EFFECT_BONUS), ENHANCEMENT_POTION_EFFECT_MAX_DURATION));
							}
						}
					}
				}
			}
		}
	}

	public static void heavenlyBoonSound(Player player) {
		// copied from sacred provisions sound
		player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_COW_BELL, SoundCategory.PLAYERS, 0.65f, 2f);
	}
}

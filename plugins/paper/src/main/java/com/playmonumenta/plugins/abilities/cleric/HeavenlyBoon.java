package com.playmonumenta.plugins.abilities.cleric;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker.KillTriggeredAbility;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.Collection;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;


public final class HeavenlyBoon extends Ability implements KillTriggeredAbility {

	private static final double HEAVENLY_BOON_1_CHANCE = 0.1;
	private static final double HEAVENLY_BOON_2_CHANCE = 0.2;
	private static final double HEAVENLY_BOON_RADIUS = 12;
	private static final double HEAVENLY_BOON_TRIGGER_INTENSITY = 0;
	private static final double ENHANCEMENT_POTION_EFFECT_BONUS = 0.2;
	private static final int ENHANCEMENT_POTION_EFFECT_MAX_BOOST = 24 * 20;
	private static final int ENHANCEMENT_POTION_EFFECT_MAX_DURATION = 3 * 60 * 20;

	private static final int BOSS_DAMAGE_THRESHOLD_R1 = 100;
	private static final int BOSS_DAMAGE_THRESHOLD_R2 = 200;
	private static final int BOSS_DAMAGE_THRESHOLD_R3 = 300;

	public static final String CHARM_CHANCE = "Heavenly Boon Potion Chance";
	public static final String CHARM_DURATION = "Heavenly Boon Potion Duration";
	public static final String CHARM_RADIUS = "Heavenly Boon Radius";

	public static final AbilityInfo<HeavenlyBoon> INFO =
		new AbilityInfo<>(HeavenlyBoon.class, "Heavenly Boon", HeavenlyBoon::new)
			.scoreboardId("HeavenlyBoon")
			.shorthandName("HB")
			.descriptions(
				"Whenever you are hit with a positive splash potion, the effects are also given to other players in a 12 block radius. In addition, whenever you kill an undead mob or deal damage to a boss (R1 100/R2 200/R3 300), you have a 10% chance to be splashed with an Instant Health I potion, with an additional effect of either Regen I, +10% Attack Damage, +10 Damage Resistance, +20% Speed, or +20% Absorption with 20 second duration.",
				"The chance to be splashed upon killing an Undead increases to 20%, the effect potions now give Instant Health 2 and the durations of each are increased to 50 seconds.",
				String.format(
					"When a potion is created by this skill, also increase all current positive potion durations by %s%%" +
						" (capped at +%ss, and up to a maximum of %s minutes) on all players in the radius.",
					(int) (ENHANCEMENT_POTION_EFFECT_BONUS * 100),
					ENHANCEMENT_POTION_EFFECT_MAX_BOOST / 20,
					ENHANCEMENT_POTION_EFFECT_MAX_DURATION / (60 * 20)
				))
			.displayItem(new ItemStack(Material.SPLASH_POTION, 1));

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
	private final double mRadius;

	public HeavenlyBoon(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mTracker = new KillTriggeredAbilityTracker(this, BOSS_DAMAGE_THRESHOLD_R1, BOSS_DAMAGE_THRESHOLD_R2, BOSS_DAMAGE_THRESHOLD_R3);

		mChance = CharmManager.getLevelPercentDecimal(player, CHARM_CHANCE) + (isLevelOne() ? HEAVENLY_BOON_1_CHANCE : HEAVENLY_BOON_2_CHANCE);
		mDurationChange = CharmManager.getExtraDuration(player, CHARM_DURATION);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, HEAVENLY_BOON_RADIUS);
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

				// Apply custom effects from potion
				if (BOON_DROPS.contains(potion.getItem().getItemMeta().getDisplayName())) {
					ItemStatUtils.changeEffectsDuration(p, potion.getItem(), mDurationChange);
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
		return false;
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (shouldGenDrops) {
			triggerOnKill(event.getEntity());
		}
	}

	@Override
	public void triggerOnKill(LivingEntity mob) {
		if (Crusade.enemyTriggersAbilities(mob)
			    && FastUtils.RANDOM.nextDouble() < mChance) {
			ImmutableList<NamespacedKey> lootTables = isLevelOne() ? LEVEL_1_POTIONS : LEVEL_2_POTIONS;
			NamespacedKey lootTable = lootTables.get(FastUtils.RANDOM.nextInt(lootTables.size()));
			ItemStack potion = InventoryUtils.getItemFromLootTable(mPlayer, lootTable);
			if (potion == null) {
				return;
			}
			Location pos = mPlayer.getLocation().add(0, 1, 0);
			EntityUtils.spawnCustomSplashPotion(mPlayer, potion, pos);

			if (isEnhanced()) {
				for (Player p : PlayerUtils.playersInRange(mPlayer.getLocation(), mRadius, true)) {
					mPlugin.mPotionManager.modifyPotionDuration(p,
						potionInfo -> {
							if (potionInfo.mDuration > ENHANCEMENT_POTION_EFFECT_MAX_DURATION
								    || potionInfo.mType == null
								    || !PotionUtils.hasPositiveEffects(potionInfo.mType)) {
								return potionInfo.mDuration;
							}
							return Math.min(potionInfo.mDuration + Math.min((int) (potionInfo.mDuration * ENHANCEMENT_POTION_EFFECT_BONUS), ENHANCEMENT_POTION_EFFECT_MAX_BOOST), ENHANCEMENT_POTION_EFFECT_MAX_DURATION);
						});
					List<Effect> effects = mPlugin.mEffectManager.getEffects(p);
					if (effects != null) {
						for (Effect e : effects) {
							if (e.isBuff()) {
								e.setDuration(Math.min(e.getDuration() + Math.min((int) (e.getDuration() * ENHANCEMENT_POTION_EFFECT_BONUS), ENHANCEMENT_POTION_EFFECT_MAX_BOOST), ENHANCEMENT_POTION_EFFECT_MAX_DURATION));
							}
						}
					}
				}
			}
		}
	}
}

package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.BezoarCS;
import com.playmonumenta.plugins.effects.CustomRegeneration;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentPotionRecharge;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.WHITE;

public class Bezoar extends Ability {
	private static final int FREQUENCY = 5;
	private static final double RADIUS = 16;
	private static final int LINGER_TIME = 10 * 20;
	private static final int DEBUFF_REDUCTION = 10 * 20;
	private static final int HEAL_DURATION = 2 * 20;
	private static final double HEAL_PERCENT = 0.05;
	private static final int DAMAGE_DURATION = 8 * 20;
	private static final double DAMAGE_PERCENT = 0.15;
	private static final int POTIONS = 1;

	private static final int PHILOSOPHER_STONE_BEZOAR_COUNT = 5;
	public static final int PHILOSOPHER_STONE_ABSORPTION_AMOUNT = 4;
	public static final int PHILOSOPHER_STONE_ABSORPTION_DURATION = 12 * 20;
	public static final String PHILOSOPHER_STONE_RECHARGE_RATE_MULTIPLIER_NAME = "PhilosophersStoneRechargeRateMultiplier";
	public static final double PHILOSOPHER_STONE_RECHARGE_RATE_BONUS = 1;
	public static final int PHILOSOPHER_STONE_RECHARGE_RATE_REDUCTION_DURATION = 8 * 20;

	public static final String CHARM_REQUIREMENT = "Bezoar Generation Requirement";
	public static final String CHARM_LINGER_TIME = "Bezoar Linger Duration";
	public static final String CHARM_DEBUFF_REDUCTION = "Bezoar Debuff Reduction";
	public static final String CHARM_HEAL_DURATION = "Bezoar Healing Duration";
	public static final String CHARM_HEALING = "Bezoar Healing";
	public static final String CHARM_DAMAGE_DURATION = "Bezoar Damage Duration";
	public static final String CHARM_DAMAGE = "Bezoar Damage Modifier";
	public static final String CHARM_POTIONS = "Bezoar Potions";
	public static final String CHARM_RADIUS = "Bezoar Radius";
	public static final String CHARM_PHILOSOPHER_STONE_SPAWN_RATE = "Bezoar Philosopher Stone Spawn Rate";
	public static final String CHARM_PHILOSOPHER_STONE_RECHARGE_RATE_BONUS = "Bezoar Philosopher Stone Recharge Rate Bonus";
	public static final String CHARM_PHILOSOPHER_STONE_RECHARGE_RATE_DURATION = "Bezoar Philosopher Stone Recharge Rate Duration";
	public static final String CHARM_PHILOSOPHER_STONE_ABSORPTION = "Bezoar Philosopher Stone Absorption Health";
	public static final String CHARM_PHILOSOPHER_STONE_ABSORPTION_DURATION = "Bezoar Philosopher Stone Absorption Duration";

	public static final Style BEZOAR_COLOR = Style.style(TextColor.color(0x549923));
	public static final Style PHILOSOPHER_COLOR = Style.style(TextColor.color(0x991F27));

	public static final AbilityInfo<Bezoar> INFO =
		new AbilityInfo<>(Bezoar.class, "Bezoar", Bezoar::new)
			.linkedSpell(ClassAbility.BEZOAR)
			.scoreboardId("Bezoar")
			.shorthandName("BZ")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Every few mobs that are killed nearby, spawn an item that can be picked up for damage and healing buffs.")
			.displayItem(Material.LIME_CONCRETE);

	private final int mLingerTime;
	private final int mPotions;
	private final int mPhilosophersStoneBezoarCount;
	private final double mPhilosophersStoneRechargeRateBonus;
	private final int mPhilosophersStoneRechargeRateDuration;
	private final double mPhilosophersStoneAbsorptionAmount;
	private final int mPhilosophersStoneAbsorptionDuration;
	private final int mDebuffReduction;
	private final int mHealDuration;
	private final double mHealPercent;
	private final int mDamageDuration;
	private final double mDamagePercent;
	private final int mFrequency;
	private final double mRadius;
	private final BezoarCS mCosmetic;

	private int mKills = 0;
	private int mBezoarsSpawned = 0;
	private @Nullable AlchemistPotions mAlchemistPotions;

	public Bezoar(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mLingerTime = CharmManager.getDuration(mPlayer, CHARM_LINGER_TIME, LINGER_TIME);
		mPotions = POTIONS + (int) CharmManager.getLevel(mPlayer, CHARM_POTIONS);
		mPhilosophersStoneAbsorptionAmount = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_PHILOSOPHER_STONE_ABSORPTION, PHILOSOPHER_STONE_ABSORPTION_AMOUNT);
		mPhilosophersStoneAbsorptionDuration = CharmManager.getDuration(mPlayer, CHARM_PHILOSOPHER_STONE_ABSORPTION_DURATION, PHILOSOPHER_STONE_ABSORPTION_DURATION);
		mPhilosophersStoneBezoarCount = PHILOSOPHER_STONE_BEZOAR_COUNT + (int) CharmManager.getLevel(mPlayer, CHARM_PHILOSOPHER_STONE_SPAWN_RATE);
		mPhilosophersStoneRechargeRateBonus = PHILOSOPHER_STONE_RECHARGE_RATE_BONUS + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_PHILOSOPHER_STONE_RECHARGE_RATE_BONUS);
		mPhilosophersStoneRechargeRateDuration = CharmManager.getDuration(mPlayer, CHARM_PHILOSOPHER_STONE_RECHARGE_RATE_DURATION, PHILOSOPHER_STONE_RECHARGE_RATE_REDUCTION_DURATION);
		mDebuffReduction = CharmManager.getDuration(mPlayer, CHARM_DEBUFF_REDUCTION, DEBUFF_REDUCTION);
		mHealDuration = CharmManager.getDuration(mPlayer, CHARM_HEAL_DURATION, HEAL_DURATION);
		mHealPercent = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEALING, HEAL_PERCENT);
		mDamageDuration = CharmManager.getDuration(mPlayer, CHARM_DAMAGE_DURATION, DAMAGE_DURATION);
		mDamagePercent = DAMAGE_PERCENT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE);
		mFrequency = FREQUENCY + (int) CharmManager.getLevel(mPlayer, CHARM_REQUIREMENT);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new BezoarCS());

		Bukkit.getScheduler().runTask(plugin, () ->
			mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class));
	}

	public void dropBezoar(EntityDeathEvent event) {
		mKills = 0;
		Location loc = event.getEntity().getLocation().add(0, 0.25, 0);
		// Every n bezoars spawned, it should spawn a philosopher stone instead.
		if (isEnhanced()) {
			mBezoarsSpawned++;
			if (mBezoarsSpawned >= mPhilosophersStoneBezoarCount) {
				mBezoarsSpawned = 0;
				spawnPhilosopherStone(loc);
			} else {
				spawnBezoar(loc);
			}
		} else {
			spawnBezoar(loc);
		}
	}

	private void spawnBezoar(Location loc) {
		World world = loc.getWorld();
		Item item = spawnItem(world, loc, false);

		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				mT++;
				Location itemLoc = item.getLocation();
				mCosmetic.periodicBezoarEffects(mPlayer, itemLoc, mT, false);
				for (Player p : new Hitbox.UprightCylinderHitbox(itemLoc, 0.7, 0.7).getHitPlayers(true)) {
					if (p != mPlayer) {
						applyEffects(p, false);
						mCosmetic.targetEffects(p, itemLoc, false);
					}
					applyEffects(mPlayer, false);
					mCosmetic.targetEffects(mPlayer, itemLoc, false);

					if (mAlchemistPotions != null) {
						mAlchemistPotions.incrementCharges(mPotions);
					}

					item.remove();
					mCosmetic.pickupEffects(mPlayer, itemLoc, false);

					this.cancel();
					return;
				}

				if (mT >= mLingerTime || item.isDead()) {
					this.cancel();
					item.remove();
					mCosmetic.expireEffects(mPlayer, itemLoc, false);
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}


	private void applyEffects(Player player, boolean isPhilosopherStone) {
		if (isPhilosopherStone) {
			AbsorptionUtils.addAbsorption(player, mPlayer, mPhilosophersStoneAbsorptionAmount, mPhilosophersStoneAbsorptionAmount, mPhilosophersStoneAbsorptionDuration);
			if (mAlchemistPotions != null) {
				mPlugin.mEffectManager.addEffect(
					mPlayer,
					PHILOSOPHER_STONE_RECHARGE_RATE_MULTIPLIER_NAME,
					new PercentPotionRecharge(
						mPhilosophersStoneRechargeRateDuration,
						mPhilosophersStoneRechargeRateBonus,
						PHILOSOPHER_STONE_RECHARGE_RATE_MULTIPLIER_NAME,
						mAlchemistPotions
					)
				);
			}
			return;
		}

		PotionUtils.reduceAllDebuffsDuration(mPlugin, player, mDebuffReduction);
		double maxHealth = EntityUtils.getMaxHealth(player);
		mPlugin.mEffectManager.addEffect(player, "BezoarHealing",
			new CustomRegeneration(mHealDuration,
				maxHealth * mHealPercent, mPlayer, mPlugin).deleteOnAbilityUpdate(true));

		if (isLevelTwo()) {
			mPlugin.mEffectManager.addEffect(player, "BezoarPercentDamageDealtEffect", new PercentDamageDealt(mDamageDuration, mDamagePercent).deleteOnAbilityUpdate(true));
		}
	}

	private void spawnPhilosopherStone(Location loc) {
		World world = loc.getWorld();
		Item item = spawnItem(world, loc, true);

		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				mT++;
				Location itemLoc = item.getLocation();
				mCosmetic.periodicBezoarEffects(mPlayer, itemLoc, mT, true);
				for (Player p : PlayerUtils.playersInRange(itemLoc, 1, true)) {
					if (p != mPlayer) {
						applyPhilosopherEffects(p);
						mCosmetic.targetEffects(p, itemLoc, true);
					}
					applyPhilosopherEffects(mPlayer);
					mCosmetic.targetEffects(mPlayer, itemLoc, true);

					item.remove();
					mCosmetic.pickupEffects(mPlayer, itemLoc, true);

					this.cancel();
					return;
				}

				if (mT >= mLingerTime || item.isDead()) {
					this.cancel();
					item.remove();
					mCosmetic.expireEffects(mPlayer, itemLoc, true);
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void applyPhilosopherEffects(Player player) {
		applyEffects(player, true);
	}

	private Item spawnItem(World world, Location loc, boolean philosophersStone) {
		return AbilityUtils.spawnAbilityItem(
			world,
			loc,
			mCosmetic.bezoarMat(philosophersStone),
			mCosmetic.bezoarName(philosophersStone),
			true,
			0,
			true,
			true,
			mCosmetic.bezoarGlowColor(philosophersStone));
	}

	public boolean shouldDrop() {
		return mKills >= mFrequency;
	}

	@Override
	public void entityDeathRadiusEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		mKills++;
		if (shouldDrop()) {
			dropBezoar(event);
		}
	}

	@Override
	public double entityDeathRadius() {
		return mRadius;
	}

	private static Description<Bezoar> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addLine("Spawn a *Bezoar* for every %d mobs killed").styles(BEZOAR_COLOR)
				.statValues(stat(a -> a.mFrequency, FREQUENCY))
			.addLine("within %d blocks of you, lasting for %t.")
				.statValues(
					stat(a -> a.mRadius, RADIUS),
					stat(a -> a.mLingerTime, LINGER_TIME))
			.addLine()
			.addLine("Players can pick up *Bezoars*, granting that player").styles(BEZOAR_COLOR)
			.addLine("and yourself healing over time and reducing")
			.addLine("the duration of potion debuffs on both players.")
			.addLine()
			.addLine("Gain *1* potion when a *Bezoar* is picked up.").styles(WHITE, BEZOAR_COLOR)
			.addLine()
			.addStat("Healing: %p HP every 1s for %t")
				.statValues(
					stat(a -> a.mHealPercent, HEAL_PERCENT),
					stat(a -> a.mHealDuration, HEAL_DURATION))
			.addStat("Effect: -%t Debuff Duration")
				.statValues(stat(a -> a.mDebuffReduction, DEBUFF_REDUCTION))
			.addDashedLine();
	}

	private static Description<Bezoar> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("*Bezoars* now also grant both").styles(BEZOAR_COLOR)
			.addLine("players a damage boost.")
			.addLine()
			.addStat("Effect: +%p Damage for %t")
				.statValues(
					stat(a -> a.mDamagePercent, DAMAGE_PERCENT),
					stat(a -> a.mDamageDuration, DAMAGE_DURATION))
			.addDashedLine();
	}

	private static Description<Bezoar> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("Every %d *Bezoars* becomes a *Philosopher's Stone*").styles(BEZOAR_COLOR, PHILOSOPHER_COLOR)
				.statValues(stat(PHILOSOPHER_STONE_BEZOAR_COUNT))
			.addLine("instead, and picking it up grants you increased")
			.addLine("potion recharge rate and absorption.")
			.addLine()
			.addStat("Effect: +%p Potion Recharge Rate for %t")
				.statValues(stat(a -> a.mPhilosophersStoneRechargeRateBonus, PHILOSOPHER_STONE_RECHARGE_RATE_BONUS),
					stat(a -> a.mPhilosophersStoneRechargeRateDuration, PHILOSOPHER_STONE_RECHARGE_RATE_REDUCTION_DURATION))
			.addStat("Effect: +%d Absorption for %t")
				.statValues(stat(a -> a.mPhilosophersStoneAbsorptionAmount, PHILOSOPHER_STONE_ABSORPTION_AMOUNT),
					stat(a -> a.mPhilosophersStoneAbsorptionDuration, PHILOSOPHER_STONE_ABSORPTION_DURATION))
			.addDashedLine();
	}
}

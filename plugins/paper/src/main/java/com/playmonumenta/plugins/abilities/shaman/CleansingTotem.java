package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.shaman.CleansingTotemCS;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

public class CleansingTotem extends TotemAbility {
	private static final int COOLDOWN = 30 * 20;
	private static final int AOE_RANGE = 6;
	private static final double HEAL_PERCENT = 0.06;
	private static final int INTERVAL = 20;
	private static final int DURATION_1 = 8 * 20;
	private static final int DURATION_2 = 12 * 20;
	private static final int CLEANSES = 2;
	private static final double ENHANCE_HEALING_PERCENT = 0.03;
	private static final int ENHANCE_ABSORB_CAP = 4;

	public static String CHARM_DURATION = "Cleansing Totem Duration";
	public static String CHARM_RADIUS = "Cleansing Totem Radius";
	public static String CHARM_COOLDOWN = "Cleansing Totem Cooldown";
	public static String CHARM_HEALING = "Cleansing Totem Healing";
	public static String CHARM_CLEANSES = "Cleansing Totem Cleanses";
	public static String CHARM_ENHANCE_ABSORB_MAX = "Cleansing Totem Enhance Absorption Maximum";
	public static String CHARM_PULSE_DELAY = "Cleansing Totem Pulse Delay";

	public static final AbilityInfo<CleansingTotem> INFO =
		new AbilityInfo<>(CleansingTotem.class, "Cleansing Totem", CleansingTotem::new)
			.linkedSpell(ClassAbility.CLEANSING_TOTEM)
			.scoreboardId("CleansingTotem")
			.shorthandName("CT")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Summon a totem that heals and cleanses players over its duration.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", CleansingTotem::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.displayItem(Material.BLUE_STAINED_GLASS);

	private final double mRadius;
	private final int mInterval;
	private final double mHealPercentBase;
	private final double mHealPercentEnhance;
	private final double mHealPercent;
	private final double mAbsorbCap;
	private final int mCleanses;
	private final CleansingTotemCS mCosmetic;

	public CleansingTotem(Plugin plugin, Player player) {
		super(plugin, player, INFO, "Cleansing Totem Projectile", "CleansingTotem", "Cleansing Totem");
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, isLevelOne() ? DURATION_1 : DURATION_2);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, AOE_RANGE);
		mInterval = CharmManager.getDuration(mPlayer, CHARM_PULSE_DELAY, INTERVAL);
		mHealPercentBase = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEALING, HEAL_PERCENT);
		mHealPercentEnhance = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEALING, ENHANCE_HEALING_PERCENT);
		mHealPercent = mHealPercentBase + (isEnhanced() ? mHealPercentEnhance : 0);
		mAbsorbCap = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ENHANCE_ABSORB_MAX, ENHANCE_ABSORB_CAP);
		mCleanses = CLEANSES + (int) CharmManager.getLevel(mPlayer, CHARM_CLEANSES);
		mChargeUpTicks = 0;
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new CleansingTotemCS());
	}

	@Override
	public void placeTotem(Location standLocation, Player player, ArmorStand stand) {
		mCosmetic.cleansingTotemSpawn(mPlayer.getWorld(), standLocation);
	}

	@Override
	public void onTotemTick(int ticks, ArmorStand stand, World world, Location standLocation, ItemStatManager.PlayerItemStats stats) {
		if (ticks % mInterval == 0) {
			pulse(standLocation, stats, false);
		}
		if (isLevelTwo() && ticks == mDuration / mCleanses - 1) {
			List<Player> cleansePlayers = PlayerUtils.playersInRange(standLocation, mRadius, true);
			cleanseTargets(cleansePlayers);
			mCosmetic.cleansingTotemCleanse(mPlayer, standLocation, mRadius);
		}
	}

	@Override
	public void pulse(Location standLocation, ItemStatManager.PlayerItemStats stats, boolean bonusAction) {
		if (bonusAction) {
			List<Player> players = PlayerUtils.playersInRange(standLocation, mRadius, true);
			for (Player p : players) {
				PlayerUtils.healPlayer(mPlugin, p,
					EntityUtils.getMaxHealth(p) * mHealPercent * (ChainLightning.ENHANCE_POSITIVE_EFFICIENCY + CharmManager.getLevelPercentDecimal(mPlayer, ChainLightning.CHARM_POSITIVE_TOTEM_EFFICIENCY)));
				mCosmetic.cleansingTotemHeal(mPlayer);
			}
			if (isLevelTwo()) {
				cleanseTargets(players);
				new PPCircle(Particle.HEART, standLocation, mRadius)
					.ringMode(false).countPerMeter(0.8).spawnAsPlayerActive(mPlayer);
			}
		} else {
			List<Player> affectedPlayers = PlayerUtils.playersInRange(standLocation, mRadius, true);

			for (Player p : affectedPlayers) {
				double maxHealth = EntityUtils.getMaxHealth(p);
				double totalHealing = maxHealth * mHealPercent;
				double healed = PlayerUtils.healPlayer(mPlugin, p, totalHealing);
				double remainingHealing = totalHealing - healed;
				if (remainingHealing > 0 && isEnhanced()) {
					AbsorptionUtils.addAbsorption(p, remainingHealing, mAbsorbCap, 15 * 20);
				}
			}
			dealSanctuaryImpacts(EntityUtils.getNearbyMobsInSphere(standLocation, mRadius, null), 40);
			mCosmetic.cleansingTotemPulse(mPlayer, standLocation, mRadius);
		}
	}

	@Override
	public void onTotemExpire(World world, Location standLocation) {
		mCosmetic.cleansingTotemExpire(world, standLocation, mPlayer);
	}

	private void cleanseTargets(List<Player> cleansePlayers) {
		for (Player player : cleansePlayers) {
			PotionUtils.clearNegatives(mPlugin, player);
			EntityUtils.setWeakenTicks(mPlugin, player, 0);
			EntityUtils.setSlowTicks(mPlugin, player, 0);

			if (player.getFireTicks() > 1) {
				player.setFireTicks(1);
			}
		}
	}

	private static Description<CleansingTotem> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to fire a projectile that summons a Cleansing Totem. Players within ")
			.add(a -> a.mRadius, AOE_RANGE)
			.add(" blocks of this totem heal ")
			.addPercent(a -> a.mHealPercentBase, HEAL_PERCENT)
			.add(" max health per second. Duration: ")
			.addDuration(a -> a.mDuration, DURATION_1, false, Ability::isLevelOne)
			.add("s.")
			.addCooldown(COOLDOWN);
	}

	private static Description<CleansingTotem> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Duration is increased to ")
			.addDuration(a -> a.mDuration, DURATION_2, false, Ability::isLevelTwo)
			.add(" seconds. Now additionally cleanses debuffs from players ")
			.add(a -> a.mCleanses, CLEANSES)
			.add(" times evenly throughout the duration.");
	}

	private static Description<CleansingTotem> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Healing is increased by ")
			.addPercent(a -> a.mHealPercentEnhance, ENHANCE_HEALING_PERCENT)
			.add(" max health. Healing done while at full health is now converted to absorption, up to ")
			.add(a -> a.mAbsorbCap, ENHANCE_ABSORB_CAP)
			.add(" absorption health.");
	}
}

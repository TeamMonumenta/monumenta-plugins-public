package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.shaman.CleansingTotemCS;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.StringUtils;
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

	private final double mRadius;
	private final int mInterval;
	private final double mHealPercent;
	private final double mAbsorbCap;

	public static final AbilityInfo<CleansingTotem> INFO =
		new AbilityInfo<>(CleansingTotem.class, "Cleansing Totem", CleansingTotem::new)
			.linkedSpell(ClassAbility.CLEANSING_TOTEM)
			.scoreboardId("CleansingTotem")
			.shorthandName("CT")
			.descriptions(
				String.format("Left click while holding a melee weapon and sneaking to fire a projectile that summons a Cleansing Totem. Players within %s blocks of this totem " +
					"heal for %s%% of their maximum health per second. No chargeup time, Duration: %ss. Cooldown: %ss.",
					AOE_RANGE,
					StringUtils.multiplierToPercentage(HEAL_PERCENT),
					StringUtils.ticksToSeconds(DURATION_1),
					StringUtils.ticksToSeconds(COOLDOWN)
				),
				String.format("Duration is increased to %ss and now cleanses debuffs for players %s times evenly throughout it's duration.",
					StringUtils.ticksToSeconds(DURATION_2),
					CLEANSES),
				String.format("Increases healing by %s%% and now overheals into absorption with a maximum of %s hearts.",
					StringUtils.multiplierToPercentage(ENHANCE_HEALING_PERCENT),
					ENHANCE_ABSORB_CAP / 2
				)
			)
			.simpleDescription("Summon a totem that heals and cleanses players over its duration.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", CleansingTotem::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.displayItem(Material.BLUE_STAINED_GLASS);
	private final CleansingTotemCS mCosmetic;

	public CleansingTotem(Plugin plugin, Player player) {
		super(plugin, player, INFO, "Cleansing Totem Projectile", "CleansingTotem", "Cleansing Totem");
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AbilityUtils.resetClass(player);
		}
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, isLevelOne() ? DURATION_1 : DURATION_2);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, AOE_RANGE);
		mInterval = CharmManager.getDuration(mPlayer, CHARM_PULSE_DELAY, INTERVAL);
		mHealPercent = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEALING, HEAL_PERCENT)
			+ (isEnhanced() ? ENHANCE_HEALING_PERCENT : 0);
		mAbsorbCap = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ENHANCE_ABSORB_MAX, ENHANCE_ABSORB_CAP);
		mChargeUpTicks = 0;
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new CleansingTotemCS());
	}

	@Override
	public void onTotemTick(int ticks, ArmorStand stand, World world, Location standLocation, ItemStatManager.PlayerItemStats stats) {
		if (ticks == 0) {
			mCosmetic.cleansingTotemSpawn(world, standLocation);
		}
		if (ticks % mInterval == 0) {
			pulse(standLocation, stats, false);
		}
		if (isLevelTwo() && ticks == mDuration / (CLEANSES + (int) CharmManager.getLevel(mPlayer, CHARM_CLEANSES)) - 1) {
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
					EntityUtils.getMaxHealth(p) * mHealPercent * ChainLightning.ENHANCE_POSITIVE_EFFICIENCY);
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
}

package com.playmonumenta.plugins.abilities.shaman.soothsayer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.abilities.shaman.ChainLightning;
import com.playmonumenta.plugins.abilities.shaman.TotemAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.shaman.soothsayer.WhirlwindTotemCS;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.effects.ShamanCooldownDecreasePerSecond;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

public class WhirlwindTotem extends TotemAbility {

	private static final int COOLDOWN = 26 * 20;
	private static final int INTERVAL = 2 * 20;
	private static final int AOE_RANGE = 5;
	private static final int DURATION_1 = 8 * 20;
	private static final int DURATION_2 = 12 * 20;
	private static final double CDR_PERCENT = 0.025;
	private static final int CDR_MAX_PER_SECOND = 20;
	private static final double SPEED_PERCENT = 0.1;
	private static final String WHIRLWIND_SPEED_EFFECT_NAME = "WhirlwindSpeedEffect";
	public static final double DURATION_BOOST = 0.25;

	public static final String CHARM_DURATION = "Whirlwind Totem Duration";
	public static final String CHARM_RADIUS = "Whirlwind Totem Radius";
	public static final String CHARM_COOLDOWN = "Whirlwind Totem Cooldown";
	public static final String CHARM_CDR = "Whirlwind Totem Cooldown Reduction Per Second";
	public static final String CHARM_MAX_CDR = "Whirlind Totem Maximum Cooldown Reduction Per Second";
	public static final String CHARM_SPEED = "Whirlwind Totem Speed Amplifier";
	public static final String CHARM_DURATION_BOOST = "Whirlwind Totem Duration Buff";
	public static final String CHARM_PULSE_DELAY = "Whirlwind Totem Pulse Delay";

	public static final AbilityInfo<WhirlwindTotem> INFO =
		new AbilityInfo<>(WhirlwindTotem.class, "Whirlwind Totem", WhirlwindTotem::new)
			.linkedSpell(ClassAbility.WHIRLWIND_TOTEM)
			.scoreboardId("WhirlwindTotem")
			.shorthandName("WWT")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Summon a totem that provides cooldown reduction to players within its radius.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", WhirlwindTotem::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.displayItem(Material.BLUE_STAINED_GLASS);

	private final double mRadius;
	private final double mCDRPerSecond;
	private final int mCDRMax;
	private final double mSpeed;
	private final double mDurationBoost;
	private final int mInterval;
	private final WhirlwindTotemCS mCosmetic;

	public WhirlwindTotem(Plugin plugin, Player player) {
		super(plugin, player, INFO, "Whirlwind Totem Projectile", "WhirlwindTotem", "Whirlwind Totem");
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, isLevelOne() ? DURATION_1 : DURATION_2);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, AOE_RANGE);
		mCDRPerSecond = CDR_PERCENT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_CDR);
		mCDRMax = CharmManager.getDuration(mPlayer, CHARM_MAX_CDR, CDR_MAX_PER_SECOND);
		mSpeed = SPEED_PERCENT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED);
		mDurationBoost = DURATION_BOOST + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DURATION_BOOST);
		mInterval = CharmManager.getDuration(mPlayer, CHARM_PULSE_DELAY, INTERVAL);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new WhirlwindTotemCS());
	}

	@Override
	public void placeTotem(Location standLocation, Player player, ArmorStand stand) {
		mCosmetic.whirlwindTotemSpawn(player.getWorld(), mPlayer, standLocation, stand);
		applyWhirlwindDurationBoost();
	}

	@Override
	public void onTotemTick(int ticks, ArmorStand stand, World world, Location standLocation, ItemStatManager.PlayerItemStats stats) {
		if (ticks % mInterval == 0) {
			pulse(standLocation, stats, false);
		}
	}

	@Override
	public void pulse(Location standLocation, ItemStatManager.PlayerItemStats stats, boolean bonusAction) {
		List<Player> affectedPlayers = PlayerUtils.playersInRange(standLocation, mRadius, true);

		for (Player p : affectedPlayers) {
			mPlugin.mEffectManager.addEffect(p, "WhirlwindTotemCDR",
				new ShamanCooldownDecreasePerSecond(50, mCDRPerSecond, mCDRMax, mPlugin).deleteOnAbilityUpdate(true));
			if (isLevelTwo()) {
				mPlugin.mEffectManager.addEffect(p, WHIRLWIND_SPEED_EFFECT_NAME,
					new PercentSpeed(50, mSpeed, WHIRLWIND_SPEED_EFFECT_NAME).deleteOnAbilityUpdate(true));
			}
			if (bonusAction) {
				for (Ability abil : mPlugin.mAbilityManager.getPlayerAbilities(p).getAbilities()) {
					ClassAbility linkedSpell = abil.getInfo().getLinkedSpell();
					if (linkedSpell == null || linkedSpell == ClassAbility.WHIRLWIND_TOTEM) {
						continue;
					}
					int totalCD = abil.getModifiedCooldown();
					int reducedCD = Math.min((int) (totalCD * mCDRPerSecond
						* (ChainLightning.ENHANCE_POSITIVE_EFFICIENCY + CharmManager.getLevelPercentDecimal(mPlayer, ChainLightning.CHARM_POSITIVE_TOTEM_EFFICIENCY))), mCDRMax);
					mPlugin.mTimers.updateCooldown(p, linkedSpell, reducedCD);
					mPlugin.mTimers.updateCooldown(p, linkedSpell, reducedCD);
				}
			}
		}

		mCosmetic.whirlwindTotemPulse(mPlayer, standLocation, mRadius);
		dealSanctuaryImpacts(EntityUtils.getNearbyMobsInSphere(standLocation, mRadius, null), INTERVAL + 20);
		applyWhirlwindDurationBoost();
	}

	@Override
	public void onTotemExpire(World world, Location standLocation) {
		mCosmetic.whirlwindTotemExpire(mPlayer, world, standLocation, mRadius);
	}

	public void applyWhirlwindDurationBoost() {
		for (Ability abil : mPlugin.mAbilityManager.getPlayerAbilities(mPlayer).getAbilities()) {
			if (abil instanceof TotemAbility totemAbility && totemAbility.getRemainingAbilityDuration() > 0 && !(abil instanceof WhirlwindTotem)) {
				totemAbility.mWhirlwindBuffPercent = 1 + mDurationBoost;
			}
		}
	}

	private static Description<WhirlwindTotem> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to fire a projectile that summons a Whirlwind Totem. Every ")
			.addDuration(a -> a.mInterval, INTERVAL, true)
			.add(" seconds, players within ")
			.add(a -> a.mRadius, AOE_RANGE)
			.add(" blocks of the totem have their cooldowns reduced by ")
			.addPercent(a -> a.mCDRPerSecond, CDR_PERCENT)
			.add(" (maximum ")
			.addDuration(a -> a.mCDRMax, CDR_MAX_PER_SECOND)
			.add(" seconds). Cannot decrease the cooldown of this ability. Additionally, other totems existing during this totem's duration gain ")
			.addPercent(a -> a.mDurationBoost, DURATION_BOOST)
			.add(" duration. Charge up time: ")
			.addDuration(PULSE_DELAY)
			.add("s. Duration: ")
			.addDuration(a -> a.mDuration, DURATION_1, false, Ability::isLevelOne)
			.add("s.")
			.addCooldown(COOLDOWN);
	}

	private static Description<WhirlwindTotem> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Duration is increased to ")
			.addDuration(a -> a.mDuration, DURATION_2, false, Ability::isLevelTwo)
			.add(" seconds. Now additionally gives ")
			.addPercent(a -> a.mSpeed, SPEED_PERCENT)
			.add(" speed to players within range.");
	}
}

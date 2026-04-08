package com.playmonumenta.plugins.abilities.shaman.soothsayer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.shaman.ChainLightning;
import com.playmonumenta.plugins.abilities.shaman.TotemAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.shaman.soothsayer.WhirlwindTotemCS;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.effects.ShamanCooldownDecreasePerSecond;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class WhirlwindTotem extends TotemAbility {
	private static final int COOLDOWN = 26 * 20;
	private static final int INTERVAL = 2 * 20;
	private static final int AOE_RANGE = 6;
	private static final int DURATION_1 = 10 * 20;
	private static final int DURATION_2 = 14 * 20;
	private static final double CDR_PERCENT = 0.025;
	private static final int CDR_MAX_PER_SECOND = 15;
	private static final double SPEED_PERCENT = 0.15;
	private static final String WHIRLWIND_SPEED_EFFECT_NAME = "WhirlwindSpeedEffect";
	public static final double DURATION_BOOST = 0.25;

	public static final String CHARM_DURATION = "Whirlwind Totem Duration";
	public static final String CHARM_RADIUS = "Whirlwind Totem Radius";
	public static final String CHARM_COOLDOWN = "Whirlwind Totem Cooldown";
	public static final String CHARM_CDR = "Whirlwind Totem Cooldown Reduction";
	public static final String CHARM_MAX_CDR = "Whirlwind Totem Maximum Cooldown Reduction";
	public static final String CHARM_SPEED = "Whirlwind Totem Speed Amplifier";
	public static final String CHARM_DURATION_BOOST = "Whirlwind Totem Duration Buff Amplifier";
	public static final String CHARM_PULSE_DELAY = "Whirlwind Totem Pulse Delay";

	public static final AbilityInfo<WhirlwindTotem> INFO =
		new AbilityInfo<>(WhirlwindTotem.class, "Whirlwind Totem", WhirlwindTotem::new)
			.linkedSpell(ClassAbility.WHIRLWIND_TOTEM)
			.scoreboardId("WhirlwindTotem")
			.shorthandName("WWT")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Summon a totem that provides cooldown reduction to players within its radius.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", WhirlwindTotem::cast, new AbilityTrigger(AbilityTrigger.Key.DROP).sneaking(false)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)))
			.displayItem(Material.BLUE_STAINED_GLASS);

	private final double mCDRPercent;
	private final int mCDRMax;
	private final double mSpeed;
	private final double mDurationBoost;
	private final int mInterval;
	private final WhirlwindTotemCS mCosmetic;

	public WhirlwindTotem(Plugin plugin, Player player) {
		super(plugin, player, INFO, "Whirlwind Totem Projectile", "WhirlwindTotem", "Whirlwind Totem");
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, isLevelOne() ? DURATION_1 : DURATION_2);
		setRadius(CharmManager.getRadius(mPlayer, CHARM_RADIUS, AOE_RANGE));
		mCDRPercent = CDR_PERCENT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_CDR);
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
	public void pulse(Location standLocation, ItemStatManager.PlayerItemStats stats, boolean chainLightning) {
		for (Player p : getPlayersInRange()) {
			if (isLevelTwo()) {
				mPlugin.mEffectManager.addEffect(p, WHIRLWIND_SPEED_EFFECT_NAME,
					new PercentSpeed(50, mSpeed, WHIRLWIND_SPEED_EFFECT_NAME).deleteOnAbilityUpdate(true));
			}
			if (chainLightning) {
				double chainLightningModifier = ChainLightning.ENHANCE_SUPPORT_EFFICIENCY + CharmManager.getLevelPercentDecimal(mPlayer, ChainLightning.CHARM_SUPPORT_TOTEM_EFFICIENCY);
				for (Ability abil : mPlugin.mAbilityManager.getPlayerAbilities(p).getAbilities()) {
					ClassAbility linkedSpell = abil.getInfo().getLinkedSpell();
					if (linkedSpell == null || linkedSpell == ClassAbility.WHIRLWIND_TOTEM) {
						continue;
					}
					int totalCD = abil.getModifiedCooldown();
					int reducedCD = (int) (Math.min(totalCD * mCDRPercent, mCDRMax) * chainLightningModifier);
					mPlugin.mTimers.updateCooldown(p, linkedSpell, reducedCD);
				}
			} else {
				mPlugin.mEffectManager.addEffect(p, "WhirlwindTotemCDR", new ShamanCooldownDecreasePerSecond(50, mCDRPercent, mCDRMax, mPlugin).deleteOnAbilityUpdate(true));
			}
		}

		mCosmetic.whirlwindTotemPulse(mPlayer, standLocation, getTotemRadius());
		applyWhirlwindDurationBoost();
	}

	@Override
	public void onTotemExpire(World world, Location standLocation) {
		mCosmetic.whirlwindTotemExpire(mPlayer, world, standLocation, getTotemRadius());
	}

	public void applyWhirlwindDurationBoost() {
		for (Ability abil : mPlugin.mAbilityManager.getPlayerAbilities(mPlayer).getAbilities()) {
			if (abil instanceof TotemAbility totemAbility && totemAbility.getRemainingAbilityDuration() > 0 && !(abil instanceof WhirlwindTotem)) {
				totemAbility.mWhirlwindBuffPercent = 1 + mDurationBoost;
			}
		}
	}

	private static Description<WhirlwindTotem> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Summon a *Totem* that periodically reduces nearby").styles(Shaman.TOTEM_COLOR)
			.addLine("players' ability cooldowns while inside the totem's")
			.addLine("area, and for %t after leaving.")
				.statValues(stat(50))
			.addLine("(Doesn't reduce this ability's cooldown)")
			.addLine()
			.addStat("Cooldown Reduction: %p (max %t) every %t")
				.statValues(stat(a -> a.mCDRPercent, CDR_PERCENT), stat(a -> a.mCDRMax, CDR_MAX_PER_SECOND), stat(20))
			.addStat("Radius: %r").statValues(stat(a -> a.mRadius, AOE_RANGE))
			.addStat("Duration: %t1").statValues(stat(a -> a.mDuration, DURATION_1))
			.addStat("Cooldown: %t").statValues(cooldown(COOLDOWN))
			.addLine()
			.addLine("*Totems* summoned while *Whirlwind Totem* is active").styles(Shaman.TOTEM_COLOR, UNDERLINED)
			.addLine("gain +%p increased duration.")
				.statValues(stat(a -> a.mDurationBoost, DURATION_BOOST))
			.addDashedLine();
	}

	private static Description<WhirlwindTotem> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Whirlwind Totem*'s duration.").styles(UNDERLINED)
			.addLine()
			.addStatComparison("Duration: %t1 -> %t2").statValues(stat(DURATION_1), stat(a -> a.mDuration, DURATION_2))
			.addLine()
			.addLine("*Whirlwind Totem* now grants players speed.").styles(UNDERLINED)
			.addLine()
			.addStat("Effect: +%p Speed").statValues(stat(a -> a.mSpeed, SPEED_PERCENT))
			.addDashedLine();
	}
}

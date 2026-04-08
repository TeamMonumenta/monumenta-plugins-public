package com.playmonumenta.plugins.abilities.shaman.hexbreaker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.shaman.ChainLightning;
import com.playmonumenta.plugins.abilities.shaman.FlameTotem;
import com.playmonumenta.plugins.abilities.shaman.LightningTotem;
import com.playmonumenta.plugins.abilities.shaman.TotemAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.shaman.hexbreaker.DecayedTotemCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class DecayedTotem extends TotemAbility {
	private static final int COOLDOWN = 26 * 20;
	private static final int AOE_RANGE = 8;
	private static final int INTERVAL_1 = 20;
	private static final int INTERVAL_2 = 15;
	private static final int DURATION_1 = 10 * 20;
	private static final int DURATION_2 = 12 * 20;
	private static final int DAMAGE = 5;
	private static final double SLOWNESS_PERCENT = 0.3;
	private static final int TARGETS = 3;
	private static final double FLAME_TOTEM_DAMAGE_BUFF_1 = 1;
	private static final double FLAME_TOTEM_DAMAGE_BUFF_2 = 1.5;
	private static final double LIGHTNING_TOTEM_DAMAGE_BUFF_1 = 1.5;
	private static final double LIGHTNING_TOTEM_DAMAGE_BUFF_2 = 2.5;

	public static final String CHARM_DURATION = "Decayed Totem Duration";
	public static final String CHARM_RADIUS = "Decayed Totem Radius";
	public static final String CHARM_COOLDOWN = "Decayed Totem Cooldown";
	public static final String CHARM_TARGETS = "Decayed Totem Targets";
	public static final String CHARM_DAMAGE = "Decayed Totem Damage";
	public static final String CHARM_SLOWNESS = "Decayed Totem Slowness Amplifier";
	public static final String CHARM_FLAME_TOTEM_DAMAGE_BUFF = "Decayed Totem Flame Totem Damage";
	public static final String CHARM_LIGHTNING_TOTEM_DAMAGE_BUFF = "Decayed Totem Lightning Totem Damage";
	public static final String CHARM_PULSE_DELAY = "Decayed Totem Pulse Delay";

	public static final AbilityInfo<DecayedTotem> INFO =
		new AbilityInfo<>(DecayedTotem.class, "Decayed Totem", DecayedTotem::new)
			.linkedSpell(ClassAbility.DECAYED_TOTEM)
			.scoreboardId("DecayedTotem")
			.shorthandName("DT")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Summons a totem, dealing damage and heavily slowing some of the mobs within range.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", DecayedTotem::cast, new AbilityTrigger(AbilityTrigger.Key.DROP).sneaking(false)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)))
			.displayItem(Material.WITHER_ROSE);

	private final double mDamage;
	private final double mSlowness;
	private final int mTargetCount;
	private final List<LivingEntity> mTargets = new ArrayList<>();
	private final double mFlameTotemBuff;
	private final double mLightningTotemBuff;
	private final int mInterval;
	private final DecayedTotemCS mCosmetic;

	public DecayedTotem(Plugin plugin, Player player) {
		super(plugin, player, INFO, "Decayed Totem Projectile", "DecayedTotem", "Decayed Totem");
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, DAMAGE);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, isLevelOne() ? DURATION_1 : DURATION_2);
		setRadius(CharmManager.getRadius(mPlayer, CHARM_RADIUS, AOE_RANGE));
		mInterval = CharmManager.getDuration(mPlayer, CHARM_PULSE_DELAY, isLevelTwo() ? INTERVAL_2 : INTERVAL_1);
		mSlowness = SLOWNESS_PERCENT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SLOWNESS);
		mTargetCount = TARGETS + (int) CharmManager.getLevel(mPlayer, CHARM_TARGETS);
		mFlameTotemBuff = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_FLAME_TOTEM_DAMAGE_BUFF, isLevelOne() ? FLAME_TOTEM_DAMAGE_BUFF_1 : FLAME_TOTEM_DAMAGE_BUFF_2);
		mLightningTotemBuff = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_LIGHTNING_TOTEM_DAMAGE_BUFF, isLevelOne() ? LIGHTNING_TOTEM_DAMAGE_BUFF_1 : LIGHTNING_TOTEM_DAMAGE_BUFF_2);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new DecayedTotemCS());
	}

	@Override
	public void placeTotem(Location standLocation, Player player, ArmorStand stand) {
		applyDecayedDamageBoost();
		mCosmetic.decayedTotemSpawn(mPlayer, stand);
	}

	@Override
	public void onTotemTick(int ticks, ArmorStand stand, World world, Location standLocation, ItemStatManager.PlayerItemStats stats) {
		mCosmetic.decayedTotemTick(mPlayer, stand);
		mTargets.removeIf(mob -> !mob.getWorld().equals(standLocation.getWorld()) || standLocation.distance(mob.getLocation()) >= getTotemRadius() || mob.isDead());
		if (mTargets.size() < mTargetCount) {
			List<LivingEntity> affectedMobs = EntityUtils.getNearbyMobsInSphere(standLocation, getTotemRadius(), null);
			Collections.shuffle(affectedMobs);

			for (LivingEntity mob : affectedMobs) {
				if (mTargets.contains(mob) || standLocation.distance(mob.getLocation()) >= getTotemRadius()) {
					continue;
				}
				impactMob(mob, mInterval + 5, false, false, stats);
				mTargets.add(mob);
				if (mTargets.size() >= mTargetCount) {
					break;
				}
			}
		}

		if (ticks % 5 == 0) {
			for (LivingEntity target : mTargets) {
				mCosmetic.decayedTotemAnchor(mPlayer, stand, target);
			}
		}
		if (ticks % mInterval == 0) {
			pulse(standLocation, stats, false);
		}
	}

	private void impactMob(LivingEntity target, int duration, boolean dealDamage, boolean chainLightning, ItemStatManager.PlayerItemStats stats) {
		if (dealDamage) {
			DamageUtils.damage(mPlayer, target,
				new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC, mInfo.getLinkedSpell(), stats),
				mDamage * mSpiritualismMultiplier *
					(chainLightning ? (ChainLightning.ENHANCE_OFFENSIVE_EFFICIENCY + CharmManager.getLevelPercentDecimal(mPlayer, ChainLightning.CHARM_OFFENSIVE_TOTEM_EFFICIENCY)) : 1),
				true, false, false);
		}
		EntityUtils.applySlow(mPlugin, duration, mSlowness, target);
	}

	@Override
	public void onTotemExpire(World world, Location standLocation) {
		mCosmetic.decayedTotemExpire(mPlayer, world, standLocation);
		mTargets.clear();
		applyDecayedDamageBoost();
	}

	@Override
	public void pulse(Location standLocation, ItemStatManager.PlayerItemStats stats, boolean chainLightning) {
		applyDecayedDamageBoost();
		for (LivingEntity target : mTargets) {
			impactMob(target, mInterval + 20, true, chainLightning, stats);
		}
	}

	public void applyDecayedDamageBoost() {
		for (Ability abil : mPlugin.mAbilityManager.getPlayerAbilities(mPlayer).getAbilities()) {
			if (abil instanceof TotemAbility totemAbility && totemAbility.getRemainingAbilityDuration() > 0 && !(abil instanceof DecayedTotem)) {
				if (totemAbility instanceof FlameTotem totem) {
					totem.mDecayedTotemBuff = mFlameTotemBuff;
					totemAbility.mDecayedBuffed = true;
				} else if (totemAbility instanceof LightningTotem totem) {
					totem.mDecayedTotemBuff = mLightningTotemBuff;
					totemAbility.mDecayedBuffed = true;
				}
			}
		}
	}

	private static Description<DecayedTotem> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Summon a *Totem* that anchors to up to %d mobs,").styles(Shaman.TOTEM_COLOR)
				.statValues(stat(a -> a.mTargetCount, TARGETS))
			.addLine("dealing damage to them and slowing them.")
			.addLine()
			.addStat("Damage: %d (s) every %t1")
				.statValues(stat(a -> a.mDamage, DAMAGE), stat(a -> a.mInterval, INTERVAL_1))
			.addStat("Effect: %p Slowness")
				.statValues(stat(a -> a.mSlowness, SLOWNESS_PERCENT))
			.addStat("Radius: %r")
				.statValues(stat(a -> a.mRadius, AOE_RANGE))
			.addStat("Duration: %t1")
				.statValues(stat(a -> a.mDuration, DURATION_1))
			.addStat("Cooldown: %t")
				.statValues(cooldown(COOLDOWN))
			.addLine()
			.addLine("*Flame* and *Lightning Totems* summoned while").styles(UNDERLINED, UNDERLINED)
			.addLine("*Decayed Totem* is active deal increased damage.").styles(UNDERLINED)
			.addLine()
			.addStat("Flame Totem Boost: +%d1 (s)")
				.statValues(stat(a -> a.mFlameTotemBuff, FLAME_TOTEM_DAMAGE_BUFF_1))
			.addStat("Lightning Totem Boost: +%d1 (s)")
				.statValues(stat(a -> a.mLightningTotemBuff, LIGHTNING_TOTEM_DAMAGE_BUFF_1))
			.addDashedLine();
	}

	private static Description<DecayedTotem> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Decayed Totem*'s rate of damage").styles(UNDERLINED)
			.addLine("and its duration.")
			.addLine()
			.addLine("Increase the damage boost given to *Flame*").styles(UNDERLINED)
			.addLine("and *Lightning Totems*.").styles(UNDERLINED)
			.addLine()
			.addStatComparison("Interval: %t1 -> %t2")
				.statValues(stat(INTERVAL_1), stat(a -> a.mInterval, INTERVAL_2))
			.addStatComparison("Duration: %t1 -> %t2")
				.statValues(stat(DURATION_1), stat(a -> a.mDuration, DURATION_2))
			.addStatComparison("Flame Totem Boost: +%d1 -> +%d2 (s)")
				.statValues(stat(FLAME_TOTEM_DAMAGE_BUFF_1), stat(a -> a.mFlameTotemBuff, FLAME_TOTEM_DAMAGE_BUFF_2))
			.addStatComparison("Lightning Totem Boost: +%d1 -> +%d2 (s)")
				.statValues(stat(LIGHTNING_TOTEM_DAMAGE_BUFF_1), stat(a -> a.mLightningTotemBuff, LIGHTNING_TOTEM_DAMAGE_BUFF_2))
			.addDashedLine();
	}
}

package com.playmonumenta.plugins.abilities.warlock.reaper;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warlock.reaper.VoodooBondsCS;
import com.playmonumenta.plugins.effects.VoodooBondsCurse;
import com.playmonumenta.plugins.effects.VoodooBondsOtherPlayer;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class VoodooBonds extends MultipleChargeAbility {

	private static final int COOLDOWN = 10 * 20;
	private static final int MAX_CHARGES = 2;
	private static final double PIN_DAMAGE = 3;
	private static final double PIN_ADDITIONAL_DAMAGE = 9;
	private static final double RANGE = 10;
	private static final double CURSE_DAMAGE_1 = 0.15;
	private static final double CURSE_DAMAGE_2 = 0.2;
	private static final double CURSE_RADIUS = 4;
	private static final int CURSE_SPREAD_COUNT_1 = 1;
	private static final int CURSE_SPREAD_COUNT_2 = 2;
	private static final double CURSE_SPREAD_RADIUS = 10;
	private static final int CURSE_DURATION = 8 * 20;
	private static final int PROTECTION_DURATION = 8 * 20;
	private static final double PROTECTION_RESIST = 0.25;

	public static final String CURSE_EFFECT = "VoodooBondsCurse";
	public static final String PROTECTION_EFFECT = "VoodooBondsOtherPlayer";

	public static final String CHARM_COOLDOWN = "Voodoo Bonds Cooldown";
	public static final String CHARM_CHARGES = "Voodoo Bonds Charges";
	public static final String CHARM_DAMAGE = "Voodoo Bonds Damage"; // affects pin and curse damage
	public static final String CHARM_PIN_DAMAGE = "Voodoo Bonds Pin Damage";
	public static final String CHARM_PIN_RANGE = "Voodoo Bonds Pin Range";
	public static final String CHARM_CURSE_DAMAGE = "Voodoo Bonds Curse Damage";
	public static final String CHARM_CURSE_RADIUS = "Voodoo Bonds Curse Radius";
	public static final String CHARM_CURSE_SPREAD_COUNT = "Voodoo Bonds Curse Spread Count";
	public static final String CHARM_CURSE_SPREAD_RADIUS = "Voodoo Bonds Curse Spread Radius";
	public static final String CHARM_CURSE_DURATION = "Voodoo Bonds Curse Duration";
	public static final String CHARM_PROTECT_DURATION = "Voodoo Bonds Protection Duration";
	public static final String CHARM_RECEIVED_DAMAGE = "Voodoo Bonds Received Damage";

	public static final AbilityInfo<VoodooBonds> INFO =
		new AbilityInfo<>(VoodooBonds.class, "Voodoo Bonds", VoodooBonds::new)
			.linkedSpell(ClassAbility.VOODOO_BONDS)
			.scoreboardId("VoodooBonds")
			.shorthandName("VB")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Slash and fire a pin that curses mobs and protects players.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", VoodooBonds::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sprinting(true), AbilityTriggerInfo.HOLDING_SCYTHE_RESTRICTION))
			.displayItem(Material.JACK_O_LANTERN);

	private final double mRange;
	private final double mPinDamage;
	private final double mPinAdditionalDamage;
	private final double mCurseDamage;
	private final double mCurseRadius;
	private final int mCurseSpreadCount;
	private final double mCurseSpreadRadius;
	private final int mCurseDuration;
	private final int mProtectionDuration;
	private final double mProtectionResist;

	private int mLastCastTicks = 0;
	private final VoodooBondsCS mCosmetic;

	public VoodooBonds(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mMaxCharges = MAX_CHARGES + (int) CharmManager.getLevel(player, CHARM_CHARGES);
		mCharges = getTrackedCharges();

		mRange = CharmManager.calculateFlatAndPercentValue(player, CHARM_PIN_RANGE, RANGE);
		mPinDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_PIN_DAMAGE, CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, PIN_DAMAGE));
		mPinAdditionalDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_PIN_DAMAGE, CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, PIN_ADDITIONAL_DAMAGE));
		mCurseDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_CURSE_DAMAGE, CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? CURSE_DAMAGE_1 : CURSE_DAMAGE_2));
		mCurseRadius = CharmManager.getRadius(player, CHARM_CURSE_RADIUS, CURSE_RADIUS);
		mCurseSpreadCount = (isLevelOne() ? CURSE_SPREAD_COUNT_1 : CURSE_SPREAD_COUNT_2) + (int) CharmManager.getLevel(player, CHARM_CURSE_SPREAD_COUNT);
		mCurseSpreadRadius = CharmManager.getRadius(player, CHARM_CURSE_SPREAD_RADIUS, CURSE_SPREAD_RADIUS);
		mCurseDuration = CharmManager.getDuration(player, CHARM_CURSE_DURATION, CURSE_DURATION);
		mProtectionDuration = CharmManager.getDuration(player, CHARM_PROTECT_DURATION, PROTECTION_DURATION);
		mProtectionResist = PROTECTION_RESIST;

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new VoodooBondsCS());
	}

	public boolean cast() {
		int ticks = Bukkit.getServer().getCurrentTick();
		// Prevent double casting on accident
		if (ticks - mLastCastTicks <= 4 || !consumeCharge()) {
			return false;
		}
		mLastCastTicks = ticks;

		Location startLoc = mPlayer.getEyeLocation();
		Vector direction = mPlayer.getEyeLocation().getDirection();
		launchPin(startLoc, direction);

		return true;
	}

	public void launchPin(Location startLoc, Vector direction) {
		Location endLoc = LocationUtils.rayTraceToBlock(startLoc, direction, mRange, null);
		Set<LivingEntity> hitMobs = new HashSet<>(EntityUtils.getMobsInLine(startLoc, endLoc, 0.4)); // Set, as to not double-count mobs
		hitMobs.addAll(Hitbox.approximateCone(mPlayer.getEyeLocation(), 5, Math.toRadians(45)).getHitMobs());
		for (LivingEntity mob : hitMobs) {
			mCosmetic.hitMob(mPlayer, mob);
			DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MELEE_SKILL, mPinDamage, ClassAbility.VOODOO_BONDS_PIN, true, true);

			if (isLevelTwo() && mPlugin.mEffectManager.hasEffect(mob, CURSE_EFFECT)) {
				DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MELEE_SKILL, mPinAdditionalDamage, ClassAbility.VOODOO_BONDS, true, false);
			}

			// add on the next server tick to prevent it from affecting later iterations of this loop
			Bukkit.getScheduler().runTask(mPlugin, () -> mPlugin.mEffectManager.addEffect(mob, CURSE_EFFECT, new VoodooBondsCurse(mPlayer, mCurseDuration, mCurseDamage, mCurseRadius, mCurseSpreadCount, mCurseSpreadRadius, mCosmetic)));
		}

		List<Player> hitPlayers = EntityUtils.getPlayersInLine(startLoc, startLoc.getDirection(), mRange, 0.4, mPlayer);
		for (Player player : hitPlayers) {
			mCosmetic.hitPlayer(mPlayer, player);
			mPlugin.mEffectManager.addEffect(player, PROTECTION_EFFECT, new VoodooBondsOtherPlayer(mProtectionDuration, mPlayer, mProtectionResist));
		}

		mCosmetic.launchPin(mPlayer, startLoc, endLoc);
	}

	private static Description<VoodooBonds> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to slash in a small arc and fire a pin that travels ")
			.add(a -> a.mRange, RANGE)
			.add(" blocks. Mobs hit take ")
			.add(a -> a.mPinDamage, PIN_DAMAGE)
			.add(" melee damage and are cursed for ")
			.addDuration(a -> a.mCurseDuration, CURSE_DURATION)
			.add(" seconds. When a cursed mob takes melee, projectile, or magic damage (except from this ability), all other cursed mobs within ")
			.add(a -> a.mCurseRadius, CURSE_RADIUS)
			.add(" blocks take ")
			.addPercent(a -> a.mCurseDamage, CURSE_DAMAGE_1, false, Ability::isLevelOne)
			.add(" of the suffered damage. Critical melee hits against cursed mobs will spread it to ")
			.add(a -> a.mCurseSpreadCount, CURSE_SPREAD_COUNT_1, false, Ability::isLevelOne)
			.add(" other un-cursed mob within ")
			.add(a -> a.mCurseSpreadRadius, CURSE_SPREAD_RADIUS)
			.add(" blocks. Players hit are bonded to you instead; the next hit they take within ")
			.addDuration(a -> a.mProtectionDuration, PROTECTION_DURATION)
			.add(" seconds will be redirected to you based on the percentage of health that player would have lost, but cannot reduce your health below 1. Charges: ")
			.add(a -> a.mMaxCharges, MAX_CHARGES)
			.add(".")
			.addCooldown(COOLDOWN);
	}

	private static Description<VoodooBonds> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The curse now deals ")
			.addPercent(a -> a.mCurseDamage, CURSE_DAMAGE_2, false, Ability::isLevelTwo)
			.add(" of taken damage, and it spreads to ")
			.add(a -> a.mCurseSpreadCount, CURSE_SPREAD_COUNT_2, false, Ability::isLevelTwo)
			.add(" other mobs on critical strikes. The slash and pin deal ")
			.add(a -> a.mPinAdditionalDamage, PIN_ADDITIONAL_DAMAGE)
			.add(" additional damage to already-cursed mobs. Take ")
			.addPercent(a -> a.mProtectionResist, PROTECTION_RESIST)
			.add(" less damage when redirecting damage from other players.");
	}
}

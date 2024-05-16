package com.playmonumenta.plugins.abilities.warlock.reaper;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
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
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
	private static final int MAX_TARGETS = 3;
	private static final double RANGE = 10;
	private static final double CURSE_SPREAD_DAMAGE = 0.2;
	private static final double CURSE_SPREAD_RADIUS = 3;
	private static final int CURSE_DURATION = 8 * 20;
	private static final int PROTECTION_DURATION = 8 * 20;
	private static final double CURSE_DEATH_DAMAGE = 6;
	private static final int CURSE_DEATH_EXTENSION = 3 * 20;

	public static final String CURSE_EFFECT = "VoodooBondsCurse";
	public static final String PROTECTION_EFFECT = "VoodooBondsOtherPlayer";

	public static final String CHARM_COOLDOWN = "Voodoo Bonds Cooldown";
	public static final String CHARM_CHARGES = "Voodoo Bonds Charges";
	public static final String CHARM_DAMAGE = "Voodoo Bonds Damage"; // affects pin, curse, and death damage
	public static final String CHARM_PIN_DAMAGE = "Voodoo Bonds Pin Damage";
	public static final String CHARM_PIN_RANGE = "Voodoo Bonds Pin Range";
	public static final String CHARM_MAX_TARGETS = "Voodoo Bonds Pin Max Targets";
	public static final String CHARM_CURSE_DAMAGE = "Voodoo Bonds Curse Damage";
	public static final String CHARM_CURSE_RADIUS = "Voodoo Bonds Curse Radius";
	public static final String CHARM_CURSE_DURATION = "Voodoo Bonds Curse Duration";
	public static final String CHARM_PROTECT_DURATION = "Voodoo Bonds Protection Duration";
	public static final String CHARM_RECEIVED_DAMAGE = "Voodoo Bonds Received Damage";
	public static final String CHARM_DEATH_DAMAGE = "Voodoo Bonds Curse Death Damage";
	public static final String CHARM_DEATH_EXTENSION = "Voodoo Bonds Curse Death Extension";

	public static final AbilityInfo<VoodooBonds> INFO =
		new AbilityInfo<>(VoodooBonds.class, "Voodoo Bonds", VoodooBonds::new)
			.linkedSpell(ClassAbility.VOODOO_BONDS)
			.scoreboardId("VoodooBonds")
			.shorthandName("VB")
			.descriptions(
				("Left click while holding a projectile weapon to fire a pin that travels forwards for %s blocks and hits the first %s mobs or players in its path. " +
					"Mobs hit take %s melee damage and are cursed for %ss. When a cursed mob takes non-ailment damage (except from this ability), all other mobs of the same type in a %s block radius take %s%% of the suffered damage. " +
					"Players hit are bonded to you instead; the next hit they take within the next %ss will be redirected to you based on the percentage of health that player would have lost, but cannot reduce your health below 1. " +
					"Charges: %s. Charge Cooldown: %ss.")
				.formatted(StringUtils.to2DP(RANGE), MAX_TARGETS, StringUtils.to2DP(PIN_DAMAGE), StringUtils.ticksToSeconds(CURSE_DURATION),
					StringUtils.to2DP(CURSE_SPREAD_RADIUS), StringUtils.multiplierToPercentage(CURSE_SPREAD_DAMAGE),
					StringUtils.ticksToSeconds(PROTECTION_DURATION), MAX_CHARGES, StringUtils.ticksToSeconds(COOLDOWN)),
				("Pins now pierce all mobs and players in their path instead of stopping at %s. " +
					"Mobs hit by Judgement Chain automatically have a pin fired at them. " +
					"When a cursed mob dies, 3 other cursed mobs take %s melee damage and have their curse extended by %ss.")
					.formatted(MAX_TARGETS, StringUtils.to2DP(CURSE_DEATH_DAMAGE), StringUtils.ticksToSeconds(CURSE_DEATH_EXTENSION)))
			.simpleDescription("Fire a pin that curses mobs and protects players.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", VoodooBonds::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).keyOptions(AbilityTrigger.KeyOptions.REQUIRE_PROJECTILE_WEAPON)))
			.displayItem(Material.JACK_O_LANTERN);

	private final double mRange;
	private final int mMaxTargets;
	private final double mPinDamage;
	private final double mCurseDamage;
	private final double mCurseRadius;
	private final int mCurseDuration;
	private final int mProtectionDuration;
	private final double mCurseDeathDamage;
	private final int mCurseExtension;
	private int mLastCastTicks = 0;
	private final VoodooBondsCS mCosmetic;

	public VoodooBonds(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mMaxCharges = MAX_CHARGES + (int) CharmManager.getLevel(player, CHARM_CHARGES);
		mCharges = getTrackedCharges();

		mRange = CharmManager.calculateFlatAndPercentValue(player, CHARM_PIN_RANGE, RANGE);
		mMaxTargets = MAX_TARGETS + (int) CharmManager.getLevel(player, CHARM_MAX_TARGETS);
		mPinDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_PIN_DAMAGE, CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, PIN_DAMAGE));
		mCurseDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_CURSE_DAMAGE, CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, CURSE_SPREAD_DAMAGE));
		mCurseRadius = CharmManager.calculateFlatAndPercentValue(player, CHARM_CURSE_RADIUS, CURSE_SPREAD_RADIUS);
		mCurseDuration = CharmManager.getDuration(player, CHARM_CURSE_DURATION, CURSE_DURATION);
		mProtectionDuration = CharmManager.getDuration(player, CHARM_PROTECT_DURATION, PROTECTION_DURATION);
		mCurseDeathDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DEATH_DAMAGE, CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, CURSE_DEATH_DAMAGE));
		mCurseExtension = CharmManager.getDuration(player, CHARM_DEATH_EXTENSION, CURSE_DEATH_EXTENSION);

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
		launchPin(startLoc, direction, true, true);

		return true;
	}

	public void launchPin(Location startLoc, Vector direction, boolean doKnockback, boolean doSound) {
		Location endLoc = LocationUtils.rayTraceToBlock(startLoc, direction, mRange, null);

		List<LivingEntity> hitMobs = EntityUtils.getMobsInLine(startLoc, endLoc, 0.4);
		List<Player> hitPlayers = EntityUtils.getPlayersInLine(startLoc, startLoc.getDirection(), mRange, 0.4, mPlayer);
		List<LivingEntity> hitTargets = new ArrayList<>(); // we need a list of both mobs and players in the line, sorted by distance
		hitTargets.addAll(hitMobs);
		hitTargets.addAll(hitPlayers);
		hitTargets.sort(Comparator.comparingDouble(target -> target.getLocation().distance(startLoc)));

		int i = 0;
		for (LivingEntity target : hitTargets) {
			if (target instanceof Player player) {
				mCosmetic.hitPlayer(mPlayer, player);

				mPlugin.mEffectManager.addEffect(player, PROTECTION_EFFECT, new VoodooBondsOtherPlayer(mProtectionDuration, mPlayer));
			} else {
				mCosmetic.hitMob(mPlayer, target, doSound);

				DamageUtils.damage(mPlayer, target, DamageEvent.DamageType.MELEE_SKILL, mPinDamage, ClassAbility.VOODOO_BONDS, true, doKnockback);
				mPlugin.mEffectManager.addEffect(target, CURSE_EFFECT, new VoodooBondsCurse(mPlayer, mCurseDuration, mCurseDamage, mCurseRadius, isLevelTwo(), mCurseDeathDamage, mCurseExtension, mCosmetic));
			}

			i++;
			if (isLevelOne() && i >= mMaxTargets) {
				break;
			}
		}

		mCosmetic.launchPin(mPlayer, startLoc, endLoc, doSound);
	}
}

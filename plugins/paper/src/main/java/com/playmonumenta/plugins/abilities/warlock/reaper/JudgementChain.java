package com.playmonumenta.plugins.abilities.warlock.reaper;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warlock.reaper.JudgementChainCS;
import com.playmonumenta.plugins.effects.JudgementChainMobEffect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import java.util.function.Predicate;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class JudgementChain extends MultipleChargeAbility {
	private static final int COOLDOWN = 12 * 20;
	private static final int MAX_CHARGES = 2;
	private static final int RANGE = 20;
	private static final int CHAIN_DURATION = 8 * 20;
	private static final double SLOWNESS_AMOUNT = 0.4;
	private static final double WEAKNESS_AMOUNT = 0.4;
	private static final double DMG_BOOST_1 = 0.1;
	private static final double DMG_BOOST_2 = 0.2;
	private static final int DEBUFF_DURATION = 3 * 20;
	private static final int EXTRA_TARGETS = 2;
	private static final double EXTRA_TARGET_RADIUS = 3;
	public static final String EFFECT_NAME = "JudgementChainEffect";
	public static final String CHARM_CHARGES = "Judgement Chain Charges";
	public static final String CHARM_COOLDOWN = "Judgement Chain Cooldown";
	public static final String CHARM_RANGE = "Judgement Chain Range";
	public static final String CHARM_CHAIN_DURATION = "Judgement Chain Duration";
	public static final String CHARM_CHAIN_DAMAGE = "Judgement Chain Damage Boost";
	public static final String CHARM_SLOWNESS = "Judgement Chain Slowness Amplifier";
	public static final String CHARM_WEAKNESS = "Judgement Chain Weakness Amplifier";
	public static final String CHARM_DEBUFF_DURATION = "Judgement Chain Debuff Duration";
	public static final String CHARM_EXTRA_TARGETS = "Judgement Chain Extra Targets";
	public static final String CHARM_EXTRA_TARGET_RADIUS = "Judgement Chain Extra Target Radius";

	public static final AbilityInfo<JudgementChain> INFO =
		new AbilityInfo<>(JudgementChain.class, "Judgement Chain", JudgementChain::new)
			.linkedSpell(ClassAbility.JUDGEMENT_CHAIN)
			.scoreboardId("JudgementChain")
			.shorthandName("JC")
			.actionBarColor(TextColor.color(115, 115, 115))
			.descriptions(
				("Swap hands while looking at an unchained mob within %s blocks to teleport them in front of you and chain them to you for %ss, " +
					"taunting them and afflicting them with %s%% Slowness and %s%% Weakness for %ss. " +
					"You deal %s%% more damage to chained mobs. " +
					"Bosses and crowd control/knockback immune mobs cannot be teleported, but will be chained and debuffed. Charges: %s. Charge Cooldown: %ss.")
				.formatted(RANGE, StringUtils.ticksToSeconds(CHAIN_DURATION), StringUtils.multiplierToPercentage(SLOWNESS_AMOUNT),
					StringUtils.multiplierToPercentage(WEAKNESS_AMOUNT), StringUtils.ticksToSeconds(DEBUFF_DURATION),
					StringUtils.multiplierToPercentage(DMG_BOOST_1),
					MAX_CHARGES, StringUtils.ticksToSeconds(COOLDOWN)),
				("Judgement Chain now additionally teleports and chains the %s closest mobs within %s blocks of the targeted mob, " +
					"and you deal %s%% more damage to chained mobs.")
				.formatted(EXTRA_TARGETS, StringUtils.to2DP(EXTRA_TARGET_RADIUS), StringUtils.multiplierToPercentage(DMG_BOOST_2)))
			.simpleDescription("Teleport a mob to you and debuff it.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", JudgementChain::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false),
				AbilityTriggerInfo.HOLDING_SCYTHE_RESTRICTION))
			.displayItem(Material.CHAIN);

	private final double mRange;
	private final int mChainDuration;
	private final double mChainDmgBonus;
	private final double mSlowAmount;
	private final double mWeakenAmount;
	private final int mDebuffDuration;
	private final int mExtraTargets;
	private final double mExtraTargetRadius;
	private int mLastCastTicks = 0;
	private final JudgementChainCS mCosmetic;

	public JudgementChain(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mMaxCharges = MAX_CHARGES + (int) CharmManager.getLevel(player, CHARM_CHARGES);
		mCharges = getTrackedCharges();

		mRange = CharmManager.getRadius(player, CHARM_RANGE, RANGE);
		mChainDuration = CharmManager.getDuration(player, CHARM_CHAIN_DURATION, CHAIN_DURATION);
		mChainDmgBonus = (isLevelOne() ? DMG_BOOST_1 : DMG_BOOST_2) + CharmManager.getLevelPercentDecimal(player, CHARM_CHAIN_DAMAGE);
		mSlowAmount = SLOWNESS_AMOUNT + CharmManager.getLevelPercentDecimal(player, CHARM_SLOWNESS);
		mWeakenAmount = WEAKNESS_AMOUNT + CharmManager.getLevelPercentDecimal(player, CHARM_WEAKNESS);
		mDebuffDuration = CharmManager.getDuration(player, CHARM_DEBUFF_DURATION, DEBUFF_DURATION);
		mExtraTargets = EXTRA_TARGETS + (int) CharmManager.getLevel(player, CHARM_EXTRA_TARGETS);
		mExtraTargetRadius = CharmManager.getRadius(player, CHARM_EXTRA_TARGET_RADIUS, EXTRA_TARGET_RADIUS);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new JudgementChainCS());
	}

	public boolean cast() {
		Predicate<Entity> filter = e -> EntityUtils.isHostileMob(e) && !ScoreboardUtils.checkTag(e, AbilityUtils.IGNORE_TAG) && !e.isDead() && e.isValid() && !mPlugin.mEffectManager.hasEffect(e, EFFECT_NAME);
		// try three times, with increasing hitbox size if no mob is found (0.6, 1.2, 1.8)
		LivingEntity entity = null;
		for (int i = 1; i <= 3; i++) {
			entity = EntityUtils.getEntityAtCursor(mPlayer, mRange, filter, 0.6 * i);
			if (entity != null) {
				break;
			}
		}

		if (entity == null) {
			return false;
		}

		int ticks = Bukkit.getServer().getCurrentTick();
		// Prevent double casting on accident
		if (ticks - mLastCastTicks <= 4 || !consumeCharge()) {
			return false;
		}
		mLastCastTicks = ticks;

		Location entityLoc = entity.getLocation();

		summonChain(entity, new Vector(0, 1, 3));

		if (isLevelTwo()) {
			List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(entityLoc, mExtraTargetRadius, entity);
			nearbyMobs.removeIf(e -> mPlugin.mEffectManager.hasEffect(e, EFFECT_NAME));
			for (int i = 0; i < mExtraTargets; i++) {
				LivingEntity nearestMob = EntityUtils.getNearestMob(entityLoc, nearbyMobs);
				if (nearestMob != null) {
					double angle = Math.floorDiv(i + 2, 2) * 25 * (i % 2 == 0 ? 1 : -1);
					summonChain(nearestMob, VectorUtils.rotateYAxis(new Vector(0, 1, 3), angle));
					nearbyMobs.remove(nearestMob);
					if (nearbyMobs.isEmpty()) {
						break;
					}
				}
			}
		}

		return true;
	}

	public void summonChain(LivingEntity entity, Vector offset) {
		Location loc = mPlayer.getLocation();
		Location oldLocation = entity.getLocation();

		if (!(EntityUtils.isBoss(entity) || EntityUtils.isCCImmuneMob(entity) || EntityUtils.getAttributeOrDefault(entity, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 0) >= 1)) {
			Location destination = loc.clone().add(VectorUtils.rotateTargetDirection(offset, loc.getYaw(), loc.getPitch()));

			// don't teleport into the ground
			// if below us and inside a block, move up to the top, if it's there
			if (destination.getY() < loc.getY() && destination.getBlock().isSolid()) {
				destination = LocationUtils.fallToGround(destination.add(0, 1.5, 0), loc.getY());
			}
			// if below us and still not visible somehow, just place on the same y-level
			if (destination.getY() < loc.getY() && !mPlayer.hasLineOfSight(destination)) {
				destination.setY(loc.getY());
			}

			final Location finalDestination = destination;
			new BukkitRunnable() { // teleport multiple times so that it actually updates

				int mTicks = 0;
				@Override
				public void run() {
					EntityUtils.teleportStack(entity, finalDestination);

					mTicks++;
					if (mTicks >= 5) {
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}

		mCosmetic.onSummonChain(mPlayer, entity, oldLocation);

		EntityUtils.applyTaunt(entity, mPlayer);
		EntityUtils.applySlow(mPlugin, mDebuffDuration, mSlowAmount, entity);
		EntityUtils.applyWeaken(mPlugin, mDebuffDuration, mWeakenAmount, entity);
		mPlugin.mEffectManager.addEffect(entity, EFFECT_NAME, new JudgementChainMobEffect(mChainDuration, mPlayer, mCosmetic.glowColor()));

		cancelOnDeath(new BukkitRunnable() {
			int mT = 0;
			final LivingEntity mTarget = entity;

			@Override
			public void run() {
				mCosmetic.chain(mPlayer, mTarget, mT);

				if (mPlayer.isDead() || mTarget.isDead() || !mTarget.isValid() || mPlayer.getLocation().distance(mTarget.getLocation()) > 30 || mT > mChainDuration) {
					mCosmetic.onBreakChain(mPlayer, mTarget);
					mPlugin.mEffectManager.clearEffects(entity, EFFECT_NAME);
					this.cancel();
				}
				mT++;
			}
		}.runTaskTimer(mPlugin, 0, 1));
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (mPlugin.mEffectManager.hasEffect(enemy, EFFECT_NAME)) {
			event.setDamage(event.getFlatDamage() * (1 + mChainDmgBonus));
		}
		return false;
	}
}

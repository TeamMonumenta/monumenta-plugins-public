package com.playmonumenta.plugins.abilities.warlock.reaper;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warlock.reaper.JudgementChainCS;
import com.playmonumenta.plugins.effects.JudgementChainMobEffect;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class JudgementChain extends Ability {
	private static final int COOLDOWN = 15 * 20;
	private static final int RANGE = 20;
	private static final int CHAIN_DURATION = 10 * 20;
	private static final double SLOWNESS_AMOUNT = 0.4;
	private static final double WEAKNESS_AMOUNT = 0.4;
	private static final int DEBUFF_DURATION = 2 * 20;
	private static final int EXTRA_TARGETS = 2;
	private static final double EXTRA_TARGET_RADIUS = 3;
	private static final double STRENGTH_AMOUNT = 0.03;
	private static final double RESISTANCE_AMOUNT = 0.03;
	private static final int MAX_MOBS_FOR_BUFF = 5;
	private static final int BONUS_DARK_PACT_EXTENSION = 20;
	public static final String EFFECT_NAME = "JudgementChainEffect";
	private static final String STRENGTH_EFFECT_NAME = "JudgementChainStrength";
	private static final String RESISTANCE_EFFECT_NAME = "JudgementChainResistance";

	public static final String CHARM_COOLDOWN = "Judgement Chain Cooldown";
	public static final String CHARM_RANGE = "Judgement Chain Range";
	public static final String CHARM_CHAIN_DURATION = "Judgement Chain Chain Duration";
	public static final String CHARM_SLOWNESS = "Judgement Chain Slowness Amplifier";
	public static final String CHARM_WEAKNESS = "Judgement Chain Weakness Amplifier";
	public static final String CHARM_DEBUFF_DURATION = "Judgement Chain Debuff Duration";
	public static final String CHARM_EXTRA_TARGETS = "Judgement Chain Extra Targets";
	public static final String CHARM_EXTRA_TARGET_RADIUS = "Judgement Chain Extra Target Radius";
	public static final String CHARM_STRENGTH = "Judgement Chain Strength Amplifier";
	public static final String CHARM_RESISTANCE = "Judgement Chain Resistance Amplifier";
	public static final String CHARM_DARK_PACT_EXTENSION = "Judgement Chain Dark Pact Extension";

	public static final AbilityInfo<JudgementChain> INFO =
		new AbilityInfo<>(JudgementChain.class, "Judgement Chain", JudgementChain::new)
			.linkedSpell(ClassAbility.JUDGEMENT_CHAIN)
			.scoreboardId("JudgementChain")
			.shorthandName("JC")
			.actionBarColor(TextColor.color(115, 115, 115))
			.descriptions(
				("Swap hands while looking at an unchained mob within %s blocks to teleport them in front of you and chain them to you for %ss, " +
					"taunting them and afflicting them with %s%% Slowness and %s%% Weakness for %ss. " +
					"Bosses and crowd control immune mobs cannot be teleported, but will be chained and debuffed. Cooldown: %ss.")
				.formatted(RANGE, StringUtils.ticksToSeconds(CHAIN_DURATION), StringUtils.multiplierToPercentage(SLOWNESS_AMOUNT),
					StringUtils.multiplierToPercentage(WEAKNESS_AMOUNT), StringUtils.ticksToSeconds(DEBUFF_DURATION), StringUtils.ticksToSeconds(COOLDOWN)),
				("Judgement Chain now additionally teleports and chains the %s closest mobs within %s blocks of the targeted mob. " +
					"Passively gain %s%% Strength and %s%% Resistance for each mob currently chained to you, capped at %s mobs. " +
					"Killing a chained mob extends your Dark Pact by an additional %ss if you have one currently active.")
				.formatted(EXTRA_TARGETS, StringUtils.to2DP(EXTRA_TARGET_RADIUS), StringUtils.multiplierToPercentage(STRENGTH_AMOUNT),
					StringUtils.multiplierToPercentage(RESISTANCE_AMOUNT), MAX_MOBS_FOR_BUFF, StringUtils.ticksToSeconds(BONUS_DARK_PACT_EXTENSION)))
			.simpleDescription("Teleport a mob to you and debuff it.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", JudgementChain::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false),
				AbilityTriggerInfo.HOLDING_SCYTHE_RESTRICTION))
			.displayItem(Material.CHAIN);

	private final double mRange;
	private final int mChainDuration;
	private final double mSlowAmount;
	private final double mWeakenAmount;
	private final int mDebuffDuration;
	private final int mExtraTargets;
	private final double mExtraTargetRadius;
	private final double mStrengthAmount;
	private final double mResistanceAmount;
	private final int mBonusDarkPactExtension;
	private final List<LivingEntity> mAffectedEntities = new ArrayList<>();
	private @Nullable VoodooBonds mVoodooBonds;
	private final JudgementChainCS mCosmetic;

	public JudgementChain(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRange = CharmManager.getRadius(player, CHARM_RANGE, RANGE);
		mChainDuration = CharmManager.getDuration(player, CHARM_CHAIN_DURATION, CHAIN_DURATION);
		mSlowAmount = SLOWNESS_AMOUNT + CharmManager.getLevelPercentDecimal(player, CHARM_SLOWNESS);
		mWeakenAmount = WEAKNESS_AMOUNT + CharmManager.getLevelPercentDecimal(player, CHARM_WEAKNESS);
		mDebuffDuration = CharmManager.getDuration(player, CHARM_DEBUFF_DURATION, DEBUFF_DURATION);
		mExtraTargets = EXTRA_TARGETS + (int) CharmManager.getLevel(player, CHARM_EXTRA_TARGETS);
		mExtraTargetRadius = CharmManager.getRadius(player, CHARM_EXTRA_TARGET_RADIUS, EXTRA_TARGET_RADIUS);
		mStrengthAmount = STRENGTH_AMOUNT + CharmManager.getLevelPercentDecimal(player, CHARM_STRENGTH);
		mResistanceAmount = RESISTANCE_AMOUNT + CharmManager.getLevelPercentDecimal(player, CHARM_RESISTANCE);
		mBonusDarkPactExtension = BONUS_DARK_PACT_EXTENSION + (int) CharmManager.getLevel(player, CHARM_DARK_PACT_EXTENSION);
		Bukkit.getScheduler().runTask(plugin, () -> mVoodooBonds = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, VoodooBonds.class));
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new JudgementChainCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		LivingEntity entity = EntityUtils.getHostileEntityAtCursor(mPlayer, mRange, e -> !mPlugin.mEffectManager.hasEffect(e, EFFECT_NAME));

		if (entity == null) {
			return false;
		}

		putOnCooldown();

		Location entityLoc = entity.getLocation();

		summonChain(entity, new Vector(0, 1, 3));

		if (isLevelTwo()) {
			List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(entityLoc, mExtraTargetRadius, entity);
			nearbyMobs.removeIf(e -> mPlugin.mEffectManager.hasEffect(e, EFFECT_NAME));
			for (int i = 0; i < mExtraTargets; i++) {
				LivingEntity nearestMob = EntityUtils.getNearestMob(entityLoc, nearbyMobs);
				if (nearestMob != null) {
					double angle = Math.floorDiv(i + 2, 2) * 30 * (i % 2 == 0 ? 1 : -1);
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

		if (!EntityUtils.isBoss(entity) && !EntityUtils.isCCImmuneMob(entity)) {
			Location destination = loc.clone().add(VectorUtils.rotateTargetDirection(offset, loc.getYaw(), loc.getPitch()));

			// don't teleport into the ground
			if (destination.getY() < loc.getY() && destination.getBlock().isSolid()) {
				destination = LocationUtils.fallToGround(destination.add(0, 1.5, 0), loc.getY());
			}

			EntityUtils.selfRoot(entity, 5); // tiny root to prevent weird movement after TP
			EntityUtils.teleportStack(entity, destination);
		}

		mCosmetic.onSummonChain(mPlayer, entity, oldLocation);

		EntityUtils.applyTaunt(entity, mPlayer);
		EntityUtils.applySlow(mPlugin, mDebuffDuration, mSlowAmount, entity);
		EntityUtils.applyWeaken(mPlugin, mDebuffDuration, mWeakenAmount, entity);
		mPlugin.mEffectManager.addEffect(entity, EFFECT_NAME, new JudgementChainMobEffect(mChainDuration, mPlayer, mCosmetic.createTeam()));
		mAffectedEntities.add(entity);

		cancelOnDeath(new BukkitRunnable() {
			int mT = 0;
			final LivingEntity mTarget = entity;

			@Override
			public void run() {
				mCosmetic.chain(mPlayer, mTarget, mT);

				if (mPlayer.isDead() || mTarget.isDead() || !mTarget.isValid() || mPlayer.getLocation().distance(mTarget.getLocation()) > 30 || mT > mChainDuration) {
					mCosmetic.onBreakChain(mPlayer, mTarget);
					mPlugin.mEffectManager.clearEffects(entity, EFFECT_NAME);
					mAffectedEntities.remove(mTarget);
					this.cancel();
				}
				mT++;
			}
		}.runTaskTimer(mPlugin, 0, 1));

		if (mVoodooBonds != null && mVoodooBonds.isLevelTwo()) {
			Location startLoc = LocationUtils.getEntityCenter(mPlayer);
			Vector direction = LocationUtils.getDirectionTo(LocationUtils.getEntityCenter(entity), LocationUtils.getEntityCenter(mPlayer));
			mVoodooBonds.launchPin(startLoc, direction, false, false);
		}
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		mAffectedEntities.removeIf(e -> mPlugin.mEffectManager.getEffects(e, EFFECT_NAME) == null);
		if (!mAffectedEntities.isEmpty()) {
			int chainedMobs = Math.min(MAX_MOBS_FOR_BUFF, mAffectedEntities.size());
			mPlugin.mEffectManager.addEffect(mPlayer, STRENGTH_EFFECT_NAME, new PercentDamageDealt(20, mStrengthAmount * chainedMobs).displaysTime(false));
			mPlugin.mEffectManager.addEffect(mPlayer, RESISTANCE_EFFECT_NAME, new PercentDamageReceived(20, -mResistanceAmount * chainedMobs).displaysTime(false));
		}
	}

	public int getBonusDarkPactExtension() {
		return mBonusDarkPactExtension;
	}
}

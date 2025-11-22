package com.playmonumenta.plugins.abilities.shaman.soothsayer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.abilities.shaman.TotemAbility;
import com.playmonumenta.plugins.abilities.shaman.TotemicEmpowerment;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.shaman.soothsayer.ChainHealingWaveCS;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class ChainHealingWave extends MultipleChargeAbility {
	public static final int COOLDOWN = 10 * 20;
	public static final int TARGETS_1 = 4;
	public static final int TARGETS_2 = 6;
	public static final int BOUNCE_RANGE_1 = 10;
	public static final int BOUNCE_RANGE_2 = 15;
	public static final double HEAL_PERCENT_1 = 0.3;
	public static final double HEAL_PERCENT_2 = 0.4;
	public static final int CDR_ON_KILL = 20;
	public static final int CHARGES = 2;

	public static final String CHARM_COOLDOWN = "Chain Healing Wave Cooldown";
	public static final String CHARM_HEALING = "Chain Healing Wave Healing";
	public static final String CHARM_CDR = "Chain Healing Wave Cooldown Reduction";
	public static final String CHARM_RADIUS = "Chain Healing Wave Bounce Radius";
	public static final String CHARM_TARGETS = "Chain Healing Wave Targets";
	public static final String CHARM_CHARGES = "Chain Healing Wave Charges";

	public static final AbilityInfo<ChainHealingWave> INFO =
		new AbilityInfo<>(ChainHealingWave.class, "Chain Healing Wave", ChainHealingWave::new)
			.linkedSpell(ClassAbility.CHAIN_HEALING_WAVE)
			.scoreboardId("ChainHealWave")
			.shorthandName("CHW")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Cast a heal that will bounce between teammates and your totems.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", ChainHealingWave::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).keyOptions(AbilityTrigger.KeyOptions.REQUIRE_PROJECTILE_WEAPON)))
			.displayItem(Material.WHITE_CANDLE);

	public final double mBounceRange;
	public final int mTargets;
	public final double mHealPercent;
	private final List<LivingEntity> mHitTargets = new ArrayList<>();
	private int mLastCastTicks = 0;
	private final int mCooldownReduction;
	private final ChainHealingWaveCS mCosmetic;

	public ChainHealingWave(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mMaxCharges = CHARGES + (int) CharmManager.getLevel(mPlayer, CHARM_CHARGES);
		mBounceRange = CharmManager.getRadius(mPlayer, CHARM_RADIUS, isLevelTwo() ? BOUNCE_RANGE_2 : BOUNCE_RANGE_1);
		mTargets = (int) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_TARGETS, isLevelOne() ? TARGETS_1 : TARGETS_2);
		mHealPercent = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEALING, isLevelOne() ? HEAL_PERCENT_1 : HEAL_PERCENT_2);
		mCooldownReduction = CharmManager.getDuration(mPlayer, CHARM_CDR, CDR_ON_KILL);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new ChainHealingWaveCS());
	}

	public boolean cast() {
		int ticks = Bukkit.getServer().getCurrentTick();
		if (ticks - mLastCastTicks <= 10 || mCharges <= 0) {
			return false;
		}
		mLastCastTicks = ticks;
		mHitTargets.clear();
		mHitTargets.add(mPlayer);

		Hitbox hitbox = Hitbox.approximateCone(mPlayer.getEyeLocation(), mBounceRange + 4, Math.toRadians(45))
			.union(new Hitbox.SphereHitbox(mPlayer.getLocation(), 1.5));

		List<Player> nearbyPlayers = hitbox.getHitPlayers(mPlayer, true);
		if (nearbyPlayers.isEmpty()) {
			List<LivingEntity> nearbyTotems = new ArrayList<>(TotemicEmpowerment.getTotemList(mPlayer));
			nearbyTotems.removeIf(totem -> !hitbox.intersects(totem.getBoundingBox()));
			if (!nearbyTotems.isEmpty()) {
				LivingEntity totem = FastUtils.getRandomElement(nearbyTotems);
				mHitTargets.add(totem);
				startChain(totem);
			}
		} else {
			mHitTargets.add(nearbyPlayers.get(0));
			startChain(nearbyPlayers.get(0));
		}

		return true;
	}

	private void startChain(LivingEntity starterEntity) {
		LivingEntity lastTarget = starterEntity;
		LivingEntity nextTarget;
		int safetyCounter = 0;
		while (currentBounces(mHitTargets) < mTargets && safetyCounter <= 40) {
			safetyCounter++;
			nextTarget = locatePlayerInRange(lastTarget.getLocation());
			if (nextTarget != null) {
				mHitTargets.add(nextTarget);
				lastTarget = nextTarget;
			} else {
				nextTarget = locateTotemInRange(lastTarget.getLocation());
				if (nextTarget != null) {
					mHitTargets.add(nextTarget);
					lastTarget = nextTarget;
				} else {
					break;
				}
			}
		}
		if (!consumeCharge()) {
			return;
		}
		PlayerUtils.healPlayer(mPlugin, mPlayer, mHealPercent * EntityUtils.getMaxHealth(mPlayer) / 2, mPlayer);

		for (int i = 0; i < mHitTargets.size() - 1; i++) {
			LivingEntity target = mHitTargets.get(i + 1);
			if (target instanceof Player targetPlayer) {
				mCosmetic.chainHeal(mPlayer, target);
				PlayerUtils.healPlayer(mPlugin, targetPlayer, mHealPercent * EntityUtils.getMaxHealth(targetPlayer), mPlayer);
			}
			mCosmetic.chainBeam(mHitTargets, i, target, mPlayer);
		}
		mHitTargets.removeIf(target -> !(target instanceof ArmorStand));
		Collection<Ability> abilities = mPlugin.mAbilityManager.getPlayerAbilities(mPlayer).getAbilities();
		for (LivingEntity stand : mHitTargets) {
			for (Ability abil : abilities) {
				if (abil instanceof TotemAbility totemAbility
					&& totemAbility.getRemainingAbilityDuration() > 0
					&& totemAbility.mDisplayName.equalsIgnoreCase(stand.getName())) {
					ClassAbility linkedSpell = abil.getInfo().getLinkedSpell();
					if (linkedSpell != null) {
						mPlugin.mTimers.updateCooldown(mPlayer, linkedSpell, mCooldownReduction);
					}
				}
			}
		}
		mHitTargets.clear();
	}

	private @Nullable Player locatePlayerInRange(Location loc) {
		List<Player> possiblePlayers = PlayerUtils.playersInRange(loc, mBounceRange, true);
		for (LivingEntity entity : mHitTargets) {
			possiblePlayers.removeIf(player -> entity.getUniqueId().equals(player.getUniqueId()));
		}
		if (!possiblePlayers.isEmpty()) {
			Collections.shuffle(possiblePlayers);
			return possiblePlayers.get(0);
		}
		return null;
	}

	private @Nullable LivingEntity locateTotemInRange(Location loc) {
		List<LivingEntity> totemList = new ArrayList<>(TotemicEmpowerment.getTotemList(mPlayer));
		totemList.removeIf(totem -> totem.getLocation().distance(loc) >= mBounceRange);
		for (LivingEntity entity : mHitTargets) {
			totemList.removeIf(totem -> totem.getUniqueId().equals(entity.getUniqueId()));
		}
		if (!totemList.isEmpty()) {
			return totemList.get(0);
		}
		return null;
	}

	private int currentBounces(List<LivingEntity> targetList) {
		int totalBounces = targetList.size();
		int totalTotems = 0;
		for (LivingEntity target : targetList) {
			if (target instanceof ArmorStand) {
				totalTotems++;
			}
		}
		return totalBounces - totalTotems;
	}

	private static Description<ChainHealingWave> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to cast a beam of healing, bouncing between up to ")
			.add(a -> a.mTargets, TARGETS_1, false, Ability::isLevelOne)
			.add(" players within ")
			.add(a -> a.mBounceRange, BOUNCE_RANGE_1, false, Ability::isLevelOne)
			.add(" blocks of the last target hit, healing each player for ")
			.addPercent(a -> a.mHealPercent, HEAL_PERCENT_1, false, Ability::isLevelOne)
			.add(" of their health. The beam can also bounce to nearby totems, decreasing their cooldowns by ")
			.addDuration(a -> a.mCooldownReduction, CDR_ON_KILL)
			.add(" second without consuming a heal. Charges: ")
			.add(a -> a.mMaxCharges, CHARGES)
			.add(". Any healing to yourself is half as effective.")
			.addCooldown(COOLDOWN);
	}

	private static Description<ChainHealingWave> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Healing is increased to ")
			.addPercent(a -> a.mHealPercent, HEAL_PERCENT_2, false, Ability::isLevelTwo)
			.add(". Range between bounces is increased to ")
			.add(a -> a.mBounceRange, BOUNCE_RANGE_2, false, Ability::isLevelTwo)
			.add(". Up to ")
			.add(a -> a.mTargets, TARGETS_2, false, Ability::isLevelTwo)
			.add(" players can now be targeted.");
	}
}

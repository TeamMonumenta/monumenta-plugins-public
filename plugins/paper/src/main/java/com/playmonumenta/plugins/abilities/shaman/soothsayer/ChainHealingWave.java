package com.playmonumenta.plugins.abilities.shaman.soothsayer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.abilities.shaman.TotemAbility;
import com.playmonumenta.plugins.abilities.shaman.TotemicEmpowerment;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
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
	public static final String CHARM_RADIUS = "Chain Healing Wave Bounce Radius";
	public static final String CHARM_TARGETS = "Chain Healing Wave Targets";
	public static final String CHARM_CHARGES = "Chain Healing Wave Charges";

	public static final AbilityInfo<ChainHealingWave> INFO =
		new AbilityInfo<>(ChainHealingWave.class, "Chain Healing Wave", ChainHealingWave::new)
			.linkedSpell(ClassAbility.CHAIN_HEALING_WAVE)
			.scoreboardId("ChainHealWave")
			.shorthandName("CHW")
			.descriptions(
				String.format("Punch while holding a projectile weapon to cast a beam of healing, bouncing between up to %s players within %s blocks of the last target hit " +
					"and healing for %s%% of their health. Will also bounce to nearby totems, decreasing their cooldowns by %ss without consuming a heal. %s charges, %ss cooldown.",
					TARGETS_1,
					BOUNCE_RANGE_1,
					StringUtils.multiplierToPercentage(HEAL_PERCENT_1),
					StringUtils.ticksToSeconds(CDR_ON_KILL),
					CHARGES,
					StringUtils.ticksToSeconds(COOLDOWN)
				),
				String.format("Heal is increased to %s%%, range between bounces has increased to %s, and can now heal up to %s targets.",
					StringUtils.multiplierToPercentage(HEAL_PERCENT_2),
					BOUNCE_RANGE_2,
					TARGETS_2)
			)
			.simpleDescription("Cast a heal that will bounce between teammates and your totems.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", ChainHealingWave::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).keyOptions(AbilityTrigger.KeyOptions.REQUIRE_PROJECTILE_WEAPON)))
			.displayItem(Material.WHITE_CANDLE);

	public final double mBounceRange;
	public final int mTargets;
	public final double mHealPercent;
	private final List<LivingEntity> mHitTargets = new ArrayList<>();
	private int mLastCastTicks = 0;

	public ChainHealingWave(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AbilityUtils.resetClass(player);
		}
		mMaxCharges = CHARGES + (int) CharmManager.getLevel(mPlayer, CHARM_CHARGES);
		mBounceRange = CharmManager.getRadius(mPlayer, CHARM_RADIUS, isLevelTwo() ? BOUNCE_RANGE_2 : BOUNCE_RANGE_1);
		mTargets = (int) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_TARGETS, isLevelOne() ? TARGETS_1 : TARGETS_2);
		mHealPercent = CharmManager.getExtraPercent(mPlayer, CHARM_HEALING, isLevelOne() ? HEAL_PERCENT_1 : HEAL_PERCENT_2);
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
		PlayerUtils.healPlayer(mPlugin, mPlayer, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEALING, mHealPercent * EntityUtils.getMaxHealth(mPlayer)), mPlayer);

		for (int i = 0; i < mHitTargets.size() - 1; i++) {
			LivingEntity target = mHitTargets.get(i + 1);
			if (target instanceof Player) {
				mPlayer.getWorld().playSound(target.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 2.0f, 1.6f);
				mPlayer.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.1f, 1.0f);
				PlayerUtils.healPlayer(mPlugin, (Player) target, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEALING, mHealPercent * EntityUtils.getMaxHealth(target)), mPlayer);
			}
			new PPLine(Particle.VILLAGER_HAPPY, mHitTargets.get(i).getEyeLocation().add(0, -0.5, 0), target.getEyeLocation().add(0, -0.5, 0)).countPerMeter(8).spawnAsPlayerActive(mPlayer);
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
						mPlugin.mTimers.updateCooldown(mPlayer, linkedSpell, CDR_ON_KILL);
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
}

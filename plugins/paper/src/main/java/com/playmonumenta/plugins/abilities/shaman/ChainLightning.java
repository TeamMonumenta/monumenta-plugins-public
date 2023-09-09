package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.abilities.shaman.hexbreaker.DestructiveExpertise;
import com.playmonumenta.plugins.abilities.shaman.soothsayer.SupportExpertise;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.ArrayList;
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

public class ChainLightning extends MultipleChargeAbility {
	public static final int COOLDOWN = 6 * 20;
	public static final int CHARGES = 2;
	public static final int TARGETS_1 = 2;
	public static final int TARGETS_2 = 4;
	public static final int BOUNCE_RANGE = 8;
	public static final int DAMAGE_1 = 6;
	public static final int DAMAGE_2 = 8;

	public static final String CHARM_COOLDOWN = "Chain Lightning Cooldown";
	public static final String CHARM_DAMAGE = "Chain Lightning Damage";
	public static final String CHARM_RADIUS = "Chain Lightning Bounce Radius";
	public static final String CHARM_TARGETS = "Chain Lightning Targets";
	public static final String CHARM_CHARGES = "Chain Lightning Charges";

	public static final AbilityInfo<ChainLightning> INFO =
		new AbilityInfo<>(ChainLightning.class, "Chain Lightning", ChainLightning::new)
			.linkedSpell(ClassAbility.CHAIN_LIGHTNING)
			.scoreboardId("ChainLightning")
			.shorthandName("CL")
			.descriptions(
				String.format("Right click while holding a melee weapon to cast a splitting beam of lightning, bouncing between up to %s mobs within %s blocks of the last target hit " +
					"and dealing %s damage to each. Will also bounce to nearby totems without consuming a hit target. %s charges, %ss cooldown.",
					TARGETS_1,
					BOUNCE_RANGE,
					DAMAGE_1,
					CHARGES,
					StringUtils.ticksToSeconds(COOLDOWN)
				),
				String.format("Damage increased to %s and now hits %s targets.",
					DAMAGE_2,
					TARGETS_2)
			)
			.simpleDescription("Flings lightning at mobs in a medium radius in front of you, bouncing between mobs and totems.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", ChainLightning::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(false)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.displayItem(Material.BLAZE_ROD);

	public final double mBounceRange;
	public final int mTargets;
	public double mDamage;
	private int mLastCastTicks = 0;


	private final List<LivingEntity> mHitTargets = new ArrayList<>();

	public ChainLightning(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AbilityUtils.resetClass(player);
		}
		mMaxCharges = CHARGES + (int) CharmManager.getLevel(mPlayer, CHARM_CHARGES);
		mBounceRange = CharmManager.getRadius(mPlayer, CHARM_RADIUS, BOUNCE_RANGE);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mDamage *= SupportExpertise.damageBuff(mPlayer);
		mDamage *= DestructiveExpertise.damageBuff(mPlayer);
		mTargets = (int) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_TARGETS, isLevelOne() ? TARGETS_1 : TARGETS_2);
	}

	public void cast() {
		int ticks = Bukkit.getServer().getCurrentTick();
		if (ticks - mLastCastTicks <= 10 || mCharges <= 0) {
			return;
		}
		mLastCastTicks = ticks;
		mHitTargets.clear();
		mHitTargets.add(mPlayer);

		Hitbox hitbox = Hitbox.approximateCone(mPlayer.getEyeLocation(), mBounceRange + 4, Math.toRadians(45))
			.union(new Hitbox.SphereHitbox(mPlayer.getLocation(), 1.5));

		List<LivingEntity> nearbyMobs = hitbox.getHitMobs();
		nearbyMobs.sort((a, b) -> (int) (a.getLocation().distance(mPlayer.getLocation()) - b.getLocation().distance(mPlayer.getLocation())));
		if (nearbyMobs.isEmpty()) {
			List<LivingEntity> nearbyTotems = new ArrayList<>(TotemicEmpowerment.getTotemList(mPlayer));
			nearbyTotems.removeIf(totem -> !hitbox.intersects(totem.getBoundingBox()));
			if (!nearbyTotems.isEmpty()) {
				LivingEntity totem = FastUtils.getRandomElement(nearbyTotems);
				mHitTargets.add(totem);
				startChain(totem, false);
			}
		} else {
			mHitTargets.add(nearbyMobs.get(0));
			startChain(nearbyMobs.get(0), true);
		}
	}

	private void startChain(LivingEntity starterEntity, boolean foundMob) {
		LivingEntity lastTarget = starterEntity;
		LivingEntity nextTarget;
		boolean atLeastOneMob = foundMob;
		int safetyCounter = 0;
		while (currentBounces(mHitTargets) <= mTargets && safetyCounter <= 40) {
			safetyCounter++;
			nextTarget = locateMobInRange(lastTarget.getLocation());
			if (nextTarget != null) {
				mHitTargets.add(nextTarget);
				lastTarget = nextTarget;
				atLeastOneMob = true;
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
		if (!atLeastOneMob) {
			return;
		}
		int ticks = Bukkit.getServer().getCurrentTick();
		// Prevent double casting on accident
		if (!consumeCharge()) {
			return;
		}
		mLastCastTicks = ticks;

		mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.PLAYERS, 2.0f, 2.0f);
		for (int i = 0; i < mHitTargets.size() - 1; i++) {
			LivingEntity target = mHitTargets.get(i + 1);
			if (target != null) {
				DamageUtils.damage(mPlayer, target, DamageEvent.DamageType.MAGIC, mDamage, ClassAbility.CHAIN_LIGHTNING, true, true);
				new PPLine(Particle.END_ROD, mHitTargets.get(i).getEyeLocation().add(0, -0.5, 0), target.getEyeLocation().add(0, -0.5, 0), 0.08).deltaVariance(true).countPerMeter(8).spawnAsPlayerActive(mPlayer);
			}
		}
		mHitTargets.clear();
	}

	private @Nullable LivingEntity locateMobInRange(Location loc) {
		List<LivingEntity> possibleMobs = EntityUtils.getNearbyMobsInSphere(loc, mBounceRange, null);
		possibleMobs.sort((a, b) -> (int) (a.getLocation().distance(loc) - b.getLocation().distance(loc)));
		for (LivingEntity entity : mHitTargets) {
			possibleMobs.removeIf(mob -> entity.getUniqueId().equals(mob.getUniqueId()));
		}
		if (!possibleMobs.isEmpty()) {
			Collections.shuffle(possibleMobs);
			return possibleMobs.get(0);
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

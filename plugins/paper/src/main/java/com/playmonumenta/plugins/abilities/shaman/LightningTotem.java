package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.shaman.LightningTotemCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.checkerframework.checker.nullness.qual.Nullable;

public class LightningTotem extends TotemAbility {

	private static final int COOLDOWN = 22 * 20;
	private static final int TOTEM_DURATION = 10 * 20;
	private static final int INTERVAL = 2 * 20;
	private static final int AOE_RANGE = 7;
	private static final int DAMAGE_1 = 14;
	private static final int DAMAGE_2 = 21;
	private static final double STORM_DAMAGE_PERCENT = 0.35;
	private static final int STORM_DAMAGE_RADIUS = 2;
	private static final int STORM_DURATION = 7 * 20;
	private static final int STORM_INTERVAL = 2 * 20;
	public static final Particle.DustOptions YELLOW = new Particle.DustOptions(Color.fromRGB(255, 255, 0), 1.25f);
	public static final Particle.DustOptions DUST_GRAY_LARGE = new Particle.DustOptions(Color.fromRGB(51, 51, 51), 2);

	public static final String CHARM_DURATION = "Lightning Totem Duration";
	public static final String CHARM_RADIUS = "Lightning Totem Radius";
	public static final String CHARM_COOLDOWN = "Lightning Totem Cooldown";
	public static final String CHARM_DAMAGE = "Lightning Totem Damage";
	public static final String CHARM_STORM_DAMAGE = "Lightning Totem Lightning Storm Damage";
	public static final String CHARM_STORM_RADIUS = "Lightning Totem Lightning Storm Radius";
	public static final String CHARM_STORM_DURATION = "Lightning Totem Lightning Storm Duration";
	public static final String CHARM_PULSE_DELAY = "Lightning Totem Pulse Delay";
	public static final String CHARM_STORM_PULSE_DELAY = "Lightning Totem Lightning Storm Pulse Delay";

	public static final AbilityInfo<LightningTotem> INFO =
		new AbilityInfo<>(LightningTotem.class, "Lightning Totem", LightningTotem::new)
			.linkedSpell(ClassAbility.LIGHTNING_TOTEM)
			.scoreboardId("LightningTotem")
			.shorthandName("LT")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Summon a totem which will strike a mob within range for high damage throughout its duration.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", LightningTotem::cast, new AbilityTrigger(AbilityTrigger.Key.DROP).sneaking(false)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.displayItem(Material.YELLOW_WOOL);

	private final double mDamage;
	private final double mRadius;
	private @Nullable LivingEntity mTarget = null;
	public double mDecayedTotemBuff = 0;
	private final double mStormDamagePercent;
	private final double mStormRadius;
	private final int mStormDuration;
	private final List<Location> mAllLocs = new ArrayList<>();
	private final int mInterval;
	private final int mStormInterval;
	private final List<BukkitTask> mStormTasks = new ArrayList<>();
	private final LightningTotemCS mCosmetic;

	public LightningTotem(Plugin plugin, Player player) {
		super(plugin, player, INFO, "Lightning Totem Projectile", "LightningTotem", "Lightning Totem");
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, TOTEM_DURATION);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, AOE_RANGE);
		mStormDamagePercent = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_STORM_DAMAGE, STORM_DAMAGE_PERCENT);
		mStormRadius = CharmManager.getRadius(mPlayer, CHARM_STORM_RADIUS, STORM_DAMAGE_RADIUS);
		mStormDuration = CharmManager.getDuration(mPlayer, CHARM_STORM_DURATION, STORM_DURATION);
		mInterval = CharmManager.getDuration(mPlayer, CHARM_PULSE_DELAY, INTERVAL);
		mStormInterval = CharmManager.getDuration(mPlayer, CHARM_STORM_PULSE_DELAY, STORM_INTERVAL);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new LightningTotemCS());
	}

	@Override
	public void placeTotem(Location standLocation, Player player, ArmorStand stand) {
		mCosmetic.lightningTotemSpawn(standLocation, player, stand, mRadius);
	}

	@Override
	public void onTotemTick(int ticks, ArmorStand stand, World world, Location standLocation, ItemStatManager.PlayerItemStats stats) {
		mCosmetic.lightningTotemTick(mPlayer, mRadius, standLocation, stand);
		mStormTasks.removeIf(BukkitTask::isCancelled);
		if (ticks % mInterval == 0) {
			pulse(standLocation, stats, false);
		}
	}

	@Override
	public void pulse(Location standLocation, ItemStatManager.PlayerItemStats stats, boolean bonusAction) {
		if (mTarget == null || mTarget.isDead() || !mTarget.isValid() || mTarget.getLocation().distance(standLocation) > mRadius) {
			mTarget = null;
			List<LivingEntity> affectedMobs = EntityUtils.getNearbyMobsInSphere(standLocation, mRadius, null);
			affectedMobs.removeIf(mob -> ScoreboardUtils.checkTag(mob, AbilityUtils.IGNORE_TAG));
			affectedMobs.removeIf(mob -> DamageUtils.isImmuneToDamage(mob, DamageEvent.DamageType.MAGIC));
			if (!affectedMobs.isEmpty()) {
				Collections.shuffle(affectedMobs);
				for (LivingEntity mob : affectedMobs) {
					if (mTarget == null) {
						mTarget = mob;
					}
					if (EntityUtils.isBoss(mob) || EntityUtils.isElite(mob)) {
						mTarget = mob;
						break;
					}
				}
			}
		}
		double damageApplied = (mDamage + mDecayedTotemBuff)
			* (bonusAction ? (ChainLightning.ENHANCE_NEGATIVE_EFFICIENCY + CharmManager.getLevelPercentDecimal(mPlayer, ChainLightning.CHARM_NEGATIVE_TOTEM_EFFICIENCY)) : 1);
		if (mTarget != null) {
			DamageUtils.damage(mPlayer, mTarget, new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC,
				mInfo.getLinkedSpell(), stats), damageApplied, true, false, false);
			mCosmetic.lightningTotemStrike(mPlayer, standLocation, mTarget);
		}
		dealSanctuaryImpacts(EntityUtils.getNearbyMobsInSphere(standLocation, mRadius, null), INTERVAL + 20);
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		Location targetLoc = mTarget != null ? mTarget.getLocation() : null;
		if (isEnhanced()
			&& targetLoc != null
			&& event.getEntity().equals(mTarget)) {
			List<Location> locsWithinKill = new ArrayList<>(mAllLocs);
			locsWithinKill.removeIf(loc -> loc.distance(targetLoc) > mStormRadius);
			if (locsWithinKill.isEmpty()) {
				mAllLocs.add(targetLoc);
				mStormTasks.add(new BukkitRunnable() {
					final Location mLoc = targetLoc.clone();
					final ItemStatManager.PlayerItemStats mStats = mPlugin.mItemStatManager
						.getPlayerItemStatsCopy(mPlayer);
					int mTicks = 0;

					@Override
					public void run() {
						mCosmetic.lightningTotemEnhancementStorm(mPlayer, mLoc, mStormRadius);

						if (mTicks % mStormInterval == 0) {
							mCosmetic.lightningTotemEnhancementStrike(mPlayer, mLoc, mStormRadius);
							for (LivingEntity entity : EntityUtils.getNearbyMobsInSphere(mLoc, mStormRadius, mTarget)) {
								DamageUtils.damage(mPlayer, entity, new DamageEvent.Metadata(
										DamageEvent.DamageType.MAGIC, mInfo.getLinkedSpell(), mStats),
									(mDamage + mDecayedTotemBuff) * mStormDamagePercent, true, false, false);
							}
						}
						if (mTicks++ > mStormDuration) {
							this.cancel();
						}
					}
				}.runTaskTimer(mPlugin, 0, 1));
			}
		}
	}

	@Override
	public void onTotemExpire(World world, Location standLocation) {
		mCosmetic.lightningTotemExpire(mPlayer, standLocation, world);
		mTarget = null;
		mDecayedTotemBuff = 0;
		mWhirlwindBuffPercent = 0;
		mAllLocs.clear();
	}

	@Override
	public void onTotemHitEntity(Entity entity) {
		if (entity instanceof LivingEntity target
			&& EntityUtils.isHostileMob(target)) {
			mTarget = target;
		}
	}

	private static Description<LightningTotem> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to fire a projectile that summons a Lightning Totem. The totem will target a mob within ")
			.add(a -> a.mRadius, AOE_RANGE)
			.add(" blocks with priority towards Boss and Elite mobs and deal ")
			.add(a -> a.mDamage, DAMAGE_1, false, Ability::isLevelOne)
			.add(" magic damage every ")
			.addDuration(a -> a.mInterval, INTERVAL, true)
			.add(" seconds. Charge up time: ")
			.addDuration(PULSE_DELAY)
			.add("s. Duration: ")
			.addDuration(a -> a.mDuration, TOTEM_DURATION)
			.add("s.")
			.addCooldown(COOLDOWN);
	}

	private static Description<LightningTotem> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Damage is increased to ")
			.add(a -> a.mDamage, DAMAGE_2, false, Ability::isLevelTwo)
			.add(".");
	}

	private static Description<LightningTotem> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("When this ability kills a mob, it spawns a lightning storm at the mob's location (unless the mob is already in an existing storm). The lightning storm deals ")
			.addPercent(a -> a.mStormDamagePercent, STORM_DAMAGE_PERCENT)
			.add(" of the main magic damage to all mobs within ")
			.add(a -> a.mStormRadius, STORM_DAMAGE_RADIUS)
			.add(" blocks of the center every ")
			.addDuration(a -> a.mStormInterval, STORM_INTERVAL)
			.add(" seconds for ")
			.addDuration(a -> a.mStormDuration, STORM_DURATION)
			.add(" seconds.");
	}
}

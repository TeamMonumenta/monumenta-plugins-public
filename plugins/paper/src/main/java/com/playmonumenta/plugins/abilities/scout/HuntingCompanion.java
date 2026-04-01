package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.bosses.bosses.abilities.HuntingCompanionBoss;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.HuntingCompanionCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MetadataUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fox;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Strider;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.perRegion;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class HuntingCompanion extends Ability {
	private static final int TICK_INTERVAL = 5;
	private static final String NOT_AFK_METADATA = "HuntingCompanionNotAFKEffect";
	public static final Style FOX_COLOR = Style.style(TextColor.color(0xE68129));

	private static final double MELEE_RANGE = 12;
	private static final double DAMAGE_R1 = 3;
	private static final double DAMAGE_R2 = 5;
	private static final double DAMAGE_R3 = 7;
	private static final double POUNCE_DAMAGE_L1_R1 = 7;
	private static final double POUNCE_DAMAGE_L1_R2 = 10;
	private static final double POUNCE_DAMAGE_L1_R3 = 16;
	private static final double POUNCE_DAMAGE_L2_R1 = 10;
	private static final double POUNCE_DAMAGE_L2_R2 = 14;
	private static final double POUNCE_DAMAGE_L2_R3 = 20;
	private static final int POUNCE_COOLDOWN = Constants.TICKS_PER_SECOND * 5;
	private static final int RECALL_COOLDOWN = Constants.TICKS_PER_SECOND * 3;
	private static final double POUNCE_RADIUS = 3;
	private static final double HEALING_PERCENT = 0.1;
	private static final double MAX_TARGET_Y = 4; // Not charmable

	public static final String CHARM_DAMAGE = "Hunting Companion Damage";
	public static final String CHARM_RANGE = "Hunting Companion Range";
	public static final String CHARM_POUNCE_DAMAGE = "Hunting Companion Pounce Damage";
	public static final String CHARM_HEALING = "Hunting Companion Healing";
	public static final String CHARM_SPEED = "Hunting Companion Speed";
	public static final String CHARM_FOXES = "Hunting Companion Foxes";
	public static final String CHARM_POUNCE_COOLDOWN = "Hunting Companion Pounce Cooldown";
	public static final String CHARM_POUNCE_RADIUS = "Hunting Companion Pounce Radius";

	public static final AbilityInfo<HuntingCompanion> INFO =
		new AbilityInfo<>(HuntingCompanion.class, "Hunting Companion", HuntingCompanion::new)
			.linkedSpell(ClassAbility.HUNTING_COMPANION)
			.scoreboardId("HuntingCompanion")
			.shorthandName("HC")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Staggering enemies will have your fox to follow up.")
			.addTrigger(new AbilityTriggerInfo<>("recall", "recall companion", null,
				HuntingCompanion::recallCompanion, new AbilityTrigger(AbilityTrigger.Key.DROP).sneaking(true)
				.lookDirections(AbilityTrigger.LookDirection.DOWN), null))
			.displayItem(Material.SWEET_BERRIES);

	private final double mBiteDamage;
	private final double mRange;
	private final double mPounceDamage;
	private final double mHealingPercent;
	private final int mPounceCooldown;
	private final double mPounceRadius;
	private final int mFoxCount;

	private final BukkitRunnable mCosmeticRunnable;
	private final HuntingCompanionCS mCosmetic;

	private final HashSet<HuntingCompanionBoss> mSummons;
	private int mWasInLava = Bukkit.getCurrentTick();
	private int mRecalled = Bukkit.getCurrentTick();

	public HuntingCompanion(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		double pounceDamage =
			isLevelOne() ? AbilityUtils.regionalScale(player, POUNCE_DAMAGE_L1_R1, POUNCE_DAMAGE_L1_R2, POUNCE_DAMAGE_L1_R3)
				: AbilityUtils.regionalScale(player, POUNCE_DAMAGE_L2_R1, POUNCE_DAMAGE_L2_R2, POUNCE_DAMAGE_L2_R3);

		mBiteDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, AbilityUtils.regionalScale(player, DAMAGE_R1, DAMAGE_R2, DAMAGE_R3));
		mRange = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RANGE, MELEE_RANGE);
		mPounceDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_POUNCE_DAMAGE, pounceDamage);
		mPounceCooldown = CharmManager.getDuration(mPlayer, CHARM_POUNCE_COOLDOWN, POUNCE_COOLDOWN);
		mPounceRadius = CharmManager.getRadius(mPlayer, CHARM_POUNCE_RADIUS, POUNCE_RADIUS);
		mFoxCount = (isEnhanced() ? 2 : 1) + (int) CharmManager.getLevel(mPlayer, CHARM_FOXES);
		mHealingPercent = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEALING, HEALING_PERCENT);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new HuntingCompanionCS());

		mSummons = new HashSet<>();

		mCosmeticRunnable = new BukkitRunnable() {
			int mTicksElapsed = 0;
			final int mCount = mFoxCount;

			@Override
			public void run() {
				if (player == null || mCount == 0) {
					this.cancel();
					return;
				}

				if (AbilityManager.getManager().getPlayerAbility(player, HuntingCompanion.class) == null
					|| !player.isOnline()) {
					if (!AbilityManager.getManager().getPlayerAbilities(player).isSilenced()) {
						this.cancel();
					}
					return;
				}

				mSummons.forEach(HuntingCompanionBoss::cosmeticTick);
				mTicksElapsed++;
			}
		};

		cancelOnDeath(mCosmeticRunnable.runTaskTimer(mPlugin, TICK_INTERVAL, 1));
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mPlayer == null || mFoxCount == 0) {
			return;
		}

		if (mSummons.size() != mFoxCount) {
			clearSummons();
			spawnSummons();
			return;
		}

		Iterator<HuntingCompanionBoss> it = mSummons.iterator();
		HashSet<HuntingCompanionBoss> transformed = new HashSet<>();

		while (it.hasNext()) {
			HuntingCompanionBoss summon = it.next();

			// Check if the summon needs to be removed
			// 1. If the tick returns false (ie its dead, invalid, or diff. worlds)
			// 2. If it can transform (ie into a strider)
			HuntingCompanionBoss attempt = attemptChange(summon);
			if (!summon.tick() || attempt != null) {
				Mob mob = summon.getBoss();
				if (mob != null) {
					mob.remove();
				}

				if (attempt != null) {
					transformed.add(attempt);
				}

				it.remove();
			}
		}

		mSummons.addAll(transformed);
	}

	private void spawnSummons() {
		for (int i = 0; i < mFoxCount; i++) {
			HuntingCompanionBoss summon = summon(mCosmetic.getFoxName(), null, false);
			if (summon == null) {
				break;
			} else {
				mSummons.add(summon);
			}
		}
	}

	public boolean recallCompanion() {
		int currTick = Bukkit.getCurrentTick();
		if (currTick - mRecalled < RECALL_COOLDOWN) {
			return false;
		}
		mRecalled = currTick;

		for (HuntingCompanionBoss fox : mSummons) {
			fox.teleportCompanion();
		}

		return true;
	}

	@Override
	public void invalidate() {
		if (mCosmeticRunnable != null) {
			mCosmeticRunnable.cancel();
		}
		clearSummons();
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (!ClassAbility.HUNTING_COMPANION.equals(event.getAbility())) {
			MetadataUtils.markThisTick(mPlugin, mPlayer, NOT_AFK_METADATA);
		}

		if (event.getType() == DamageType.PROJECTILE && event.getDamager() instanceof Projectile proj && EntityUtils.isAbilityTriggeringProjectile(proj, false)) {
			HuntingCompanionBoss nearestSummon = findNearestSummon(mPlayer, enemy, mSummons, mRange);
			if (nearestSummon != null) {
				LivingEntity summon = nearestSummon.getBoss();
				mCosmetic.onAggro(summon.getWorld(), summon.getLocation(), mPlayer, summon);
				nearestSummon.getBoss().setTarget(enemy);
			}
		}

		return true; // only one targeting instance per tick
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (Bukkit.getCurrentTick() - mWasInLava >= Constants.TICKS_PER_SECOND * 5
			&& event.getCause().equals(EntityDamageEvent.DamageCause.LAVA)
		) {
			mWasInLava = Bukkit.getCurrentTick();
			attemptPounce(mPlayer, mPlayer, false);
		}
	}

	private void clearSummons() {
		mSummons.forEach(HuntingCompanionBoss::remove);
		mSummons.clear();
	}

	// Relies on mobs from the LoS. These mobs must have certain tags and must be invulnerable
	// boss_huntingcompanion, UNPUSHABLE, boss_ccimmune, boss_canceldamage, and summon_ignore
	private @Nullable HuntingCompanionBoss summon(String name, @Nullable Location location, boolean playSound) {
		Location loc = location == null ?
			LocationUtils.randomLocationInDonut(mPlayer.getLocation(), 0.5, 1)
			: location;
		Vector facingDirection = mPlayer.getEyeLocation().getDirection().normalize();
		Vector perp = new Vector(-facingDirection.getZ(), 0, facingDirection.getX()).normalize(); // projection of the perpendicular vector to facingDirection onto the xz plane
		perp.multiply(1);

		Vector sideOffset = new Vector();
		Location pos = loc.clone().add(perp);
		Location neg = loc.clone().subtract(perp);
		if (canSpawnAt(pos)) {
			sideOffset = perp;
		} else if (canSpawnAt(neg)) {
			sideOffset = perp.clone().multiply(-1);
		} else if (!loc.isChunkLoaded()) { // Abort if somewhere not loaded
			return null;
		}

		loc.add(sideOffset).add(facingDirection.clone().setY(0).normalize().multiply(-0.25));

		Creature summon = (Creature) LibraryOfSoulsIntegration.summon(loc, name);
		if (summon == null) {
			MMLog.warning("Failed to spawn " + name + " from Library of Souls");
			return null;
		}

		HuntingCompanionBoss companion = mPlugin.mBossManager.getBoss(summon, HuntingCompanionBoss.class);
		if (companion == null) {
			MMLog.warning("Failed to create HuntingCompanionBoss for " + name + " from Library of Souls");
			summon.remove();
			return null;
		}
		companion.spawn(mPlayer, isLevelTwo(), mBiteDamage, mPounceDamage, mPounceRadius,
			mPounceCooldown, mHealingPercent, mRange, mCosmetic);

		if (playSound) {
			mCosmetic.onSummon(mPlayer.getWorld(), loc, mPlayer, summon);
		}

		return companion;
	}

	private boolean canSpawnAt(Location test) {
		if (test.isChunkLoaded()) {
			Block block = test.getBlock();
			if (!block.isSolid()) {
				Block block1 = block.getRelative(BlockFace.UP);
				if (!block1.isSolid()) {
					Block block2 = block1.getRelative(BlockFace.UP);
					return !block2.isSolid();
				}
			}
		}
		return false;
	}

	private @Nullable HuntingCompanionBoss attemptChange(HuntingCompanionBoss companion) {
		Mob summon = companion.getBoss();
		Location loc = summon.getLocation();
		HuntingCompanionBoss swap = null;

		if (!(summon instanceof Fox) && summon.isOnGround() && !summon.isInWater() && !summon.isInLava()) {
			swap = summon(mCosmetic.getFoxName(), loc, true);
			// fox summon
		} else if (!(summon instanceof Axolotl) && LocationUtils.isLocationInWater(loc)) {
			swap = summon(mCosmetic.getAxolotlName(), loc, true);
		} else if (!(summon instanceof Strider) && summon.isInLava()) {
			swap = summon(mCosmetic.getStriderName(), loc, true);
		}
		return swap;
	}

	// Static Util methods

	public static void staggerApplied(Player player, LivingEntity entity) {
		attemptPounce(player, entity, true);
	}

	public static boolean isAFK(Player player) {
		return !MetadataUtils.happenedInRecentTicks(player, NOT_AFK_METADATA, Constants.TICKS_PER_SECOND * 10);
	}

	public static @Nullable LivingEntity findNearestNonTargetedMob(LivingEntity summon, Player player, double range) {
		Location loc = player.getLocation();
		List<LivingEntity> nearbyMobs = getNearbyValidMob(loc, range);

		List<LivingEntity> unfilteredNearbyMobs = new ArrayList<>(nearbyMobs);

		HuntingCompanion hcAbility = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, HuntingCompanion.class);
		if (hcAbility != null) {
			hcAbility.mSummons.forEach(e -> nearbyMobs.remove(e.getTarget()));
		}

		if (summon instanceof Fox || summon instanceof Strider) {
			nearbyMobs.removeIf(mob -> Math.abs(mob.getLocation().getY() - loc.getY()) > MAX_TARGET_Y);
			nearbyMobs.removeIf(mob -> EntityUtils.isFlyingMob(EntityUtils.getEntityStackBase(mob)));
		} else if (summon instanceof Axolotl) {
			nearbyMobs.removeIf(mob -> !EntityUtils.isInWater(mob));
		}

		// If there are no other mobs, double up
		if (nearbyMobs.isEmpty()) {
			return EntityUtils.getNearestMob(summon.getLocation(), unfilteredNearbyMobs);
		}

		return EntityUtils.getNearestMob(summon.getLocation(), nearbyMobs);
	}

	private static void attemptPounce(Player player, LivingEntity entity, boolean isAttack) {
		HuntingCompanion companion = Plugin.getInstance().mAbilityManager.getPlayerAbilityIgnoringSilence(player, HuntingCompanion.class);
		if (companion != null) {
			for (HuntingCompanionBoss fox : companion.mSummons) {
				if (fox.pounce(entity, isAttack)) {
					break;
				}
			}
		}
	}

	private static List<LivingEntity> getNearbyValidMob(Location loc, double range) {
		List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(loc, range);

		// Remove immune & ignored

		nearbyMobs.removeIf(mob -> DamageUtils.isImmuneToDamage(mob, DamageType.PROJECTILE_SKILL));
		nearbyMobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));

		return nearbyMobs;
	}

	private static @Nullable HuntingCompanionBoss findNearestSummon(Player player, LivingEntity target,
																	Set<HuntingCompanionBoss> summons, double range) {
		Location targetLoc = target.getLocation();
		List<HuntingCompanionBoss> companionBoss = new ArrayList<>(summons);

		// Alternatively stack up foxes on a single target for maximum mayhem
		companionBoss.removeIf(summon ->
			player.getLocation().distanceSquared(targetLoc) > range * range
				|| target.equals(summon.getTarget()));

		Location pLoc = player.getLocation();

		// Should always be a mob, unless its null
		return companionBoss.stream()
			.min(Comparator.comparingDouble(e -> e.getBoss().getLocation().distanceSquared(pLoc)))
			.orElse(null);
	}


	private static Description<HuntingCompanion> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addLine("A *Fox* will follow you around, attacking").styles(FOX_COLOR)
			.addLine("nearby mobs within a %d block radius of you.")
			.statValues(stat(a -> a.mRange, MELEE_RANGE))
			.addLine()
			.addStat("Damage: %d (p) every 1s")
			.statValues(perRegion(a -> a.mBiteDamage, DAMAGE_R1, DAMAGE_R2, DAMAGE_R3))
			.addLine()
			.addLine("The *Fox* will pounce when a mob is").styles(FOX_COLOR)
			.addLine("staggered, dealing area damage on impact.")
			.addIf((a, p) -> a != null && a.mFoxCount > 1, desc -> desc
				.addLine("(Multiple companions have their own separate cooldown)"))
			.addLine()
			.addStat("Damage: %d1 (p)")
			.statValues(perRegion(a -> a.mPounceDamage, POUNCE_DAMAGE_L1_R1, POUNCE_DAMAGE_L1_R2, POUNCE_DAMAGE_L1_R3))
			.addStat("Radius: %r")
			.statValues(stat(a -> a.mPounceRadius, POUNCE_RADIUS))
			.addStat("Cooldown: %t")
			.statValues(cooldown(POUNCE_COOLDOWN))
			.addDashedLine();
	}

	private static Description<HuntingCompanion> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Hunting Companion*'s pounce damage.").styles(UNDERLINED)
			.addLine()
			.addStatComparison("Damage: %d1 -> %d2 (p)")
			.statValues(perRegion(POUNCE_DAMAGE_L1_R1, POUNCE_DAMAGE_L1_R2, POUNCE_DAMAGE_L1_R3),
				perRegion(a -> a.mPounceDamage, POUNCE_DAMAGE_L2_R1, POUNCE_DAMAGE_L2_R2, POUNCE_DAMAGE_L2_R3))
			.addLine()
			.addLine("Heal yourself over time when a *Fox* pounces.").styles(FOX_COLOR)
			.addLine("(Duration stacks per pounce, up to 3s)")
			.addLine()
			.addStat("Healing: %p HP over 1s")
			.statValues(stat(a -> a.mHealingPercent, HEALING_PERCENT))
			.addDashedLine();
	}

	private static Description<HuntingCompanion> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("Gain an additional *Hunting Companion*.").styles(UNDERLINED)
			.addLine("(Multiple companions have their own separate cooldown)")
			.addDashedLine();
	}
}

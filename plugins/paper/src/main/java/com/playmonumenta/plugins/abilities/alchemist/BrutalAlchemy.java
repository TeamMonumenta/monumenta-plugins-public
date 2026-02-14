package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.Alchemist;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class BrutalAlchemy extends Ability implements PotionAbility {
	public static final int DOT_DURATION = 3 * 20;
	private static final int DOT_PERIOD = 20;
	private static final int REFRESHES_NEEDED_TO_EXPLODE = 3;
	private static final int COOLDOWN_AFTER_EXPLOSION = 5;
	private static final double DOT_FLAT_BASE = 1;
	private static final double DOT_MULT_BASE = 0.1;

	private static final double DOT_MULT_INCREASE_1 = 0.05;
	private static final double DOT_FLAT_INCREASE_1 = 0.5;
	private static final double DOT_EXPLOSION_MULT_1 = 0.5;

	public static final double DOT_MULT_INCREASE_2 = 0.1;
	public static final double DOT_FLAT_INCREASE_2 = 1;
	public static final double DOT_EXPLOSION_MULT_2 = 0.65;

	public static final double DOT_MULT_INCREASE_3 = 0.15;
	public static final double DOT_FLAT_INCREASE_3 = 1.5;
	public static final double DOT_EXPLOSION_MULT_3 = 0.8;

	public static final double BRUTAL_POTION_DAMAGE_MULTIPLIER = 1;
	public static final int ENHANCEMENT_ADDITIONAL_TICKS = 1;

	public static final String CHARM_DAMAGE_MULTIPLIER = "Brutal Alchemy Damage Multiplier";
	public static final String CHARM_DURATION = "Brutal Alchemy Duration";
	public static final String CHARM_DOT_BASE_DAMAGE = "Brutal Alchemy DoT Base Damage";
	public static final String CHARM_DOT_INCREASE_DAMAGE_FLAT = "Brutal Alchemy DoT Reapplication Damage Flat";
	public static final String CHARM_DOT_INCREASE_DAMAGE_MULT = "Brutal Alchemy DoT Reapplication Damage Multiplier";
	public static final String CHARM_REFRESHES_NEEDED_TO_EXPLODE = "Brutal Alchemy Reapplications Needed To Explode";
	public static final String CHARM_DOT_EXPLOSION_DAMAGE_MULT = "Brutal Alchemy DoT Explosion Damage Multiplier";
	public static final String CHARM_ENHANCEMENT_ADDITIONAL_TICKS = "Brutal Alchemy Enhancement Additional Ticks";

	public static final AbilityInfo<BrutalAlchemy> INFO =
		new AbilityInfo<>(BrutalAlchemy.class, "Brutal Alchemy", BrutalAlchemy::new)
			.linkedSpell(ClassAbility.BRUTAL_ALCHEMY)
			.scoreboardId("BrutalAlchemy")
			.shorthandName("BA")
			.canUse(player -> AbilityUtils.getClassNum(player) == Alchemist.CLASS_ID)
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Throw potions that apply a magic damage over time effect to enemies.")
			.displayItem(Material.REDSTONE);

	private class BrutalDotInfo {
		private final LivingEntity mTarget;
		private final ItemStatManager.PlayerItemStats mPlayerItemStats;
		private final int mMaxDamageTicks;
		private final int mMaxRefreshes;
		private int mRefreshes;
		private int mDamageTicksReceived;
		private int mTimer;
		private int mLevel;
		private double mDamage;

		private BrutalDotInfo(LivingEntity target, int level, ItemStatManager.PlayerItemStats playerItemStats) {
			mTarget = target;
			mPlayerItemStats = playerItemStats;
			mMaxDamageTicks = mDuration / DOT_PERIOD;
			mMaxRefreshes = mRefreshesNeededToExplode;
			mRefreshes = 0;
			mDamageTicksReceived = 0;
			mTimer = 0;
			mLevel = level;
			mDamage = 0;
			if (mAlchemistPotions != null) {
				mDamage = mAlchemistPotions.getDamage(playerItemStats) * mDotBaseMultDamage + mDotBaseFlatDamage;
			}
		}

		private double calculateTickDamage() {
			if (mAlchemistPotions == null) {
				return 0;
			}
			return mDamage + (getMultIncrease(mLevel) * mAlchemistPotions.getDamage(mPlayerItemStats) + getFlatIncrease(mLevel)) * mRefreshes;
		}

		private void dealAdditionalTicks(int ticks) {
			dealDamage(true);
			if (ticks > 1) {
				// Cool staggered effect
				new BukkitRunnable() {
					private int mDamagesDealt = 1;

					@Override
					public void run() {
						dealDamage(true);
						mDamagesDealt++;

						if (mDamagesDealt >= ticks) {
							cancel();
						}
					}
				}.runTaskTimer(mPlugin, 2, 2);
			}
		}

		private void dealDamage(boolean isAdditionalTick) {
			DamageUtils.damage(
				mPlayer,
				mTarget,
				new DamageEvent.Metadata(
					DamageEvent.DamageType.MAGIC,
					ClassAbility.BRUTAL_ALCHEMY,
					mPlayerItemStats
				),
				calculateTickDamage(),
				true,
				false,
				false
			);
			if (mAlchemistPotions != null) {
				mAlchemistPotions.mCosmetic.brutalDotTickEffects(mTarget);
				if (isAdditionalTick) {
					mAlchemistPotions.mCosmetic.brutalDotAdditionalTicksEffects(mTarget);
				}
			}
		}

		private void tick() {
			mTimer += 5;
			if (mTimer >= DOT_PERIOD) {
				mTimer -= DOT_PERIOD;
				mDamageTicksReceived++;
				dealDamage(false);
			}
		}

		private void periodicVisuals() {
			if (mAlchemistPotions != null) {
				mAlchemistPotions.mCosmetic.brutalPeriodicEffects(mTarget, mRefreshes + 1, mMaxRefreshes, mLevel);
			}
		}

		private void setLevel(int level) {
			mLevel = level;
		}

		private void refresh() {
			if (mRefreshes + 1 >= mMaxRefreshes && mAlchemistPotions != null) {
				// DoT pops: immediately deals the remaining ticks + the pop damage
				mLastDotExplosions.put(mTarget.getUniqueId(), Bukkit.getCurrentTick());
				double finalDamage = calculateTickDamage() * (mMaxDamageTicks - mDamageTicksReceived)
					+ mAlchemistPotions.getDamage(mPlayerItemStats) * getExplosionDamageMult(mLevel);
				DamageUtils.damage(
					mPlayer,
					mTarget,
					new DamageEvent.Metadata(
						DamageEvent.DamageType.MAGIC,
						ClassAbility.BRUTAL_ALCHEMY,
						mPlayerItemStats
					),
					finalDamage,
					true,
					false,
					false
				);
				mAlchemistPotions.mCosmetic.brutalDotExplosionEffects(mTarget);
			}
			// Increment later to avoid calculating for 1 more refresh than wanted.
			mRefreshes++;
			mDamageTicksReceived = 0;
		}

		private boolean isDone() {
			return mTarget.isDead() ||
				!mTarget.isValid() ||
				mAlchemistPotions == null ||
				mRefreshes >= mMaxRefreshes ||
				mDamageTicksReceived >= mMaxDamageTicks;
		}
	}

	private final int mDuration;
	private final double mDotBaseFlatDamage;
	private final double mDotBaseMultDamage;
	private final int mEnhancementAdditionalTicks;
	private final int mRefreshesNeededToExplode;
	private @Nullable AlchemistPotions mAlchemistPotions;
	private final HashMap<UUID, BrutalDotInfo> mActiveDoTs = new HashMap<>();
	private final HashMap<UUID, Integer> mLastDotExplosions = new HashMap<>();
	private final ConcurrentHashMap<UUID, Integer> mAfflictedMobs = new ConcurrentHashMap<>();

	public BrutalAlchemy(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DOT_DURATION);
		mDotBaseFlatDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DOT_BASE_DAMAGE, DOT_FLAT_BASE);
		mDotBaseMultDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DOT_BASE_DAMAGE, DOT_MULT_BASE);
		mEnhancementAdditionalTicks = ENHANCEMENT_ADDITIONAL_TICKS + (int) CharmManager.getLevel(mPlayer, CHARM_ENHANCEMENT_ADDITIONAL_TICKS);
		mRefreshesNeededToExplode = REFRESHES_NEEDED_TO_EXPLODE + (int) CharmManager.getLevel(mPlayer, CHARM_REFRESHES_NEEDED_TO_EXPLODE);
		Bukkit.getScheduler().runTask(plugin, () -> {
			mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(mPlayer, AlchemistPotions.class);
		});
	}

	public int getLevel() {
		if (isLevelOne()) {
			return 1;
		}
		if (isLevelTwo()) {
			return 2;
		}
		return 0;
	}

	public double getFlatIncrease(int level) {
		double base = 0;
		switch (level) {
			case 1 -> base = DOT_FLAT_INCREASE_1;
			case 2 -> base = DOT_FLAT_INCREASE_2;
			case 3 -> base = DOT_FLAT_INCREASE_3;
			default -> new IllegalStateException("Unexpected ability level: " + level + " for player " + mPlayer).printStackTrace();
		}
		return base + CharmManager.getLevel(mPlayer, CHARM_DOT_INCREASE_DAMAGE_FLAT);
	}

	public double getMultIncrease(int level) {
		double base = 0;
		switch (level) {
			case 1 -> base = DOT_MULT_INCREASE_1;
			case 2 -> base = DOT_MULT_INCREASE_2;
			case 3 -> base = DOT_MULT_INCREASE_3;
			default -> new IllegalStateException("Unexpected ability level: " + level + " for player " + mPlayer).printStackTrace();
		}
		return base + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DOT_INCREASE_DAMAGE_MULT);
	}

	public double getExplosionDamageMult(int level) {
		double base = 0;
		switch (level) {
			case 1 -> base = DOT_EXPLOSION_MULT_1;
			case 2 -> base = DOT_EXPLOSION_MULT_2;
			case 3 -> base = DOT_EXPLOSION_MULT_3;
			default -> new IllegalStateException("Unexpected ability level: " + level + " for player " + mPlayer).printStackTrace();
		}
		return base + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DOT_EXPLOSION_DAMAGE_MULT);
	}

	private void applyDoT(LivingEntity mob, ItemStatManager.PlayerItemStats playerItemStats, int level) {
		if (mAlchemistPotions == null) {
			return;
		}

		@Nullable Integer lastExplosionTicks = mLastDotExplosions.get(mob.getUniqueId());
		if (lastExplosionTicks != null && Bukkit.getCurrentTick() - lastExplosionTicks <= COOLDOWN_AFTER_EXPLOSION) {
			return;
		}

		mAfflictedMobs.put(mob.getUniqueId(), Bukkit.getCurrentTick());
		cleanAfflictedMap();
		@Nullable BrutalDotInfo brutalDotInfo = mActiveDoTs.get(mob.getUniqueId());
		if (brutalDotInfo == null) {
			if (level > 0) {
				mActiveDoTs.put(
					mob.getUniqueId(),
					new BrutalDotInfo(mob, level, playerItemStats)
				);
			}
		} else {
			brutalDotInfo.refresh();
			if (brutalDotInfo.mLevel < level) {
				// Upgrade the DoT to a higher level (e.g. from Volatile Reaction)
				brutalDotInfo.setLevel(level);
			}
			// Clear the ones that popped due to reaching max refreshes
			mActiveDoTs.values().removeIf(BrutalDotInfo::isDone);
		}
	}

	private void cleanAfflictedMap() {
		int currentTick = Bukkit.getCurrentTick();
		mAfflictedMobs.values().removeIf(applicationTicks -> currentTick - applicationTicks > mDuration);
	}

	public boolean isAfflicted(LivingEntity mob) {
		int currentTick = Bukkit.getCurrentTick();
		return mAfflictedMobs.containsKey(mob.getUniqueId()) &&
			currentTick - mAfflictedMobs.get(mob.getUniqueId()) <= mDuration;
	}

	public static void tryDoEnhancementEffect(@Nullable BrutalAlchemy brutalAlchemy, LivingEntity mob) {
		if (brutalAlchemy == null) {
			return;
		}

		brutalAlchemy.internalTryDoEnhancementEffect(mob);
	}

	private void internalTryDoEnhancementEffect(LivingEntity mob) {
		if (!isEnhanced()) {
			return;
		}

		@Nullable BrutalDotInfo brutalDotInfo = mActiveDoTs.get(mob.getUniqueId());
		if (brutalDotInfo == null) {
			return;
		}

		brutalDotInfo.dealAdditionalTicks(mEnhancementAdditionalTicks);
	}

	@Override
	public void apply(LivingEntity mob, boolean isGruesome, ItemStatManager.PlayerItemStats playerItemStats, int level, boolean refreshBrutalDot) {
		if (!isGruesome && refreshBrutalDot && mAlchemistPotions != null) {
			applyDoT(mob, playerItemStats, level);
		}
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		// Triggers every 5 ticks
		mActiveDoTs.values().forEach(BrutalDotInfo::tick);
		mActiveDoTs.values().removeIf(BrutalDotInfo::isDone);
		mActiveDoTs.values().forEach(BrutalDotInfo::periodicVisuals);
	}

	public void applyHigherLevel(LivingEntity mob, ItemStatManager.PlayerItemStats playerItemStats) {
		if (mAlchemistPotions != null) {
			mAlchemistPotions.applyEffects(mob, false, playerItemStats, getLevel() + 1);
		}
	}

	private static Description<BrutalAlchemy> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addLine("*Brutal* potions now inflict mobs with an additional").styles(Alchemist.BRUTAL_COLOR)
			.addLine("damage over time effect.")
			.addLine()
			.addStat("Base Damage: %d + %p (s) every %t for %t")
				.statValues(stat(a -> a.mDotBaseFlatDamage, DOT_FLAT_BASE), stat(a -> a.mDotBaseMultDamage, DOT_MULT_BASE), stat(DOT_PERIOD), stat(a -> a.mDuration, DOT_DURATION))
			.tab().addLine("(of potion damage)")
			.addLine()
			.addLine("Reapplying *Brutal* onto a mob refreshes its").styles(Alchemist.BRUTAL_COLOR)
			.addLine("duration and increases the effect's damage.")
			.addLine()
			.addStat("Stack Damage: +%d1 + %p1 (s) per extra stack")
				.statValues(stat(a -> a.getFlatIncrease(1), DOT_FLAT_INCREASE_1), stat(a -> a.getMultIncrease(1), DOT_MULT_INCREASE_1))
			.addLine()
			.addLine("Reaching %d stacks of *Brutal* on a mob causes").styles(Alchemist.BRUTAL_COLOR)
			.statValues(stat(a -> a.mRefreshesNeededToExplode + 1, REFRESHES_NEEDED_TO_EXPLODE + 1))
			.addLine("the effect to explode, instantly dealing its")
			.addLine("remaining damage, plus bonus damage, and")
			.addLine("clearing the effect.")
			.addLine()
			.addStat("Bonus Damage: %p1 (s) (of potion damage)")
				.statValues(stat(a -> a.getExplosionDamageMult(1), DOT_EXPLOSION_MULT_1))
			.addDashedLine();
	}

	private static Description<BrutalAlchemy> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase your *Brutal* potions' damage per").styles(Alchemist.BRUTAL_COLOR)
			.addLine("stack and the bonus damage on explosion.")
			.addLine()
			.addStatComparison("Stack Damage: +%d1 + %p1 -> +%d2 + %p2 (s)")
				.statValues(stat(DOT_FLAT_INCREASE_1), stat(DOT_MULT_INCREASE_1), stat(a -> a.getFlatIncrease(2), DOT_FLAT_INCREASE_2), stat(a -> a.getMultIncrease(2), DOT_MULT_INCREASE_2))
			.addStatComparison("Bonus Damage: %p1 -> %p2 (s)")
				.statValues(stat(DOT_EXPLOSION_MULT_1), stat(a -> a.getExplosionDamageMult(2), DOT_EXPLOSION_MULT_2))
			.addLine()
			.addLine("When boosted by *Volatile Reaction* to Level 3:").styles(UNDERLINED)
			.addLine()
			.addStatComparison("Stack Damage: +%d2 + %p2 -> +%d3 + %p3 (s)")
				.statValues(stat(DOT_FLAT_INCREASE_2), stat(DOT_MULT_INCREASE_2), stat(a -> a.getFlatIncrease(3), DOT_FLAT_INCREASE_3), stat(a -> a.getMultIncrease(3), DOT_MULT_INCREASE_3))
			.addStatComparison("Bonus Damage: %p2 -> %p3 (s)")
				.statValues(stat(DOT_EXPLOSION_MULT_2), stat(a -> a.getExplosionDamageMult(3), DOT_EXPLOSION_MULT_3))
			.addDashedLine();
	}

	private static Description<BrutalAlchemy> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("Hitting *Brutal*-afflicted mobs with powerful").styles(Alchemist.BRUTAL_COLOR)
			.addLine("abilities makes them instantly take %d")
				.statValues(stat(a -> a.mEnhancementAdditionalTicks, ENHANCEMENT_ADDITIONAL_TICKS))
			.addLine("additional tick of the effect's damage.")
			.addLine()
			.addStat("Powerful Abilities:")
				.addListItem("Alchemical Artillery")
				.addListItem("Unstable Amalgam")
				.addListItem("Volatile Reaction")
				.addListItem("Esoteric Enhancements")
				.addListItem("Panacea")
				.addListItem("Transmutation Ring")
			.addDashedLine();
	}
}

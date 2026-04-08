package com.playmonumenta.plugins.abilities.alchemist.apothecary;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.abilities.alchemist.BrutalAlchemy;
import com.playmonumenta.plugins.abilities.alchemist.GruesomeAlchemy;
import com.playmonumenta.plugins.abilities.alchemist.PotionAbility;
import com.playmonumenta.plugins.classes.Alchemist;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.apothecary.PanaceaCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;
import static com.playmonumenta.plugins.utils.DescriptionUtils.WHITE;

public class Panacea extends Ability implements AbilityWithDuration {

	private static final double DAMAGE_FRACTION = 1.35;
	private static final double SHIELD_ALLIES_1 = 2;
	private static final double SHIELD_ALLIES_2 = 4;
	private static final double SHIELD_MOBS = 1;
	private static final double MOB_SHIELD_RADIUS = 6;
	private static final int MAX_MOB_HITS = 6;
	private static final int DEBUFF_REDUCTION = 5 * 20;
	private static final int MAX_SHIELD = 16;
	private static final int ABSORPTION_DURATION = 16 * 20;
	// Calculate the range with MAX_DURATION * MOVE_SPEED
	private static final int MAX_DURATION = 20 * 2;
	private static final double MOVE_SPEED = 0.4;
	public static final double RADIUS = 1.5;
	private static final int SLOW_TICKS_1 = (int) (1.5 * 20);
	private static final int SLOW_TICKS_2 = 2 * 20;
	private static final int COOLDOWN = 12 * 20;

	public static final String CHARM_DAMAGE = "Panacea Damage";
	public static final String CHARM_ABSORPTION_PLAYERS = "Panacea Player Absorption Health";
	public static final String CHARM_ABSORPTION_MOBS = "Panacea Mob Absorption Health";
	public static final String CHARM_ABSORPTION_MAX = "Panacea Max Absorption Health";
	public static final String CHARM_ABSORPTION_DURATION = "Panacea Absorption Duration";
	public static final String CHARM_MOVEMENT_DURATION = "Panacea Movement Duration";
	public static final String CHARM_MOVEMENT_SPEED = "Panacea Movement Speed";
	public static final String CHARM_RADIUS = "Panacea Radius";
	public static final String CHARM_SLOW_DURATION = "Panacea Slow Duration";
	public static final String CHARM_COOLDOWN = "Panacea Cooldown";
	public static final String CHARM_MAX_MOB_SHIELD_HITS = "Panacea Max Shieldings From Hit Mobs";
	public static final String CHARM_MOB_SHIELD_RADIUS = "Panacea Mob Shield Radius";
	public static final String CHARM_DEBUFF_REDUCTION = "Panacea Debuff Reduction";

	public static final AbilityInfo<Panacea> INFO =
		new AbilityInfo<>(Panacea.class, "Panacea", Panacea::new)
			.linkedSpell(ClassAbility.PANACEA)
			.scoreboardId("Panacea")
			.shorthandName("Pn")
			.actionBarColor(TextColor.color(255, 255, 100))
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Launch a slow moving projectile that inflicts Gruesome and Brutal effects to mobs hit, and shields allies.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Panacea::cast, new AbilityTrigger(AbilityTrigger.Key.DROP).sneaking(true),
				PotionAbility.HOLDING_ALCHEMIST_BAG_RESTRICTION))
			.displayItem(Material.PURPLE_CONCRETE_POWDER);

	private final double mShieldAllies;
	private final double mShieldMobs;
	private final int mMaxMobShieldHits;
	private final double mMobShieldRadius;
	private final double mMaxAbsorption;
	private final int mAbsorptionDuration;
	private final int mDebuffReduction;
	private final int mSlowTicks;
	private final double mRadius;
	private final double mMoveSpeed;
	private final int mMaxDuration;
	private final double mDamageMult;
	private final PanaceaCS mCosmetic;
	private int mCurrMobShieldHits = 0;
	private @Nullable AlchemistPotions mAlchemistPotions;
	private @Nullable GruesomeAlchemy mGruesomeAlchemy;
	private @Nullable BrutalAlchemy mBrutalAlchemy;

	public Panacea(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mSlowTicks = CharmManager.getDuration(mPlayer, CHARM_SLOW_DURATION, (isLevelOne() ? SLOW_TICKS_1 : SLOW_TICKS_2));
		mShieldAllies = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION_PLAYERS, isLevelOne() ? SHIELD_ALLIES_1 : SHIELD_ALLIES_2);
		mShieldMobs = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION_MOBS, SHIELD_MOBS);
		mMaxMobShieldHits = MAX_MOB_HITS + (int) CharmManager.getLevel(mPlayer, CHARM_MAX_MOB_SHIELD_HITS);
		mMobShieldRadius = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_MOB_SHIELD_RADIUS, MOB_SHIELD_RADIUS);
		mMaxAbsorption = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION_MAX, MAX_SHIELD);
		mAbsorptionDuration = CharmManager.getDuration(mPlayer, CHARM_ABSORPTION_DURATION, ABSORPTION_DURATION);
		mDebuffReduction = CharmManager.getDuration(mPlayer, CHARM_DEBUFF_REDUCTION, DEBUFF_REDUCTION);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
		mMoveSpeed = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_MOVEMENT_SPEED, MOVE_SPEED);
		mMaxDuration = CharmManager.getDuration(mPlayer, CHARM_MOVEMENT_DURATION, MAX_DURATION);
		mDamageMult = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, DAMAGE_FRACTION);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new PanaceaCS());

		Bukkit.getScheduler().runTask(
			plugin,
			() -> {
				mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
				mGruesomeAlchemy = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, GruesomeAlchemy.class);
				mBrutalAlchemy = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, BrutalAlchemy.class);
			}
		);
	}

	private int mCurrDuration = -1;

	public boolean cast() {
		if (isOnCooldown() || mAlchemistPotions == null) {
			return false;
		}

		putOnCooldown();
		mCurrMobShieldHits = 0;
		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);
		mCosmetic.castEffects(mPlayer, mRadius);

		double potionDamage = mAlchemistPotions.getDamage();
		double damage = potionDamage * mDamageMult;
		PotionUtils.reduceAllDebuffsDuration(mPlugin, mPlayer, mDebuffReduction);

		mCurrDuration = 0;
		ClientModHandler.updateAbility(mPlayer, this);

		cancelOnDeath(new BukkitRunnable() {
			final Location mLoc = mPlayer.getEyeLocation();
			Vector mIncrement = mLoc.getDirection().multiply(mMoveSpeed);

			final Set<UUID> mHitEntities = new HashSet<>();

			int mTicks = 0;
			int mTotalTicks = 0;
			boolean mReverse = false;

			@Override
			public void run() {
				if (!mPlayer.getWorld().equals(mLoc.getWorld())) {
					cancel();
					return;
				}

				mLoc.add(mIncrement);

				Hitbox hitbox = new Hitbox.SphereHitbox(mLoc, mRadius);
				for (LivingEntity mob : hitbox.getHitMobs()) {
					if (!mHitEntities.add(mob.getUniqueId())) {
						continue;
					}
					if (mCurrMobShieldHits < mMaxMobShieldHits) {
						mCurrMobShieldHits++;
						new Hitbox.SphereHitbox(mLoc, mMobShieldRadius)
							.getHitPlayers(true)
							.forEach(p -> AbsorptionUtils.addAbsorption(p, mShieldMobs, mMaxAbsorption, mAbsorptionDuration));
					}
					DamageUtils.damage(
						mPlayer,
						mob,
						new DamageEvent.Metadata(
							DamageEvent.DamageType.MAGIC,
							mInfo.getLinkedSpell(),
							playerItemStats),
						damage,
						true,
						true,
						false);

					GruesomeAlchemy.tryDoEnhancementEffect(mGruesomeAlchemy, mob);
					BrutalAlchemy.tryDoEnhancementEffect(mBrutalAlchemy, mob);

					if (mAlchemistPotions != null) {
						boolean isGruesome = mGruesomeAlchemy == null || !mGruesomeAlchemy.isAfflicted(mob);
						mAlchemistPotions.applyEffects(mob, isGruesome, playerItemStats, isLevelOne() ? 1 : 2);
					}

					if (!EntityUtils.isBoss(mob)) {
						EntityUtils.applySlow(mPlugin, mSlowTicks, 1, mob);
					}
					mCosmetic.projectileHitEffects(mPlayer, mob, mRadius);
				}
				for (Player player : hitbox.getHitPlayers(mPlayer, true)) {
					if (!mHitEntities.add(player.getUniqueId())) {
						continue;
					}
					AbsorptionUtils.addAbsorption(player, mPlayer, mShieldAllies, mMaxAbsorption, mAbsorptionDuration);
					PotionUtils.reduceAllDebuffsDuration(mPlugin, player, mDebuffReduction);
					mCosmetic.projectileHitEffects(mPlayer, player, mRadius);
				}

				mCosmetic.projectileEffects(mPlayer, mLoc, mRadius, mTotalTicks, mMoveSpeed, mIncrement);

				if (!mReverse && (!mLoc.isChunkLoaded() || LocationUtils.collidesWithSolid(mLoc) || mTicks >= mMaxDuration)) {
					mCosmetic.projectileReverseEffects(mPlayer, mLoc, mRadius);
					mHitEntities.clear();
					mReverse = true;
				}

				if (mReverse) {
					if (mTicks <= 0) {
						mCosmetic.projectileEndEffects(mPlayer, mLoc, mRadius);
						this.cancel();
						return;
					}

					// The mIncrement is calculated by the distance to the player divided by the number of increments left
					mIncrement = mPlayer.getEyeLocation().toVector().subtract(mLoc.toVector()).multiply(1.0 / mTicks);
					if (mIncrement.lengthSquared() > 4) { // player teleported away or otherwise moved too quickly
						mCosmetic.projectileEndEffects(mPlayer, mLoc, mRadius);
						cancel();
						return;
					}

					// To make the particles function without rewriting the particle code, manually calculate and set pitch and yaw
					double x = mIncrement.getX();
					double y = mIncrement.getY();
					double z = mIncrement.getZ();
					// As long as Z is nonzero, we won't get division by 0
					if (z == 0) {
						z = 0.0001;
					}
					float pitch = (float) Math.toDegrees(-Math.atan(y / Math.sqrt(Math.pow(x, 2) + Math.pow(z, 2))));
					float yaw = (float) Math.toDegrees(Math.atan(-x / z));
					if (z < 0) {
						yaw += 180;
					}
					mLoc.setPitch(pitch);
					mLoc.setYaw(yaw);

					mTicks--;
					if (mCurrDuration > 0) {
						mCurrDuration--;
					}
				} else {
					mTicks++;
					if (mCurrDuration >= 0) {
						mCurrDuration++;
					}
				}
				mTotalTicks++;
			}

			@Override
			public synchronized void cancel() {
				super.cancel();
				mCurrDuration = -1;
				ClientModHandler.updateAbility(mPlayer, Panacea.this);
			}
		}.runTaskTimer(mPlugin, 0, 1));

		return true;
	}

	@Override
	public int getInitialAbilityDuration() {
		return MAX_DURATION;
	}

	@Override
	public int getRemainingAbilityDuration() {
		return this.mCurrDuration >= 0 ? getInitialAbilityDuration() - this.mCurrDuration : 0;
	}

	private static Description<Panacea> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Fire a slow-moving mixture in front of you.")
			.addLine("Reduce the duration of your potion debuffs.")
			.addLine()
			.addLine("Other players hit by the mixture gain absorption,")
			.addLine("and have their potion debuffs' durations reduced.")
			.addLine()
			.addStat("Effect: +%d1 Absorption for %t (max +%d)")
				.statValues(stat(a -> a.mShieldAllies, SHIELD_ALLIES_1), stat(a -> a.mAbsorptionDuration, ABSORPTION_DURATION), stat(a -> a.mMaxAbsorption, MAX_SHIELD))
			.addStat("Effect: -%t Debuff Duration")
				.statValues(stat(a -> a.mDebuffReduction, DEBUFF_REDUCTION))
			.addLine()
			.addLine("Mobs hit by the mixture take damage, are rooted,")
			.addLine("and are afflicted with *Level 1* *Gruesome*, or").styles(WHITE, Alchemist.GRUESOME_COLOR)
			.addLine("*Brutal* if already afflicted by *Gruesome*.").styles(Alchemist.BRUTAL_COLOR, Alchemist.GRUESOME_COLOR)
			.addLine("When the mixture hits a mob, all players near")
			.addLine("that mob gain absorption. (max %d mobs per cast)")
				.statValues(stat(a -> a.mMaxMobShieldHits, MAX_MOB_HITS))
			.addLine()
			.addStat("Damage: %p (s) (of potion damage)")
				.statValues(stat(a -> a.mDamageMult, DAMAGE_FRACTION))
			.addStat("Effect: Root for %t1")
				.statValues(stat(a -> a.mSlowTicks, SLOW_TICKS_1))
			.addStat("Effect: +%d Absorption per mob for %t")
				.statValues(stat(a -> a.mShieldMobs, SHIELD_MOBS), stat(a -> a.mAbsorptionDuration, ABSORPTION_DURATION), stat(a -> a.mMaxAbsorption, MAX_SHIELD))
			.addStat("Absorption Radius: %r")
				.statValues(stat(a -> a.mMobShieldRadius, MOB_SHIELD_RADIUS))
			.addLine()
			.addLine("After hitting a block or traveling %d blocks,")
				.statValues(stat(a -> a.mMaxDuration * a.mMoveSpeed, MAX_DURATION * MOVE_SPEED))
			.addLine("the mixture returns back to you and can hit")
			.addLine("mobs and players a second time.")
			.addLine()
			.addStat("Mixture Radius: %r")
				.statValues(stat(a -> a.mRadius, RADIUS))
			.addStat("Cooldown: %t")
				.statValues(cooldown(COOLDOWN))
			.addDashedLine();
	}

	private static Description<Panacea> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Panacea*'s absorption for players and").styles(UNDERLINED)
			.addLine("root duration for mobs.")
			.addLine()
			.addLine("*Panacea* now inflicts *Level 2* *Gruesome* and *Brutal*.").styles(UNDERLINED, WHITE, Alchemist.GRUESOME_COLOR, Alchemist.BRUTAL_COLOR)
			.addLine()
			.addStatComparison("Effect: +%d1 -> +%d2 Absorption")
				.statValues(stat(SHIELD_ALLIES_1), stat(a -> a.mShieldAllies, SHIELD_ALLIES_2))
			.addStatComparison("Effect: %t1 -> %t2 Root")
				.statValues(stat(SLOW_TICKS_1), stat(a -> a.mSlowTicks, SLOW_TICKS_2))
			.addDashedLine();
	}
}

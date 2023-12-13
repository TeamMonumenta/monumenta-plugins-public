package com.playmonumenta.plugins.abilities.alchemist.apothecary;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.abilities.alchemist.PotionAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.apothecary.PanaceaCS;
import com.playmonumenta.plugins.effects.CustomDamageOverTime;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.StringUtils;
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


public class Panacea extends Ability implements AbilityWithDuration {

	private static final double PANACEA_DAMAGE_FRACTION = 1;
	private static final int PANACEA_1_SHIELD = 2;
	private static final int PANACEA_2_SHIELD = 4;
	private static final int PANACEA_MAX_SHIELD = 16;
	private static final int PANACEA_ABSORPTION_DURATION = 20 * 24;
	// Calculate the range with MAX_DURATION * MOVE_SPEED
	private static final int PANACEA_MAX_DURATION = 20 * 2;
	private static final double PANACEA_MOVE_SPEED = 0.35;
	public static final double PANACEA_RADIUS = 1.5;
	private static final int PANACEA_1_SLOW_TICKS = (int) (1.5 * 20);
	private static final int PANACEA_2_SLOW_TICKS = 2 * 20;
	private static final int COOLDOWN = 20 * 20;
	private static final double PANACEA_LEVEL_1_DOT_MULTIPLIER = 0.15;
	private static final double PANACEA_LEVEL_2_DOT_MULTIPLIER = 0.30;
	private static final String PANACEA_DOT_EFFECT_NAME = "PanaceaDamageOverTimeEffect";
	private static final int PANACEA_DOT_PERIOD = 10;
	private static final int PANACEA_DOT_DURATION = 20 * 9;

	public static final String CHARM_DAMAGE = "Panacea Damage";
	public static final String CHARM_DOT_DAMAGE = "Panacea DoT Damage";
	public static final String CHARM_ABSORPTION = "Panacea Absorption Health";
	public static final String CHARM_ABSORPTION_MAX = "Panacea Max Absorption Health";
	public static final String CHARM_ABSORPTION_DURATION = "Panacea Absorption Duration";
	public static final String CHARM_MOVEMENT_DURATION = "Panacea Movement Duration";
	public static final String CHARM_MOVEMENT_SPEED = "Panacea Movement Speed";
	public static final String CHARM_RADIUS = "Panacea Radius";
	public static final String CHARM_SLOW_DURATION = "Panacea Slow Duration";
	public static final String CHARM_COOLDOWN = "Panacea Cooldown";

	public static final AbilityInfo<Panacea> INFO =
		new AbilityInfo<>(Panacea.class, "Panacea", Panacea::new)
			.linkedSpell(ClassAbility.PANACEA)
			.scoreboardId("Panacea")
			.shorthandName("Pn")
			.actionBarColor(TextColor.color(255, 255, 100))
			.descriptions(
				("Sneak Drop with an Alchemist Bag to shoot a mixture that deals %s%% of your potion damage, " +
				"applies 100%% Slow for %ss, applies a %s%% base magic Damage Over Time effect every %ss for %ss, " +
				"and adds %s absorption health to other players per enemy touched (maximum %s absorption), lasting %ss. " +
				"After hitting a block or traveling %s blocks, the mixture traces and returns to you, able to " +
				"damage enemies and shield allies a second time. Cooldown: %ss.")
					.formatted(
							StringUtils.multiplierToPercentage(PANACEA_DAMAGE_FRACTION),
							StringUtils.ticksToSeconds(PANACEA_1_SLOW_TICKS),
							StringUtils.multiplierToPercentage(PANACEA_LEVEL_1_DOT_MULTIPLIER),
							StringUtils.ticksToSeconds(PANACEA_DOT_PERIOD),
							StringUtils.ticksToSeconds(PANACEA_DOT_DURATION),
							PANACEA_1_SHIELD,
							PANACEA_MAX_SHIELD,
							StringUtils.ticksToSeconds(PANACEA_ABSORPTION_DURATION),
							StringUtils.to2DP(PANACEA_MAX_DURATION * PANACEA_MOVE_SPEED),
							StringUtils.ticksToSeconds(COOLDOWN)
					),
				("Absorption health added is increased to %s, Slow duration is increased to %ss, " +
				"Damage Over Time is increased to %s%% base magic damage.")
					.formatted(
							PANACEA_2_SHIELD,
							StringUtils.ticksToSeconds(PANACEA_2_SLOW_TICKS),
							StringUtils.multiplierToPercentage(PANACEA_LEVEL_2_DOT_MULTIPLIER)
					)
			)
			.simpleDescription("Launch a slow moving projectile that inflicts a heavy damage over time effect to mobs hit, shielding allies in the process.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Panacea::cast, new AbilityTrigger(AbilityTrigger.Key.DROP).sneaking(true),
				PotionAbility.HOLDING_ALCHEMIST_BAG_RESTRICTION))
			.displayItem(Material.PURPLE_CONCRETE_POWDER);

	private final double mShield;
	private final int mSlowTicks;
	private final PanaceaCS mCosmetic;
	private @Nullable AlchemistPotions mAlchemistPotions;

	public Panacea(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mSlowTicks = CharmManager.getDuration(mPlayer, CHARM_SLOW_DURATION, (isLevelOne() ? PANACEA_1_SLOW_TICKS : PANACEA_2_SLOW_TICKS));
		mShield = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION, isLevelOne() ? PANACEA_1_SHIELD : PANACEA_2_SHIELD);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new PanaceaCS());

		Bukkit.getScheduler().runTask(plugin, () -> {
			mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
		});
	}

	private int mCurrDuration = -1;

	public boolean cast() {
		if (isOnCooldown() || mAlchemistPotions == null) {
			return false;
		}

		putOnCooldown();

		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

		double radius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, PANACEA_RADIUS);
		double moveSpeed = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_MOVEMENT_SPEED, PANACEA_MOVE_SPEED);
		int maxDuration = CharmManager.getDuration(mPlayer, CHARM_MOVEMENT_DURATION, PANACEA_MAX_DURATION);

		double maxAbsorption = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION_MAX, PANACEA_MAX_SHIELD);
		int absorptionDuration = CharmManager.getDuration(mPlayer, CHARM_ABSORPTION_DURATION, PANACEA_ABSORPTION_DURATION);

		mCosmetic.castEffects(mPlayer, radius);

		double damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, mAlchemistPotions.getDamage() * PANACEA_DAMAGE_FRACTION);
		double dotDamage = mAlchemistPotions.getDamage() * CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DOT_DAMAGE, isLevelOne() ? PANACEA_LEVEL_1_DOT_MULTIPLIER : PANACEA_LEVEL_2_DOT_MULTIPLIER);

		mCurrDuration = 0;
		ClientModHandler.updateAbility(mPlayer, this);

		cancelOnDeath(new BukkitRunnable() {
			final Location mLoc = mPlayer.getEyeLocation();
			Vector mIncrement = mLoc.getDirection().multiply(moveSpeed);

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

				Hitbox hitbox = new Hitbox.SphereHitbox(mLoc, radius);
				for (LivingEntity mob : hitbox.getHitMobs()) {
					if (!mHitEntities.add(mob.getUniqueId())) {
						continue;
					}
					DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC, mInfo.getLinkedSpell(), playerItemStats), damage, true, true, false);
					CustomDamageOverTime damageOverTime = new CustomDamageOverTime(PANACEA_DOT_DURATION, dotDamage, PANACEA_DOT_PERIOD, mPlayer, playerItemStats, mInfo.getLinkedSpell(), DamageEvent.DamageType.MAGIC);
					damageOverTime.setVisuals(mCosmetic::damageOverTimeEffects);
					mPlugin.mEffectManager.addEffect(mob, PANACEA_DOT_EFFECT_NAME, damageOverTime);

					if (!EntityUtils.isBoss(mob)) {
						EntityUtils.applySlow(mPlugin, mSlowTicks, 1, mob);
					}
					mCosmetic.projectileHitEffects(mPlayer, mob, radius);
				}
				for (Player player : hitbox.getHitPlayers(mPlayer, true)) {
					if (!mHitEntities.add(player.getUniqueId())) {
						continue;
					}
					AbsorptionUtils.addAbsorption(player, mShield, maxAbsorption, absorptionDuration);
					mCosmetic.projectileHitEffects(mPlayer, player, radius);
				}

				mCosmetic.projectileEffects(mPlayer, mLoc, radius, mTotalTicks, moveSpeed, mIncrement);

				if (!mReverse && (!mLoc.isChunkLoaded() || LocationUtils.collidesWithSolid(mLoc) || mTicks >= maxDuration)) {
					mCosmetic.projectileReverseEffects(mPlayer, mLoc, radius);
					mHitEntities.clear();
					mReverse = true;
				}

				if (mReverse) {
					if (mTicks <= 0) {
						mCosmetic.projectileEndEffects(mPlayer, mLoc, radius);
						this.cancel();
						return;
					}

					// The mIncrement is calculated by the distance to the player divided by the number of increments left
					mIncrement = mPlayer.getEyeLocation().toVector().subtract(mLoc.toVector()).multiply(1.0 / mTicks);
					if (mIncrement.lengthSquared() > 4) { // player teleported away or otherwise moved too quickly
						mCosmetic.projectileEndEffects(mPlayer, mLoc, radius);
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
		return PANACEA_MAX_DURATION;
	}

	@Override
	public int getRemainingAbilityDuration() {
		return this.mCurrDuration >= 0 ? getInitialAbilityDuration() - this.mCurrDuration : 0;
	}
}

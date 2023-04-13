package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.UnstableAmalgamCS;
import com.playmonumenta.plugins.effects.UnstableAmalgamDisable;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class UnstableAmalgam extends Ability {

	private static final int UNSTABLE_AMALGAM_1_COOLDOWN = 20 * 20;
	private static final int UNSTABLE_AMALGAM_2_COOLDOWN = 16 * 20;
	private static final int UNSTABLE_AMALGAM_1_DAMAGE = 10;
	private static final int UNSTABLE_AMALGAM_2_DAMAGE = 15;
	private static final int UNSTABLE_AMALGAM_CAST_RANGE = 7;
	private static final int UNSTABLE_AMALGAM_DURATION = 3 * 20;
	private static final int UNSTABLE_AMALGAM_RADIUS = 4;
	private static final float UNSTABLE_AMALGAM_KNOCKBACK_SPEED_HORIZONTAL = 2.5f;
	private static final float UNSTABLE_AMALGAM_KNOCKBACK_SPEED_VERTICAL = 1.0f;
	private static final int UNSTABLE_AMALGAM_ENHANCEMENT_UNSTABLE_DURATION = 20 * 8;
	private static final double UNSTABLE_AMALGAM_ENHANCEMENT_UNSTABLE_DAMAGE = 0.4;
	private static final String DISABLE_SOURCE = "UnstableAmalgamDisable";

	public static final String ROCKET_JUMP_OBJECTIVE = "RocketJumper";

	public static final String CHARM_COOLDOWN = "Unstable Amalgam Cooldown";
	public static final String CHARM_DAMAGE = "Unstable Amalgam Damage";
	public static final String CHARM_RANGE = "Unstable Amalgam Cast Range";
	public static final String CHARM_RADIUS = "Unstable Amalgam Radius";
	public static final String CHARM_DURATION = "Unstable Amalgam Duration";
	public static final String CHARM_KNOCKBACK = "Unstable Amalgam Knockback Speed";
	public static final String CHARM_INSTABILITY_DURATION = "Unstable Amalgam Instability Duration";
	public static final String CHARM_POTION_DAMAGE = "Unstable Amalgam Dropped Potion Damage Modifier";

	public static final AbilityInfo<UnstableAmalgam> INFO =
		new AbilityInfo<>(UnstableAmalgam.class, "Unstable Amalgam", UnstableAmalgam::new)
			.linkedSpell(ClassAbility.UNSTABLE_AMALGAM)
			.scoreboardId("UnstableAmalgam")
			.shorthandName("UA")
			.descriptions(
				("Shift left click while holding an Alchemist's Bag to consume a potion to place an Amalgam with 1 health at the location you are looking, up to %s blocks away. " +
				 "Shift left click again to detonate it, dealing your Alchemist Potion's damage + %s magic damage to mobs in a %s block radius and applying potion effects from all abilities. " +
				 "The Amalgam also explodes when killed, or %s seconds after being placed. " +
				 "Mobs and players in the radius are knocked away from the Amalgam. For each mob damaged, gain an Alchemist's Potion. Cooldown: %ss.")
					.formatted(
							UNSTABLE_AMALGAM_CAST_RANGE,
							UNSTABLE_AMALGAM_1_DAMAGE,
							UNSTABLE_AMALGAM_RADIUS,
							StringUtils.ticksToSeconds(UNSTABLE_AMALGAM_DURATION),
							StringUtils.ticksToSeconds(UNSTABLE_AMALGAM_1_COOLDOWN)
					),
				"The damage is increased to %s and the cooldown is reduced to %ss."
					.formatted(
							UNSTABLE_AMALGAM_2_DAMAGE,
							StringUtils.ticksToSeconds(UNSTABLE_AMALGAM_2_COOLDOWN)
					),
				("Enemies hit by the Amalgam's explosion become unstable for %s seconds, and they are knocked straight up. " +
					 "When an unstable mob is killed, a potion that deals %s%% of your potion damage is dropped at its location. " +
					 "These potions apply both Brutal and Gruesome effects, and bypass both normal and Alchemist Potion iframes.")
					.formatted(
							StringUtils.ticksToSeconds(UNSTABLE_AMALGAM_ENHANCEMENT_UNSTABLE_DURATION),
							StringUtils.multiplierToPercentage(UNSTABLE_AMALGAM_ENHANCEMENT_UNSTABLE_DAMAGE)
					)
			)
			.simpleDescription("Summon a ticking bomb that launches mobs (and players, if enabled) in the air, refunding you potions.")
			.cooldown(UNSTABLE_AMALGAM_1_COOLDOWN, UNSTABLE_AMALGAM_2_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", UnstableAmalgam::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true),
				PotionAbility.HOLDING_ALCHEMIST_BAG_RESTRICTION))
			.addTrigger(new AbilityTriggerInfo<>("toggleRocketJump", "toggle rocket jump", "Toggles knockback from the Amalgam on or off, just like using the /rocketjump command.",
				UnstableAmalgam::toggleRocketJump, new AbilityTrigger(AbilityTrigger.Key.DROP).enabled(false).lookDirections(AbilityTrigger.LookDirection.DOWN).sneaking(true), PotionAbility.HOLDING_ALCHEMIST_BAG_RESTRICTION))
			.displayItem(Material.GUNPOWDER);

	private @Nullable AlchemistPotions mAlchemistPotions;
	private boolean mHasGruesome;
	private @Nullable Slime mAmalgam;
	private @Nullable ItemStatManager.PlayerItemStats mPlayerItemStats;
	private final double mDamage;
	private final double mRadius;
	private final Map<ThrownPotion, ItemStatManager.PlayerItemStats> mEnhancementPotionPlayerStat;
	private final UnstableAmalgamCS mCosmetic;

	public UnstableAmalgam(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mAmalgam = null;
		mEnhancementPotionPlayerStat = new HashMap<>();

		mDamage = isLevelOne() ? UNSTABLE_AMALGAM_1_DAMAGE : UNSTABLE_AMALGAM_2_DAMAGE;
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, UNSTABLE_AMALGAM_RADIUS);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new UnstableAmalgamCS());

		Bukkit.getScheduler().runTask(plugin, () -> {
			mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
			mHasGruesome = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, GruesomeAlchemy.class) != null;
		});
	}

	public void cast() {
		// cast preconditions
		if (mAlchemistPotions == null
			    || mPlugin.mEffectManager.hasEffect(mPlayer, DISABLE_SOURCE)) {
			return;
		}

		// explode existing amalgam
		if (MetadataUtils.checkOnceInRecentTicks(mPlugin, mPlayer, "UnstableAmalgamCast", 2) && mAmalgam != null) {
			explode(mAmalgam.getLocation());
			mAmalgam.remove();
			mAmalgam = null;
			return;
		}
		if (isOnCooldown()) {
			return;
		}

		// cast new amalgam
		if (mAlchemistPotions.decrementCharge()) {
			putOnCooldown();

			Location loc = mPlayer.getEyeLocation();
			double step = 0.125;
			Vector dir = loc.getDirection().normalize().multiply(step);
			double range = CharmManager.getRadius(mPlayer, CHARM_RANGE, UNSTABLE_AMALGAM_CAST_RANGE);
			for (double i = 0; i < range; i += step) {
				loc.add(dir);

				if (NmsUtils.getVersionAdapter().hasCollision(loc.getWorld(), BoundingBox.of(loc, 0.21, 0.21, 0.21))) {
					loc.subtract(dir);
					spawnAmalgam(loc);

					return;
				}
			}

			spawnAmalgam(loc);
		}
	}

	private void spawnAmalgam(Location loc) {
		if (mAlchemistPotions == null) {
			return;
		}

		loc.setY(loc.getY() - 0.26); // spawn location is the bottom of the mob, so lower loc by half the slime's size

		mPlayerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

		int duration = CharmManager.getDuration(mPlayer, CHARM_DURATION, UNSTABLE_AMALGAM_DURATION);

		Entity e = LibraryOfSoulsIntegration.summon(loc, "UnstableAmalgam");
		if (e instanceof Slime amalgam) {
			mAmalgam = amalgam;

			new BukkitRunnable() {
				int mTicks = 0;
				@Override
				public void run() {
					if (mAmalgam == null) {
						this.cancel();
						return;
					}

					if (!mPlayer.isOnline()) {
						mAmalgam.remove();
						mAmalgam = null;
						this.cancel();
						return;
					}

					if (mAmalgam.isDead() || mTicks >= duration) {
						explode(mAmalgam.getLocation());
						mAmalgam.remove();
						mAmalgam = null;
						this.cancel();
						return;
					}

					mCosmetic.periodicEffects(mPlayer, loc, mRadius, mTicks, duration);

					mTicks++;
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	private void explode(Location loc) {
		if (!mPlayer.isOnline() || mAlchemistPotions == null || mPlayerItemStats == null) {
			return;
		}

		float horizontalKnockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, UNSTABLE_AMALGAM_KNOCKBACK_SPEED_HORIZONTAL);
		float verticalKnockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, UNSTABLE_AMALGAM_KNOCKBACK_SPEED_VERTICAL);
		float playerVertical = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, UNSTABLE_AMALGAM_KNOCKBACK_SPEED_VERTICAL * 2);

		Hitbox hitbox = new Hitbox.SphereHitbox(loc, mRadius);
		List<LivingEntity> mobs = hitbox.getHitMobs(mAmalgam);
		mobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));

		double damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, mDamage + mAlchemistPotions.getDamage());
		for (LivingEntity mob : mobs) {
			DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageType.MAGIC, mInfo.getLinkedSpell(), mPlayerItemStats), damage, true, true, false);

			applyEffects(mob, mPlayerItemStats);

			if (!EntityUtils.isBoss(mob)) {
				if (isEnhanced()) {
					mob.setVelocity(new Vector(0, verticalKnockback, 0));
				} else {
					MovementUtils.knockAwayRealistic(loc, mob, horizontalKnockback, verticalKnockback, true);
				}
			}
			mAlchemistPotions.incrementCharge();
		}

		if (isEnhanced()) {
			int duration = CharmManager.getDuration(mPlayer, CHARM_INSTABILITY_DURATION, UNSTABLE_AMALGAM_ENHANCEMENT_UNSTABLE_DURATION);

			if (!mobs.isEmpty()) {
				unstableMobs(mobs, duration);
			}
		}

		if (!ZoneUtils.hasZoneProperty(loc, ZoneProperty.NO_MOBILITY_ABILITIES)) {
			for (Player player : hitbox.getHitPlayers(true)) {
				if (!ZoneUtils.hasZoneProperty(player, ZoneProperty.NO_MOBILITY_ABILITIES)) {
					if (!player.equals(mPlayer) && ScoreboardUtils.getScoreboardValue(player, ROCKET_JUMP_OBJECTIVE).orElse(0) == 100) {
						MovementUtils.knockAwayRealistic(loc, player, horizontalKnockback, playerVertical, false);
						disable(player);
					} else if (player.equals(mPlayer) && ScoreboardUtils.getScoreboardValue(player, ROCKET_JUMP_OBJECTIVE).orElse(1) > 0) {
						// by default any Alch can use Rocket Jump with their UA
						MovementUtils.knockAwayRealistic(loc, player, horizontalKnockback, playerVertical, false);
						disable(player);
					}
				}
			}
		}

		mCosmetic.explodeEffects(mPlayer, loc, mRadius);
	}

	private void disable(Player player) {
		mPlugin.mEffectManager.addEffect(player, DISABLE_SOURCE, new UnstableAmalgamDisable(9999));
	}

	private void unstableMobs(List<LivingEntity> mobs, int duration) {
		if (mAlchemistPotions == null) {
			return;
		}

		new BukkitRunnable() {
			int mTimes = 0;
			@Override public void run() {
				mTimes++;
				if (mPlayerItemStats != null) {
					mobs.clear();
					cancel();
				}

				for (Iterator<LivingEntity> iterator = mobs.iterator(); iterator.hasNext(); ) {
					LivingEntity mob = iterator.next();
					if (mob.isDead()) {
						iterator.remove();
						ThrownPotion potion = mPlayer.launchProjectile(ThrownPotion.class);
						potion.teleport(mob.getEyeLocation());
						potion.setVelocity(new Vector(0, -1, 0));
						setEnhancementThrownPotion(potion, mPlayerItemStats);
						mCosmetic.unstableMobDeath(mPlayer, mob);
					} else {
						mCosmetic.unstableMobEffects(mPlayer, mob);
					}
				}

				if (mobs.isEmpty() || mTimes >= duration || !mPlayer.isValid()) {
					mobs.clear();
					cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	public void setEnhancementThrownPotion(ThrownPotion potion, ItemStatManager.PlayerItemStats playerItemStats) {
		mEnhancementPotionPlayerStat.put(potion, playerItemStats);
		if (mAlchemistPotions != null) {
			mAlchemistPotions.setPotionAlchemistPotionAesthetic(potion, mAlchemistPotions.isGruesomeMode());
		}
	}

	@Override
	public boolean playerSplashPotionEvent(Collection<LivingEntity> affectedEntities, ThrownPotion potion, PotionSplashEvent event) {
		if (mAlchemistPotions == null) {
			return true;
		}
		ItemStatManager.PlayerItemStats stats = mEnhancementPotionPlayerStat.remove(potion);
		if (isEnhanced() && stats != null) {
			Location loc = potion.getLocation();

			double damage = mAlchemistPotions.getDamage(stats) * (UNSTABLE_AMALGAM_ENHANCEMENT_UNSTABLE_DAMAGE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_POTION_DAMAGE));
			double radius = mAlchemistPotions.getRadius(stats);
			mCosmetic.unstablePotionSplash(mPlayer, loc, radius);
			for (LivingEntity entity : new Hitbox.SphereHitbox(loc, radius).getHitMobs()) {
				DamageUtils.damage(mPlayer, entity, new DamageEvent.Metadata(DamageType.MAGIC, mInfo.getLinkedSpell(), stats), damage, true, true, false);
				applyEffects(entity, stats);
			}
		}

		mEnhancementPotionPlayerStat.keySet().removeIf(pot -> pot.isDead() || !pot.isValid());
		return true;
	}

	private void applyEffects(LivingEntity entity, ItemStatManager.PlayerItemStats stats) {
		if (mAlchemistPotions != null) {
			if (mHasGruesome) {
				mAlchemistPotions.applyEffects(entity, true, stats);
			}
			mAlchemistPotions.applyEffects(entity, false, stats);
		}
	}

	private void toggleRocketJump() {
		if (ScoreboardUtils.getScoreboardValue(mPlayer, ROCKET_JUMP_OBJECTIVE).orElse(0) == 0) {
			ScoreboardUtils.setScoreboardValue(mPlayer, ROCKET_JUMP_OBJECTIVE, 1);
			mPlayer.sendActionBar(Component.text("Rocket jump enabled"));
		} else {
			ScoreboardUtils.setScoreboardValue(mPlayer, ROCKET_JUMP_OBJECTIVE, 0);
			mPlayer.sendActionBar(Component.text("Rocket jump disabled"));
		}
	}

}

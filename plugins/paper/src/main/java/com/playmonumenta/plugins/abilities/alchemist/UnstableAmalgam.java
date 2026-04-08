package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.EsotericEnhancements;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.classes.Alchemist;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.UnstableAmalgamCS;
import com.playmonumenta.plugins.effects.UnstableAmalgamDisable;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
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

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;
import static com.playmonumenta.plugins.utils.DescriptionUtils.WHITE;

public class UnstableAmalgam extends Ability implements AbilityWithDuration {

	private static final int UNSTABLE_AMALGAM_1_COOLDOWN = 18 * 20;
	private static final int UNSTABLE_AMALGAM_2_COOLDOWN = 14 * 20;
	private static final int UNSTABLE_AMALGAM_1_DAMAGE = 10;
	private static final int UNSTABLE_AMALGAM_2_DAMAGE = 15;
	private static final int UNSTABLE_AMALGAM_CAST_RANGE = 7;
	private static final int UNSTABLE_AMALGAM_DURATION = 1 * 20;
	private static final int UNSTABLE_AMALGAM_RADIUS = 4;
	private static final float UNSTABLE_AMALGAM_KNOCKBACK_SPEED_HORIZONTAL = 2f;
	private static final float UNSTABLE_AMALGAM_KNOCKBACK_SPEED_VERTICAL = 0.8f;
	private static final int UNSTABLE_AMALGAM_ENHANCEMENT_UNSTABLE_DURATION = 20 * 8;
	private static final double UNSTABLE_AMALGAM_ENHANCEMENT_UNSTABLE_DAMAGE = 0.4;
	private static final String DISABLE_SOURCE = "UnstableAmalgamDisable";

	public static final String ROCKET_JUMP_OBJECTIVE = "RocketJumper";

	public static final String CHARM_COOLDOWN = "Unstable Amalgam Cooldown";
	public static final String CHARM_DAMAGE = "Unstable Amalgam Damage";
	public static final String CHARM_RANGE = "Unstable Amalgam Cast Range";
	public static final String CHARM_RADIUS = "Unstable Amalgam Radius";
	public static final String CHARM_DURATION = "Unstable Amalgam Duration";
	public static final String CHARM_KNOCKBACK_MOBS = "Unstable Amalgam Mob Knockback Speed";
	public static final String CHARM_KNOCKBACK_PLAYERS = "Unstable Amalgam Player Knockback Speed";
	public static final String CHARM_INSTABILITY_DURATION = "Unstable Amalgam Instability Duration";
	public static final String CHARM_POTION_DAMAGE = "Unstable Amalgam Dropped Potion Damage Modifier";

	public static final Style AMALGAM_COLOR = Style.style(TextColor.color(0xE68EE6));
	public static final Style UNSTABLE_COLOR = Style.style(TextColor.color(0x9043BF));

	public static final AbilityInfo<UnstableAmalgam> INFO =
		new AbilityInfo<>(UnstableAmalgam.class, "Unstable Amalgam", UnstableAmalgam::new)
			.linkedSpell(ClassAbility.UNSTABLE_AMALGAM)
			.scoreboardId("UnstableAmalgam")
			.shorthandName("UA")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Summon a ticking bomb that launches mobs (and players, if enabled) in the air, refunding you potions.")
			.cooldown(UNSTABLE_AMALGAM_1_COOLDOWN, UNSTABLE_AMALGAM_2_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", UnstableAmalgam::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true),
				PotionAbility.HOLDING_ALCHEMIST_BAG_RESTRICTION))
			.addTrigger(new AbilityTriggerInfo<>("toggleRocketJump", "toggle rocket jump", "Toggles knockback from the Amalgam on or off, just like using the /rocketjump command.",
				UnstableAmalgam::toggleRocketJump, new AbilityTrigger(AbilityTrigger.Key.DROP).enabled(false).lookDirections(AbilityTrigger.LookDirection.DOWN).sneaking(true), PotionAbility.HOLDING_ALCHEMIST_BAG_RESTRICTION))
			.displayItem(Material.GUNPOWDER);

	private @Nullable AlchemistPotions mAlchemistPotions;
	private @Nullable GruesomeAlchemy mGruesomeAlchemy;
	private @Nullable BrutalAlchemy mBrutalAlchemy;
	private @Nullable EsotericEnhancements mEsotericEnhancements;
	private @Nullable Slime mAmalgam = null;
	private @Nullable ItemStatManager.PlayerItemStats mPlayerItemStats = null;
	private final double mFlatDamage;
	private final double mPercentDamage;
	private final double mRadius;
	private final double mRange;
	private final float mMobHorizontalKnockback;
	private final float mMobVerticalKnockback;
	private final float mPlayerHorizontalKnockback;
	private final float mPlayerVerticalKnockback;
	private final int mInstabilityDuration;
	private final double mPotionDamageMult;
	private final Map<ThrownPotion, ItemStatManager.PlayerItemStats> mEnhancementPotionPlayerStat = new HashMap<>();
	private final UnstableAmalgamCS mCosmetic;

	private final int mMaxDuration;
	private int mCurrDuration = -1;

	public UnstableAmalgam(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mFlatDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE,
			isLevelOne() ? UNSTABLE_AMALGAM_1_DAMAGE : UNSTABLE_AMALGAM_2_DAMAGE);
		mPercentDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, 1);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, UNSTABLE_AMALGAM_RADIUS);
		mMaxDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, UNSTABLE_AMALGAM_DURATION);
		mRange = CharmManager.getRadius(mPlayer, CHARM_RANGE, UNSTABLE_AMALGAM_CAST_RANGE);
		mInstabilityDuration = CharmManager.getDuration(mPlayer, CHARM_INSTABILITY_DURATION,
			UNSTABLE_AMALGAM_ENHANCEMENT_UNSTABLE_DURATION);
		mPotionDamageMult = UNSTABLE_AMALGAM_ENHANCEMENT_UNSTABLE_DAMAGE +
			CharmManager.getLevelPercentDecimal(mPlayer, CHARM_POTION_DAMAGE);
		mMobHorizontalKnockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer,
			CHARM_KNOCKBACK_MOBS, UNSTABLE_AMALGAM_KNOCKBACK_SPEED_HORIZONTAL);
		mMobVerticalKnockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer,
			CHARM_KNOCKBACK_MOBS, UNSTABLE_AMALGAM_KNOCKBACK_SPEED_VERTICAL);
		mPlayerHorizontalKnockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer,
			CHARM_KNOCKBACK_PLAYERS, UNSTABLE_AMALGAM_KNOCKBACK_SPEED_HORIZONTAL * 1.25);
		mPlayerVerticalKnockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer,
			CHARM_KNOCKBACK_PLAYERS, UNSTABLE_AMALGAM_KNOCKBACK_SPEED_VERTICAL * 2.5);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new UnstableAmalgamCS());

		Bukkit.getScheduler().runTask(
			plugin,
			() -> {
				mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
				mGruesomeAlchemy = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, GruesomeAlchemy.class);
				mBrutalAlchemy = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, BrutalAlchemy.class);
				mEsotericEnhancements = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, EsotericEnhancements.class);
			}
		);
	}

	public boolean cast() {
		// cast preconditions
		if (mAlchemistPotions == null
			|| mPlugin.mEffectManager.hasEffect(mPlayer, DISABLE_SOURCE)) {
			return false;
		}

		if (isOnCooldown()) {
			return false;
		}

		// cast new amalgam
		if (mAlchemistPotions.decrementCharge()) {
			putOnCooldown();

			Location loc = mPlayer.getEyeLocation();
			double step = 0.125;
			Vector dir = loc.getDirection().normalize().multiply(step);
			for (double i = 0; i < mRange; i += step) {
				loc.add(dir);

				if (NmsUtils.getVersionAdapter().hasCollision(loc.getWorld(), BoundingBox.of(loc, 0.21, 0.21, 0.21))) {
					loc.subtract(dir);
					spawnAmalgam(loc);

					return true;
				}
			}

			spawnAmalgam(loc);
		}
		return true;
	}

	private void spawnAmalgam(Location loc) {
		if (mAlchemistPotions == null) {
			return;
		}

		loc.setY(loc.getY() - 0.26); // spawn location is the bottom of the mob, so lower loc by half the slime's size

		mPlayerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);
		mCurrDuration = 0;

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

					if (mAmalgam.isDead() || mTicks >= mMaxDuration) {
						explode(mAmalgam.getLocation());
						mAmalgam.remove();
						mAmalgam = null;
						this.cancel();
						return;
					}

					mCosmetic.periodicEffects(mPlayer, loc, mRadius, mTicks, mMaxDuration);

					mTicks++;
					mCurrDuration++;
				}

				@Override
				public synchronized void cancel() {
					super.cancel();
					mCurrDuration = -1;
					ClientModHandler.updateAbility(mPlayer, UnstableAmalgam.this);
				}
			}.runTaskTimer(mPlugin, 0, 1);

			ClientModHandler.updateAbility(mPlayer, this);
		}
	}

	private void explode(Location loc) {
		if (!mPlayer.isOnline() || mAlchemistPotions == null || mPlayerItemStats == null) {
			return;
		}

		Hitbox hitbox = new Hitbox.SphereHitbox(loc, mRadius);
		List<LivingEntity> mobs = hitbox.getHitMobs(mAmalgam);
		mobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));

		if (mEsotericEnhancements != null) {
			mEsotericEnhancements.createPuddle(loc, true, mPlayerItemStats, mRadius);
			mEsotericEnhancements.createPuddle(loc, false, mPlayerItemStats, mRadius);
		}

		double damage = mFlatDamage + mPercentDamage * mAlchemistPotions.getDamage();
		for (LivingEntity mob : mobs) {
			DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageType.MAGIC, mInfo.getLinkedSpell(), mPlayerItemStats), damage, true, true, false);

			GruesomeAlchemy.tryDoEnhancementEffect(mGruesomeAlchemy, mob);
			BrutalAlchemy.tryDoEnhancementEffect(mBrutalAlchemy, mob);
			applyEffects(mob, mPlayerItemStats);

			if (!EntityUtils.isBoss(mob) && !EntityUtils.isCCImmuneMob(mob)) {
				if (isEnhanced()) {
					mob.setVelocity(new Vector(0, mMobVerticalKnockback, 0));
					BossManager.getInstance().entityKnockedAway(mob, mMobVerticalKnockback);
				} else {
					MovementUtils.knockAwayRealistic(loc, mob, mMobHorizontalKnockback, mMobVerticalKnockback, true);
				}
			}
			mAlchemistPotions.incrementCharge();
		}

		if (isEnhanced()) {
			if (!mobs.isEmpty()) {
				unstableMobs(mobs, mInstabilityDuration);
			}
		}

		if (!ZoneUtils.hasZoneProperty(loc, ZoneProperty.NO_MOBILITY_ABILITIES)) {
			for (Player player : hitbox.getHitPlayers(true)) {
				if (!ZoneUtils.hasZoneProperty(player, ZoneProperty.NO_MOBILITY_ABILITIES)) {
					if (!player.equals(mPlayer) && ScoreboardUtils.getScoreboardValue(player, ROCKET_JUMP_OBJECTIVE).orElse(0) == 100) {
						MovementUtils.knockAwayRealistic(loc, player, mPlayerHorizontalKnockback, mPlayerVerticalKnockback, false);
						disable(player);
					} else if (player.equals(mPlayer) && ScoreboardUtils.getScoreboardValue(player, ROCKET_JUMP_OBJECTIVE).orElse(1) > 0) {
						// by default any Alch can use Rocket Jump with their UA
						MovementUtils.knockAwayRealistic(loc, player, mPlayerHorizontalKnockback, mPlayerVerticalKnockback, false);
						disable(player);
					}
				}
			}
		}

		mCosmetic.explodeEffects(mPlayer, loc, mRadius);
	}

	private void disable(Player player) {
		mPlugin.mEffectManager.addEffect(player, DISABLE_SOURCE, new UnstableAmalgamDisable(9999)
			.deleteOnAbilityUpdate(true));
	}

	private void unstableMobs(List<LivingEntity> mobs, int duration) {
		if (mAlchemistPotions == null) {
			return;
		}

		new BukkitRunnable() {
			int mTimes = 0;

			@Override
			public void run() {
				mTimes++;
				if (mPlayerItemStats == null) {
					mobs.clear();
					cancel();
					return;
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

				if (mobs.isEmpty() || mTimes >= duration || !mPlayer.isOnline() || mPlayer.isDead()) {
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

			double damage = mAlchemistPotions.getDamage(stats) * mPotionDamageMult;
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
			mAlchemistPotions.applyEffects(entity, true, stats);
			mAlchemistPotions.applyEffects(entity, false, stats);
		}
	}

	private boolean toggleRocketJump() {
		if (ScoreboardUtils.getScoreboardValue(mPlayer, ROCKET_JUMP_OBJECTIVE).orElse(0) == 0) {
			ScoreboardUtils.setScoreboardValue(mPlayer, ROCKET_JUMP_OBJECTIVE, 1);
			mPlayer.sendActionBar(Component.text("Rocket jump enabled"));
		} else {
			ScoreboardUtils.setScoreboardValue(mPlayer, ROCKET_JUMP_OBJECTIVE, 0);
			mPlayer.sendActionBar(Component.text("Rocket jump disabled"));
		}
		return true;
	}

	@Override
	public int getInitialAbilityDuration() {
		return mMaxDuration;
	}

	@Override
	public int getRemainingAbilityDuration() {
		return this.mCurrDuration >= 0 ? getInitialAbilityDuration() - this.mCurrDuration : 0;
	}

	private static Description<UnstableAmalgam> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Spend *1* potion to place an *Amalgam* at a").styles(WHITE, AMALGAM_COLOR)
			.addLine("location up to %d blocks away.")
				.statValues(stat(a -> a.mRange, UNSTABLE_AMALGAM_CAST_RANGE))
			.addLine()
			.addLine("The *Amalgam* explodes when hit, or after %t,").styles(AMALGAM_COLOR)
				.statValues(stat(a -> a.mMaxDuration, UNSTABLE_AMALGAM_DURATION))
			.addLine("dealing damage, knockback, and applying both")
			.addLine("*Gruesome* and *Brutal* onto mobs hit.").styles(Alchemist.GRUESOME_COLOR, Alchemist.BRUTAL_COLOR)
			.addLine()
			.addLine("Gain *1* potion for each mob hit.").styles(WHITE)
			.addLine()
			.addStat("Damage: %d1 + %p1 (s) (of potion damage)")
				.statValues(stat(a -> a.mFlatDamage, UNSTABLE_AMALGAM_1_DAMAGE), stat(a -> a.mPercentDamage, 1))
			.addStat("Radius: %r")
				.statValues(stat(a -> a.mRadius, UNSTABLE_AMALGAM_RADIUS))
			.addStat("Cooldown: %t1")
				.statValues(cooldown(UNSTABLE_AMALGAM_1_COOLDOWN))
			.addDashedLine();
	}

	private static Description<UnstableAmalgam> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Unstable Amalgam*'s damage").styles(UNDERLINED)
			.addLine("and reduce its cooldown.")
			.addLine()
			.addStatComparison("Damage: %d1 + %p1 -> %d2 + %p2 (s)")
				.statValues(stat(UNSTABLE_AMALGAM_1_DAMAGE), stat(1), stat(a -> a.mFlatDamage, UNSTABLE_AMALGAM_2_DAMAGE), stat(a -> a.mPercentDamage, 1))
			.addStatComparison("Cooldown: %t1 -> %t2")
				.statValues(cooldown(UNSTABLE_AMALGAM_1_COOLDOWN), cooldown(UNSTABLE_AMALGAM_2_COOLDOWN))
			.addDashedLine();
	}

	private static Description<UnstableAmalgam> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("Mobs hit by the *Amalgam* become *Unstable* for").styles(AMALGAM_COLOR, UNSTABLE_COLOR)
			.addLine("%t and are launched vertically.")
				.statValues(stat(a -> a.mInstabilityDuration, UNSTABLE_AMALGAM_ENHANCEMENT_UNSTABLE_DURATION))
			.addLine()
			.addLine("When an *Unstable* mob dies, it drops a potion").styles(UNSTABLE_COLOR)
			.addLine("that deals damage and applies both *Gruesome*").styles(Alchemist.GRUESOME_COLOR)
			.addLine("and *Brutal*.").styles(Alchemist.BRUTAL_COLOR)
			.addLine()
			.addStat("Unstable Damage: %p (s) (of potion damage)")
				.statValues(stat(a -> a.mPotionDamageMult, UNSTABLE_AMALGAM_ENHANCEMENT_UNSTABLE_DAMAGE))
			.addDashedLine();
	}
}

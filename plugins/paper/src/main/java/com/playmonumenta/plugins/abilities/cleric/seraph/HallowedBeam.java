package com.playmonumenta.plugins.abilities.cleric.seraph;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.seraph.HallowedBeamCS;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.Chaotic;
import com.playmonumenta.plugins.itemstats.enchantments.Duelist;
import com.playmonumenta.plugins.itemstats.enchantments.Grappling;
import com.playmonumenta.plugins.itemstats.enchantments.HexEater;
import com.playmonumenta.plugins.itemstats.enchantments.PointBlank;
import com.playmonumenta.plugins.itemstats.enchantments.Slayer;
import com.playmonumenta.plugins.itemstats.enchantments.Smite;
import com.playmonumenta.plugins.itemstats.enchantments.Sniper;
import com.playmonumenta.plugins.itemstats.enchantments.ThrowingKnife;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.listeners.DamageListener;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.perRegion;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class HallowedBeam extends MultipleChargeAbility {
	private static final int HALLOWED_MAX_CHARGES = 2;
	private static final int HALLOWED_COOLDOWN = 20 * 12;
	private static final int HALLOWED_COOLDOWN_2 = 20 * 10;
	private static final int HALLOWED_DAMAGE_FLAT_1_R2 = 22;
	private static final int HALLOWED_DAMAGE_FLAT_1_R3 = 28;
	private static final int HALLOWED_DAMAGE_FLAT_2_R2 = 25;
	private static final int HALLOWED_DAMAGE_FLAT_2_R3 = 32;
	private static final double HALLOWED_DAMAGE_PCT = 0.5;
	private static final double HALLOWED_HEAL_PERCENT_ALLY = 0.3;
	private static final double HALLOWED_DAMAGE_REDUCTION_PERCENT_ALLY = 0.15;
	private static final int HALLOWED_DAMAGE_REDUCTION_DURATION = 20 * 5;
	private static final String PERCENT_DAMAGE_RESIST_EFFECT_NAME = "HallowedPercentDamageResistEffect";
	private static final double HALLOWED_RADIUS = 2.5;
	private static final double HALLOWED_RADIUS_SCALING = 0.15;
	private static final int HALLOWED_SCALING_DISTANCE = 15;
	private static final int HALLOWED_STUN = 25;
	private static final int HALLOWED_FIRE = 5 * 20;
	private static final int CAST_RANGE = 30;
	private static final String MODE_SCOREBOARD = "HallowedBeamMode";
	public static final String BEAM_2_BOON_MARK = "HallowedBeamBoonMark";

	public static final String CHARM_DAMAGE = "Hallowed Beam Damage";
	public static final String CHARM_COOLDOWN = "Hallowed Beam Cooldown";
	public static final String CHARM_HEAL = "Hallowed Beam Healing";
	public static final String CHARM_RANGE = "Hallowed Beam Range";
	public static final String CHARM_RADIUS = "Hallowed Beam Base Radius";
	public static final String CHARM_RADIUS_SCALING = "Hallowed Beam Radius Scaling";
	public static final String CHARM_SCALING_DISTANCE = "Hallowed Beam Max Scaling Distance";
	public static final String CHARM_STUN = "Hallowed Beam Stun Duration";
	public static final String CHARM_RESISTANCE = "Hallowed Beam Resistance";
	public static final String CHARM_RESISTANCE_DURATION = "Hallowed Beam Resistance Duration";
	public static final String CHARM_CHARGE = "Hallowed Beam Charges";

	public static final AbilityInfo<HallowedBeam> INFO =
		new AbilityInfo<>(HallowedBeam.class, "Hallowed Beam", HallowedBeam::new)
			.linkedSpell(ClassAbility.HALLOWED_BEAM)
			.scoreboardId("HallowedBeam")
			.shorthandName("HB")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Heal a targeted player, damage a targeted Heretic, or stun a targeted non-Heretic from a distance.")
			.cooldown(HALLOWED_COOLDOWN, HALLOWED_COOLDOWN_2, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("swapMode", "swap mode", HallowedBeam::swapMode, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false).enabled(false).keyOptions(AbilityTrigger.KeyOptions.REQUIRE_PROJECTILE_WEAPON))).displayItem(Material.BOW);

	private enum Mode {
		DEFAULT(0, "Default"),
		HEALING(1, "Healing"),
		ATTACK(2, "Attack");

		public final int mScore;
		private final String mLabel;

		Mode(int score, String label) {
			mScore = score;
			mLabel = label;
		}
	}

	private Mode mMode = Mode.DEFAULT;
	private final HallowedBeamCS mCosmetic;
	private final double mDamageFlat;
	private final double mDamagePct;
	private final double mRange;
	private final double mBaseRadius;
	private final double mAddRadius;
	private final int mScalingDistance;
	private final double mHeal;
	private final double mResistance;
	private final int mResistanceDuration;
	private final int mStunDuration;

	private int mLastCastTicks = 0;
	private @Nullable KeeperVirtueShieldingFlare mKeeperVirtueShieldingFlare;

	public HallowedBeam(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamageFlat = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, ServerProperties.getAbilityEnhancementsEnabled(player) ? (isLevelTwo() ? HALLOWED_DAMAGE_FLAT_2_R3 : HALLOWED_DAMAGE_FLAT_1_R3) : (isLevelTwo() ? HALLOWED_DAMAGE_FLAT_2_R2 : HALLOWED_DAMAGE_FLAT_1_R2));
		mDamagePct = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, HALLOWED_DAMAGE_PCT);
		mMaxCharges = (int) CharmManager.getLevel(player, CHARM_CHARGE) + HALLOWED_MAX_CHARGES;
		mCharges = getChargesOffCooldown();
		mRange = CharmManager.getRadius(mPlayer, CHARM_RANGE, CAST_RANGE);
		mBaseRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, HALLOWED_RADIUS);
		mAddRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS_SCALING, HALLOWED_RADIUS_SCALING);
		mScalingDistance = (int) CharmManager.getRadius(mPlayer, CHARM_SCALING_DISTANCE, HALLOWED_SCALING_DISTANCE);
		mHeal = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEAL, HALLOWED_HEAL_PERCENT_ALLY);
		mResistance = HALLOWED_DAMAGE_REDUCTION_PERCENT_ALLY + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_RESISTANCE);
		mResistanceDuration = CharmManager.getDuration(mPlayer, CHARM_RESISTANCE_DURATION, HALLOWED_DAMAGE_REDUCTION_DURATION);
		mStunDuration = CharmManager.getDuration(mPlayer, CHARM_STUN, HALLOWED_STUN);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new HallowedBeamCS());

		Bukkit.getScheduler().runTask(plugin, () -> mKeeperVirtueShieldingFlare = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, KeeperVirtueShieldingFlare.class));

		if (player != null) {
			int modeIndex = ScoreboardUtils.getScoreboardValue(player, MODE_SCOREBOARD).orElse(0);
			mMode = Mode.values()[Math.max(0, Math.min(modeIndex, Mode.values().length - 1))];
		}
	}

	@Override
	public boolean playerShotProjectileEvent(Projectile projectile) {
		if (projectile.isDead() || !mPlayer.isSneaking() || Grappling.playerHoldingHook(mPlayer) || getCharges() <= 0 || Bukkit.getCurrentTick() - mLastCastTicks < 3) {
			return true;
		}
		projectile.remove();
		consumeCharge();
		mLastCastTicks = Bukkit.getCurrentTick();

		Location loc = mPlayer.getEyeLocation();
		Vector direction = loc.getDirection();
		World world = loc.getWorld();

		Predicate<Entity> eligiblePlayer = e -> e instanceof Player player && player != mPlayer;
		Predicate<Entity> eligibleMob = e -> EntityUtils.isHostileMob(e) && !ScoreboardUtils.checkTag(e, AbilityUtils.IGNORE_TAG) && e.isValid();
		RayTraceResult result = world.rayTrace(loc, direction, mRange, FluidCollisionMode.NEVER, true, 0.3,
			e -> (mMode == Mode.HEALING ? eligiblePlayer.test(e)
				: (mMode == Mode.ATTACK ? eligibleMob.test(e)
				: eligiblePlayer.test(e) || eligibleMob.test(e))));

		Location endLoc;
		@Nullable LivingEntity target = null;
		if (result == null) {
			endLoc = loc.clone().add(direction.multiply(mRange));
		} else {
			endLoc = result.getHitPosition().toLocation(world);
			target = (LivingEntity) result.getHitEntity();
		}

		@Nullable LivingEntity finalTarget = target;
		Bukkit.getScheduler().runTask(mPlugin, () -> {
			explode(endLoc, finalTarget, projectile);
			Location startLoc = mPlayer.getEyeLocation();
			mCosmetic.beamCast(mPlayer, startLoc, mRange, endLoc);
		});
		return true;
	}

	private void explode(Location loc, @Nullable LivingEntity target, Projectile projectile) {
		ItemStatManager.PlayerItemStats playerItemStats = DamageListener.getProjectileItemStats(projectile);
		if (playerItemStats == null) {
			return;
		}
		ItemStatManager.PlayerItemStats.ItemStatsMap itemStatsMap = playerItemStats.getItemStats();

		double damageMultiplier;
		boolean isArrow = (projectile instanceof Arrow || projectile instanceof SpectralArrow) && !ThrowingKnife.isThrowingKnife((AbstractArrow) projectile);
		if (isArrow && !((AbstractArrow) projectile).isShotFromCrossbow()) {
			damageMultiplier = PlayerUtils.calculateBowDraw((AbstractArrow) projectile);
		} else {
			damageMultiplier = 1;
		}
		if (damageMultiplier < 0.3) {
			return;
		}
		double finalDamageMultiplier = damageMultiplier;

		double radius = mBaseRadius + mAddRadius * Math.min(mScalingDistance, mPlayer.getEyeLocation().distance(loc));
		Hitbox aoeHitbox = new Hitbox.SphereHitbox(loc, radius);
		aoeHitbox.getHitMobs().forEach(mob -> {
			double damage = itemStatsMap.get(AttributeType.PROJECTILE_DAMAGE_ADD);
			// Apply all base damage enchants
			damage += Sniper.apply(mPlayer, mob, itemStatsMap.get(EnchantmentType.SNIPER));
			damage += PointBlank.apply(mPlayer, mob, itemStatsMap.get(EnchantmentType.POINT_BLANK));
			damage += HexEater.calculateHexDamage(mPlugin, true, mPlayer, (int) itemStatsMap.get(EnchantmentType.HEX_EATER), mob);
			damage += Smite.calculateSmiteDamage(true, mPlayer, itemStatsMap.get(EnchantmentType.SMITE), mob);
			damage += Slayer.calculateSlayerDamage(true, mPlayer, itemStatsMap.get(EnchantmentType.SLAYER), mob);
			damage += Duelist.calculateDuelistDamage(true, mPlayer, itemStatsMap.get(EnchantmentType.DUELIST), mob);
			damage += Chaotic.calculateChaoticDamage(true, mPlayer, itemStatsMap.get(EnchantmentType.CHAOTIC), mob);
			damage *= itemStatsMap.get(AttributeType.PROJECTILE_DAMAGE_MULTIPLY);
			DamageUtils.damage(mPlayer, mob,
				new DamageEvent.Metadata(
					DamageEvent.DamageType.MAGIC,
					mInfo.getLinkedSpell(),
					playerItemStats),
				finalDamageMultiplier * (target != null && mob == target ? mDamageFlat + mDamagePct * damage : mDamageFlat),
				true, false, false);

			MovementUtils.knockAway(loc, mob, 0.5f, 0.2f, true);
			EntityUtils.applyStun(mPlugin, mStunDuration, mob);
			EntityUtils.applyFire(mPlugin, HALLOWED_FIRE, mob, mPlayer);
		});

		List<Player> hitPlayers = aoeHitbox.getHitPlayers(mPlayer, true);
		hitPlayers.forEach(p -> {
			PlayerUtils.healPlayer(mPlugin, p, mHeal * EntityUtils.getMaxHealth(p));
			if (isLevelTwo()) {
				mPlugin.mEffectManager.addEffect(p, PERCENT_DAMAGE_RESIST_EFFECT_NAME, new PercentDamageReceived(mResistanceDuration, -mResistance));
			}
		});

		if (mKeeperVirtueShieldingFlare != null && !hitPlayers.isEmpty()) {
			Player virtueShield = target instanceof Player player ? player : EntityUtils.getNearestPlayer(loc, hitPlayers);
			if (virtueShield != null) {
				mKeeperVirtueShieldingFlare.shieldPlayer(virtueShield);
			}
		}

		mCosmetic.beamSplash(mPlayer, loc, radius);
	}

	public boolean swapMode() {
		if (mMode == Mode.DEFAULT) {
			mMode = Mode.HEALING;
		} else if (mMode == Mode.HEALING) {
			mMode = Mode.ATTACK;
		} else {
			mMode = Mode.DEFAULT;
		}
		sendActionBarMessage(ClassAbility.HALLOWED_BEAM.getName() + " Mode: " + mMode.mLabel);
		ScoreboardUtils.setScoreboardValue(mPlayer, MODE_SCOREBOARD, mMode.mScore);
		ClientModHandler.updateAbility(mPlayer, this);
		return true;
	}

	@Override
	public @Nullable String getMode() {
		return mMode.name().toLowerCase(Locale.ROOT);
	}

	private static Description<HallowedBeam> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addCustomTrigger("Shoot Projectile while Sneaking")
			.addDashedLine()
			.addLine("Shoot a beam of light that bursts on contact with a")
			.addLine("block, mob, or player.")
			.addLine()
			.addStat("Range: %r")
				.statValues(stat(a -> a.mRange, CAST_RANGE))
			.addStat("Radius: %r + %d per block travelled")
				.statValues(stat(a -> a.mBaseRadius, HALLOWED_RADIUS),
					stat(a -> a.mAddRadius, HALLOWED_RADIUS_SCALING))
			.tab().addLine("(capped at %d blocks travelled)")
				.statValues(stat(a -> a.mScalingDistance, HALLOWED_SCALING_DISTANCE))
			.addStat("Charges: %d")
				.statValues(stat(a -> a.mMaxCharges, HALLOWED_MAX_CHARGES))
			.addStat("Cooldown: %t1 (per charge)")
				.statValues(cooldown(HALLOWED_COOLDOWN))
			.addLine()
			.addLine("Heal all allies in the radius.")
			.addLine()
			.addStat("Healing: %p HP")
				.statValues(stat(a -> a.mHeal, HALLOWED_HEAL_PERCENT_ALLY))
			.addLine()
			.addLine("Damage, stun, and set fire to all mobs in the radius.").styles(KeeperVirtue.VIRTUE_COLOR)
			.addLine("Deal more damage to mobs the beam hits directly.")
			.addLine()
			.addStat("Damage: %d1R (s)")
				.statValues(perRegion(a -> a.mDamageFlat, HALLOWED_DAMAGE_FLAT_1_R2, HALLOWED_DAMAGE_FLAT_1_R3))
			.addStat("Bonus Damage: +%p (s) (of the projectile's damage)")
				.statValues(stat(a -> a.mDamagePct, HALLOWED_DAMAGE_PCT))
			.addStat("Effect: Stun for %t")
				.statValues(stat(a -> a.mStunDuration, HALLOWED_STUN))
			.addStat("Effect: Fire for %t")
				.statValues(stat(HALLOWED_FIRE))
			.addDashedLine();
	}

	private static Description<HallowedBeam> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Reduce *Hallowed Beam*'s cooldown, increase its").styles(UNDERLINED)
			.addLine("damage, and grant resistance to all players healed.")
			.addLine()
			.addStatComparison("Cooldown: %t1 -> %t2 (per charge)")
				.statValues(cooldown(HALLOWED_COOLDOWN), cooldown(HALLOWED_COOLDOWN_2))
			.addStatComparison("Damage: %d1 -> %d2R (s)")
				.statValues(perRegion(HALLOWED_DAMAGE_FLAT_1_R2, HALLOWED_DAMAGE_FLAT_1_R3),
					perRegion(a -> a.mDamageFlat, HALLOWED_DAMAGE_FLAT_2_R2, HALLOWED_DAMAGE_FLAT_2_R3))
			.addStat("Effect: %p Resistance for %t")
				.statValues(stat(a -> a.mResistance, HALLOWED_DAMAGE_REDUCTION_PERCENT_ALLY),
					stat(a -> a.mResistanceDuration, HALLOWED_DAMAGE_REDUCTION_DURATION))
			.addDashedLine();
	}
}

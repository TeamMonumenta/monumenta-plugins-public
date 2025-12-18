package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warlock.SanguineHarvestCS;
import com.playmonumenta.plugins.effects.Bleed;
import com.playmonumenta.plugins.effects.SanguineHarvestBlight;
import com.playmonumenta.plugins.effects.SanguineMark;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;
import static com.playmonumenta.plugins.utils.DescriptionUtils.WHITE;

public class SanguineHarvest extends Ability implements AbilityWithDuration {

	private static final int BLEED_LEVEL = 1;
	private static final int RANGE = 8;
	private static final int RADIUS = 4;
	private static final double HEAL_PERCENT_1 = 0.05;
	private static final double HEAL_PERCENT_2 = 0.1;
	private static final double DAMAGE_BOOST = 0.1;
	private static final int MARK_DURATION = 20 * 20;
	private static final int COOLDOWN = 20 * 20;
	private static final double HITBOX_LENGTH = 0.55;
	private static final int MARKS = 1;

	private static final double ENHANCEMENT_DMG_INCREASE = 0.03;
	private static final int ENHANCEMENT_BLIGHT_DURATION = 20 * 6;
	private static final String BLIGHT_EFFECT_NAME = "SanguineHarvestBlightEffect";

	private static final String SANGUINE_NAME = "SanguineEffect";

	public static final String CHARM_RADIUS = "Sanguine Harvest Radius";
	public static final String CHARM_RANGE = "Sanguine Harvest Range";
	public static final String CHARM_COOLDOWN = "Sanguine Harvest Cooldown";
	public static final String CHARM_HEAL = "Sanguine Harvest Healing";
	public static final String CHARM_KNOCKBACK = "Sanguine Harvest Knockback";
	public static final String CHARM_DAMAGE_BOOST = "Sanguine Harvest Damage Boost Amplifier";
	public static final String CHARM_BLIGHT_DURATION = "Sanguine Harvest Blight Duration";
	public static final String CHARM_BLIGHT_VULN_PER_DEBUFF = "Sanguine Harvest Blight Vulnerability Per Debuff";
	public static final String CHARM_MARKS = "Sanguine Harvest Marks";

	public static final Style MARK_COLOR = Style.style(TextColor.color(0xB30947));

	public static final AbilityInfo<SanguineHarvest> INFO =
		new AbilityInfo<>(SanguineHarvest.class, "Sanguine Harvest", SanguineHarvest::new)
			.linkedSpell(ClassAbility.SANGUINE_HARVEST)
			.scoreboardId("SanguineHarvest")
			.shorthandName("SH")
			.actionBarColor(TextColor.color(179, 0, 0))
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Passively apply Bleed with your abilities. Activate to mark mobs, healing whoever kills them.")
			.quest216Message("-------s-------m-------")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", SanguineHarvest::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(false),
				AbilityTriggerInfo.HOLDING_SCYTHE_RESTRICTION))
			.displayItem(Material.NETHER_STAR);

	private final double mRadius;
	private final double mHealPercent;
	private final double mDamageBoost;
	private final double mRange;
	private final int mBlightDuration;
	private final double mBlightVulnPerDebuff;
	private final int mMarks;

	private int mCurrDuration = -1;

	private final List<Location> mMarkedLocations = new ArrayList<>(); // To mark locations (Even if block is not replaced)

	private final SanguineHarvestCS mCosmetic;

	public SanguineHarvest(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRadius = CharmManager.getRadius(player, CHARM_RADIUS, RADIUS);
		mHealPercent = CharmManager.calculateFlatAndPercentValue(player, CHARM_HEAL, isLevelOne() ? HEAL_PERCENT_1 : HEAL_PERCENT_2);
		mDamageBoost = CharmManager.getLevelPercentDecimal(player, CHARM_DAMAGE_BOOST) + DAMAGE_BOOST;
		mRange = CharmManager.getRadius(mPlayer, CHARM_RANGE, RANGE);
		mBlightDuration = CharmManager.getDuration(mPlayer, CHARM_BLIGHT_DURATION, ENHANCEMENT_BLIGHT_DURATION);
		mBlightVulnPerDebuff = ENHANCEMENT_DMG_INCREASE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_BLIGHT_VULN_PER_DEBUFF);
		mMarks = MARKS + (int) CharmManager.getLevel(player, CHARM_MARKS);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new SanguineHarvestCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();

		Location loc = mPlayer.getEyeLocation();
		Vector direction = loc.getDirection();

		World world = mPlayer.getWorld();
		mCosmetic.onCast(world, loc);

		if (isEnhanced()) {
			mMarkedLocations.clear();
			mCosmetic.onEnhancementCast(mPlayer, mPlayer.getLocation());
			Vector v;
			for (double degree = -40; degree < 40; degree += 10) {
				for (double r = 0; r <= mRange; r += HITBOX_LENGTH) {
					double radian = Math.toRadians(degree);
					v = new Vector(Math.cos(radian) * r, 0, Math.sin(radian) * r);
					v = VectorUtils.rotateZAxis(v, mPlayer.getLocation().getPitch());
					v = VectorUtils.rotateYAxis(v, mPlayer.getLocation().getYaw() + 90);

					Location location = mPlayer.getEyeLocation().clone().add(v);

					Location marker = location.clone();

					// If enhanced, we want to find where the lowest block is.
					// First, search downwards by 5 blocks until a block is reached.
					// And then set it to just above the block as the saved location.
					while (marker.distance(location) <= 5) {
						Block block = marker.getBlock();
						if (block.isSolid()) {
							// Success, add this location as cursed.
							marker.setY(1.1 + (int) marker.getY());

							// don't add mark locations too close together when they would cover the same space anyways
							if (mMarkedLocations.stream().allMatch(markLoc -> markLoc.distance(marker) > HITBOX_LENGTH)) {
								mMarkedLocations.add(marker);
							}

							break;
						} else {
							marker.add(0, -0.5, 0);
						}
					}

					if (location.getBlock().isSolid()) {
						// Break here because I decided that this ability shouldn't pass through blocks.
						// How mean!
						break;
					}
				}
			}

			if (!mMarkedLocations.isEmpty()) {
				runMarkerRunnable();
			}
		}

		RayTraceResult result = world.rayTrace(loc, direction, mRange, FluidCollisionMode.NEVER, true, 0.425,
			e -> EntityUtils.isHostileMob(e) && !ScoreboardUtils.checkTag(e, AbilityUtils.IGNORE_TAG) && !e.isDead() && e.isValid());

		Location endLoc;
		if (result == null) {
			endLoc = loc.clone().add(direction.multiply(mRange));
		} else {
			endLoc = result.getHitPosition().toLocation(world);
		}
		explode(endLoc);

		mCosmetic.projectileParticle(mPlayer, loc, endLoc);
		return true;
	}

	private void explode(Location loc) {
		World world = mPlayer.getWorld();
		mCosmetic.onExplode(mPlayer, world, loc, mRadius);

		Hitbox hitbox = new Hitbox.SphereHitbox(loc, mRadius);
		for (LivingEntity mob : hitbox.getHitMobs()) {
			MovementUtils.knockAway(loc, mob, (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, 0.2f), 0.2f, true);
			mPlugin.mEffectManager.addEffect(mob, SANGUINE_NAME, new SanguineMark(isLevelTwo(), mHealPercent, mDamageBoost, mMarks, MARK_DURATION, mPlayer, mPlugin, mCosmetic));
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getAbility() == null
			|| event.getAbility().isFake()
			|| event.getAbility() == ClassAbility.CURSED_WOUND
			|| event.getAbility() == ClassAbility.WITHERING_GAZE
			|| event.getAbility() == ClassAbility.PHLEGMATIC_RESOLVE) {
			return false;
		}
		EntityUtils.applyBleed(mPlugin, mPlayer, enemy, BLEED_LEVEL);
		return false; // applies bleed on damage to all mobs hit, causes no recursion
	}

	private void runMarkerRunnable() {
		if (!mMarkedLocations.isEmpty() &&
			isEnhanced()) {
			new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					if (mTicks > mBlightDuration || mMarkedLocations.isEmpty()) {
						mMarkedLocations.clear();
						this.cancel();
						return;
					}

					for (Location location : mMarkedLocations) {
						BoundingBox boundingBox = BoundingBox.of(location, HITBOX_LENGTH, HITBOX_LENGTH, HITBOX_LENGTH);
						mCosmetic.atMarkedLocation(mPlayer, location);

						if (mTicks % 20 == 0) {
							List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(location, 1);
							for (LivingEntity mob : nearbyMobs) {
								if (mob.getBoundingBox().overlaps(boundingBox)) {
									mPlugin.mEffectManager.addEffect(mob, BLIGHT_EFFECT_NAME, new SanguineHarvestBlight(20, mBlightVulnPerDebuff, mPlugin));
								}
							}
						}
					}

					mTicks += 10;
					mCurrDuration += 10;
				}

				@Override
				public synchronized void cancel() {
					super.cancel();
					mCurrDuration = -1;
					ClientModHandler.updateAbility(mPlayer, SanguineHarvest.this);
				}
			}.runTaskTimer(mPlugin, 0, 10);
			mCurrDuration = 0;
			ClientModHandler.updateAbility(mPlayer, this);
		}
	}

	@Override
	public int getInitialAbilityDuration() {
		return isEnhanced() ? mBlightDuration : 0;
	}

	@Override
	public int getRemainingAbilityDuration() {
		return this.mCurrDuration >= 0 && isEnhanced() ? getInitialAbilityDuration() - this.mCurrDuration : 0;
	}

	private static Description<SanguineHarvest> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Passively, damage from your active abilities")
			.addLine("inflicts mobs with *1* stack of *Bleed*.").styles(WHITE, Bleed.BLEED_COLOR)
			.addLine("(Excludes Cursed Wound and Withering Gaze)")
			.addLine()
			.addLine("Activate to fire a burst of magic that")
			.addLine("explodes on impact, knocking back mobs")
			.addLine("and *Marking* them for %t.").styles(MARK_COLOR)
				.statValues(stat(MARK_DURATION))
			.addLine()
			.addLine("*Marked* mobs heal the player who kills them.").styles(MARK_COLOR)
			.addLine()
			.addStat("Radius: %r")
				.statValues(stat(a -> a.mRadius, RADIUS))
			.addStat("Healing: %p1 HP per *Mark*").styles(MARK_COLOR)
				.statValues(stat(a -> a.mHealPercent, HEAL_PERCENT_1))
			.addStat("Cooldown: %t")
				.statValues(cooldown(COOLDOWN))
			.addDashedLine();
	}

	private static Description<SanguineHarvest> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Sanguine Harvest*'s healing.").styles(UNDERLINED)
			.addLine()
			.addLine("Attacks against *Marked* mobs deal").styles(MARK_COLOR)
			.addLine("increased damage, heal the player,")
			.addLine("and remove the *Mark*.").styles(MARK_COLOR)
			.addLine()
			.addStatComparison("Healing: %p1 -> %p2 HP per *Mark*").styles(MARK_COLOR)
				.statValues(stat(HEAL_PERCENT_1), stat(a -> a.mHealPercent, HEAL_PERCENT_2))
			.addStat("Damage Boost: +%p (m)")
				.statValues(stat(a -> a.mDamageBoost, DAMAGE_BOOST))
			.addDashedLine();
	}

	private static Description<SanguineHarvest> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("*Sanguine Harvest* creates a blighted area").styles(UNDERLINED)
			.addLine("in front of you that lasts for %t.")
				.statValues(stat(ENHANCEMENT_BLIGHT_DURATION))
			.addLine()
			.addLine("Mobs in the area take increased damage")
			.addLine("for each debuff they have.")
			.addLine()
			.addStat("Damage Boost: +%p per debuff")
				.statValues(stat(a -> a.mBlightVulnPerDebuff, ENHANCEMENT_DMG_INCREASE))
			.addStat("Radius: %r (Cone-Shaped)")
				.statValues(stat(a -> a.mRange, RANGE))
			.addDashedLine();
	}
}

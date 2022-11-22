package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.SanguineHarvestBlight;
import com.playmonumenta.plugins.effects.SanguineMark;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SanguineHarvest extends Ability {

	private static final int RANGE = 8;
	private static final int RADIUS_1 = 3;
	private static final int RADIUS_2 = 4;
	private static final double BLEED_LEVEL_1 = 0.1;
	private static final double BLEED_LEVEL_2 = 0.2;
	private static final double HEAL_PERCENT_1 = 0.05;
	private static final double HEAL_PERCENT_2 = 0.1;
	private static final int BLEED_DURATION = 10 * 20;
	private static final int COOLDOWN = 20 * 20;
	private static final double HITBOX_LENGTH = 0.55;

	private static final double ENHANCEMENT_DMG_INCREASE = 0.03;
	private static final int ENHANCEMENT_BLIGHT_DURATION = 20 * 6;
	private static final String BLIGHT_EFFECT_NAME = "SanguineHarvestBlightEffect";

	private static final String SANGUINE_NAME = "SanguineEffect";

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(179, 0, 0), 1.0f);

	public static final String CHARM_RADIUS = "Sanguine Harvest Radius";
	public static final String CHARM_RANGE = "Sanguine Harvest Range";
	public static final String CHARM_COOLDOWN = "Sanguine Harvest Cooldown";
	public static final String CHARM_HEAL = "Sanguine Harvest Healing";
	public static final String CHARM_KNOCKBACK = "Sanguine Harvest Knockback";
	public static final String CHARM_BLEED = "Sanguine Harvest Bleed Amplifier";

	public static final AbilityInfo<SanguineHarvest> INFO =
		new AbilityInfo<>(SanguineHarvest.class, "Sanguine Harvest", SanguineHarvest::new)
			.linkedSpell(ClassAbility.SANGUINE_HARVEST)
			.scoreboardId("SanguineHarvest")
			.shorthandName("SH")
			.descriptions(
				("Enemies you damage with an ability are afflicted with Bleed I for %s seconds. " +
					 "Bleed gives mobs 10%% Slowness and 10%% Weaken per level if the mob is below 50%% Max Health. " +
					 "Additionally, right click while holding a scythe and not sneaking to fire a burst of darkness. " +
					 "This projectile travels up to %s blocks and upon contact with a surface or an enemy, it explodes, " +
					 "knocking back and marking all mobs within %s blocks of the explosion for a harvest. " +
					 "Any player that kills a marked mob is healed for %s%% of max health. Cooldown: %ss.")
					.formatted(StringUtils.ticksToSeconds(BLEED_DURATION), RANGE, RADIUS_1, StringUtils.multiplierToPercentage(HEAL_PERCENT_1), StringUtils.ticksToSeconds(COOLDOWN)),
				"Increase passive Bleed level to II, and increase the radius to %s blocks. Players killing a marked mob are healed by %s%%."
					.formatted(RADIUS_2, StringUtils.multiplierToPercentage(HEAL_PERCENT_2)),
				("Sanguine now seeps into the ground where it lands, causing blocks in the cone to become Blighted. " +
					 "Mobs standing on these Blighted blocks take %s%% extra damage per debuff. The Blight disappears after %ss and is not counted as a debuff.")
					.formatted(StringUtils.multiplierToPercentage(ENHANCEMENT_DMG_INCREASE), StringUtils.ticksToSeconds(ENHANCEMENT_BLIGHT_DURATION)))
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", SanguineHarvest::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(false),
				AbilityTriggerInfo.HOLDING_SCYTHE_RESTRICTION))
			.displayItem(new ItemStack(Material.NETHER_STAR, 1));

	private final double mRadius;
	private final double mBleedLevel;
	private final double mHealPercent;

	private ArrayList<Location> mMarkedLocations = new ArrayList<>(); // To mark locations (Even if block is not replaced)

	public SanguineHarvest(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRadius = CharmManager.getRadius(player, CHARM_RADIUS, isLevelOne() ? RADIUS_1 : RADIUS_2);
		mHealPercent = CharmManager.calculateFlatAndPercentValue(player, CHARM_HEAL, isLevelOne() ? HEAL_PERCENT_1 : HEAL_PERCENT_2);
		mBleedLevel = CharmManager.getLevelPercentDecimal(player, CHARM_BLEED) + (isLevelOne() ? BLEED_LEVEL_1 : BLEED_LEVEL_2);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		putOnCooldown();

		Location loc = mPlayer.getEyeLocation();
		Vector direction = loc.getDirection();
		Vector shift = direction.normalize().multiply(HITBOX_LENGTH);
		BoundingBox box = BoundingBox.of(loc, HITBOX_LENGTH, HITBOX_LENGTH, HITBOX_LENGTH);
		box.shift(direction);

		World world = mPlayer.getWorld();
		world.playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1, 0.9f);

		double range = CharmManager.getRadius(mPlayer, CHARM_RANGE, RANGE);
		Set<LivingEntity> nearbyMobs = new HashSet<>(EntityUtils.getNearbyMobs(loc, range));

		if (isEnhanced()) {
			mMarkedLocations.clear();
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1, 1);
			Vector v;
			for (double degree = -40; degree < 40; degree += 10) {
				for (double r = 0; r <= range; r += 0.55) {
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
							mMarkedLocations.add(marker);
							break;
						} else {
							marker.add(0, -1, 0);
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

		for (double r = 0; r < range; r += HITBOX_LENGTH) {
			Location bLoc = box.getCenter().toLocation(world);

			new PartialParticle(Particle.SMOKE_NORMAL, bLoc, 10, 0.15, 0.15, 0.15, 0.075).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.REDSTONE, bLoc, 16, 0.2, 0.2, 0.2, 0.1, COLOR).spawnAsPlayerActive(mPlayer);

			if (!bLoc.isChunkLoaded() || bLoc.getBlock().getType().isSolid()) {
				bLoc.subtract(direction.multiply(0.5));
				explode(bLoc);
				return;
			}

			Iterator<LivingEntity> iter = nearbyMobs.iterator();
			while (iter.hasNext()) {
				LivingEntity mob = iter.next();
				if (mob.getBoundingBox().overlaps(box)) {
					if (EntityUtils.isHostileMob(mob)) {
						explode(bLoc);
						return;
					}
				}
			}
			box.shift(shift);
		}
	}

	private void explode(Location loc) {
		World world = mPlayer.getWorld();
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 25, 0, 0, 0, 0.125).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, loc, 10, 0, 0, 0, 0.1, COLOR).spawnAsPlayerActive(mPlayer);

		new PartialParticle(Particle.REDSTONE, loc, 75, mRadius, mRadius, mRadius, 0.25, COLOR).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.FALLING_DUST, loc, 75, mRadius, mRadius, mRadius, Material.RED_CONCRETE.createBlockData()).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 55, mRadius, mRadius, mRadius, 0.25).spawnAsPlayerActive(mPlayer);

		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1, 0.3f);
		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1, 0.5f);

		Hitbox hitbox = new Hitbox.SphereHitbox(loc, mRadius);
		for (LivingEntity mob : hitbox.getHitMobs()) {
			MovementUtils.knockAway(loc, mob, (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, 0.2f), 0.2f, true);
			mPlugin.mEffectManager.addEffect(mob, SANGUINE_NAME, new SanguineMark(mHealPercent, 20 * 30, mPlayer, mPlugin));
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getAbility() == null) {
			return false;
		}
		EntityUtils.applyBleed(mPlugin, BLEED_DURATION, mBleedLevel, enemy);
		return false; // applies bleed on damage to all mobs hit, causes no recursion
	}

	private void runMarkerRunnable() {
		if (!mMarkedLocations.isEmpty() &&
			isEnhanced()) {
			new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					if (mTicks > ENHANCEMENT_BLIGHT_DURATION || mMarkedLocations.isEmpty()) {
						mMarkedLocations.clear();
						this.cancel();
						return;
					}

					for (Location location : mMarkedLocations) {
						BoundingBox boundingBox = BoundingBox.of(location, HITBOX_LENGTH, HITBOX_LENGTH, HITBOX_LENGTH);
						new PartialParticle(Particle.REDSTONE, location, 3, 0.25, 0, 0.25, 0.1, COLOR).spawnAsPlayerActive(mPlayer);

						if (mTicks % 20 == 0) {
							List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(location, 1);
							for (LivingEntity mob : nearbyMobs) {
								if (mob.getBoundingBox().overlaps(boundingBox) && !EntityUtils.isBlighted(mPlugin, mob)) {
									mPlugin.mEffectManager.addEffect(mob, BLIGHT_EFFECT_NAME, new SanguineHarvestBlight(20, ENHANCEMENT_DMG_INCREASE, mPlugin));
								}
							}
						}
					}

					mTicks += 10;
				}
			}.runTaskTimer(mPlugin, 0, 10);
		}
	}
}

package com.playmonumenta.plugins.depths.abilities.prismatic;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class SolarRay extends DepthsAbility implements AbilityWithChargesOrStacks {

	public static final String ABILITY_NAME = "Solar Ray";
	public static final double[] BASE_DAMAGE = {12, 14, 16, 18, 20, 24};
	public static final int COOLDOWN = 9 * 20;
	public static final double DAMAGE_INCREASE = 0.25;
	public static final double TRAVEL_SPEED = 2;
	public static final int MAX_LIFETIME = 4 * 20;
	public static final float BEAM_RADIUS = 0.75f;
	public static final int SAME_MOB_MAX_HITS = 2;

	public static final DepthsAbilityInfo<SolarRay> INFO =
		new DepthsAbilityInfo<>(SolarRay.class, ABILITY_NAME, SolarRay::new, DepthsTree.PRISMATIC, DepthsTrigger.RIGHT_CLICK)
			.linkedSpell(ClassAbility.SOLAR_RAY)
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", SolarRay::cast, DepthsTrigger.RIGHT_CLICK))
			.displayItem(Material.END_ROD)
			.descriptions(SolarRay::getDescription);

	private double mDamage;

	public SolarRay(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = getDamage();
		// Update stacks because the abilities refresh mid run too
		ClientModHandler.updateAbility(mPlayer, this);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		putOnCooldown();
		ItemStatManager.PlayerItemStats playerItemStats = Plugin.getInstance().mItemStatManager.getPlayerItemStatsCopy(mPlayer);
		mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 2, 1.75f);
		cancelOnDeath(new BukkitRunnable() {
			final Location mCurrentLoc = mPlayer.getEyeLocation();
			final HashMap<UUID, Integer> mHitMobCounts = new HashMap<>();
			final ArrayList<UUID> mHitMobs = new ArrayList<>();
			final double mThetaStep = 0.2 * TRAVEL_SPEED;

			int mTicks = 0;
			double mTheta = 0;

			@Override
			public void run() {
				RayTraceResult rayTraceResult = mCurrentLoc.getWorld().rayTraceBlocks(mCurrentLoc, mCurrentLoc.getDirection(), TRAVEL_SPEED, FluidCollisionMode.NEVER, true);

				Location oldLoc = mCurrentLoc.clone();
				if (rayTraceResult == null) {
					// Nothing hit. Advance and try to hit mobs in the way.
					mCurrentLoc.add(mCurrentLoc.getDirection().multiply(TRAVEL_SPEED));
					tryHitMobs(oldLoc, mCurrentLoc);
				} else {
					Block block = rayTraceResult.getHitBlock();
					BlockFace blockFace = rayTraceResult.getHitBlockFace();
					if (block != null && blockFace != null) {
						Vector bounceVector = new Vector(blockFace.getModX() != 0 ? -1 : 1, blockFace.getModY() != 0 ? -1 : 1, blockFace.getModZ() != 0 ? -1 : 1);
						mCurrentLoc.setDirection(mCurrentLoc.getDirection().multiply(bounceVector));
						mCurrentLoc.set(block.getX(), block.getY(), block.getZ()).add(blockFace.getDirection());
						handleBlockCollision();
					}
					Vector hitPos = rayTraceResult.getHitPosition();
					mCurrentLoc.set(hitPos.getX(), hitPos.getY(), hitPos.getZ());
					tryHitMobs(oldLoc, mCurrentLoc);
				}

				travelAesthetics(oldLoc, mCurrentLoc);

				mTicks++;
				mTheta += mThetaStep;
				if (mTicks >= MAX_LIFETIME) {
					expireAnimation();
					cancel();
				}
			}

			public void tryHitMobs(Location oldLoc, Location newLoc) {
				handleMobCollision(Hitbox.approximateCylinder(oldLoc, newLoc, BEAM_RADIUS, false).accuracy(0.5).getHitMobs());
			}

			public void handleBlockCollision() {
				mHitMobs.clear();
				// Bounce sound!
				mCurrentLoc.getWorld().playSound(mCurrentLoc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1, 2);
			}

			public void handleMobCollision(List<LivingEntity> hitMobs) {
				hitMobs.forEach(mob -> {
					if (mHitMobs.contains(mob.getUniqueId())) {
						return;
					}

					if (mHitMobCounts.containsKey(mob.getUniqueId()) && mHitMobCounts.get(mob.getUniqueId()) >= SAME_MOB_MAX_HITS) {
						return;
					}

					mHitMobs.add(mob.getUniqueId());
					DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC, mInfo.getLinkedSpell(), playerItemStats), mDamage, true, false, false);
					if (mob.isDead()) {
						tryIncreaseDamage(mob.getName());
					}
					new PartialParticle(Particle.LAVA, LocationUtils.getHalfHeightLocation(mob), 5).spawnAsPlayerActive(mPlayer);
					mPlayer.getWorld().playSound(mob.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 1, 2);

					Integer currentHits = mHitMobCounts.get(mob.getUniqueId());
					mHitMobCounts.put(mob.getUniqueId(), currentHits == null ? 1 : currentHits + 1);
				});
			}

			public void travelAesthetics(Location oldLoc, Location newLoc) {
				new PPLine(Particle.END_ROD, oldLoc, newLoc).countPerMeter(2).spawnAsPlayerActive(mPlayer);

				Vector vec = new Vector(FastUtils.cos(mTheta) * 0.325, 0, FastUtils.sin(mTheta) * 0.325);
				vec = VectorUtils.rotateXAxis(vec, mCurrentLoc.getPitch() - 90);
				vec = VectorUtils.rotateYAxis(vec, mCurrentLoc.getYaw());

				new PartialParticle(Particle.SPELL_INSTANT, mCurrentLoc.clone().add(vec.clone().multiply(2)), 1).extra(0.01).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.SPELL_INSTANT, mCurrentLoc.clone().subtract(vec.clone().multiply(2)), 1).extra(0.015).spawnAsPlayerActive(mPlayer);
			}

			public void expireAnimation() {
				Vector axis1 = new Vector(FastUtils.cos(mTheta) * 0.325, 0, FastUtils.sin(mTheta) * 0.325);
				axis1 = VectorUtils.rotateXAxis(axis1, mCurrentLoc.getPitch() - 90);
				axis1 = VectorUtils.rotateYAxis(axis1, mCurrentLoc.getYaw());
				Vector axis2 = axis1.clone().crossProduct(mCurrentLoc.getDirection());
				// Nova explosion thing
				new PPCircle(Particle.FIREWORKS_SPARK, mCurrentLoc, BEAM_RADIUS * 3).axes(axis1, axis2).ringMode(true)
					.countPerMeter(2).extra(0.7).delta(mCurrentLoc.getDirection().getX(), mCurrentLoc.getDirection().getY(), mCurrentLoc.getDirection().getZ())
					.directionalMode(true).rotateDelta(true).spawnAsPlayerActive(mPlayer);
				new PPCircle(Particle.FIREWORKS_SPARK, mCurrentLoc, BEAM_RADIUS * 3).axes(axis1, axis2).ringMode(true)
					.countPerMeter(1).extra(0.1).delta(mCurrentLoc.getDirection().getX(), mCurrentLoc.getDirection().getY(), mCurrentLoc.getDirection().getZ())
					.spawnAsPlayerActive(mPlayer);
				new PPCircle(Particle.FIREWORKS_SPARK, mCurrentLoc, BEAM_RADIUS * 3).axes(axis1.multiply(-1), axis2).ringMode(true)
					.countPerMeter(2).extra(0.45).delta(mCurrentLoc.getDirection().getX(), mCurrentLoc.getDirection().getY(), mCurrentLoc.getDirection().getZ())
					.directionalMode(true).rotateDelta(true).spawnAsPlayerActive(mPlayer);
				mPlayer.getWorld().playSound(mCurrentLoc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 2, 2);
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1));

		return true;
	}

	private double getDamage() {
		DepthsPlayer dPlayer = DepthsManager.getInstance().getDepthsPlayer(mPlayer.getUniqueId());
		if (dPlayer == null) {
			return 0;
		}

		List<String> uniqueMobsKilled = dPlayer.getSolarRayUniqueMobNames();
		if (uniqueMobsKilled == null) {
			return BASE_DAMAGE[mRarity - 1];
		}
		return BASE_DAMAGE[mRarity - 1] + dPlayer.getSolarRayUniqueMobNames().size() * DAMAGE_INCREASE;
	}

	private void tryIncreaseDamage(String mobName) {
		DepthsPlayer dPlayer = DepthsManager.getInstance().getDepthsPlayer(mPlayer.getUniqueId());
		if (dPlayer == null) {
			return;
		}

		List<String> uniqueMobsKilled = dPlayer.getSolarRayUniqueMobNames();
		if (uniqueMobsKilled != null && !uniqueMobsKilled.contains(mobName)) {
			uniqueMobsKilled.add(mobName);
			mPlayer.getWorld().playSound(mPlayer, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 1, 1);
			mDamage = getDamage();
			ClientModHandler.updateAbility(mPlayer, this);
			sendActionBarMessage("Solar Ray upgraded! New stacks: " + uniqueMobsKilled.size());
		}
	}

	@Override
	public int getCharges() {
		DepthsPlayer dPlayer = DepthsManager.getInstance().getDepthsPlayer(mPlayer.getUniqueId());
		if (dPlayer == null) {
			return 0;
		}

		List<String> uniqueMobsKilled = dPlayer.getSolarRayUniqueMobNames();
		return uniqueMobsKilled.size();
	}

	@Override
	public int getMaxCharges() {
		return 9999;
	}

	@Override
	public ChargeType getChargeType() {
		return ChargeType.STACKS;
	}

	private static Description<SolarRay> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<SolarRay>(color)
			.add("Right click to summon a beam of radiant energy which travels in a straight line for ")
			.addDuration(MAX_LIFETIME)
			.add(" seconds, and bounces off of surfaces. The beam deals ")
			.addDepthsDamage(a -> BASE_DAMAGE[rarity - 1], BASE_DAMAGE[rarity - 1], true)
			.add(" magic damage when it comes into contact with a mob. The same mob can be hit " +
				"one time per bounce, and up to " + SAME_MOB_MAX_HITS + " times in total. " +
				"Killing unique mobs with this ability permanently increases its damage by " + DAMAGE_INCREASE + ".")
			.addCooldown(COOLDOWN)
			.addConditional(a -> a != null ? Component.text("\nCurrent stacks: " + a.getCharges()) : Component.empty());
	}
}

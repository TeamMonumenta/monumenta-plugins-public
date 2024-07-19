package com.playmonumenta.plugins.depths.abilities.frostborn;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.kyori.adventure.text.format.TextColor;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;


public class FrigidCombos extends DepthsAbility {

	public static final String ABILITY_NAME = "Frigid Combos";
	public static final int COOLDOWN = 6 * 20;
	public static final int DURATION = 2 * 20;
	public static final double[] SLOW_AMPLIFIER = {0.2, 0.25, 0.3, 0.35, 0.4, 0.5};
	public static final int[] DAMAGE = {2, 3, 4, 5, 6, 8};
	public static final int[] SHATTER_DAMAGE = {8, 10, 12, 14, 16, 20};
	public static final int RADIUS = 4;
	public static final int SHATTER_RADIUS = 6;
	public static final Color TIP_COLOR = Color.fromRGB(184, 216, 242);
	public static final Color BASE_COLOR = Color.fromRGB(95, 159, 212);

	public static final String CHARM_COOLDOWN = "Frigid Combos Cooldown";

	public static final DepthsAbilityInfo<FrigidCombos> INFO =
		new DepthsAbilityInfo<>(FrigidCombos.class, ABILITY_NAME, FrigidCombos::new, DepthsTree.FROSTBORN, DepthsTrigger.COMBO)
			.linkedSpell(ClassAbility.FRIGID_COMBOS)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.BLUE_DYE)
			.descriptions(FrigidCombos::getDescription)
			.singleCharm(false);

	private final double mRadius;
	private final double mDamage;
	private final double mShatterRadius;
	private final double mShatterDamage;
	private final double mSlow;
	private final int mDuration;

	public FrigidCombos(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.FRIGID_COMBOS_RADIUS.mEffectName, RADIUS);
		mShatterRadius = CharmManager.getRadius(mPlayer, CharmEffects.FRIGID_COMBOS_RADIUS.mEffectName, SHATTER_RADIUS);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.FRIGID_COMBOS_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mShatterDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.FRIGID_COMBOS_DAMAGE.mEffectName, SHATTER_DAMAGE[mRarity - 1]);
		mSlow = SLOW_AMPLIFIER[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.FRIGID_COMBOS_SLOW_AMPLIFIER.mEffectName);
		mDuration = CharmManager.getDuration(mPlayer, CharmEffects.FRIGID_COMBOS_SLOW_DURATION.mEffectName, DURATION);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (DepthsUtils.isValidComboAttack(event, mPlayer) && !isOnCooldown()) {
			putOnCooldown();

			activate(enemy, mPlayer, mPlugin, mRadius, mShatterRadius, mDamage, mShatterDamage, mSlow, mDuration, mInfo.getLinkedSpell());
		}
		return false;
	}

	public static void activate(LivingEntity enemy, Player player) {
		// for Deep God's Endowment
		activate(enemy, player, Plugin.getInstance(), RADIUS, SHATTER_RADIUS, DAMAGE[0], SHATTER_DAMAGE[0], SLOW_AMPLIFIER[0], DURATION, null);
	}

	public static void activate(LivingEntity enemy, Player player, Plugin plugin, double normalRadius, double shatterRadius, double normalDamage, double shatterDamage, double slow, int duration, @Nullable ClassAbility classAbility) {
		Location targetLoc = enemy.getLocation();
		World world = targetLoc.getWorld();

		boolean isOnIce = DepthsUtils.isOnIce(enemy);
		double damage = isOnIce ? shatterDamage : normalDamage;
		double radius = isOnIce ? shatterRadius : normalRadius;

		Location playerLoc = player.getLocation().add(0, 1, 0);
		if (isOnIce) {
			HashSet<Location> iceToBreak = new HashSet<>(DepthsUtils.iceActive.keySet());
			iceToBreak.removeIf(l -> !l.isWorldLoaded() || l.getWorld() != targetLoc.getWorld() || l.clone().add(0.5, 0.5, 0.5).distance(targetLoc) > 1.5 || !DepthsUtils.isIce(l.getBlock().getType()));
			for (Location l : iceToBreak) {
				Block b = l.getBlock();
				if (b.getType() == Permafrost.PERMAFROST_ICE_MATERIAL) {
					//If special permafrost ice, set to normal ice instead of destroying
					b.setType(DepthsUtils.ICE_MATERIAL);
				} else {
					b.setBlockData(DepthsUtils.iceActive.get(l));
					DepthsUtils.iceActive.remove(l);
				}
				Location aboveLoc = l.clone().add(0.5, 1, 0.5);
				new PartialParticle(Particle.CLOUD, aboveLoc.clone().add(0, 0.25, 0), 8, 0.3, 0.3, 0.3, 0).spawnAsPlayerActive(player);
				new PartialParticle(Particle.BLOCK_CRACK, aboveLoc.clone().add(0, 0.25, 0), 8, 0.3, 0.3, 0.3, 0, Material.ICE.createBlockData()).spawnAsPlayerActive(player);
			}

			new PartialParticle(Particle.EXPLOSION_LARGE, LocationUtils.getHalfHeightLocation(enemy), 1, 0, 0, 0, 0.2).spawnAsPlayerActive(player);

			world.playSound(playerLoc, Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.PLAYERS, 1f, 1f);
			world.playSound(playerLoc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1.2f, 1.5f);
			world.playSound(playerLoc, Sound.ENTITY_SHULKER_BULLET_HURT, SoundCategory.PLAYERS, 1.2f, 0.5f);
			world.playSound(playerLoc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 2f, 1.6f);
			world.playSound(playerLoc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.2f, 1.0f);
			world.playSound(playerLoc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.2f, 0.5f);
			world.playSound(playerLoc, Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 1.2f, 0.5f);
		}

		for (LivingEntity mob : EntityUtils.getNearbyMobs(targetLoc, radius)) {
			new PartialParticle(Particle.CRIT_MAGIC, LocationUtils.getHalfHeightLocation(mob), 25, .5, .2, .5, 0.65).spawnAsPlayerActive(player);
			EntityUtils.applySlow(plugin, duration, slow, mob);
			DamageUtils.damage(player, mob, DamageType.MAGIC, damage, classAbility, true);
		}

		playSounds(world, playerLoc);
		for (int i = 0; i < 6; i++) {
			Vector dir = VectorUtils.randomUnitVector().multiply(FastUtils.randomDoubleInRange(1, 2));
			drawLineSlash(LocationUtils.getHalfHeightLocation(enemy).clone().add(dir.multiply(radius / 4)), dir, 0, radius / 2, 0.1, 3, (Location lineLoc, double middleProgress, double endProgress, boolean middle) ->
				new PartialParticle(Particle.REDSTONE, lineLoc, 3, 0, 0, 0, 0, new Particle.DustOptions(
					ParticleUtils.getTransition(BASE_COLOR, TIP_COLOR, endProgress), 1.5f - (float) (endProgress * 1.3)))
					.spawnAsPlayerActive(player));
		}

		new BukkitRunnable() {
			double mRadius = 0;
			final Location mLoc = enemy.getLocation().add(0, 0.15, 0);

			@Override
			public void run() {
				mRadius += 1.2;
				new PPCircle(Particle.CRIT_MAGIC, mLoc, mRadius).count(25).extra(0.45).spawnAsPlayerActive(player);

				if (mRadius >= radius) {
					this.cancel();
				}
			}

		}.runTaskTimer(plugin, 0, 1);
	}

	public static void playSounds(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 1.2f, 1.0f);
		world.playSound(loc, Sound.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 1.2f, 0.6f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1.2f, 1.4f);
		world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 2f, 1.6f);
	}

	public static void drawLineSlash(Location loc, Vector dir, double angle, double length, double spacing, int duration, ParticleUtils.LineSlashAnimation animation) {
		Location l = loc.clone();
		l.setDirection(dir);

		List<Vector> points = new ArrayList<>();
		Vector vec = new Vector(0, 0, 1);
		vec = VectorUtils.rotateZAxis(vec, angle);
		vec = VectorUtils.rotateXAxis(vec, l.getPitch());
		vec = VectorUtils.rotateYAxis(vec, l.getYaw());
		vec = vec.normalize();

		for (double ln = -length; ln < length; ln += spacing) {
			Vector point = l.toVector().add(vec.clone().multiply(ln));
			points.add(point);
		}

		if (duration <= 0) {
			boolean midReached = false;
			for (int i = 0; i < points.size(); i++) {
				Vector point = points.get(i);
				boolean middle = !midReached && i == points.size() / 2;
				if (middle) {
					midReached = true;
				}
				animation.lineSlashAnimation(point.toLocation(loc.getWorld()),
					1D - (point.distance(l.toVector()) / length), (double) (i + 1) / points.size(), middle);
			}
		} else {
			new BukkitRunnable() {
				final int mPointsPerTick = (int) (points.size() * (1D / duration));
				int mT = 0;
				boolean mMidReached = false;

				@Override
				public void run() {


					for (int i = mPointsPerTick * mT; i < FastMath.min(points.size(), mPointsPerTick * (mT + 1)); i++) {
						Vector point = points.get(i);
						boolean middle = !mMidReached && i == points.size() / 2;
						if (middle) {
							mMidReached = true;
						}
						animation.lineSlashAnimation(point.toLocation(loc.getWorld()),
							1D - (point.distance(l.toVector()) / length), (double) (i + 1) / points.size(), middle);
					}
					mT++;

					if (mT >= duration) {
						this.cancel();
					}
				}

			}.runTaskTimer(Plugin.getInstance(), 0, 1);
		}
	}

	private static Description<FrigidCombos> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<FrigidCombos>(color)
			.add("When you melee attack an enemy, deal ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" magic damage to mobs within ")
			.add(a -> a.mRadius, RADIUS)
			.add(" blocks and apply ")
			.addPercent(a -> a.mSlow, SLOW_AMPLIFIER[rarity - 1], false, true)
			.add(" slowness to them for ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" seconds. If the mob was standing on ice, shatter it and increase the damage to ")
			.addDepthsDamage(a -> a.mShatterDamage, SHATTER_DAMAGE[rarity - 1], true)
			.add(" and the radius to ")
			.add(a -> a.mShatterRadius, SHATTER_RADIUS)
			.add(" blocks instead.")
			.addCooldown(COOLDOWN);
	}
}


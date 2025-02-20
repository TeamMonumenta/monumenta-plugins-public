package com.playmonumenta.plugins.depths.abilities.frostborn;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
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
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
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
import org.bukkit.util.Vector;

public class IceLance extends DepthsAbility {

	public static final String ABILITY_NAME = "Ice Lance";
	public static final double[] DAMAGE = {12.5, 15.0, 17.5, 20.0, 22.5, 27.5};
	private static final Color ICE_COLOR = Color.fromRGB(194, 224, 249);
	private static final Color DARK_COLOR = Color.fromRGB(135, 191, 237);
	private static final Particle.DustOptions ICE_LANCE_COLOR = new Particle.DustOptions(Color.fromRGB(194, 224, 249), 1.0f);
	private static final int COOLDOWN = 6 * 20;
	private static final int DURATION = 2 * 20;
	private static final double AMPLIFIER = 0.2;
	private static final int RANGE = 8;
	public static final int ICE_TICKS = 6 * 20;

	public static final String CHARM_COOLDOWN = "Ice Lance Cooldown";

	public static final DepthsAbilityInfo<IceLance> INFO =
		new DepthsAbilityInfo<>(IceLance.class, ABILITY_NAME, IceLance::new, DepthsTree.FROSTBORN, DepthsTrigger.RIGHT_CLICK)
			.linkedSpell(ClassAbility.ICE_LANCE)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.actionBarColor(TextColor.color(194, 224, 249))
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", IceLance::cast, DepthsTrigger.RIGHT_CLICK))
			.displayItem(Material.SNOWBALL)
			.descriptions(IceLance::getDescription);

	private final double mRange;
	private final double mDamage;
	private final double mAmplifier;
	private final int mDuration;
	private final int mIceDuration;

	public IceLance(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRange = CharmManager.getRadius(mPlayer, CharmEffects.ICE_LANCE_RANGE.mEffectName, RANGE);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.ICE_LANCE_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mAmplifier = AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.ICE_LANCE_DEBUFF_AMPLIFIER.mEffectName);
		mDuration = CharmManager.getDuration(mPlayer, CharmEffects.ICE_LANCE_DURATION.mEffectName, DURATION);
		mIceDuration = CharmManager.getDuration(mPlayer, CharmEffects.ICE_LANCE_ICE_DURATION.mEffectName, ICE_TICKS);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();

		Location startLoc = mPlayer.getEyeLocation();
		Vector direction = startLoc.getDirection().multiply(0.5);
		World world = startLoc.getWorld();

		Location endLoc = startLoc.clone();
		Location checkLoc = startLoc.clone();
		for (int i = 0; i < 40; i++) {
			if (startLoc.distance(checkLoc) > mRange) {
				endLoc = checkLoc.clone();
				break;
			}
			if ((checkLoc.getBlock().isSolid() && !DepthsUtils.isIce(checkLoc.getBlock().getType()))) {
				endLoc = checkLoc.clone();

				// if we hit a solid (non ice block, also play particles too
				new PartialParticle(Particle.CLOUD, endLoc, 30, 0, 0, 0, 0.125).spawnAsPlayerActive(mPlayer);
				world.playSound(endLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1, 0.85f);
				world.playSound(endLoc, Sound.ITEM_TRIDENT_HIT_GROUND, SoundCategory.PLAYERS, 1, 0.75f);

				break;
			}

			checkLoc.add(direction);
		}

		for (LivingEntity mob : Hitbox.approximateCylinder(startLoc, endLoc, 0.7, true).accuracy(0.5).getHitMobs()) {
			DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MAGIC, mDamage, mInfo.getLinkedSpell(), true);

			EntityUtils.applySlow(mPlugin, mDuration, mAmplifier, mob);
			EntityUtils.applyWeaken(mPlugin, mDuration, mAmplifier, mob);
			MovementUtils.knockAway(mPlayer.getLocation(), mob, 0.25f, 0.25f, true);

			// place ice under mob
			Block deathSpot = mob.getLocation().add(0, -1, 0).getBlock();
			List<Block> iceLocations = ImmutableList.of(
				deathSpot.getRelative(-1, 0, -1),
				deathSpot.getRelative(-1, 0, 0),
				deathSpot.getRelative(-1, 0, 1),
				deathSpot.getRelative(0, 0, -1),
				deathSpot.getRelative(0, 0, 0),
				deathSpot.getRelative(0, 0, 1),
				deathSpot.getRelative(1, 0, -1),
				deathSpot.getRelative(1, 0, 0),
				deathSpot.getRelative(1, 0, 1),
				deathSpot.getRelative(-2, 0, 0),
				deathSpot.getRelative(2, 0, 0),
				deathSpot.getRelative(0, 0, -2),
				deathSpot.getRelative(0, 0, 2)
			);
			for (Block b : iceLocations) {
				DepthsUtils.iceExposedBlock(b, mIceDuration, mPlayer);
			}
		}

		new PartialParticle(Particle.EXPLOSION_NORMAL, startLoc, 10, 0, 0, 0, 0.125).spawnAsPlayerActive(mPlayer);

		new PPLine(Particle.EXPLOSION_NORMAL, startLoc, endLoc).shiftStart(0.75).countPerMeter(2).minParticlesPerMeter(0).delta(0.05).extra(0.025).spawnAsPlayerActive(mPlayer);
		new PPLine(Particle.REDSTONE, startLoc, endLoc).shiftStart(0.75).countPerMeter(15).delta(0.2).data(ICE_LANCE_COLOR).spawnAsPlayerActive(mPlayer);
		Location l = startLoc.clone();
		Vector dir = startLoc.getDirection().multiply(1.0 / 3);
		double rotation = 0;
		double radius = 0.75;
		double distance = startLoc.distance(endLoc);
		for (int i = 0; i < distance * 3; i++) {
			l.add(dir);
			rotation += 6;
			radius -= 0.75D / (distance * 3);
			for (int j = 0; j < 3; j++) {
				double radian = FastMath.toRadians(rotation + (j * 120));
				Vector vec = new Vector(FastUtils.cos(radian) * radius, 0,
					FastUtils.sin(radian) * radius);
				vec = VectorUtils.rotateXAxis(vec, l.getPitch() + 90);
				vec = VectorUtils.rotateYAxis(vec, l.getYaw());
				Location helixLoc = l.clone().add(vec);
				new PartialParticle(Particle.DUST_COLOR_TRANSITION, helixLoc, 3, 0.05, 0.05, 0.05, 0.25,
					new Particle.DustTransition(DARK_COLOR, ICE_COLOR, 1f))
					.spawnAsPlayerActive(mPlayer);
			}
		}

		world.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_SHOOT, SoundCategory.PLAYERS, 1, 0.9f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, SoundCategory.PLAYERS, 1, 0.7f);
		world.playSound(mPlayer.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 1, 0.7f);
		world.playSound(mPlayer.getLocation(), Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1, 0.7f);

		return true;
	}

	private static Description<IceLance> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<>(() -> INFO, color)
			.addTrigger()
			.add(" to shoot an ice lance that travels for ")
			.add(a -> a.mRange, RANGE)
			.add(" blocks and pierces through mobs, dealing ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" magic damage and applying ")
			.addPercent(a -> a.mAmplifier, AMPLIFIER)
			.add(" Slowness and Weaken for ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" seconds. Mobs hit by the lance have ice created under them that lasts for ")
			.addDuration(a -> a.mIceDuration, ICE_TICKS)
			.add(" seconds. The lance can pass through ice blocks.")
			.addCooldown(COOLDOWN);
	}

}


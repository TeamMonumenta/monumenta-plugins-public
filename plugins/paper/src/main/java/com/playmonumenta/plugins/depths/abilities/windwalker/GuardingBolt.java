package com.playmonumenta.plugins.depths.abilities.windwalker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsContent;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.List;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class GuardingBolt extends DepthsAbility {

	public static final String ABILITY_NAME = "Guarding Bolt";
	public static final int COOLDOWN = 20 * 20;
	private static final int RADIUS = 4;
	private static final int RANGE = 25;
	private static final int[] DAMAGE = {15, 18, 21, 24, 27, 35};
	private static final int[] STUN_DURATION = {20, 25, 30, 35, 40, 50};
	public static final Particle.DustOptions COLOR_YELLOW = new Particle.DustOptions(Color.YELLOW, 0.75f);
	public static final Particle.DustOptions COLOR_AQUA = new Particle.DustOptions(Color.AQUA, 0.75f);

	public static final String CHARM_COOLDOWN = "Guarding Bolt Cooldown";

	public static final DepthsAbilityInfo<GuardingBolt> INFO =
		new DepthsAbilityInfo<>(GuardingBolt.class, ABILITY_NAME, GuardingBolt::new, DepthsTree.WINDWALKER, DepthsTrigger.SHIFT_LEFT_CLICK)
			.linkedSpell(ClassAbility.GUARDING_BOLT)
			.cooldown(CHARM_COOLDOWN, COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", GuardingBolt::cast, DepthsTrigger.SHIFT_LEFT_CLICK))
			.displayItem(Material.HORN_CORAL)
			.descriptions(GuardingBolt::getDescription);

	private final double mRadius;
	private final double mRange;
	private final double mDamage;
	private final int mStunDuration;

	public GuardingBolt(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.GUARDING_BOLT_RADIUS.mEffectName, RADIUS);
		mRange = CharmManager.getRadius(mPlayer, CharmEffects.GUARDING_BOLT_RANGE.mEffectName, RANGE);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.GUARDING_BOLT_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mStunDuration = CharmManager.getDuration(mPlayer, CharmEffects.GUARDING_BOLT_STUN_DURATION.mEffectName, STUN_DURATION[mRarity - 1]);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		Location startLoc = mPlayer.getEyeLocation();
		Vector dir = startLoc.getDirection();
		World world = startLoc.getWorld();
		RayTraceResult result = world.rayTraceEntities(startLoc, dir, mRange, 0.5,
			e -> (e instanceof Player player && player != mPlayer && player.getGameMode() != GameMode.SPECTATOR) || DepthsUtils.isDepthsGrave(e));
		// if no one was found, try again 2 more times with increasing ray size until someone is found
		if (result == null) {
			result = world.rayTraceEntities(startLoc, dir, mRange, 0.75,
				e -> (e instanceof Player player && player != mPlayer && player.getGameMode() != GameMode.SPECTATOR) || DepthsUtils.isDepthsGrave(e));
		}
		if (result == null) {
			result = world.rayTraceEntities(startLoc, dir, mRange, 1.00,
				e -> (e instanceof Player player && player != mPlayer && player.getGameMode() != GameMode.SPECTATOR) || DepthsUtils.isDepthsGrave(e));
		}

		if (result != null && result.getHitEntity() instanceof LivingEntity target) {
			if (ZoneUtils.hasZoneProperty(target, ZoneUtils.ZoneProperty.LOOTROOM)) {
				return false;
			}

			putOnCooldown();

			new PPLine(Particle.CLOUD, mPlayer.getEyeLocation(), target.getEyeLocation())
				.countPerMeter(12)
				.delta(0.25)
				.spawnAsPlayerActive(mPlayer);

			for (int k = 0; k < 120; k++) {
				double x = FastUtils.randomDoubleInRange(-3, 3);
				double z = FastUtils.randomDoubleInRange(-3, 3);
				Location to = target.getLocation().add(x, 0.15, z);
				Vector pdir = LocationUtils.getDirectionTo(to, target.getLocation().add(0, 0.15, 0));
				new PartialParticle(Particle.CLOUD, target.getLocation().add(0, 0.15, 0), 0, (float) pdir.getX(), 0f, (float) pdir.getZ(), FastUtils.randomDoubleInRange(0.1, 0.4)).spawnAsPlayerActive(mPlayer);
			}

			for (int k = 0; k < 60; k++) {
				double x = FastUtils.randomDoubleInRange(-3, 3);
				double z = FastUtils.randomDoubleInRange(-3, 3);
				Location to = target.getLocation().add(x, 0.15, z);
				Vector pdir = LocationUtils.getDirectionTo(to, target.getLocation().add(0, 0.15, 0));
				new PartialParticle(Particle.REDSTONE, target.getLocation().add(0, 0.15, 0), 0, (float) pdir.getX(), 0f, (float) pdir.getZ(), FastUtils.randomDoubleInRange(0.15, 0.5), COLOR_YELLOW).spawnAsPlayerActive(mPlayer);
			}

			Location userLoc = mPlayer.getLocation();
			Location otherLoc = target.getLocation().setDirection(mPlayer.getEyeLocation().getDirection());
			Location targetLoc = otherLoc.clone().subtract(dir.clone().multiply(0.5)).add(0, 0.5, 0);
			BoundingBox box = mPlayer.getBoundingBox().shift(targetLoc.clone().subtract(mPlayer.getLocation()));
			if (LocationUtils.collidesWithBlocks(box, mPlayer.getWorld())) {
				targetLoc = otherLoc;
			}
			if (userLoc.distance(targetLoc) > 1) {
				mPlayer.teleport(targetLoc);

				if (!DepthsUtils.isDepthsGrave(target)) {
					Vector vec = targetLoc.clone().subtract(userLoc).toVector().normalize().multiply(0.25);
					vec.setY(0.4);
					mPlayer.setVelocity(vec);
				}
			}
			doDamage(targetLoc);

			world.playSound(targetLoc, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.PLAYERS, 0.75f, 0.9f);

			return true;
		}

		return false;
	}

	private void doDamage(Location location) {
		World world = mPlayer.getWorld();
		world.playSound(location, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.75f, 1.5f);
		new PartialParticle(Particle.REDSTONE, location, 125, mRadius, mRadius, mRadius, 3, COLOR_YELLOW).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, location, 125, mRadius, mRadius, mRadius, 3, COLOR_AQUA).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.FLASH, location.clone().add(location.getDirection()), 1, 0, 0, 0, 10).minimumCount(1).spawnAsPlayerActive(mPlayer);

		List<LivingEntity> enemies = EntityUtils.getNearbyMobs(location, mRadius);
		// The more enemies, the less particles for each one
		int mobParticles = Math.max(
			1,
			20 / Math.max(1, enemies.size()) // Never divide by 0. Always maximum 20 particles for <= 1 enemy
		);

		for (LivingEntity enemy : enemies) {
			DamageUtils.damage(mPlayer, enemy, DamageType.MAGIC, mDamage, mInfo.getLinkedSpell(), true);
			EntityUtils.applyStun(mPlugin, mStunDuration, enemy);

			Location enemyParticleLocation = enemy.getLocation().add(0, enemy.getHeight() / 2, 0);
			new PartialParticle(Particle.CLOUD, enemyParticleLocation, mobParticles, 0.5, 0.5, 0.5, 0.5).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.END_ROD, enemyParticleLocation, mobParticles, 0.5, 0.5, 0.5, 0.5).spawnAsPlayerActive(mPlayer);
		}
	}

	private static Description<GuardingBolt> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<>(() -> INFO, color)
			.addTrigger(" looking at a player " + (DepthsUtils.getDepthsContent() == DepthsContent.CELESTIAL_ZENITH ? "or grave" : ""))
			.add(" within ")
			.add(a -> a.mRange, RANGE)
			.add(" blocks to dash to their location. Mobs within ")
			.add(a -> a.mRadius, RADIUS)
			.add(" blocks of the destination are dealt ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" magic damage, knocked back, and stunned for ")
			.addDuration(a -> a.mStunDuration, STUN_DURATION[rarity - 1], false, true)
			.add(" seconds.")
			.addCooldown(COOLDOWN);
	}
}

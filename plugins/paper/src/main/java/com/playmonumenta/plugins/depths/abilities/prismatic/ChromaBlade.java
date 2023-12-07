package com.playmonumenta.plugins.depths.abilities.prismatic;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.earthbound.Bulwark;
import com.playmonumenta.plugins.depths.abilities.windwalker.DepthsDodging;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.Operation;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.apache.logging.log4j.util.TriConsumer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class ChromaBlade extends DepthsAbility {
	public static final Particle.DustOptions FLAMECALLER_COLOR = new Particle.DustOptions(Color.fromRGB(255, 100, 0), 1f);
	public static final Particle.DustOptions FROSTBORN_COLOR = new Particle.DustOptions(Color.fromRGB(180, 180, 255), 1f);
	public static final Particle.DustOptions DAWNBRINGER_COLOR = new Particle.DustOptions(Color.fromRGB(255, 190, 0), 1f);
	public static final Particle.DustOptions EARTHBOUND_COLOR = new Particle.DustOptions(Color.fromRGB(100, 50, 0), 1f);
	public static final Particle.DustOptions SHADOWDANCER_COLOR = new Particle.DustOptions(Color.fromRGB(70, 0, 70), 1f);
	public static final Particle.DustOptions STEELSAGE_COLOR = new Particle.DustOptions(Color.fromRGB(130, 130, 130), 1f);
	public static final Particle.DustOptions WINDWALKER_COLOR = new Particle.DustOptions(Color.fromRGB(200, 240, 220), 1f);
	public static final Particle.DustOptions PRISMATIC_COLOR = new Particle.DustOptions(Color.fromRGB(245, 200, 245), 1f);


	public static final String ABILITY_NAME = "Chroma Blade";
	public static final double[] DAMAGE = {14, 16, 18, 20, 22, 26};
	public static final int COOLDOWN = 12 * 20;

	public static final DepthsAbilityInfo<ChromaBlade> INFO =
		new DepthsAbilityInfo<>(ChromaBlade.class, ABILITY_NAME, ChromaBlade::new, DepthsTree.PRISMATIC, DepthsTrigger.SHIFT_RIGHT_CLICK)
			.linkedSpell(ClassAbility.CHROMA_BLADE)
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", ChromaBlade::cast, DepthsTrigger.SHIFT_RIGHT_CLICK))
			.displayItem(Material.DIAMOND_SWORD)
			.descriptions(ChromaBlade::getDescription);

	private final double mDamage;
	private @Nullable DepthsTree mLastTree;

	public ChromaBlade(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = DAMAGE[mRarity - 1];
		mLastTree = null;
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		putOnCooldown();

		ItemStack mainhand = mPlayer.getInventory().getItemInMainHand();
		double atkSpeed = 4 + ItemStatUtils.getAttributeAmount(mainhand, AttributeType.ATTACK_SPEED, Operation.ADD, Slot.MAINHAND);
		boolean isFast = atkSpeed >= 1.3;
		int startup = isFast ? 2 : 5;

		if (mLastTree == DepthsTree.WINDWALKER) {
			startup = 0;
		}

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			if (mLastTree == null) {
				noTreeAttack(isFast);
				mLastTree = DepthsTree.PRISMATIC;
				return;
			}

			switch (mLastTree) {
				case FROSTBORN -> frostbornAttack(isFast);
				case FLAMECALLER -> flamecallerAttack(isFast);
				case DAWNBRINGER -> dawnbringerAttack(isFast);
				case EARTHBOUND -> earthboundAttack(isFast);
				case SHADOWDANCER -> shadowdancerAttack(isFast);
				case STEELSAGE -> steelsageAttack(isFast);
				case WINDWALKER -> windwalkerAttack(isFast);
				case PRISMATIC -> prismaticAttack(isFast);
				default -> noTreeAttack(isFast);
			}

			mLastTree = DepthsTree.PRISMATIC;
		}, startup);

		// increased cooldown if slow
		if (!isFast) {
			mPlugin.mTimers.updateCooldown(mPlayer, ClassAbility.CHROMA_BLADE, (int) (-0.25 * getModifiedCooldown()));
		}
	}

	public void flamecallerAttack(boolean isFast) {
		// flamecaller: 75% larger slash size

		ArrayList<LivingEntity> hitMobs = new ArrayList<>();
		new PartialParticle(Particle.FLAME, getPlayerLocation(0.5), 40, 0.5, 0.5, 0.5, 0.1).spawnAsPlayerActive(mPlayer);

		double angle1 = isFast ? -20 : -5;
		double angle2 = 180 - angle1;
		double startingDegrees = isFast ? -15 : -30;
		double endingDegrees = isFast ? 140 : 155;
		int rings = isFast ? 23 : 36;
		long delay = isFast ? 7 : 14;

		Consumer<Double> action = angle -> {
			slash(isFast, DepthsTree.FLAMECALLER, hitMobs, angle, startingDegrees, endingDegrees, rings);
		};

		action.accept(angle1);

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			hitMobs.clear();
			action.accept(angle2);
		}, delay);
	}

	public void frostbornAttack(boolean isFast) {
		// frostborn: 2nd slash replaced by an ice shockwave

		ArrayList<LivingEntity> hitMobs = new ArrayList<>();
		new PartialParticle(Particle.SNOWFLAKE, getPlayerLocation(0.5), 40, 0.5, 0.5, 0.5, 0.1).spawnAsPlayerActive(mPlayer);

		double startingDegrees = isFast ? -15 : -30;
		double endingDegrees = isFast ? 140 : 155;
		int rings = isFast ? 14 : 21;
		double radius = isFast ? 7 : 9;
		double hitboxHalfAngle = isFast ? 50 : 70;
		double runnableAngle = isFast ? 50 : 60; //Not sure why this is different from above - bug?
		double y = isFast ? 0.15 : 0;
		float pitch = isFast ? 0.9f : 0.8f;
		long delay = isFast ? 7 : 14;

		slash(isFast, DepthsTree.FROSTBORN, hitMobs, 0, startingDegrees, endingDegrees, rings);

		World world = mPlayer.getWorld();
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			Hitbox hitbox = Hitbox.approximateCone(getPlayerLocation(0), radius, Math.toRadians(hitboxHalfAngle));
			for (LivingEntity mob : hitbox.getHitMobs()) {
				DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MELEE_SKILL, mDamage, ClassAbility.CHROMA_BLADE, true);
				MovementUtils.knockAway(mPlayer, mob, 0.30f, 0.30f);
				EntityUtils.applySlow(mPlugin, 8 * 20, 0.2, mob);
			}

			frostbornRunnable(runnableAngle, radius, y, world);
			world.playSound(mPlayer.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.0f, pitch);

		}, delay);
	}

	private void frostbornRunnable(double angle, double maxRadius, double y, World world) {
		new BukkitRunnable() {
			final Location mLoc = getPlayerLocation(0);
			double mRadiusIncrement = 0.5;
			final List<Block> mIceAlreadyCreated = new ArrayList<>();
			@Override
			public void run() {
				if (mRadiusIncrement == 0.5) {
					mLoc.setDirection(mPlayer.getLocation().getDirection().setY(0).normalize());
				}
				for (int i = 0; i < 4; i++) {
					Vector vec;
					mRadiusIncrement += 0.5;
					double degree = 90 - angle;
					// particles about every 10 degrees
					int degreeSteps = ((int) (2 * angle)) / 10;
					double degreeStep = 2 * angle / degreeSteps;
					for (int step = 0; step < degreeSteps; step++, degree += degreeStep) {
						double radian1 = Math.toRadians(degree);
						vec = new Vector(FastUtils.cos(radian1) * mRadiusIncrement,
							y,
							FastUtils.sin(radian1) * mRadiusIncrement);
						vec = VectorUtils.rotateXAxis(vec, mLoc.getPitch());
						vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());

						Location l = mLoc.clone().clone().subtract(0, -1.5, 0).add(vec);

						Location checkingLoc = l.clone();
						int count = 0;
						while (!world.getBlockAt(checkingLoc).isSolid() && world.getBlockAt(checkingLoc).getType() != Material.WATER && count < 15) {
							checkingLoc = checkingLoc.clone().add(0, -1, 0);
							count++;
						}
						Block block = world.getBlockAt(checkingLoc);
						if (!mIceAlreadyCreated.contains(block)) {
							DepthsUtils.iceExposedBlock(block, 8 * 20, mPlayer);
							mIceAlreadyCreated.add(block);
						}

						new PartialParticle(Particle.SNOWFLAKE, block.getLocation().add(0.5, 1, 0.5), 2).delta(0.5).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.BLOCK_CRACK, block.getLocation().add(0.5, 1, 0.5), 2).delta(0.5).data(Material.ICE.createBlockData()).spawnAsPlayerActive(mPlayer);
					}

					if (mRadiusIncrement >= maxRadius) {
						this.cancel();
					}
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	public void dawnbringerAttack(boolean isFast) {
		// dawnbringer: mobs hit drop a bezoar-like that grants absorption

		ArrayList<LivingEntity> hitMobs = new ArrayList<>();
		new PartialParticle(Particle.REDSTONE, getPlayerLocation(0.5), 40, 0.5, 0.5, 0, DAWNBRINGER_COLOR).spawnAsPlayerActive(mPlayer);

		double angle1 = isFast ? -25 : -10;
		double angle2 = 180 - angle1;
		double startingDegrees = isFast ? -15 : -30;
		double endingDegrees = isFast ? 140 : 155;
		int rings = isFast ? 14 : 21;
		long delay = isFast ? 7 : 14;

		Consumer<Double> action = angle -> {
			slash(isFast, DepthsTree.DAWNBRINGER, hitMobs, angle, startingDegrees, endingDegrees, rings);
		};

		action.accept(angle1);

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			hitMobs.clear();
			action.accept(angle2);
		}, delay);
	}

	public void earthboundAttack(boolean isFast) {
		// earthbound: 2nd slash replaced by an earthquake

		ArrayList<LivingEntity> hitMobs = new ArrayList<>();
		new PartialParticle(Particle.REDSTONE, getPlayerLocation(0.5), 40, 0.5, 0.5, 0.5, 0, EARTHBOUND_COLOR).spawnAsPlayerActive(mPlayer);

		double startingDegrees = isFast ? -15 : -30;
		double endingDegrees = isFast ? 140 : 155;
		int rings = isFast ? 14 : 21;
		double radius = isFast ? 7 : 9;
		long delay = isFast ? 7 : 14;

		slash(isFast, DepthsTree.EARTHBOUND, hitMobs, 0, startingDegrees, endingDegrees, rings);

		World world = mPlayer.getWorld();
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			Hitbox hitbox = new Hitbox.SphereHitbox(getPlayerLocation(0), radius);
			for (LivingEntity mob : hitbox.getHitMobs()) {
				DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MELEE_SKILL, mDamage, ClassAbility.CHROMA_BLADE, true);
				MovementUtils.pullTowards(mPlayer.getLocation().add(0, 7, 0), mob, 0.12f);
				EntityUtils.applyWeaken(mPlugin, 8 * 20, 0.2, mob);
				EntityUtils.applyTaunt(mob, mPlayer);
			}

			Location loc = mPlayer.getLocation();
			world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.PLAYERS, 1f, 0.6f);
			world.playSound(loc, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.4f, 2.0f);
			world.playSound(loc, Sound.BLOCK_GRASS_BREAK, SoundCategory.PLAYERS, 1.0f, 0.9f);
			for (Material mat : List.of(Material.PODZOL, Material.GRANITE, Material.IRON_ORE)) {
				ParticleUtils.explodingRingEffect(mPlugin, loc, 7, 0.3, 5, 0.075, l -> new PartialParticle(Particle.BLOCK_CRACK, loc, 30, 0.5, 0.25, 0.5, 0.1, mat.createBlockData()).spawnAsPlayerActive(mPlayer));
			}

		}, delay);
	}

	public void shadowdancerAttack(boolean isFast) {
		// shadowdancer: silence mobs hit, and instakill mobs brought below an HP threshold

		ArrayList<LivingEntity> hitMobs = new ArrayList<>();
		new PartialParticle(Particle.SMOKE_NORMAL, getPlayerLocation(0.5), 40, 0.5, 0.5, 0.5, 0.15).spawnAsPlayerActive(mPlayer);

		double angle1 = isFast ? -25 : -10;
		double angle2 = 180 - angle1;
		double startingDegrees = isFast ? -15 : -30;
		double endingDegrees = isFast ? 140 : 155;
		int rings = isFast ? 14 : 21;
		long delay = isFast ? 7 : 14;

		Consumer<Double> action = angle -> {
			slash(isFast, DepthsTree.SHADOWDANCER, hitMobs, angle, startingDegrees, endingDegrees, rings);
		};

		action.accept(angle1);

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			hitMobs.clear();
			action.accept(angle2);
		}, delay);
	}

	public void steelsageAttack(boolean isFast) {
		// steelsage: slashes continue as projectiles and launch mobs upwards

		ArrayList<LivingEntity> hitMobs = new ArrayList<>();
		new PartialParticle(Particle.REDSTONE, getPlayerLocation(0.5), 40, 0.5, 0.5, 0.5, 0, STEELSAGE_COLOR).spawnAsPlayerActive(mPlayer);

		double angle1 = isFast ? -25 : -10;
		double angle2 = 180 - angle1;
		double startingDegrees = isFast ? -15 : -30;
		double endingDegrees = isFast ? 140 : 155;
		int rings = isFast ? 14 : 21;
		long launchDelay = isFast ? 4 : 5;
		long delay = isFast ? 7 : 14;

		BiConsumer<Double, Boolean> action = (angle, isLeft) -> {
			slash(isFast, DepthsTree.STEELSAGE, hitMobs, angle, startingDegrees, endingDegrees, rings);
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				if (hitMobs.isEmpty()) {
					launchSlash(isLeft, isFast);
				}
			}, launchDelay);
		};

		action.accept(angle1, true);

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			hitMobs.clear();
			action.accept(angle2, false);
		}, delay);
	}

	public void windwalkerAttack(boolean isFast) {
		// windwalker: adds a dash, and CDR based on slash hits

		ArrayList<LivingEntity> hitMobs = new ArrayList<>();
		new PartialParticle(Particle.CLOUD, getPlayerLocation(0.5), 40, 0.5, 0.5, 0.5, 0.15).spawnAsPlayerActive(mPlayer);

		Vector direction = mPlayer.getLocation().getDirection();
		Vector yVelocity = new Vector(0, direction.getY() * 0.16 + 0.2, 0);
		mPlayer.setVelocity(direction.multiply(1.4).add(yVelocity));

		double angle1 = isFast ? -25 : -10;
		double angle2 = 180 - angle1;
		double startingDegrees = isFast ? -20 : -30;
		double endingDegrees = isFast ? 165 : 160;
		int rings = isFast ? 16 : 23;
		long delay = isFast ? 7 : 14;

		Consumer<Double> action = angle -> {
			slash(isFast, DepthsTree.WINDWALKER, hitMobs, angle, startingDegrees, endingDegrees, rings);
			if (!hitMobs.isEmpty()) {
				reduceCooldowns();
			}
		};

		action.accept(angle1);

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			hitMobs.clear();
			action.accept(angle2);
		}, delay);
	}

	public void prismaticAttack(boolean isFast) {
		// prismatic: add a third slash that deals extra damage

		ArrayList<LivingEntity> hitMobs = new ArrayList<>();
		new PartialParticle(Particle.WAX_OFF, getPlayerLocation(0.5), 40, 0.5, 0.5, 0.5, 10).spawnAsPlayerActive(mPlayer);

		double angle1 = isFast ? -25 : -10;
		double angle2 = 180 - angle1;
		double angle3 = isFast ? -15 : -5;
		double angle4 = 180 - angle3;
		double startingDegrees = isFast ? -15 : -30;
		double endingDegrees = isFast ? 140 : 155;
		int rings1 = isFast ? 14 : 21;
		int rings2 = isFast ? 18 : 28;
		long delay1 = isFast ? 5 : 8;
		long delay2 = isFast ? 15 : 24;

		TriConsumer<Double, Integer, Double> action = (angle, rings, damageMultiplier) -> {
			slash(isFast, DepthsTree.PRISMATIC, hitMobs, angle, startingDegrees, endingDegrees, rings, damageMultiplier);
		};

		action.accept(angle1, rings1, 1.0);

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			hitMobs.clear();
			action.accept(angle2, rings1, 1.0);
		}, delay1);

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			hitMobs.clear();
			action.accept(angle3, rings2, 1.65);
			action.accept(angle4, rings2, 1.65);
		}, delay2);
	}

	public void noTreeAttack(boolean isFast) {
		ArrayList<LivingEntity> hitMobs = new ArrayList<>();
		new PartialParticle(Particle.CRIT, getPlayerLocation(0.5), 40, 0.5, 0.5, 0.5, 1).spawnAsPlayerActive(mPlayer);

		double angle1 = isFast ? -35 : -10;
		double angle2 = 180 - angle1;
		double startingDegrees = isFast ? -15 : -30;
		double endingDegrees = isFast ? 140 : 170;
		int rings = isFast ? 14 : 21;
		long delay = isFast ? 7 : 14;

		Consumer<Double> action = angle -> {
			slash(isFast, null, hitMobs, angle, startingDegrees, endingDegrees, rings);
		};

		action.accept(angle1);

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			hitMobs.clear();
			action.accept(angle2);
		}, delay);
	}

	private void slash(boolean isFast, @Nullable DepthsTree tree, List<LivingEntity> hitMobs, double angle, double startingDegrees, double endingDegrees, int rings) {
		slash(isFast, tree, hitMobs, angle, startingDegrees, endingDegrees, rings, 1);
	}

	private void slash(boolean isFast, @Nullable DepthsTree tree, List<LivingEntity> hitMobs, double angle, double startingDegrees, double endingDegrees, int rings, double damageMutliplier) {
		// Perhaps all heights should be the same?
		ParticleUtils.drawHalfArc(getPlayerLocation(0.5), 1.5, angle, startingDegrees, endingDegrees, rings, 0.32, false, isFast ? 50 : 25,
			(Location l, int ring) -> doSlashParticle(l, ring, hitMobs, isFast, tree, damageMutliplier)
		);
		playSlashSound(isFast, tree);
	}

	public Location getPlayerLocation(double height) {
		Location loc = mPlayer.getLocation();
		loc.add(0, height, 0);
		loc.setPitch(loc.getPitch() - 15);
		return loc;
	}

	// we need a separate case for prismatic's cross-slash bonus damage
	private void doSlashParticle(Location loc, int ring, List<LivingEntity> hitMobs, boolean isFast, @Nullable DepthsTree tree, double damageMultiplier) {
		if (tree != null) {
			switch (tree) {
				case FLAMECALLER -> new PartialParticle(Particle.REDSTONE, loc, 1, FLAMECALLER_COLOR).spawnAsPlayerActive(mPlayer);
				case FROSTBORN -> new PartialParticle(Particle.REDSTONE, loc, 1, FROSTBORN_COLOR).spawnAsPlayerActive(mPlayer);
				case DAWNBRINGER -> new PartialParticle(Particle.REDSTONE, loc, 1, DAWNBRINGER_COLOR).spawnAsPlayerActive(mPlayer);
				case EARTHBOUND -> new PartialParticle(Particle.REDSTONE, loc, 1, EARTHBOUND_COLOR).spawnAsPlayerActive(mPlayer);
				case SHADOWDANCER -> new PartialParticle(Particle.REDSTONE, loc, 1, SHADOWDANCER_COLOR).spawnAsPlayerActive(mPlayer);
				case STEELSAGE -> new PartialParticle(Particle.REDSTONE, loc, 1, STEELSAGE_COLOR).spawnAsPlayerActive(mPlayer);
				case WINDWALKER -> new PartialParticle(Particle.REDSTONE, loc, 1, WINDWALKER_COLOR).spawnAsPlayerActive(mPlayer);
				case PRISMATIC -> new PartialParticle(Particle.REDSTONE, loc, 1, PRISMATIC_COLOR).spawnAsPlayerActive(mPlayer);
				default -> {
				}
			}
		} else {
			new PartialParticle(Particle.CRIT, loc, 1).extra(0).spawnAsPlayerActive(mPlayer);
		}

		if (ring % 3 == 0) {
			new PartialParticle(Particle.SWEEP_ATTACK, loc, 1).extra(0).spawnAsPlayerActive(mPlayer);
		}

		Hitbox hitbox = new Hitbox.AABBHitbox(mPlayer.getWorld(), BoundingBox.of(loc, 0.5, 1, 0.5));
		List<LivingEntity> targets = hitbox.getHitMobs();
		targets.removeAll(hitMobs); // remove anything that was already hit
		for (LivingEntity target : targets) {
			hitMobs.add(target);

			DamageUtils.damage(mPlayer, target, DamageEvent.DamageType.MELEE_SKILL, mDamage * damageMultiplier, ClassAbility.CHROMA_BLADE, true);
			if (isFast) {
				MovementUtils.knockAway(mPlayer, target, 0.30f, 0.30f);
			} else {
				MovementUtils.knockAway(mPlayer, target, 0.45f, 0.45f);
			}

			if (tree != null) {
				switch (tree) {
					case FLAMECALLER -> EntityUtils.applyFire(mPlugin, 8 * 20, target, mPlayer);
					case DAWNBRINGER -> dropChroma(LocationUtils.getHalfHeightLocation(target));
					case SHADOWDANCER -> {
						EntityUtils.applySilence(mPlugin, 20, target);
						if (target.isValid() && !EntityUtils.isBoss(target) && target.getHealth() < 0.2 * EntityUtils.getMaxHealth(target)) {
							doExecute(target);
						}
					}
					default -> { }
				}
			}
		}
	}

	private void playSlashSound(boolean isFast, @Nullable DepthsTree tree) {
		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();
		if (tree != null) {
			switch (tree) {
				case FROSTBORN -> world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.0f, 1.2f);
				case FLAMECALLER -> world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 1.0f, 1.5f);
				case DAWNBRINGER -> world.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.0f, 1.5f);
				case EARTHBOUND -> world.playSound(loc, Sound.BLOCK_GRAVEL_BREAK, SoundCategory.PLAYERS, 1.0f, 0.7f);
				case SHADOWDANCER -> world.playSound(loc, Sound.ENTITY_PHANTOM_AMBIENT, SoundCategory.PLAYERS, 1.0f, 0.7f);
				case STEELSAGE -> world.playSound(loc, Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.PLAYERS, 1.0f, 2f);
				case WINDWALKER -> world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 1.0f, 1.5f);
				case PRISMATIC -> world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 1.0f, 0.8f);
				default -> {
				}
			}
		}

		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.0f, isFast ? 0.7f : 0.5f);
		world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.0f, isFast ? 0.7f : 0.5f);
		world.playSound(loc, Sound.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.PLAYERS, 1.0f, isFast ? 0.7f : 0.5f);
		world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1.0f, isFast ? 0.7f : 0.5f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0f, isFast ? 0.7f : 0.5f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.PLAYERS, 1.0f, isFast ? 1.7f : 1.4f);
		world.playSound(loc, Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.PLAYERS, 0.7f, isFast ? 2.0f : 1.5f);
	}

	private void dropChroma(Location loc) {
		World world = loc.getWorld();
		// have the item pop out with some random direction
		loc.setYaw(FastUtils.randomFloatInRange(0, 360));
		loc.setPitch(FastUtils.randomFloatInRange(-75, -30));
		Item chroma = AbilityUtils.spawnAbilityItem(world, loc, Material.YELLOW_GLAZED_TERRACOTTA, "Radiant Chroma", false, 0.3, true, true);

		new BukkitRunnable() {
			int mT = 0;
			final BlockData mFallingDustData = Material.YELLOW_GLAZED_TERRACOTTA.createBlockData();
			@Override
			public void run() {
				mT++;
				Location l = chroma.getLocation();
				new PartialParticle(Particle.FALLING_DUST, l, 1, 0.2, 0.2, 0.2, mFallingDustData).spawnAsOtherPlayerActive();

				for (Player p : PlayerUtils.playersInRange(l, 1.25, true)) {
					AbsorptionUtils.addAbsorption(p, 1, 6, 10 * 20);

					world.playSound(l, Sound.BLOCK_STONE_BREAK, SoundCategory.PLAYERS, 1, 0.75f);
					world.playSound(l, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 1, 1.44f);
					world.playSound(l, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 1, 1.2f);
					new PartialParticle(Particle.BLOCK_CRACK, l, 20, 0.15, 0.15, 0.15, 0.75f, Material.YELLOW_GLAZED_TERRACOTTA.createBlockData()).spawnAsOtherPlayerActive();
					new PartialParticle(Particle.TOTEM, l, 10, 0, 0, 0, 0.35F).spawnAsOtherPlayerActive();

					this.cancel();
					chroma.remove();
					break;
				}
				if (mT >= 10 * 20 || chroma.isDead()) {
					this.cancel();
					chroma.remove();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void doExecute(LivingEntity le) {
		DamageUtils.damage(mPlayer, le, DamageEvent.DamageType.TRUE, 9001, ClassAbility.CHROMA_BLADE, true, false);

		World world = mPlayer.getWorld();
		world.playSound(le.getLocation(), Sound.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 0.75f, 0.75f);
		world.playSound(le.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.PLAYERS, 0.5f, 1.5f);

		new PartialParticle(Particle.BLOCK_CRACK, le.getEyeLocation(), 20, 0.3, 0.3, 0.3, 1, Material.REDSTONE_BLOCK.createBlockData()).spawnAsPlayerActive(mPlayer);
	}

	private void launchSlash(boolean isLeft, boolean isFast) {
		Location playerLoc = mPlayer.getLocation().add(0, 1, 0);
		new BukkitRunnable() {
			final Vector mDir = playerLoc.getDirection();
			final Location mLoc = playerLoc.add(mDir.clone().multiply(3));
			double mDistance = 0;
			@Override
			public void run() {
				double angle = isFast ? (isLeft ? -25 : 205) : (isLeft ? -10 : 190);
				double size = isFast ? 1.75 : 2.5;

				ParticleUtils.drawHalfArc(mLoc, size, angle, 30, 150, 1, 0, false, 180,
					(Location l, int ring) -> new PartialParticle(Particle.REDSTONE, l, 1, STEELSAGE_COLOR).spawnAsPlayerActive(mPlayer)
				);
				ParticleUtils.drawHalfArc(mLoc.subtract(mDir.clone().multiply(0.15)), size, angle, 30, 150, 1, 0, false, 180,
					(Location l, int ring) -> new PartialParticle(Particle.REDSTONE, l, 1, STEELSAGE_COLOR).spawnAsPlayerActive(mPlayer)
				);

				List<LivingEntity> collision = new Hitbox.SphereHitbox(mLoc, size).getHitMobs();
				// if something was hit
				if (!collision.isEmpty()) {
					List<LivingEntity> hitMobs = new Hitbox.SphereHitbox(mLoc, size * 2).getHitMobs();
					new PartialParticle(Particle.FIREWORKS_SPARK, mLoc, 50, 0.5, 0.5, 0.5, 0.2).spawnAsPlayerActive(mPlayer);
					for (LivingEntity mob : hitMobs) {
						DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MELEE_SKILL, mDamage, ClassAbility.CHROMA_BLADE, true);
						MovementUtils.knockAway(mPlayer, mob, 0.40f, 0.40f);
					}

					this.cancel();
				}

				mLoc.add(mDir);
				mDistance++;
				if (mDistance > 50 || !mLoc.getBlock().isPassable()) {
					List<LivingEntity> hitMobs = new Hitbox.SphereHitbox(mLoc, size * 2).getHitMobs();
					new PartialParticle(Particle.FIREWORKS_SPARK, mLoc, 50, 0.5, 0.5, 0.5, 0.2).spawnAsPlayerActive(mPlayer);
					for (LivingEntity mob : hitMobs) {
						DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MELEE_SKILL, mDamage, ClassAbility.CHROMA_BLADE, true);
						MovementUtils.knockAway(mPlayer, mob, 0.40f, 0.40f);
					}

					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void reduceCooldowns() {
		for (Ability abil : mPlugin.mAbilityManager.getPlayerAbilities(mPlayer).getAbilities()) {
			ClassAbility linkedSpell = abil.getInfo().getLinkedSpell();
			if (linkedSpell == null) {
				continue;
			}
			mPlugin.mTimers.updateCooldown(mPlayer, linkedSpell, 30);
		}
	}

	@Override
	public boolean abilityCastEvent(AbilityCastEvent event) {
		if (!(event.getAbility() instanceof DepthsAbility ability)) {
			return true;
		}

		// don't count itself
		if (event.getAbility() instanceof ChromaBlade) {
			return true;
		}

		// don't count passives either
		if (event.getAbility() instanceof Bulwark || event.getAbility() instanceof DepthsDodging) {
			return true;
		}

		mLastTree = ability.getInfo().getDepthsTree();

		return true;
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mLastTree != null) {
			Particle.DustOptions color;
			switch (mLastTree) {
				case FROSTBORN -> color = FROSTBORN_COLOR;
				case FLAMECALLER -> color = FLAMECALLER_COLOR;
				case DAWNBRINGER -> color = DAWNBRINGER_COLOR;
				case EARTHBOUND -> color = EARTHBOUND_COLOR;
				case SHADOWDANCER -> color = SHADOWDANCER_COLOR;
				case STEELSAGE -> color = STEELSAGE_COLOR;
				case WINDWALKER -> color = WINDWALKER_COLOR;
				case PRISMATIC -> color = PRISMATIC_COLOR;
				default -> color = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f);
			}

			Location rightHand = PlayerUtils.getRightSide(mPlayer.getEyeLocation(), 0.45).subtract(0, .8, 0);
			Location leftHand = PlayerUtils.getRightSide(mPlayer.getEyeLocation(), -0.45).subtract(0, .8, 0);
			new PartialParticle(Particle.REDSTONE, leftHand, 4, 0.1f, 0.1f, 0.1f, 0, color).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.REDSTONE, rightHand, 4, 0.1f, 0.1f, 0.1f, 0, color).spawnAsPlayerActive(mPlayer);
		}
	}

	private static Description<ChromaBlade> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<ChromaBlade>(color)
			.add("Right click while sneaking to unleash two slashes that each deal ")
			.addDepthsDamage(a -> DAMAGE[rarity - 1], DAMAGE[rarity - 1], true)
			.add(" melee damage. If your weapon's attack speed is below 1.3, perform slower, wider slashes with 25% increased cooldown instead.")
			.add(" Additionally, gain a bonus effect depending on the tree of the last ability you used.")
			.addCooldown(COOLDOWN)
			.addConditionalTree(DepthsTree.FROSTBORN, getFrostbornDescription(color))
			.addConditionalTree(DepthsTree.FLAMECALLER, getFlamecallerDescription(color))
			.addConditionalTree(DepthsTree.DAWNBRINGER, getDawnbringerDescription(color))
			.addConditionalTree(DepthsTree.EARTHBOUND, getEarthboundDescription(color))
			.addConditionalTree(DepthsTree.SHADOWDANCER, getShadowdancerDescription(color))
			.addConditionalTree(DepthsTree.STEELSAGE, getSteelsageDescription(color))
			.addConditionalTree(DepthsTree.WINDWALKER, getWindwalkerDescription(color))
			.add(getPrismaticDescription(color));
	}

	private static Description<ChromaBlade> getFrostbornDescription(TextColor color) {
		return new DescriptionBuilder<ChromaBlade>(color)
			.add(Component.text("\nFrostborn").color(TextColor.color(DepthsUtils.FROSTBORN)))
			.add(" - Replace the second slash with a shockwave that creates ice and applies 20% Slow for 8s.");
	}

	private static Description<ChromaBlade> getFlamecallerDescription(TextColor color) {
		return new DescriptionBuilder<ChromaBlade>(color)
			.add(Component.text("\nFlamecaller").color(TextColor.color(DepthsUtils.FLAMECALLER)))
			.add(" - Increase the size of both slashes by 75% and ignite mobs hit for 8s.");
	}

	private static Description<ChromaBlade> getDawnbringerDescription(TextColor color) {
		return new DescriptionBuilder<ChromaBlade>(color)
			.add(Component.text("\nDawnbringer").color(TextColor.color(DepthsUtils.DAWNBRINGER)))
			.add(" - Mobs hit by a slash drop a piece of Chroma that when picked up, grants 1 absorption health, up to 6, for 10s.");
	}

	private static Description<ChromaBlade> getEarthboundDescription(TextColor color) {
		return new DescriptionBuilder<ChromaBlade>(color)
			.add(Component.text("\nEarthbound").color(TextColor.color(DepthsUtils.EARTHBOUND)))
			.add(" - Replace the second slash with an earthquake that pulls, taunts, and applies 20% Weaken to mobs for 8s.");
	}

	private static Description<ChromaBlade> getShadowdancerDescription(TextColor color) {
		return new DescriptionBuilder<ChromaBlade>(color)
			.add(Component.text("\nShadowdancer").color(TextColor.color(DepthsUtils.SHADOWDANCER)))
			.add(" - Silence mobs hit for 1s. If the slash brings a non-Boss mob under 20% HP, they die instantly.");
	}

	private static Description<ChromaBlade> getSteelsageDescription(TextColor color) {
		return new DescriptionBuilder<ChromaBlade>(color)
			.add(Component.text("\nSteelsage").color(TextColor.color(DepthsUtils.STEELSAGE)))
			.add(" - If a slash doesn't hit any mobs, it continues forward as a projectile.");
	}

	private static Description<ChromaBlade> getWindwalkerDescription(TextColor color) {
		return new DescriptionBuilder<ChromaBlade>(color)
			.add(Component.text("\nWindwalker").color(TextColor.color(DepthsUtils.WINDWALKER)))
			.add(" - Additionally dash forwards while slashing. Each slash reduces your skill cooldowns by 1.5s if it hits a mob.");
	}

	private static Description<ChromaBlade> getPrismaticDescription(TextColor color) {
		return new DescriptionBuilder<ChromaBlade>(color)
			.add(Component.text("\n").append(DepthsTree.PRISMATIC.color("Prismatic")))
			.add(" - Add a third cross-slash that is 50% larger and deals 65% more damage.");
	}

}

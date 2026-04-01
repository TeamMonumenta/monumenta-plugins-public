package com.playmonumenta.plugins.cosmetics.skills.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SteelTrapCS implements CosmeticSkill {
	@Override
	public ClassAbility getAbility() {
		return ClassAbility.STEEL_TRAP;
	}

	@Override
	public Material getDisplayItem() {
		return Material.TRAPPED_CHEST;
	}

	public Material getThrownItem() {
		return Material.TNT;
	}

	public Map<String, BlockDisplay> getBlockDisplayTrap(World world, Location loc) {
		HashMap<String, BlockDisplay> trap = new HashMap<>();

		BlockDisplay center = world.spawn(loc, BlockDisplay.class);
		center.setBlock(Material.IRON_TRAPDOOR.createBlockData());
		center.setBrightness(new Display.Brightness(15, 15));
		center.setTransformation(
			new Transformation(
				new Vector3f(-0.3125f, -0.075f, -0.3125f),
				new Quaternionf(),
				new Vector3f(0.625f, 0.625f, 0.625f),
				new Quaternionf()
			));
		trap.put("center", center);


		BlockDisplay tnt = world.spawn(loc, BlockDisplay.class);
		tnt.setBlock(Material.TNT.createBlockData());
		tnt.setBrightness(new Display.Brightness(15, 15));
		tnt.setTransformation(
			new Transformation(
				new Vector3f(-0.25f, -0.075f, -0.25f),
				new Quaternionf(),
				new Vector3f(0.5f, 0.1531f, 0.5f),
				new Quaternionf()
			));
		trap.put("tnt", tnt);

		return trap;
	}

	public Map<String, BlockDisplay> getUnderwaterBlockDisplayTrap(World world, Location loc) {
		HashMap<String, BlockDisplay> trap = new HashMap<>();

		BlockDisplay center = world.spawn(loc, BlockDisplay.class);
		center.setBlock(Material.COBWEB.createBlockData());
		center.setBrightness(new Display.Brightness(15, 15));
		center.setTransformation(
			new Transformation(
				new Vector3f(-0.8125f, -0.8125f, -0.8125f),
				new Quaternionf(),
				new Vector3f(1.625f, 1.625f, 1.625f),
				new Quaternionf()
			));

		trap.put("center", center);

		return trap;
	}

	public void trapThrow(World world, Player player, Location loc) {
		world.playSound(loc, Sound.ENTITY_WITCH_THROW, 1f, 0.7f);
		world.playSound(loc, Sound.ITEM_LODESTONE_COMPASS_LOCK, 1.5f, 0.5f);
	}

	public void trapMidairTick(World world, Player player, Location loc) {
		new PartialParticle(Particle.SMOKE_NORMAL, loc.add(0, 0.2, 0))
			.delta(0.1)
			.count(5)
			.spawnAsPlayerActive(player);
	}

	public void trapLand(World world, Player player, Location loc, Map<String, BlockDisplay> trap, double radius) {
		if (trap.containsKey("center")) {
			trap.get("center").setTransformation(
				new Transformation(
					new Vector3f(-0.40625f, -0.225f, -0.40625f),
					new Quaternionf(),
					new Vector3f(0.8125f, 0.8125f, 0.8125f),
					new Quaternionf()
				));
		}

		if (trap.containsKey("tnt")) {
			trap.get("tnt").setTransformation(
				new Transformation(
					new Vector3f(-0.375f, -0.225f, -0.375f),
					new Quaternionf(),
					new Vector3f(0.75f, 0.2031f, 0.75f),
					new Quaternionf()
				));
		}

		world.playSound(loc, Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1f, 0.5f);
		world.playSound(loc, Sound.ENTITY_BAT_TAKEOFF, 0.3f, 1f);
		world.playSound(loc, Sound.BLOCK_IRON_DOOR_CLOSE, 1.5f, 1f);

		new PPCircle(Particle.SMOKE_NORMAL, loc, 0.5)
			.rotateDelta(true)
			.directionalMode(true)
			.delta(0.075, 0, 0)
			.extra(radius)
			.count(30)
			.spawnAsPlayerActive(player);
	}

	public void trapPrimingTick(World world, Player player, Location loc, int ticks, int maxTicks, double radius) {
		double progress = (double) ticks / maxTicks;

		new PartialParticle(Particle.SMOKE_NORMAL, loc, 2).extra(0.1).directionalMode(true).delta(0, 1, 0).spawnAsPlayerActive(player);
		if (ticks % 20 == 0) {
			world.playSound(loc, Sound.ITEM_CROSSBOW_LOADING_START, 1.5f, (float) (1.5 * progress));
		}
		world.playSound(loc, Sound.ITEM_SPYGLASS_USE, 1f, (float) (1.5 * progress));

		new PPCircle(Particle.REDSTONE, loc, radius * (1 - progress))
			.ringMode(true)
			.count(3)
			.countPerMeter(3)
			.data(new Particle.DustOptions(Color.MAROON, 0.75f))
			.spawnAsPlayerActive(player);

	}

	public void trapPrimed(World world, Player player, Location loc, double radius) {
		new PPCircle(Particle.SMOKE_NORMAL, loc, 0.5)
			.rotateDelta(true)
			.directionalMode(true)
			.delta(0.075, 0, 0)
			.extra(radius)
			.count(30)
			.spawnAsPlayerActive(player);

		new PPCircle(Particle.FLAME, loc, 0.5)
			.rotateDelta(true)
			.directionalMode(true)
			.delta(0.075, 0, 0)
			.extra(radius)
			.count(30)
			.spawnAsPlayerActive(player);

		world.playSound(loc, Sound.ENTITY_BLAZE_HURT, 1f, 1.4f);
		world.playSound(loc, Sound.ITEM_SHIELD_BREAK, 1f, 0.7f);
	}

	public void trapPrimeTick(World world, Player player, Location loc, double triggerRadius, int ticks, boolean isEnhanced) {
		if (ticks % 5 == 0 && !LocationUtils.isLocationInWater(loc)) {
			new PartialParticle(Particle.SMOKE_LARGE, loc, 1).extra(0.1).directionalMode(true).delta(0, 1, 0).spawnAsPlayerActive(player);
		}

		if (!isEnhanced) {
			new PPCircle(Particle.REDSTONE, loc, triggerRadius)
				.ringMode(true)
				.count(1)
				.countPerMeter(1)
				.data(new Particle.DustOptions(Color.MAROON, 0.75f))
				.spawnAsPlayerActive(player);
		}
	}

	public void trapExplode(World world, Player player, Location loc, double explosionRadius) {
		new PartialParticle(Particle.EXPLOSION_LARGE, loc, 3, 0.15, 0.15, 0.15).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SMOKE_LARGE, loc, 35, 0.15, 0.15, 0.15, 0.3).spawnAsPlayerActive(player);

		world.playSound(loc, Sound.ENTITY_EVOKER_FANGS_ATTACK, 1f, 1.2f);
		world.playSound(loc, Sound.BLOCK_IRON_TRAPDOOR_OPEN, 1f, 1f);
		world.playSound(loc, Sound.ITEM_LODESTONE_COMPASS_LOCK, 2f, 0.4f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.25f);

		ParticleUtils.explodingRingEffect(Plugin.getInstance(), loc, explosionRadius, 1, 5, 0.85,
			l -> new PartialParticle(Particle.SMOKE_NORMAL, loc, 2, 0.15, 0.15, 0.15)
				.extra(0.01)
				.spawnAsPlayerActive(player));

		ParticleUtils.explodingRingEffect(Plugin.getInstance(), loc, explosionRadius, 1, 5, 0.35,
			l -> new PartialParticle(Particle.SMALL_FLAME, loc, 2, 0.15, 0.15, 0.15)
				.extra(0.01)
				.spawnAsPlayerActive(player));

		new PPCircle(Particle.LAVA, loc, explosionRadius)
			.ringMode(true)
			.count(1)
			.countPerMeter(1)
			.spawnAsPlayerActive(player);
	}

	public void trapDespawn(World world, Player player, Location loc) {
		world.playSound(loc, Sound.ENTITY_HORSE_SADDLE, 1.5f, 0.65f);

		new PPCircle(Particle.CLOUD, loc, 2.5)
			.delta(0.15, 0.15, 0.15)
			.count(30)
			.spawnAsPlayerActive(player);
	}

}

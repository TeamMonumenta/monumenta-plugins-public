package com.playmonumenta.plugins.cosmetics.skills.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;


public class SeverCS extends SpellshockCS {
	public static final String NAME = "Sever";
	private static final Particle.DustOptions BLACK = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 0.8f);
	private static final BlockData BLACK_CONCRETE = Material.BLACK_CONCRETE_POWDER.createBlockData();
	private static final BlockData OBSIDIAN = Material.OBSIDIAN.createBlockData();
	private static final double CLEAVE_RADIUS = 2;
	private static final Color COLOR_RED = Color.fromRGB(82, 7, 18);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"A reality can be likened to fabric. Those",
			"not twined in its weave are at the mercy",
			"of external forces.");
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.SPELLSHOCK;
	}

	@Override
	public Material getDisplayItem() {
		return Material.STRING;
	}

	@Override
	public void tickEffect(Entity entity) {
		Location loc = LocationUtils.getHalfHeightLocation(entity);

		new PartialParticle(Particle.BLOCK_CRACK, loc, 15, 0, 0, 0, 1, OBSIDIAN).spawnAsEnemyBuff();
		new PartialParticle(Particle.FALLING_DUST, loc, 10, 0.3, 0.3, 0.3, BLACK_CONCRETE).spawnAsEnemyBuff();
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 5, 0.1, 0.1, 0.1, 0).spawnAsEnemyBuff();
	}

	@Override
	public void meleeClearStatic(Player player, LivingEntity enemy) {
		World world = player.getWorld();
		Location loc = player.getLocation().add(0, 1, 0);
		Location eLoc = LocationUtils.getHalfHeightLocation(enemy).add(0, 0.25, 0);
		eLoc.setPitch(0);
		eLoc.setYaw(player.getLocation().getYaw());
		drawX(eLoc, player);

		Vector v1 = player.getLocation().getDirection().setY(0).normalize();
		Vector v2 = VectorUtils.rotateYAxis(v1, 90).setY(0).normalize();
		Vector v3 = VectorUtils.rotateYAxis(v1, -90).setY(0).normalize();

		for (int i = 0; i < 6; i++) {
			new PartialParticle(Particle.SQUID_INK, eLoc.clone(), 1, 0.01, 0.6, 0.01, 0.5 * FastUtils.randomDoubleInRange(0.85, 1.25)).directionalMode(true).delta(v2.getX() * FastUtils.randomDoubleInRange(0.75, 1.25), FastUtils.randomDoubleInRange(-0.3, 0.3), v2.getZ() * FastUtils.randomDoubleInRange(0.75, 1.25)).spawnAsPlayerActive(player);
			new PartialParticle(Particle.SQUID_INK, eLoc.clone(), 1, 0.01, 0.6, 0.01, 0.5 * FastUtils.randomDoubleInRange(0.85, 1.25)).directionalMode(true).delta(v3.getX() * FastUtils.randomDoubleInRange(0.75, 1.25), FastUtils.randomDoubleInRange(-0.3, 0.3), v3.getZ() * FastUtils.randomDoubleInRange(0.75, 1.25)).spawnAsPlayerActive(player);
		}

		//exclusive to melee
		new PartialParticle(Particle.SWEEP_ATTACK, loc, 1, 0, 0, 0, 0).spawnAsEnemyBuff();


		world.playSound(loc, Sound.ENTITY_SHULKER_BULLET_HURT, SoundCategory.PLAYERS, 0.85f, 0.7f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.PLAYERS, 0.8f, 1.7f);
		world.playSound(loc, Sound.ENTITY_PUFFER_FISH_BLOW_OUT, SoundCategory.PLAYERS, 0.8f, 0.7f);
		world.playSound(loc, Sound.ENTITY_SNOW_GOLEM_SHEAR, SoundCategory.PLAYERS, 0.8f, 0.5f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.8f, 0.7f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 0.8f, 0.7f);
		world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1f, 0.7f);

	}

	@Override
	public void spellshockEffect(Player player, LivingEntity enemy) {
		World world = player.getWorld();
		Location loc = player.getLocation().add(0, 1, 0);
		Location eLoc = LocationUtils.getHalfHeightLocation(enemy).add(0, 0.25, 0);
		eLoc.setPitch(0);
		eLoc.setYaw(player.getLocation().getYaw());
		drawX(eLoc, player);

		Vector v1 = player.getLocation().getDirection().setY(0).normalize();
		Vector v2 = VectorUtils.rotateYAxis(v1, 90).setY(0).normalize();
		Vector v3 = VectorUtils.rotateYAxis(v1, -90).setY(0).normalize();

		for (int i = 0; i < 6; i++) {
			new PartialParticle(Particle.SQUID_INK, eLoc.clone(), 1, 0.01, 0.6, 0.01, 0.5 * FastUtils.randomDoubleInRange(0.85, 1.25)).directionalMode(true).delta(v2.getX() * FastUtils.randomDoubleInRange(0.75, 1.25), FastUtils.randomDoubleInRange(-0.25, 0.5), v2.getZ() * FastUtils.randomDoubleInRange(0.75, 1.25)).spawnAsPlayerActive(player);
			new PartialParticle(Particle.SQUID_INK, eLoc.clone(), 1, 0.01, 0.6, 0.01, 0.5 * FastUtils.randomDoubleInRange(0.85, 1.25)).directionalMode(true).delta(v3.getX() * FastUtils.randomDoubleInRange(0.75, 1.25), FastUtils.randomDoubleInRange(-0.25, 0.5), v3.getZ() * FastUtils.randomDoubleInRange(0.75, 1.25)).spawnAsPlayerActive(player);
		}

		Vector vec = VectorUtils.rotationToVector(player.getLocation().getYaw(), player.getLocation().getPitch()).multiply(CLEAVE_RADIUS * -1.15);
		Location arcCenter = enemy.getEyeLocation().clone().add(vec).add(0, -0.075, 0);
		arcCenter.setYaw(player.getLocation().getYaw());
		arcCenter.setPitch(player.getLocation().getPitch());

		world.playSound(loc, Sound.ENTITY_SHULKER_BULLET_HURT, SoundCategory.PLAYERS, 0.75f, 0.7f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.PLAYERS, 0.7f, 1.7f);
		world.playSound(loc, Sound.ENTITY_PUFFER_FISH_BLOW_OUT, SoundCategory.PLAYERS, 0.7f, 0.7f);
		world.playSound(loc, Sound.ENTITY_SNOW_GOLEM_SHEAR, SoundCategory.PLAYERS, 0.7f, 0.5f);
		world.playSound(loc, Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 0.7f, 0.7f);
	}

	private void drawX(Location loc, Player player) {
		loc.setPitch(0);
		loc.setYaw(player.getLocation().getYaw());
		Vector dir = VectorUtils.rotateTargetDirection(player.getLocation().getDirection(), 90, 90 - 65);
		ParticleUtils.drawParticleLineSlash(loc, dir, 0, 2.25, 0.1, 4,
			(Location lineLoc, double middleProgress, double endProgress, boolean middle) -> {
				float size = (float) (0.5f + (0.3f * middleProgress));
				new PartialParticle(Particle.REDSTONE, lineLoc, 3, 0.05, 0.05, 0.05, 0.25, new Particle.DustOptions(
					ParticleUtils.getTransition(BLACK.getColor(), COLOR_RED, 1 - middleProgress), size))
					.spawnAsPlayerActive(player);
			});

		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			Vector d = VectorUtils.rotateTargetDirection(player.getLocation().getDirection(), 90, 90 + 65);
			ParticleUtils.drawParticleLineSlash(loc, d, 0, 2.25, 0.1, 4,
				(Location lineLoc, double middleProgress, double endProgress, boolean middle) -> {
					float size = (float) (0.5f + (0.3f * middleProgress));
					new PartialParticle(Particle.REDSTONE, lineLoc, 3, 0.05, 0.05, 0.05, 0.25, new Particle.DustOptions(
						ParticleUtils.getTransition(BLACK.getColor(), COLOR_RED, 1 - middleProgress), size))
						.spawnAsPlayerActive(player);
				});
			new PartialParticle(Particle.ELECTRIC_SPARK, loc, 40, 0.05, 0.05, 0.05, 0.5).spawnAsPlayerActive(player);
		}, 2);
	}
}

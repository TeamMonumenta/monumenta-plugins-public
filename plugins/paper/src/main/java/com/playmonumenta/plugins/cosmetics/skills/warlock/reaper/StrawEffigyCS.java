package com.playmonumenta.plugins.cosmetics.skills.warlock.reaper;

import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import javax.annotation.Nullable;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

public class StrawEffigyCS extends JudgementChainCS {
	private static final Particle.DustOptions STRAW = new Particle.DustOptions(Color.fromRGB(250, 230, 80), 1f);
	private static final Particle.DustOptions STRAW_SMALL = new Particle.DustOptions(Color.fromRGB(250, 230, 80), 0.6f);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Golden stalks of hay",
			"bind two souls together."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.HAY_BLOCK;
	}

	@Override
	public @Nullable String getName() {
		return "Straw Effigy";
	}

	@Override
	public void onPassDamage(Player player, LivingEntity chainedMob, LivingEntity selectedMob) {
		World world = player.getWorld();
		Location chainLoc = LocationUtils.getHalfHeightLocation(chainedMob);
		Location targetLoc = LocationUtils.getHalfHeightLocation(selectedMob);

		new PartialParticle(Particle.TOTEM, chainLoc, 6, 0.5, 0.5, 0.5, 0.3).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, chainLoc, 6, 0.5, 0.5, 0.5, 0).data(STRAW).spawnAsPlayerActive(player);

		new PartialParticle(Particle.TOTEM, targetLoc, 8, 0.5, 0.5, 0.5, 0.3).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FALLING_DUST, targetLoc, 4, 0.75, 0.75, 0.75, 0)
			.data(Material.SAND.createBlockData()).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, targetLoc, 8, 0.5, 0.5, 0.5, 0).data(STRAW).spawnAsPlayerActive(player);

		new PPLine(Particle.CRIT, chainLoc, targetLoc).countPerMeter(4).spawnAsPlayerActive(player);

		world.playSound(targetLoc, Sound.ITEM_AXE_SCRAPE, SoundCategory.PLAYERS, 1.0f, 0.8f);
		world.playSound(targetLoc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1.0f, 1.2f);
		world.playSound(targetLoc, Sound.BLOCK_CHAIN_PLACE, SoundCategory.PLAYERS, 0.8f, 0.67f);
	}

	@Override
	public void onSummonChain(World world, Location loc) {
		world.playSound(loc, Sound.ITEM_AXE_SCRAPE, SoundCategory.PLAYERS, 1.0f, 0.5f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1.0f, 0.5f);
		world.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.7f, 0.63f);
		world.playSound(loc, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.PLAYERS, 1.0f, 0.6f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 1.0f, 1.75f);
		world.playSound(loc, Sound.ENTITY_ENDER_EYE_DEATH, SoundCategory.PLAYERS, 1.0f, 2.0f);
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 1.0f, 0.65f);
	}

	@Override
	public void chain(Player player, Location playerLoc, Location targetLoc, double delta, int ticks) {
		new PartialParticle(Particle.CRIT, targetLoc, 1).delta(delta * 1.5).spawnAsPlayerActive(player);
		if (ticks % 2 == 0) {
			new PartialParticle(Particle.FALLING_DUST, targetLoc.clone().add(0, 0.5, 0), 1).data(Material.SAND.createBlockData())
				.delta(delta * 1.5).spawnAsPlayerActive(player);
		} else {
			new PartialParticle(Particle.REDSTONE, targetLoc, 2).data(STRAW).delta(delta * 1.5).spawnAsPlayerActive(player);
		}

		new PPLine(Particle.CRIT, targetLoc, playerLoc).delta(0.1).countPerMeter(0.25).offset(FastUtils.randomDoubleInRange(0, 1)).spawnAsPlayerActive(player);
		if (ticks % 2 == 0) {
			drawChain(player, playerLoc, targetLoc, false);
		}
	}

	@Override
	public void onBreakChain(Player player, LivingEntity target, boolean isLevelTwo, double effectRadius, double damageRadius) {
		World world = player.getWorld();
		Location playerLoc = LocationUtils.getHalfHeightLocation(player);
		Location targetLoc = LocationUtils.getHalfHeightLocation(target);

		drawChain(player, playerLoc, targetLoc, true);

		new PartialParticle(Particle.SWEEP_ATTACK, target.getEyeLocation(), 1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT, targetLoc, 30, 0.75, 0.75, 0.75, 0.125).spawnAsPlayerActive(player);
		if (isLevelTwo) {
			new PartialParticle(Particle.CRIT, playerLoc, 30, damageRadius, damageRadius, damageRadius, 0.125).spawnAsPlayerActive(player);
		}

		world.playSound(playerLoc, Sound.ENTITY_GUARDIAN_DEATH, SoundCategory.PLAYERS, 1.0f, 0.6f);
		world.playSound(playerLoc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.6f, 0.6f);
		world.playSound(playerLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.0f, 0.5f);
		world.playSound(playerLoc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 1.0f, 1.25f);
		world.playSound(playerLoc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 0.75f, 1.25f);
		world.playSound(playerLoc, Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.PLAYERS, 1f, 1.35f);
	}

	@Override
	public Team createTeam() {
		return ScoreboardUtils.getExistingTeamOrCreate("strawEffigy", NamedTextColor.YELLOW);
	}

	public void drawChain(Player player, Location playerLoc, Location targetLoc, boolean isBreak) {
		// we drawing a real chain out here!!
		Vector direction = LocationUtils.getDirectionTo(playerLoc, targetLoc);
		Location chainLoc = targetLoc.clone();

		Particle.DustOptions data = isBreak ? STRAW : STRAW_SMALL;

		double length = 1;
		for (int i = 0; i < 20; i++) {
			double[] rotation = VectorUtils.vectorToRotation(direction);
			Vector offset = VectorUtils.rotateTargetDirection(new Vector(0.3, 0, 0), rotation[0], rotation[1]);

			if (i % 2 == 0) {
				new PPLine(Particle.REDSTONE, chainLoc, direction, length).data(data)
					.countPerMeter(10).delta(0.01).groupingDistance(0).spawnAsPlayerActive(player);
			} else {
				new PPLine(Particle.REDSTONE, chainLoc.clone().add(offset), direction, length).data(data)
					.countPerMeter(7).groupingDistance(0).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, chainLoc.clone().subtract(offset), direction, length).data(data)
					.countPerMeter(7).groupingDistance(0).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, chainLoc.clone().add(offset), chainLoc.clone().subtract(offset)).data(data)
					.countPerMeter(7).groupingDistance(0).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, chainLoc.clone().add(direction.clone().multiply(length)).add(offset), chainLoc.clone().add(direction.clone().multiply(length)).subtract(offset)).data(data)
					.countPerMeter(7).groupingDistance(0).spawnAsPlayerActive(player);
			}

			chainLoc.add(direction.clone().multiply(length));

			if (chainLoc.distance(playerLoc) < 1.2) {
				new PPLine(Particle.REDSTONE, chainLoc, playerLoc).data(data)
					.countPerMeter(7).groupingDistance(0).spawnAsPlayerActive(player);
				break;
			}
		}
	}
}

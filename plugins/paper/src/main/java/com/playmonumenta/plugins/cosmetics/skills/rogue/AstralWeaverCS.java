package com.playmonumenta.plugins.cosmetics.skills.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class AstralWeaverCS extends DodgingCS {
	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Stay hidden, remain cloaked",
			"by the echoing starlight."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.PURPLE_DYE;
	}

	@Override
	public @Nullable String getName() {
		return "Astral Weaver";
	}

	@Override
	public void dodgeEffect(Player player, World world, Location loc) {
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 20, 0.25, 0.45, 0.25, 0.1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, loc, 50, 0.25, 0.45, 0.25, 0, new Particle.DustOptions(rollColor(), 0.5f)).spawnAsPlayerActive(player);

		ArrayList<Vector> fullStar = StarCosmeticsFunctions.interpolatePolygon(StarCosmeticsFunctions.generateStarVertices(5, 3, 0.35, true, true), 3);
		Location dodgeLoc = player.getLocation().clone().add(0, 0.1, 0);

		new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				for (Vector v : fullStar) {
					drawParticle(player, dodgeLoc.clone().add(v), 1);
				}
				mTicks++;
				if (mTicks > 4) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 4);


		world.playSound(loc, Sound.ITEM_BOTTLE_FILL_DRAGONBREATH, SoundCategory.PLAYERS, 0.4f, 2f);
		world.playSound(loc, Sound.ITEM_CHORUS_FRUIT_TELEPORT, SoundCategory.PLAYERS, 0.6f, 0.6f);
		world.playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 0.6f, 1.5f);
	}

	@Override
	public void dodgeEffectLv2(Player player, World world, Location loc) {
		new PartialParticle(Particle.VILLAGER_HAPPY, loc, 3, 0.25, 0.25, 0.25, 0).spawnAsPlayerActive(player);
	}

	@Override
	public void deflectTrailEffect(Player player, Location particleLocation) {
		drawParticle(player, particleLocation, 4);
	}

	private void drawParticle(Player player, Location loc, int countMultiplier) {
		switch (FastUtils.randomIntInRange(0, 5)) {
			case 0 ->
				new PartialParticle(Particle.CRIT_MAGIC, loc, 2 * countMultiplier, 0.03, 0.03, 0.03, 0).spawnAsPlayerActive(player);
			case 1 ->
				new PartialParticle(Particle.SPELL_WITCH, loc, 2 * countMultiplier, 0.03, 0.03, 0.03, 0).spawnAsPlayerActive(player);
			case 2 ->
				new PartialParticle(Particle.ENCHANTMENT_TABLE, loc, 2 * countMultiplier, 0.03, 0.03, 0.03, 0).spawnAsPlayerActive(player);
			default ->
				new PartialParticle(Particle.REDSTONE, loc, countMultiplier, 0.03, 0.03, 0.03, 0, new Particle.DustOptions(rollColor(), 1.2f)).spawnAsPlayerActive(player);
		}
	}

	private Color rollColor() {
		return Color.fromRGB(80 + FastUtils.randomIntInRange(0, 160), 80, 200);
	}
}

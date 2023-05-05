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

public class SupernovaShieldCS extends EscapeDeathCS {
	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"A brittle layer of protection is offered",
			"to those deemed worthy by the stars."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.PURPLE_STAINED_GLASS;
	}

	@Override
	public @Nullable String getName() {
		return "Supernova Shield";
	}

	@Override
	public void activate(Player player, World world, Location loc) {
		ArrayList<Vector> helix = new ArrayList<>();
		for (double d = 0; d < 3 * Math.PI; d += 2 * Math.PI / 40) {
			helix.add(new Vector(1.8 * FastUtils.cos(d), -0.5 + 2 * d / (3 * Math.PI), 1.8 * FastUtils.sin(d)));
			helix.add(new Vector(-1.8 * FastUtils.cos(d), -0.5 + 2 * d / (3 * Math.PI), -1.8 * FastUtils.sin(d)));
		}

		new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				for (int i = 0; i < 12; i++) {
					drawParticle(loc.clone().add(helix.get(i + mTicks * 12)), player, rollColor());
				}
				mTicks++;
				if (mTicks >= 10) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 2);

		world.playSound(loc, Sound.ENTITY_ENDER_EYE_DEATH, SoundCategory.PLAYERS, 0.75f, 0.5f);
		world.playSound(loc, Sound.ITEM_ARMOR_EQUIP_ELYTRA, SoundCategory.PLAYERS, 1f, 1.1f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.PLAYERS, 1f, 0.5f);
		world.playSound(loc, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.6f, 2f);

		new BukkitRunnable() {
			@Override
			public void run() {
				world.playSound(loc, Sound.ENTITY_ENDER_EYE_DEATH, SoundCategory.PLAYERS, 0.75f, 0.5f);
				world.playSound(loc, Sound.ITEM_ARMOR_EQUIP_ELYTRA, SoundCategory.PLAYERS, 1f, 1.1f);
			}
		}.runTaskLater(Plugin.getInstance(), 2);
		new BukkitRunnable() {
			@Override
			public void run() {
				world.playSound(loc, Sound.ENTITY_ENDER_EYE_DEATH, SoundCategory.PLAYERS, 0.75f, 0.75f);
				world.playSound(loc, Sound.ITEM_ARMOR_EQUIP_ELYTRA, SoundCategory.PLAYERS, 1f, 1.35f);
				world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.3f, 1.7f);
				world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.8f, 0.7f);
			}
		}.runTaskLater(Plugin.getInstance(), 4);
	}

	private void drawParticle(Location location, Player player, Color color) {
		new PartialParticle(Particle.REDSTONE, location, 4, 0.04, 0.02, 0.04, 0, new Particle.DustOptions(color, 1.5f))
			.spawnAsPlayerActive(player);
	}

	private Color rollColor() {
		return Color.fromRGB(80 + FastUtils.randomIntInRange(0, 160), 80, 200);
	}
}

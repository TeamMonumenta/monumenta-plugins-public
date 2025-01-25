package com.playmonumenta.plugins.cosmetics.skills.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPSpiral;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class VoodooBurstCS extends FrostNovaCS {
	public static final String NAME = "Voodoo Burst";

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Their struggle against your power is pointless.",
			"For you are the trees... you are the world all around them.");
	}

	@Override
	public Material getDisplayItem() {
		return Material.BUBBLE_CORAL;
	}

	@Override
	public void onCast(Plugin plugin, Player player, World world, double size) {
		Location loc = player.getLocation();
		world.playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1f, 0.5f);
		world.playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1.4f, 1f);
		world.playSound(loc, Sound.ENTITY_WARDEN_ROAR, SoundCategory.PLAYERS, 0.5f, 2f);
		world.playSound(loc, Sound.ENTITY_VEX_CHARGE, SoundCategory.PLAYERS, 0.6f, 0.5f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.PLAYERS, 0.6f, 0.5f);
		world.playSound(loc, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 0.5f, 1.5f);
		world.playSound(loc, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 0.5f, 1f);

		new PPSpiral(Particle.SOUL, loc.clone().add(0, 0.15, 0), size)
			.countPerBlockPerCurve(10)
			.ticks((int) size)
			.spawnAsPlayerActive(player);
		new PPCircle(Particle.REDSTONE, loc, size)
			.data(new Particle.DustOptions(Color.PURPLE, 2))
			.countPerMeter(4)
			.delta(0.3)
			.spawnAsPlayerActive(player);
		new PPCircle(Particle.REDSTONE, loc, size - 0.5)
			.data(new Particle.DustOptions(Color.PURPLE.mixColors(Color.BLACK), 1.3f))
			.countPerMeter(4)
			.delta(0.3)
			.spawnAsPlayerActive(player);
		loc.setPitch(0);
		ParticleUtils.drawParticleCircleExplosion(
			player, loc.clone().add(0, 0.1, 0), 0, 0.5, 0, 0, 75, 5F, false, 0, 0.1, Particle.CRIT_MAGIC
		);
		new PPSpiral(Particle.SPELL_WITCH, loc, 3)
			.ticks(10)
			.spawnAsPlayerActive(player);
	}
}

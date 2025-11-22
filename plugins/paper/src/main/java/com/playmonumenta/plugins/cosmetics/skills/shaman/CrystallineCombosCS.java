package com.playmonumenta.plugins.cosmetics.skills.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPPeriodic;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class CrystallineCombosCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.CRYSTALLINE_COMBOS;
	}

	@Override
	public Material getDisplayItem() {
		return Material.AMETHYST_CLUSTER;
	}

	private static final Particle.DustOptions PARTICLE_COLOR = new Particle.DustOptions(Color.fromRGB(255, 200, 200), 0.8f);
	private static final Collection<Map.Entry<Double, ParticleUtils.SpawnParticleAction>> PARTICLES =
		List.of(new AbstractMap.SimpleEntry<Double, ParticleUtils.SpawnParticleAction>(0.4,
			(Location loc) -> new PartialParticle(Particle.REDSTONE, loc, 1, 0.1, 0.1, 0.1, PARTICLE_COLOR)
				.spawnAsOtherPlayerActive()));
	private static final Collection<Map.Entry<Double, ParticleUtils.SpawnParticleAction>> PARTICLES_BREAK =
		List.of(new AbstractMap.SimpleEntry<Double, ParticleUtils.SpawnParticleAction>(0.4,
			(Location loc) -> new PartialParticle(Particle.CRIT_MAGIC, loc, 1)
				.spawnAsOtherPlayerActive()));

	public void crystallineCombosSwirl(List<PPPeriodic> mParticles, Player player) {
		mParticles.add(new PPPeriodic(Particle.REDSTONE, player.getLocation()).extra(0.1).data(new Particle.DustOptions(Color.fromRGB(100, 100, 255), 0.8f)).spawnAsPlayerPassive(player));
	}

	public void crystallineCombosTrigger(Player player) {
		player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 2, 1.5f);
	}

	public void crystallineCombosActiveSwirl(List<PPPeriodic> mParticles, Player player, double mRotationAngle, boolean mSpendingStacks, int colorAdjust, int i) {
		PPPeriodic particle = mParticles.get(i);
		particle.location(LocationUtils.getHalfHeightLocation(player)
				.add(FastUtils.cos(Math.toRadians(mRotationAngle + (i * 120))), -0.1, FastUtils.sin(Math.toRadians(mRotationAngle + (i * 120)))))
			.data(new Particle.DustOptions(Color.fromRGB(mSpendingStacks ? 255 : colorAdjust, mSpendingStacks ? 150 : colorAdjust, mSpendingStacks ? 150 : 255), 0.8f))
			.spawnAsPlayerPassive(player);
	}

	public void crystallineCombosHit(Player player, LivingEntity target, Particle.DustOptions particleColor) {
		player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_HIT,
			SoundCategory.PLAYERS, 0.6f, 1.0f);
		player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK,
			SoundCategory.PLAYERS, 1f, 1.4f);
		new PPLine(Particle.REDSTONE,
			LocationUtils.getHalfHeightLocation(player).add(0, -0.1, 0),
			LocationUtils.getHalfHeightLocation(target)).data(particleColor)
			.delta(0.1).extra(0.1).countPerMeter(10)
			.spawnAsPlayerActive(player);
	}

	public void crystallineCombosExpire(Player player, Plugin plugin) {
		player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 2.0f, 1.0f);
		ParticleUtils.explodingRingEffect(plugin, LocationUtils.getHalfHeightLocation(player), 2.5,
			1, 5, PARTICLES);
		ParticleUtils.explodingRingEffect(plugin, LocationUtils.getHalfHeightLocation(player), 2.5,
			1, 5, PARTICLES_BREAK);
	}
}

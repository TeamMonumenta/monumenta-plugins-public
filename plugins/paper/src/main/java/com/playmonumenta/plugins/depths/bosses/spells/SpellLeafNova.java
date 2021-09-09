package com.playmonumenta.plugins.depths.bosses.spells;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.bosses.spells.SpellBaseAoE;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellLeafNova extends SpellBaseAoE {

	private static final Particle.DustOptions LEAF_COLOR = new Particle.DustOptions(Color.fromRGB(14, 123, 8), 1.0f);
	private static final int RADIUS = 5;
	private static final int DURATION = 4 * 20;
	private static final double DAMAGE = 0.6;

	private int mCooldownTicks;

	public SpellLeafNova(Plugin plugin, LivingEntity launcher, int radius, int time, int cooldown) {
		super(plugin, launcher, radius, time, cooldown, false, Sound.BLOCK_BAMBOO_BREAK,
			(Location loc) -> {
				World world = loc.getWorld();
				world.spawnParticle(Particle.REDSTONE, loc, 1, ((double) radius) / 2, ((double) radius) / 2, ((double) radius) / 2, LEAF_COLOR);
				world.spawnParticle(Particle.COMPOSTER, loc, 1, ((double) radius) / 2, ((double) radius) / 2, ((double) radius) / 2, 0.05);

			},
			(Location loc) -> {
				World world = loc.getWorld();
				world.spawnParticle(Particle.REDSTONE, loc, 1, 0.25, 0.25, 0.25, LEAF_COLOR);
				world.spawnParticle(Particle.REDSTONE, loc.clone().add(0, 2, 0), 1, 0.25, 0.25, 0.25, LEAF_COLOR);
				world.spawnParticle(Particle.COMPOSTER, loc, 1, 0.25, 0.25, 0.25, 0.1);
			},
			(Location loc) -> {
				World world = loc.getWorld();
				world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.65F);
			},
			(Location loc) -> {
				World world = loc.getWorld();
				world.spawnParticle(Particle.COMPOSTER, loc, 1, 0.1, 0.1, 0.1, 0.3);
				world.spawnParticle(Particle.SLIME, loc, 2, 0.25, 0.25, 0.25, 0.1);
			},
			(Location loc) -> {
				for (Player player : PlayerUtils.playersInRange(launcher.getLocation(), radius, true)) {
					BossUtils.bossDamagePercent(launcher, player, DAMAGE, (Location) null, "Leaf Nova");
					player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 20 * 6, 4));
				}
			}
		);
	}

	public SpellLeafNova(Plugin plugin, LivingEntity launcher, int cooldown) {
		this(plugin, launcher, RADIUS, DURATION, cooldown);
		mCooldownTicks = cooldown;
	}

	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}
}

package com.playmonumenta.plugins.bosses.spells.rkitxet;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.bosses.RKitxet;
import com.playmonumenta.plugins.bosses.spells.SpellBaseAoE;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellVerdantProtection extends SpellBaseAoE {

	private static final int RADIUS = 5;
	private static final int DURATION = 4 * 20;
	private static final int DAMAGE = 11;
	private static final Particle.DustOptions VERDANT_PROTECTION_COLOR = new Particle.DustOptions(Color.fromRGB(20, 200, 20), 1f);

	private RKitxet mRKitxet;

	public SpellVerdantProtection(Plugin plugin, LivingEntity launcher, int radius, int time, int cooldown, RKitxet rKitxet) {
		super(plugin, launcher, radius, time, cooldown, false, Sound.BLOCK_ROOTS_BREAK,
			(Location loc) -> {
				World world = loc.getWorld();
				world.spawnParticle(Particle.CRIMSON_SPORE, loc, 1, ((double) radius) / 2, ((double) radius) / 2, ((double) radius) / 2);
				world.spawnParticle(Particle.COMPOSTER, loc, 1, ((double) radius) / 2, ((double) radius) / 2, ((double) radius) / 2, 0.05);

			},
			(Location loc) -> {
				World world = loc.getWorld();
				world.spawnParticle(Particle.REDSTONE, loc, 1, 0.25, 0.25, 0.25, VERDANT_PROTECTION_COLOR);
				world.spawnParticle(Particle.REDSTONE, loc.clone().add(0, 2, 0), 1, 0.25, 0.25, 0.25, VERDANT_PROTECTION_COLOR);
				world.spawnParticle(Particle.COMPOSTER, loc, 1, 0.25, 0.25, 0.25, 0.1);
			},
			(Location loc) -> {
				World world = loc.getWorld();
				world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.65F);
			},
			(Location loc) -> {
				World world = loc.getWorld();
				world.spawnParticle(Particle.COMPOSTER, loc, 1, 0.1, 0.1, 0.1, 0.3);
				world.spawnParticle(Particle.REDSTONE, loc, 1, 0.25, 0.25, 0.25, 0.1, VERDANT_PROTECTION_COLOR);
			},
			(Location loc) -> {
				rKitxet.useSpell("Verdant Protection");

				boolean hasHit = false;
				for (Player player : PlayerUtils.playersInRange(launcher.getLocation(), radius, true)) {
					BossUtils.bossDamage(launcher, player, DAMAGE, launcher.getLocation(), "Verdant Protection");

					double distance = player.getLocation().distance(loc);
					if (distance < radius / 3.0) {
						MovementUtils.knockAway(launcher, player, 2.25f);
					} else if (distance < (radius * 2.0) / 3.0) {
						MovementUtils.knockAway(launcher, player, 1.575f);
					} else if (distance < radius) {
						MovementUtils.knockAway(launcher, player, 0.9f);
					}

					hasHit = true;
				}

				if (hasHit) {
					rKitxet.mShieldSpell.applyShield(true);
				}
			}
		);
		mRKitxet = rKitxet;
	}

	public SpellVerdantProtection(Plugin plugin, LivingEntity launcher, int cooldown, RKitxet rKitxet) {
		this(plugin, launcher, RADIUS, DURATION, cooldown, rKitxet);
	}

	@Override
	public boolean canRun() {
		return mRKitxet.canUseSpell("Verdant Protection");
	}
}

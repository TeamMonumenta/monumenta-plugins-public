package com.playmonumenta.plugins.cosmetics.skills.warrior;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ColossalBruteCS extends BruteForceCS {
	//Delve theme

	public static final String NAME = "Colossal Brute";

	private static final Particle.DustOptions TWIST_COLOR = new Particle.DustOptions(Color.fromRGB(127, 0, 0), 1.0f);
	private static final Particle.DustOptions COLO_COLOR = new Particle.DustOptions(Color.fromRGB(255, 127, 0), 1.0f);

	@Override
	public Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName());
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.BRUTE_FORCE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.NETHERITE_AXE;
	}

	@Override
	public void bruteOnDamage(Player mPlayer, Location loc) {
		mPlayer.getWorld().playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, 0.75f, 0.5f);
		mPlayer.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.75f, 0.5f);
		mPlayer.getWorld().spawnParticle(Particle.REDSTONE, loc, 20, 0.5, 0.2, 0.5, 0.5, COLO_COLOR);
		mPlayer.getWorld().spawnParticle(Particle.REDSTONE, loc, 10, 0.5, 0.2, 0.5, 0.5, TWIST_COLOR);
		mPlayer.getWorld().spawnParticle(Particle.SMOKE_NORMAL, loc, 15, 0.25, 0.2, 0.25, 0.1);
	}

	@Override
	public void bruteOnSpread(Player mPlayer, LivingEntity mob) {
		mPlayer.getWorld().spawnParticle(Particle.REDSTONE, mob.getLocation(), 9, 0.5, 0.2, 0.5, 0.5, COLO_COLOR);
		mPlayer.getWorld().spawnParticle(Particle.REDSTONE, mob.getLocation(), 6, 0.5, 0.2, 0.5, 0.5, TWIST_COLOR);
	}
}

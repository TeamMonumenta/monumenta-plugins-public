package com.playmonumenta.plugins.cosmetics.skills.warrior;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

@Deprecated
public class BrutalShadowCS implements CosmeticSkill {
	//Darker brute force. Depth set: shadow
	//Deprecated before 16th Sept

	public static final String NAME = "Brutal Shadow";

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

	public static void bruteOnDamage(Player mPlayer, Location loc) {
		mPlayer.getWorld().playSound(loc, Sound.ENTITY_WITHER_SHOOT, 0.75f, 1.65f);
		mPlayer.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.75f, 0.5f);
		mPlayer.getWorld().spawnParticle(Particle.SPELL_WITCH, loc, 15, 0.5, 0.2, 0.5, 0.65);
	}

	public static void bruteOnSpread(Player mPlayer, LivingEntity mob) {
		mPlayer.getWorld().spawnParticle(Particle.SPELL_WITCH, mob.getLocation(), 10, 0.5, 0.2, 0.5, 0.65);
	}
}

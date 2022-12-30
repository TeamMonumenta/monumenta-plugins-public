package com.playmonumenta.plugins.cosmetics.skills.rogue;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class ByMyBladeCS implements CosmeticSkill {

	public static final ImmutableMap<String, ByMyBladeCS> SKIN_LIST = ImmutableMap.<String, ByMyBladeCS>builder()
		.put(DecapitationCS.NAME, new DecapitationCS())
		.build();

	@Override
	public @Nullable Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.BY_MY_BLADE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.ZOMBIE_HEAD;
	}

	@Override
	public @Nullable String getName() {
		return null;
	}

	public void bmbDamage(World world, Player mPlayer, LivingEntity enemy, int level) {
		Location loc = enemy.getLocation();
		world.playSound(loc, Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 2.0f, 0.5f);
		new PartialParticle(Particle.SPELL_MOB, loc, level * 15, 0.25, 0.5, 0.5, 0.001).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CRIT, loc, 30, 0.25, 0.5, 0.5, 0.001).spawnAsPlayerActive(mPlayer);
	}

	public void bmbDamageLv2(Player mPlayer, LivingEntity enemy) {
		new PartialParticle(Particle.SPELL_WITCH, enemy.getLocation(), 45, 0.2, 0.65, 0.2, 1.0).spawnAsPlayerActive(mPlayer);
	}

	public void bmbHeal(Player mPlayer, Location loc) {
		new PartialParticle(Particle.HEART, mPlayer.getLocation().add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001).spawnAsPlayerActive(mPlayer);
	}
}


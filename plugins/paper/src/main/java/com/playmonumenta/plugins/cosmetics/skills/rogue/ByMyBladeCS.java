package com.playmonumenta.plugins.cosmetics.skills.rogue;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPPillar;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ByMyBladeCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.BY_MY_BLADE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SKELETON_SKULL;
	}

	public void bmbDamage(World world, Player player, LivingEntity enemy, int level) {
		Location loc = enemy.getLocation();
		world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 0.2f, 0.7f);
		world.playSound(loc, Sound.ENTITY_PLAYER_DEATH, SoundCategory.PLAYERS, 1.2f, 1.0f);
		world.playSound(loc, Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1.0f, 1.5f);
		world.playSound(loc, Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(loc, Sound.ITEM_CROSSBOW_SHOOT, SoundCategory.PLAYERS, 1.2f, 1.5f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 0.9f, 2.0f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 2.0f, 0.3f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 2.0f, 1.0f);
		world.playSound(loc, Sound.ENTITY_WITHER_SKELETON_HURT, SoundCategory.PLAYERS, 0.9f, 0.6f);
		new PartialParticle(Particle.SPELL_MOB, loc, level * 15, 0.25, 0.5, 0.5, 0.001).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT, loc, 30, 0.25, 0.5, 0.5, 0.001).spawnAsPlayerActive(player);
		new PPPillar(Particle.CRIT_MAGIC, loc, 3).count(15).extra(0.1).spawnAsPlayerActive(player);
	}

	public void bmbDamageLv2(Player player, LivingEntity enemy) {
		new PartialParticle(Particle.SPELL_WITCH, enemy.getLocation(), 45, 0.2, 0.65, 0.2, 1.0).spawnAsPlayerActive(player);
	}

	public void bmbHeal(Player player, Location loc) {
		new PartialParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001).spawnAsPlayerActive(player);
	}
}


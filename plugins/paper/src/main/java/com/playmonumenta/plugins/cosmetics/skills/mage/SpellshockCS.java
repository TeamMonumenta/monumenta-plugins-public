package com.playmonumenta.plugins.cosmetics.skills.mage;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;


public class SpellshockCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.SPELLSHOCK;
	}

	@Override
	public Material getDisplayItem() {
		return Material.GLOWSTONE_DUST;
	}

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(220, 147, 249), 1.0f);

	public void tickEffect(Entity entity) {
		Location loc = entity.getLocation();
		new PartialParticle(Particle.SPELL_WITCH, loc, 3, 0.2, 0.6, 0.2, 1).spawnAsEnemyBuff();
		new PartialParticle(Particle.REDSTONE, loc, 3, 0.3, 0.6, 0.3, COLOR).spawnAsEnemyBuff();
	}

	public void meleeClearStatic(Player player, LivingEntity enemy) {
		Location loc = LocationUtils.getHalfHeightLocation(enemy);
		World world = player.getWorld();
		new PartialParticle(Particle.SPELL_WITCH, loc, 20, 1, 1, 1, 0.001).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT_MAGIC, loc, 15, 1, 1, 1, 0.25).spawnAsPlayerActive(player);
		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.PLAYERS, 0.75f, 1.5f);
	}

	public void spellshockEffect(Player player, LivingEntity enemy) {
		Location loc = LocationUtils.getHalfHeightLocation(enemy);
		World world = player.getWorld();
		new PartialParticle(Particle.SPELL_WITCH, loc, 60, 1, 1, 1, 0.001).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT_MAGIC, loc, 45, 1, 1, 1, 0.25).spawnAsPlayerActive(player);
		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.PLAYERS, 0.75f, 2.5f);
		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.PLAYERS, 0.75f, 2.0f);
		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.PLAYERS, 0.75f, 1.5f);
	}

	public void damageOverTimeEffects(LivingEntity target) {
		new PartialParticle(Particle.CRIT_MAGIC, target.getEyeLocation(), 8, 0.4, 0.4, 0.4, 0.1).spawnAsEnemy();
		new PartialParticle(Particle.ENCHANTMENT_TABLE, target.getEyeLocation(), 8, 0.4, 0.4, 0.4, 0.1).spawnAsEnemy();
	}

	public void enhancedLightning(LivingEntity from, LivingEntity to) {
		World world = from.getWorld();
		Location locFrom = from.getLocation();
		Location locTo = to.getLocation();
		Vector dir = LocationUtils.getDirectionTo(locTo, locFrom);
		dir.multiply(0.2);
		double step = locFrom.distance(locTo) / 0.2;
		Location pLoc = locFrom.add(0, 1.2, 0);
		for (int i = 0; i < step; i++) {
			pLoc.add(dir);
			new PartialParticle(Particle.END_ROD, pLoc, 2, 0.05, 0.05, 0.05, 0).spawnAsEnemy();
		}
		world.playSound(locFrom, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, SoundCategory.PLAYERS, 0.3f, 1f);
		world.playSound(locFrom, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, SoundCategory.PLAYERS, 0.3f, 1.5f);
		world.playSound(locTo, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, SoundCategory.PLAYERS, 0.3f, 1.5f);
		world.playSound(locTo, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, SoundCategory.PLAYERS, 0.3f, 2f);
	}
}

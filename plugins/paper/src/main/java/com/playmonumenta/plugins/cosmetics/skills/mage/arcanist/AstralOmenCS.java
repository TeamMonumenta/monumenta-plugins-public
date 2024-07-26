package com.playmonumenta.plugins.cosmetics.skills.mage.arcanist;

import com.playmonumenta.plugins.abilities.mage.arcanist.AstralOmen;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import java.util.Map;
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

public class AstralOmenCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.ASTRAL_OMEN;
	}

	@Override
	public Material getDisplayItem() {
		return Material.NETHER_STAR;
	}

	public static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(100, 50, 170), 1f);

	public void clearEffect(Player player, LivingEntity enemy, Map.Entry<AstralOmen.Type, Integer> entry) {
		new PartialParticle(Particle.GLOW, enemy.getLocation(), 80, 0, 0, 0, 4).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, enemy.getLocation(), entry.getValue() * 5, 0.2, 0.2, 0.2, 0.1, entry.getKey().mColor).spawnAsPlayerActive(player);
		player.getWorld().playSound(enemy.getLocation(), Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 1.3f, 1.5f);
	}

	public void arcaneStack(Player player, Entity entity, Particle.DustOptions color) {
		Location location = entity.getLocation().add(0, 1, 0);
		new PartialParticle(Particle.ENCHANTMENT_TABLE, location, 2, 0, 0, 0, 4).spawnAsEnemyBuff();
		new PartialParticle(Particle.REDSTONE, location, 8, 0.2, 0.2, 0.2, 0.1, color).spawnAsEnemyBuff();
	}

	public void fireStack(Player player, Entity entity, Particle.DustOptions color) {
		Location location = entity.getLocation().add(0, 1, 0);
		new PartialParticle(Particle.ENCHANTMENT_TABLE, location, 8, 0, 0, 0, 4).spawnAsEnemyBuff();
		new PartialParticle(Particle.REDSTONE, location, 8, 0.2, 0.2, 0.2, 0.1, color).spawnAsEnemyBuff();
	}

	public void iceStack(Player player, Entity entity, Particle.DustOptions color) {
		Location location = entity.getLocation().add(0, 1, 0);
		new PartialParticle(Particle.ENCHANTMENT_TABLE, location, 2, 0, 0, 0, 4).spawnAsEnemyBuff();
		new PartialParticle(Particle.REDSTONE, location, 8, 0.2, 0.2, 0.2, 0.1, color).spawnAsEnemyBuff();
	}

	public void thunderStack(Player player, Entity entity) {
		Location location = entity.getLocation().add(0, 1, 0);
		new PartialParticle(Particle.ENCHANTMENT_TABLE, location, 8, 0, 0, 0, 4).spawnAsEnemyBuff();
		new PartialParticle(Particle.REDSTONE, location, 8, 0.2, 0.2, 0.2, 0.1, new Particle.DustOptions(Color.fromRGB(255, 255, 0), 1)).spawnAsEnemyBuff();
	}

	public void bonusDamage(Player player, Entity entity, Particle.DustOptions color) {
		World world = entity.getWorld();
		Location loc = entity.getLocation().add(0, 1, 0);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1f, 1.25f);
		world.playSound(loc, Sound.ENTITY_SHULKER_SHOOT, SoundCategory.PLAYERS, 1f, 1.75f);
		new PartialParticle(Particle.CRIT, loc, 8, 0.25, 0.5, 0.25, 0.4).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, loc, 8, 0.2, 0.2, 0.2, 0.1, color).spawnAsPlayerActive(player);
	}

	public void bonusDamageTick(Player player, Entity entity, Particle.DustOptions color) {
		Location loc = entity.getLocation().add(0, 1, 0);
		new PartialParticle(Particle.REDSTONE, loc, 8, 0.2, 0.2, 0.2, 0.1, color).spawnAsPlayerActive(player);
	}
}

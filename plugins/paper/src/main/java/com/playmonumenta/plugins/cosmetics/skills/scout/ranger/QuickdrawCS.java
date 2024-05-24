package com.playmonumenta.plugins.cosmetics.skills.scout.ranger;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

public class QuickdrawCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.QUICKDRAW;
	}

	@Override
	public Material getDisplayItem() {
		return Material.BLAZE_POWDER;
	}

	public void quickdrawCast(Player player) {
		World world = player.getWorld();
		new PartialParticle(Particle.CRIT, player.getEyeLocation().add(player.getLocation().getDirection()), 15, 0, 0, 0, 0.6f).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT_MAGIC, player.getEyeLocation().add(player.getLocation().getDirection()), 15, 0, 0, 0, 0.6f).spawnAsPlayerActive(player);
		world.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1, 1.4f);
	}

	public void quickdrawProjectileEffect(Plugin plugin, Projectile proj) {
		plugin.mProjectileEffectTimers.addEntity(proj, Particle.FIREWORKS_SPARK);
	}
}

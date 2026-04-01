package com.playmonumenta.plugins.cosmetics.skills.scout.hunter;

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

public class QuiverStormCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.QUIVER_STORM;
	}

	@Override
	public Material getDisplayItem() {
		return Material.BLAZE_ROD;
	}

	public void arrowLaunch(Player player) {
		World world = player.getWorld();
		new PartialParticle(Particle.ELECTRIC_SPARK, player.getEyeLocation().add(player.getLocation().getDirection()), 15, 0, 0, 0, 0.6f).spawnAsPlayerActive(player);

		world.playSound(player.getLocation(), Sound.BLOCK_IRON_DOOR_OPEN, SoundCategory.PLAYERS, 0.8f, 1.6f);
		world.playSound(player.getLocation(), Sound.ITEM_CROSSBOW_SHOOT, SoundCategory.PLAYERS, 0.6f, 1.3f);
		world.playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 0.6f, 2f);
		world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 0.8f, 1.3f);
	}

	public void arrowLevel2Hit(Player player) {
		player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.5f, 1.5f);
		player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_HIT, 0.6f, 1.2f);
		player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.2f, 1.3f);

	}

	public void arrowEffect(Plugin plugin, Projectile proj) {
		plugin.mProjectileEffectTimers.addEntity(proj, Particle.WAX_OFF);
	}
}

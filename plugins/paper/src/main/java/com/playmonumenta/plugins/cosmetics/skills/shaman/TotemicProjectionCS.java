package com.playmonumenta.plugins.cosmetics.skills.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class TotemicProjectionCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.TOTEMIC_PROJECTION;
	}

	@Override
	public Material getDisplayItem() {
		return Material.ENDER_PEARL;
	}

	public void totemCast(Player player, Projectile proj, String totemName) {
		projectionCast(player, proj, null);
	}

	public void projectionCast(Player player, Projectile proj, @Nullable List<LivingEntity> totems) {
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 1.0f, 0.25f);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (!proj.isValid() || mTicks >= 100) {
					this.cancel();
				} else {
					new PartialParticle(Particle.CLOUD, proj.getLocation()).spawnAsPlayerActive(player);
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	public void projectionCollision(Player player, Location dropCenter, double mRadius, List<LivingEntity> totems) {
		player.playSound(dropCenter, Sound.BLOCK_VINE_BREAK,
			SoundCategory.PLAYERS, 2.0f, 1.0f);
		new PartialParticle(Particle.REVERSE_PORTAL, dropCenter, 20).spawnAsPlayerActive(player);
		player.getWorld().playSound(dropCenter, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE,
			SoundCategory.PLAYERS, 1.0f, 1.7f);
	}

	public void projectionAOE(Player player, Location dropCenter, double radius) {
		new PPCircle(Particle.REVERSE_PORTAL, dropCenter, radius).ringMode(false).countPerMeter(4).spawnAsPlayerActive(player);
	}
}

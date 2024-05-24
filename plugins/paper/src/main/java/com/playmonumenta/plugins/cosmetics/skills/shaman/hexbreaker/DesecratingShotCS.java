package com.playmonumenta.plugins.cosmetics.skills.shaman.hexbreaker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class DesecratingShotCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.DESECRATING_SHOT;
	}

	@Override
	public Material getDisplayItem() {
		return Material.TNT;
	}

	public void desecratingShotTrigger(Player player, LivingEntity enemy) {
		World world = enemy.getWorld();
		Location loc = enemy.getLocation();
		world.playSound(loc, Sound.ENTITY_VEX_DEATH, SoundCategory.PLAYERS, 2.0f, 0.4f);
		world.playSound(loc, Sound.ENTITY_ARROW_HIT, SoundCategory.PLAYERS, 2.0f, 0.6f);
		world.playSound(loc, Sound.ENTITY_ARROW_HIT, SoundCategory.PLAYERS, 2.0f, 1.4f);
		world.playSound(loc, Sound.ENTITY_ARROW_HIT, SoundCategory.PLAYERS, 2.0f, 1.8f);
		world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 2.0f, 0.0f);
		world.playSound(loc, Sound.ENTITY_VEX_HURT, SoundCategory.PLAYERS, 2.0f, 1.2f);
		world.playSound(loc, Sound.ENTITY_PHANTOM_DEATH, SoundCategory.PLAYERS, 0.7f, 0.1f);
	}

	public void desecratingShotEffect(Player player, LivingEntity mob, Plugin plugin) {
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				PPCircle lowerRing = new PPCircle(Particle.SPELL_WITCH, mob.getLocation().clone().add(0, 0.5, 0), 1).count(10).delta(0).extra(0.03);
				lowerRing.spawnAsPlayerActive(player);
				if (mTicks >= 4 * 20 || mob.isDead()) {
					this.cancel();
				}

				mTicks += 5;
			}
		}.runTaskTimer(plugin, 0, 5);
	}
}

package com.playmonumenta.plugins.bosses.spells.lich.undeadplayers;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;

/*
 * Gives it and other Undead within 12 blocks resistance 2 for 10 seconds every 25 second(s).
 */

public class SpellResistance extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;

	public SpellResistance(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
	}

	@Override
	public void run() {
		Location loc = mBoss.getLocation().add(0, 1, 0);
		World world = mBoss.getWorld();
		new PartialParticle(Particle.SPELL_INSTANT, loc, 10, 0.4, 0.4, 0.4, 0.25).spawnAsEnemy();
		world.playSound(mBoss.getLocation(), Sound.BLOCK_ANVIL_USE, SoundCategory.HOSTILE, 2.0f, 0.75f);
		world.playSound(mBoss.getLocation(), Sound.ITEM_SHIELD_BLOCK, SoundCategory.HOSTILE, 2.0f, 0.5f);
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20, 0, false));
		BukkitRunnable run = new BukkitRunnable() {

			@Override
			public void run() {
				new PartialParticle(Particle.SPELL_INSTANT, loc, 25, 0.4, 0.4, 0.4, 1).spawnAsEnemy();
				world.playSound(mBoss.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.HOSTILE, 2.0f, 1.35f);
				world.playSound(mBoss.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.HOSTILE, 1.25f, 1.1f);
				for (LivingEntity e : EntityUtils.getNearbyMobs(loc, 12)) {
					if (e == mBoss) {
						continue;
					}
					e.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 15, 1));
					new PartialParticle(Particle.CRIT_MAGIC, e.getLocation(), 3, 0.1, 0.1, 0.1, 0.125).spawnAsEnemy();
					new PartialParticle(Particle.SPELL_INSTANT, e.getLocation(), 1, 0, 0, 0, 0).spawnAsEnemy();
				}
			}

		};
		run.runTaskLater(mPlugin, 20);
		mActiveRunnables.add(run);
	}

	@Override
	public int cooldownTicks() {
		return 20 * 25;
	}

}


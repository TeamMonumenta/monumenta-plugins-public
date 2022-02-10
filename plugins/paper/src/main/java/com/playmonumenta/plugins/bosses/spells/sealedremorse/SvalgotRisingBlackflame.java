package com.playmonumenta.plugins.bosses.spells.sealedremorse;

import com.playmonumenta.plugins.bosses.bosses.Svalgot;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class SvalgotRisingBlackflame extends Spell {
	private static final double DAMAGE = 34;

	private Plugin mPlugin;
	private LivingEntity mBoss;

	private Svalgot mBossClass;

	public SvalgotRisingBlackflame(Plugin plugin, LivingEntity boss, Svalgot bossClass) {
		mPlugin = plugin;
		mBoss = boss;
		mBossClass = bossClass;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		mBoss.removePotionEffect(PotionEffectType.SLOW);
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 50, 1));
		new BukkitRunnable() {
			public int mTicks = 0;
			@Override
			public void run() {

				Location loc = mBoss.getLocation();
				if (mTicks % 10 == 0) {
					world.playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1, 0f);
				}

				world.spawnParticle(Particle.BLOCK_DUST, loc, 8, 0.4, 0.1, 0.4, 0.25, Material.BONE_BLOCK.createBlockData());
				world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 2, 0.25, 0.1, 0.25, 0.25);
				world.spawnParticle(Particle.SPELL_WITCH, loc, 2, 0.25, 0.1, 0.25, 0.25);
				world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 1, 0.25, 0.1, 0.25, 0.25);

				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}
				if (mTicks >= 30) {
					this.cancel();

					world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.5f, 2);
					world.playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.5f, 1);
					world.spawnParticle(Particle.BLOCK_DUST, loc, 250, 3, 0.1, 3, 0.25, Material.BONE_BLOCK.createBlockData());
					world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 100, 3, 0.1, 3, 0.25);
					world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 75, 3, 0.1, 3, 0.25);
					for (Player player : PlayerUtils.playersInRange(loc, 4, true)) {
						BossUtils.blockableDamage(mBoss, player, DamageType.MAGIC, DAMAGE, "Rising Blackflame", loc);
						MovementUtils.knockAway(loc, player, 0.50f, 1f, false);
						player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 10, 2));
					}
				}

				mTicks++;
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int cooldownTicks() {
		return (int) (9 * 20 * mBossClass.mCastSpeed);
	}
}

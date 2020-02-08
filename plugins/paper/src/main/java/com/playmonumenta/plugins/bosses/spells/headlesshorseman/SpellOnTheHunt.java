package com.playmonumenta.plugins.bosses.spells.headlesshorseman;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.MovementUtils;

/*
 * On the Hunt - The Horseman begins marking the player that currently has aggro to be the target of
his hunt (They are warned he is doing so). After 1.5s, For the next 5 seconds the horseman has his
aggro permanently set to that enemy, gaining a burst of speed to run down his mark. If the player is
dealt damage by The Horseman they are skewered through the chest, taking 28/42 damage and being given
antiheal 5 for the next 7 seconds and wither 2 for 5 seconds. (Normal mode) In hard mode the speed is
higher and the charge-up is reduced to 1 seconds.
 */
public class SpellOnTheHunt extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private boolean mActive;

	private static final Particle.DustOptions HUNT_COLOR = new Particle.DustOptions(Color.fromRGB(150, 0, 0), 1.33f);

	public SpellOnTheHunt(Plugin plugin, LivingEntity entity) {
		mPlugin = plugin;
		mBoss = entity;
		mActive = false;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		Creature c = (Creature) mBoss;
		LivingEntity target = c.getTarget();

		if (target instanceof Player) {
			Player player = (Player) target;
			player.sendMessage(ChatColor.RED + "The Headless Horseman has fixated on you for the Hunt. Flee before his wrath!");
		}

		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SPAWN, 3, 1.25f);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITCH_AMBIENT, 3, 0.5f);
		new BukkitRunnable() {
			int t = 0;
			@Override
			public void run() {
				t++;

				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
					mActive = false;
					return;
				}
				world.spawnParticle(Particle.SMOKE_NORMAL, mBoss.getLocation().add(0, 1, 0), 7, 0.4, 0.4, 0.4, 0.025);
				if (t >= 20) {
					world.playSound(mBoss.getLocation(), Sound.ENTITY_HORSE_GALLOP, 3, 0.75f);
					world.playSound(mBoss.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 3, 1.25f);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_IRON_GOLEM_DEATH, 3, 1.75f);
					world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 15, 0.4, 0.4, 0.4, 0.08);
					world.spawnParticle(Particle.SMOKE_NORMAL, mBoss.getLocation().add(0, 1, 0), 80, 0.4, 0.4, 0.4, 0.145);
					world.spawnParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 45, 0.4, 0.4, 0.4, 0.145);
					mActive = true;
					this.cancel();

					new BukkitRunnable() {
						int t = 0;
						@Override
						public void run() {

							if (mBoss.isDead() || !mBoss.isValid()) {
								this.cancel();
								mActive = false;
								return;
							}
							world.spawnParticle(Particle.REDSTONE, mBoss.getLocation().add(0, 1, 0), 5, 0.4, 0.44, 0.4, HUNT_COLOR);
							if (!mActive) {
								mActive = false;
								this.cancel();
								world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 10, 0.4, 0.4, 0.4, 0);
								world.spawnParticle(Particle.SMOKE_NORMAL, mBoss.getLocation().add(0, 1, 0), 65, 0.4, 0.4, 0.4, 0);
								world.spawnParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 45, 0.4, 0.4, 0.4, 0);
								world.playSound(mBoss.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 3, 0.8f);
								world.playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 3, 0.75f);
								return;
							}

							t++;
							c.setTarget(target);
							if (t >= 20 * 5) {
								this.cancel();
								mActive = false;
								world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 10, 0.4, 0.4, 0.4, 0);
								world.spawnParticle(Particle.SMOKE_NORMAL, mBoss.getLocation().add(0, 1, 0), 65, 0.4, 0.4, 0.4, 0);
								world.spawnParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 45, 0.4, 0.4, 0.4, 0);
								world.playSound(mBoss.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 3, 0.8f);
								world.playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 3, 0.75f);
							}
						}

					}.runTaskTimer(mPlugin, 0, 1);
				}

			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public void bossDamagedEntity(EntityDamageByEntityEvent event) {
		if (mActive) {
			mActive = false;
			World world = mBoss.getWorld();
			world.playSound(mBoss.getLocation(), Sound.ITEM_TRIDENT_HIT, 1.5f, 0.85f);
			world.playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.5f, 0.85f);
			world.playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.5f, 0.65f);
			world.spawnParticle(Particle.BLOCK_CRACK, event.getEntity().getLocation().add(0, 1, 0), 45, 0.4, 0.4, 0.4, Material.REDSTONE_WIRE.createBlockData());

			if (event.getEntity() instanceof Player) {
				Player player = (Player) event.getEntity();

				BossUtils.bossDamage(mBoss, player, 42, (bossEvent) -> {
					if (!bossEvent.isPlayerBlocking()) {
						MovementUtils.knockAway(mBoss.getLocation(), player, .6f, .6f);
					}
				});
			}
		}
	}

	@Override
	public int duration() {
		// TODO Auto-generated method stub
		return 20 * 10;
	}

	@Override
	public boolean canRun() {
		if (mBoss instanceof Creature) {
			Creature c = (Creature) mBoss;
			return c.getTarget() != null;
		}
		return false;
	}

}

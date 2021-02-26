package com.playmonumenta.plugins.bosses.spells.varcosamist;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.MovementUtils;

public class SpellDeathlyCharge extends Spell {

	private static final int DAMAGE = 20;

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private boolean mActive;
	private int mDuration;
	private String mDio;

	private static final Particle.DustOptions HUNT_COLOR = new Particle.DustOptions(Color.fromRGB(150, 0, 0), 1.33f);

	public SpellDeathlyCharge(Plugin plugin, LivingEntity entity, int duration, String dio) {
		mPlugin = plugin;
		mBoss = entity;
		mActive = false;
		mDuration = duration;
		mDio = dio;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		Creature c = (Creature) mBoss;
		LivingEntity target = c.getTarget();

		if (target instanceof Player) {
			Player player = (Player) target;
			player.sendMessage(ChatColor.RED + mDio);
		}
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 30, 1));
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SPAWN, 3, 1.25f);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITCH_AMBIENT, 3, 0.5f);
		new BukkitRunnable() {
			int mT = 0;
			@Override
			public void run() {
				mT++;

				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
					mActive = false;
					return;
				}
				world.spawnParticle(Particle.SMOKE_NORMAL, mBoss.getLocation().add(0, 1, 0), 7, 0.4, 0.4, 0.4, 0.025);
				if (mT >= 20) {
					world.playSound(mBoss.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 3, 0.75f);
					world.playSound(mBoss.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 3, 1.25f);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_IRON_GOLEM_DEATH, 3, 1.75f);
					world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 15, 0.4, 0.4, 0.4, 0.08);
					world.spawnParticle(Particle.SMOKE_NORMAL, mBoss.getLocation().add(0, 1, 0), 80, 0.4, 0.4, 0.4, 0.145);
					world.spawnParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 45, 0.4, 0.4, 0.4, 0.145);
					mActive = true;
					this.cancel();

					new BukkitRunnable() {
						int mT = 0;
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

							mT++;
							c.setTarget(target);
							if (mT >= mDuration / 1.5) {
								this.cancel();
								mActive = false;
								world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 10, 0.4, 0.4, 0.4, 0);
								world.spawnParticle(Particle.SMOKE_NORMAL, mBoss.getLocation().add(0, 1, 0), 65, 0.4, 0.4, 0.4, 0);
								world.spawnParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 45, 0.4, 0.4, 0.4, 0);
								world.playSound(mBoss.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 3, 0.8f);
								world.playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 3, 0.75f);
								mBoss.removePotionEffect(PotionEffectType.SPEED);
							}
						}

					}.runTaskTimer(mPlugin, 0, 1);
				}

			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public void bossDamagedEntity(EntityDamageByEntityEvent event) {
		if (mActive && event.getEntity() instanceof Player) {
			mActive = false;
			Player player = (Player) event.getEntity();
			World world = mBoss.getWorld();
			Location loc = mBoss.getLocation();

			world.playSound(loc, Sound.ITEM_TRIDENT_HIT, 1.5f, 0.85f);
			world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.5f, 0.85f);
			world.playSound(loc, Sound.ENTITY_PLAYER_HURT, 1.5f, 0.65f);
			world.spawnParticle(Particle.BLOCK_CRACK, player.getLocation().add(0, 1, 0), 45, 0.4, 0.4, 0.4, Material.REDSTONE_WIRE.createBlockData());
			player.setNoDamageTicks(0);
			BossUtils.bossDamage(mBoss, player, DAMAGE);
			AbilityUtils.silencePlayer(player, 5 * 20);
			player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20 * 5, 1));
			if (BossUtils.bossDamageBlocked(player, DAMAGE, loc)) {
				MovementUtils.knockAway(mBoss.getLocation(), player, .6f, .6f);
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return mDuration;
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

package com.playmonumenta.plugins.bosses.spells.varcosamist;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

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
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 3, 1.25f);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITCH_AMBIENT, SoundCategory.HOSTILE, 3, 0.5f);
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
				new PartialParticle(Particle.SMOKE_NORMAL, mBoss.getLocation().add(0, 1, 0), 7, 0.4, 0.4, 0.4, 0.025).spawnAsEntityActive(mBoss);
				if (mT >= 20) {
					world.playSound(mBoss.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.HOSTILE, 3, 0.75f);
					world.playSound(mBoss.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 3, 1.25f);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.HOSTILE, 3, 1.75f);
					new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 15, 0.4, 0.4, 0.4, 0.08).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.SMOKE_NORMAL, mBoss.getLocation().add(0, 1, 0), 80, 0.4, 0.4, 0.4, 0.145).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 45, 0.4, 0.4, 0.4, 0.145).spawnAsEntityActive(mBoss);
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
							new PartialParticle(Particle.REDSTONE, mBoss.getLocation().add(0, 1, 0), 5, 0.4, 0.44, 0.4, HUNT_COLOR).spawnAsEntityActive(mBoss);
							if (!mActive) {
								mActive = false;
								this.cancel();
								new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 10, 0.4, 0.4, 0.4, 0).spawnAsEntityActive(mBoss);
								new PartialParticle(Particle.SMOKE_NORMAL, mBoss.getLocation().add(0, 1, 0), 65, 0.4, 0.4, 0.4, 0).spawnAsEntityActive(mBoss);
								new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 45, 0.4, 0.4, 0.4, 0).spawnAsEntityActive(mBoss);
								world.playSound(mBoss.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.HOSTILE, 3, 0.8f);
								world.playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 3, 0.75f);
								return;
							}

							mT++;
							c.setTarget(target);
							if (mT >= mDuration / 1.5) {
								this.cancel();
								mActive = false;
								new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 10, 0.4, 0.4, 0.4, 0).spawnAsEntityActive(mBoss);
								new PartialParticle(Particle.SMOKE_NORMAL, mBoss.getLocation().add(0, 1, 0), 65, 0.4, 0.4, 0.4, 0).spawnAsEntityActive(mBoss);
								new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, 1, 0), 45, 0.4, 0.4, 0.4, 0).spawnAsEntityActive(mBoss);
								world.playSound(mBoss.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.HOSTILE, 3, 0.8f);
								world.playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 3, 0.75f);
								mBoss.removePotionEffect(PotionEffectType.SPEED);
							}
						}

					}.runTaskTimer(mPlugin, 0, 1);
				}

			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (mActive && damagee instanceof Player player) {
			mActive = false;
			World world = mBoss.getWorld();
			Location loc = mBoss.getLocation();

			world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.HOSTILE, 1.5f, 0.85f);
			world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.HOSTILE, 1.5f, 0.85f);
			world.playSound(loc, Sound.ENTITY_PLAYER_HURT, SoundCategory.HOSTILE, 1.5f, 0.65f);
			new PartialParticle(Particle.BLOCK_CRACK, player.getLocation().add(0, 1, 0), 45, 0.4, 0.4, 0.4, Material.REDSTONE_WIRE.createBlockData()).spawnAsEntityActive(mBoss);
			player.setNoDamageTicks(0);
			BossUtils.blockableDamage(mBoss, player, DamageType.MELEE, DAMAGE, "Deathly Charge", mBoss.getLocation());
			AbilityUtils.silencePlayer(player, 5 * 20);
			player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20 * 5, 1));
			if (BossUtils.bossDamageBlocked(player, loc)) {
				MovementUtils.knockAway(mBoss.getLocation(), player, 0.6f, 0.6f, false);
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return mDuration;
	}

	@Override
	public boolean canRun() {
		if (mBoss instanceof Mob mob) {
			return mob.getTarget() != null;
		}
		return false;
	}

}

package com.playmonumenta.plugins.specializations.objects;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

public class ElementalSpirit {

	private final Player player;

	private World mWorld;
	private List<LivingEntity> hurt = new ArrayList<LivingEntity>();
	private MagicType magic = MagicType.FIRE;

	public ElementalSpirit(Player player) {
		this.player = player;
		this.mWorld = player.getWorld();
	}

	public Player getPlayer() {
		return player;
	}

	public void setMagicType(MagicType type) {
		this.magic = type;
	}

	public MagicType getMagicType() {
		return magic;
	}

	public List<LivingEntity> getHurt() {
		return hurt;
	}

	/*
	 * Elemental Spirit: You are accompanied by a spirit of elemental energy.
	 * Upon using a spell, the spirit will rush towards the nearest enemy hit by
	 * the spell, and remain at it for 3 seconds. Based on what kind of spell
	 * was used, the effect will differ.
	 *
	 * If the spell was a fire based spell, the spirit rushes in a fiery path
	 * towards that enemy, dealing 3/6 damage to each enemy it passes on the way
	 * and continues to go for 5 blocks.
	 *
	 * If the spell was an ice based spell, the spirit rushes to that enemy,
	 * dealing 4/5 damage when it arrives at the target, and dealing an
	 * additional 1/2 damage per 4s while it remains at the target.
	 *
	 * If the spell was an arcane spell, the spirit warps to that enemy, deals
	 * 2/4 damage, and inflicts all enemies within 3 blocks with Fire and
	 * Slowness 1 for 3 seconds. (6s cooldown)
	 */
	public void damage(Player damager, LivingEntity tar, Location loc) {
		int elementalSpirit = ScoreboardUtils.getScoreboardValue(player, "ElementalSpirit");

		if (magic == MagicType.FIRE) {
			mWorld.spawnParticle(Particle.FLAME, loc, 50, 0.1, 0.1, 0.1, 0.25);
			mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc, 25, 0.1, 0.1, 0.1, 0.1);
			loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1, 1.25f);
			double dmg = elementalSpirit == 1 ? 3 : 6;
			new BukkitRunnable() {
				Vector permDir = null;
				int t = 0;

				@Override
				public void run() {

					if (permDir == null) {
						Vector dir = LocationUtils.getDirectionTo(LocationUtils.getEntityCenter(tar), loc);
						loc.add(dir);
						if (tar.isDead())
							permDir = dir;
					} else
						loc.add(permDir);

					for (LivingEntity e : EntityUtils.getNearbyMobs(loc, 0.9)) {
						EntityUtils.damageEntity(Plugin.getInstance(), e, dmg, player);
						e.setFireTicks(20 * 5);
					}
					mWorld.spawnParticle(Particle.FLAME, loc, 11, 0.75, 0.75, 0.75, 0.025);
					mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc, 5, 0.75, 0.75, 0.75, 0.025);
					if (permDir == null) {
						if (loc.distance(LocationUtils.getEntityCenter(tar)) < 1) {
							Vector dir = LocationUtils.getDirectionTo(LocationUtils.getEntityCenter(tar), loc);
							permDir = dir;
							EntityUtils.damageEntity(Plugin.getInstance(), tar, dmg, player);
							mWorld.spawnParticle(Particle.FLAME, loc, 50, 0.1, 0.1, 0.1, 0.25);
							mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc, 25, 0.1, 0.1, 0.1, 0.1);
							loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1, 1.25f);
						}
					}
					if (permDir != null) {
						t++;
						if (t >= 40)
							this.cancel();
					}

				}

			}.runTaskTimer(Plugin.getInstance(), 0, 1);
		} else if (magic == MagicType.ICE) {
			mWorld.spawnParticle(Particle.SNOWBALL, loc, 25, 0.1, 0.1, 0.1, 0.025);
			mWorld.spawnParticle(Particle.CLOUD, loc, 10, 0.1, 0.1, 0.1, 0.2);
			loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1, 0.65f);
			double dmg = elementalSpirit == 1 ? 4 : 6;
			double linger = elementalSpirit == 1 ? 1 : 2;
			new BukkitRunnable() {
				@Override
				public void run() {
					if (!tar.isDead()) {
						Vector dir = LocationUtils.getDirectionTo(LocationUtils.getEntityCenter(tar), loc);
						loc.add(dir.clone().multiply(0.65));
						mWorld.spawnParticle(Particle.SNOWBALL, loc, 4, 0.1, 0.1, 0.1, 0.025);
						mWorld.spawnParticle(Particle.CLOUD, loc, 1, 0.1, 0.1, 0.1, 0.025);
						if (tar.isDead()) {
							this.cancel();
						}

						if (loc.distance(LocationUtils.getEntityCenter(tar)) < 1) {
							this.cancel();
							EntityUtils.damageEntity(Plugin.getInstance(), tar, dmg, player);
							mWorld.spawnParticle(Particle.SNOWBALL, loc, 25, 0.1, 0.1, 0.1, 0.025);
							mWorld.spawnParticle(Particle.CLOUD, loc, 10, 0.1, 0.1, 0.1, 0.2);
							loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1, 0.65f);
							new BukkitRunnable() {
								int t = 0;

								@Override
								public void run() {
									t++;
									EntityUtils.damageEntity(Plugin.getInstance(), tar, linger, player);
									mWorld.spawnParticle(Particle.SNOWBALL, tar.getLocation().add(0, 1, 0), 25, 0.1,
											0.1, 0.1, 0.025);
									mWorld.spawnParticle(Particle.CLOUD, tar.getLocation().add(0, 1, 0), 10, 0.1, 0.1,
											0.1, 0.2);
									loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1, 1.35f);
									if (t >= 3 || tar.isDead())
										this.cancel();
								}
							}.runTaskTimer(Plugin.getInstance(), 20, 20);
						}
					} else
						this.cancel();

				}

			}.runTaskTimer(Plugin.getInstance(), 0, 1);
		} else if (magic == MagicType.ARCANE) {
			double dmg = elementalSpirit == 1 ? 2 : 4;
			mWorld.spawnParticle(Particle.SPELL_WITCH, loc, 50, 0.1, 0.1, 0.1, 0.025);
			mWorld.spawnParticle(Particle.DRAGON_BREATH, loc, 20, 0.1, 0.1, 0.1, 0.15);
			loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1.35f);

			new BukkitRunnable() {

				@Override
				public void run() {
					mWorld.spawnParticle(Particle.DRAGON_BREATH, LocationUtils.getEntityCenter(tar), 250, 0, 0, 0,
							0.35);
					mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, LocationUtils.getEntityCenter(tar), 50, 0, 0, 0,
							0.2);
					loc.getWorld().playSound(LocationUtils.getEntityCenter(tar), Sound.ENTITY_GENERIC_EXPLODE, 1,
							1.25f);
					EntityUtils.damageEntity(Plugin.getInstance(), tar, dmg, player);
					for (LivingEntity e : EntityUtils.getNearbyMobs(LocationUtils.getEntityCenter(tar), 3)) {
						e.setFireTicks(20 * 3);
						e.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 3, 1, false, true));
					}
				}

			}.runTaskLater(Plugin.getInstance(), 10);
		}
	}

}

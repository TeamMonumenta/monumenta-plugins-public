package com.playmonumenta.plugins.abilities.mage.elementalist;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.events.CustomDamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;


public class ElementalSpirit extends Ability {
	public static class Spirit {
		private final Player mPlayer;
		private final World mWorld;
		private List<LivingEntity> mHurt = new ArrayList<LivingEntity>();
		private MagicType mMagic = MagicType.FIRE;

		public Spirit(Player player) {
			this.mPlayer = player;
			this.mWorld = player.getWorld();
		}

		public Player getPlayer() {
			return mPlayer;
		}

		public void setMagicType(MagicType type) {
			this.mMagic = type;
		}

		public MagicType getMagicType() {
			return mMagic;
		}

		public List<LivingEntity> getHurt() {
			return mHurt;
		}

		/*
		 * Elemental Spirit: You are accompanied by a spirit of
		 * elemental energy. Upon using a spell, the spirit will
		 * rush towards the nearest enemy hit by the spell, and
		 * remain at it for 3 seconds (with the exception of the
		 * fire spirit, which remains in the spot past the enemy).
		 * Based on what kind of spell was used, the effect will differ.
		 *
		 * If the spell was a fire based spell, the spirit rushes
		 * a fiery path towards that enemy, dealing 5/10 damage to
		 * each enemy it passes on the way and continues to go for 2 seconds.
		 *
		 * If the spell was an ice based spell, the spirit rushes
		 * to that enemy, dealing 7/10 damage when it arrives at
		 * the target, and dealing an additional 1/2 damage per
		 * 3s while it remains at the target.
		 *
		 * If the spell was an arcane spell, the spirit warps to
		 * that enemy, deals 4/8 damage to that target, and inflicts
		 * all enemies within 3 blocks with Fire and Slowness 2
		 * for 3 seconds.
		 * (4s cooldown)
		 */
		public void damage(Player damager, LivingEntity tar, Location loc) {
			int elementalSpirit = ScoreboardUtils.getScoreboardValue(mPlayer, "ElementalSpirit");

			if (mMagic == MagicType.FIRE) {
				mWorld.spawnParticle(Particle.FLAME, loc, 50, 0.1, 0.1, 0.1, 0.25);
				mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc, 25, 0.1, 0.1, 0.1, 0.1);
				loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1, 1.25f);
				double dmg = elementalSpirit == 1 ? 5 : 10;
				new BukkitRunnable() {
					Vector mPermDir = null;
					int mTicks = 0;

					@Override
					public void run() {

						if (mPermDir == null) {
							Vector dir = LocationUtils.getDirectionTo(LocationUtils.getEntityCenter(tar), loc);
							loc.add(dir);
							if (tar.isDead()) {
								mPermDir = dir;
							}
						} else {
							loc.add(mPermDir);
						}

						for (LivingEntity e : EntityUtils.getNearbyMobs(loc, 0.9, mPlayer)) {
							EntityUtils.damageEntity(Plugin.getInstance(), e, dmg, mPlayer, MagicType.FIRE, false);
							EntityUtils.applyFire(Plugin.getInstance(), 20 * 5, e);
						}
						mWorld.spawnParticle(Particle.FLAME, loc, 11, 0.75, 0.75, 0.75, 0.025);
						mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc, 5, 0.75, 0.75, 0.75, 0.025);
						if (mPermDir == null) {
							if (loc.distance(LocationUtils.getEntityCenter(tar)) < 1) {
								Vector dir = LocationUtils.getDirectionTo(LocationUtils.getEntityCenter(tar), loc);
								mPermDir = dir;
								EntityUtils.damageEntity(Plugin.getInstance(), tar, dmg, mPlayer, MagicType.FIRE, false);
								mWorld.spawnParticle(Particle.FLAME, loc, 50, 0.1, 0.1, 0.1, 0.25);
								mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc, 25, 0.1, 0.1, 0.1, 0.1);
								loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1, 1.25f);
							}
						}
						if (mPermDir != null) {
							mTicks++;
							if (mTicks >= 40) {
								this.cancel();
							}
						}

					}

				}.runTaskTimer(Plugin.getInstance(), 0, 1);
			} else if (mMagic == MagicType.ICE) {
				mWorld.spawnParticle(Particle.SNOWBALL, loc, 25, 0.1, 0.1, 0.1, 0.025);
				mWorld.spawnParticle(Particle.CLOUD, loc, 10, 0.1, 0.1, 0.1, 0.2);
				loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1, 0.65f);
				double dmg = elementalSpirit == 1 ? 7 : 10;
				double linger = elementalSpirit == 1 ? 2 : 3;
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
								EntityUtils.damageEntity(Plugin.getInstance(), tar, dmg, mPlayer, MagicType.ICE, false);
								mWorld.spawnParticle(Particle.SNOWBALL, loc, 25, 0.1, 0.1, 0.1, 0.025);
								mWorld.spawnParticle(Particle.CLOUD, loc, 10, 0.1, 0.1, 0.1, 0.2);
								loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1, 0.65f);
								new BukkitRunnable() {
									int mTicks = 0;

									@Override
									public void run() {
										mTicks++;
										EntityUtils.damageEntity(Plugin.getInstance(), tar, linger, mPlayer, MagicType.ICE, false);
										mWorld.spawnParticle(Particle.SNOWBALL, tar.getLocation().add(0, 1, 0), 25, 0.1,
															 0.1, 0.1, 0.025);
										mWorld.spawnParticle(Particle.CLOUD, tar.getLocation().add(0, 1, 0), 10, 0.1, 0.1,
															 0.1, 0.2);
										loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1, 1.35f);
										if (mTicks >= 3 || tar.isDead()) {
											this.cancel();
										}
									}
								}.runTaskTimer(Plugin.getInstance(), 20, 20);
							}
						} else {
							this.cancel();
						}

					}

				}.runTaskTimer(Plugin.getInstance(), 0, 1);
			} else if (mMagic == MagicType.ARCANE) {
				double dmg = elementalSpirit == 1 ? 4 : 8;
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
						EntityUtils.damageEntity(Plugin.getInstance(), tar, dmg, mPlayer, MagicType.ARCANE, false);
						for (LivingEntity e : EntityUtils.getNearbyMobs(LocationUtils.getEntityCenter(tar), 3, mPlayer)) {
							EntityUtils.applyFire(Plugin.getInstance(), 20 * 3, e);
							e.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 3, 1, false, true));
						}
					}

				}.runTaskLater(Plugin.getInstance(), 10);
			}
		}
	}

	private BukkitRunnable mSpiritRunnable = null;
	private Spirit mSpirit = null;

	public ElementalSpirit(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Elemental Spirit");
		mInfo.scoreboardId = "ElementalSpirit";
		mInfo.linkedSpell = Spells.ELEMENTAL_SPIRIT;
		mInfo.cooldown = 20 * 8;
	}

	@Override
	public void playerDealtCustomDamageEvent(CustomDamageEvent event) {
		if (mSpirit != null) {
			MagicType type = event.getMagicType();
			if (type == MagicType.FIRE || type == MagicType.ARCANE || type == MagicType.ICE) {
				mSpirit.setMagicType(type);
				mSpirit.getHurt().add(event.getDamaged());
			}
		}
	}

	@Override
	public void periodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		Player player = mPlayer;
		if (oneSecond) {
			int elementalSpirit = getAbilityScore();

			if (elementalSpirit > 0) {
				if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.ELEMENTAL_SPIRIT)) {
					if (mSpirit == null) {
						mSpirit = new Spirit(player);
						mSpiritRunnable = new BukkitRunnable() {
							float mVertAngle = 0f;
							double mRotation = 0;

							@Override
							public void run() {
								Location loc = player.getLocation().add(0, 1, 0);
								mVertAngle += 0.1f;
								mRotation += 10;
								double radian1 = Math.toRadians(mRotation);
								loc.add(Math.cos(radian1), Math.sin(mVertAngle) * 0.5, Math.sin(radian1));

								// Don't display particles to player if they're in their face
								if (loc.clone().subtract(mPlayer.getLocation().add(0, 1, 0)).toVector().normalize().dot(mPlayer.getEyeLocation().getDirection()) > 0.33) {
									for (Player other : PlayerUtils.playersInRange(mPlayer, 30, false)) {
										other.spawnParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0.01);
										other.spawnParticle(Particle.SNOWBALL, loc, 3, 0, 0, 0, 0);
									}
								} else {
									mWorld.spawnParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0.01);
									mWorld.spawnParticle(Particle.SNOWBALL, loc, 3, 0, 0, 0, 0);
								}

								if (AbilityManager.getManager().getPlayerAbility(mPlayer, ElementalSpirit.class) == null ||
									!mPlayer.isOnline() || mPlayer == null || mSpirit == null || mPlayer.isDead() ||
									mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.ELEMENTAL_SPIRIT)) {
									this.cancel();
									mSpirit = null;
								}

								if (mSpirit != null && mSpirit.getHurt().size() > 0) {
									List<LivingEntity> list = mSpirit.getHurt();

									LivingEntity target = null;
									if (list.size() > 0) {
										double dist = 100;
										for (LivingEntity e : list) {
											if (e.getLocation().distance(player.getLocation()) < dist) {
												dist = e.getLocation().distance(player.getLocation());
												target = e;
											}
										}
									}
									if (target != null) {
										mSpirit.damage(player, target, loc.clone());
										putOnCooldown();
									}
									mSpirit = null;
									this.cancel();
								}
								loc.subtract(Math.cos(radian1), Math.sin(mVertAngle) * 0.5, Math.sin(radian1));
							}

						};

						mSpiritRunnable.runTaskTimer(mPlugin, 0, 1);
					}
				}
			}
		}
	}

	@Override
	public void invalidate() {
		mSpirit = null;
		if (mSpiritRunnable != null && mSpiritRunnable.isCancelled()) {
			mSpiritRunnable.cancel();
		}
	}
}

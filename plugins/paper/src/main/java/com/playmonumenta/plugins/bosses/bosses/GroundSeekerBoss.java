package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class GroundSeekerBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_groundseeker";

	public static class Parameters extends BossParameters {
		@BossParam(help = "detection radius")
		public int DETECTION = 40;
		@BossParam(help = "how often it can use the ability")
		public int COOLDOWN = 10 * 20;
		@BossParam(help = "delay before ability becomes online")
		public int DELAY = 2 * 20;
		@BossParam(help = "name of this spell")
		public String SPELL_NAME = "Ground Seeker";
		@BossParam(help = "targets of the spell")
		public EntityTargets TARGETS = EntityTargets.GENERIC_PLAYER_TARGET.clone().setFilters(List.of(EntityTargets.PLAYERFILTER.NOT_STEALTHED));
		@BossParam(help = "spell delay")
		public int SPELL_DELAY = 2 * 20;
		@BossParam(help = "whether the mob can move while charging up or not")
		public boolean CAN_MOVE = false;
		@BossParam(help = "how long the seeker travels for")
		public int DURATION = 100;
		@BossParam(help = "speed of the seeker")
		public double SPEED = 0.4;
		@BossParam(help = "how aggressively it tracks")
		public double TURN_SPEED = 0.078;
		@BossParam(help = "damage the rift will deal on direct contact")
		public double DAMAGE = 0;
		@BossParam(help = "% damage the rift will deal on direct contact")
		public double DAMAGE_PERCENTAGE = 0;
		@BossParam(help = "effects applied on hit")
		public EffectsList EFFECTS = EffectsList.EMPTY;
		@BossParam(help = "LOS name of the mob when the seeker hits its target")
		public LoSPool SPAWNED_MOB_POOL = LoSPool.LibraryPool.EMPTY;
		@BossParam(help = "particles around the boss while it telegraphs")
		public ParticlesList PARTICLE_CHARGE = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.CRIT, 8, 1.0, 0.1, 1.0, 0.25))
			.build();
		@BossParam(help = "particles created in the telegraphed line")
		public ParticlesList PARTICLE_LAUNCH = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.EXPLOSION_LARGE, 1, 0.0, 0.0, 0.0, 0.0))
			.build();
		@BossParam(help = "particles spawned as the rift is created")
		public ParticlesList PARTICLE_PROJECTILE = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.BLOCK_CRACK, 10, 0.25, 0.25, 0.25, 0.25, Material.ROOTED_DIRT.createBlockData()))
			.build();
		@BossParam(help = "particle above rift while it lingers")
		public ParticlesList PARTICLE_HIT = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.EXPLOSION_LARGE, 1, 0.0, 0.0, 0.0, 0.1))
			.build();
		@BossParam(help = "sound played when the mob starts telegraphing")
		public SoundsList SOUND_WARN = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_RAVAGER_ROAR, 1.1f, 0.5f))
			.build();
		@BossParam(help = "sound played as the seeker first shoots out")
		public SoundsList SOUND_LAUNCH = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0f, 0.75f))
			.build();
		@BossParam(help = "sound played every 2 ticks when the seeker travels")
		public SoundsList SOUND_SEEKER = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.BLOCK_GRASS_BREAK, 0.6f, 0.5f))
			.build();
		@BossParam(help = "sound played when the seeker hits")
		public SoundsList SOUND_HIT = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.BLOCK_POINTED_DRIPSTONE_LAND, 1.1f, 0.5f))
			.build();
	}

	public GroundSeekerBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		GroundSeekerBoss.Parameters p = GroundSeekerBoss.Parameters.getParameters(boss, identityTag, new GroundSeekerBoss.Parameters());

		Spell spell = new Spell() {
			@Override
			public void run() {
				World world = mBoss.getWorld();
				p.SOUND_WARN.play(mBoss.getLocation());

				List<? extends LivingEntity> targets = p.TARGETS.getTargetsList(mBoss);

				if (!p.CAN_MOVE) {
					EntityUtils.selfRoot(mBoss, p.SPELL_DELAY);
				}

				BukkitRunnable runnable = new BukkitRunnable() {
					int mT = 0;

					@Override
					public void run() {
						if (EntityUtils.shouldCancelSpells(mBoss)) {
							this.cancel();
							EntityUtils.cancelSelfRoot(mBoss);
						}

						p.PARTICLE_CHARGE.spawn(mBoss, mBoss.getLocation().clone().add(0, 1, 0));
						mT++;

						if (mT >= p.SPELL_DELAY) {
							this.cancel();
							launchSeeker(mBoss.getLocation(), targets, world);
						}
					}
				};
				mActiveRunnables.add(runnable);
				runnable.runTaskTimer(mPlugin, 0, 1);
			}

			private void launchSeeker(Location loc, List<? extends LivingEntity> players, World world) {
				for (LivingEntity player : players) {
					Vector dirToPlayer = player.getLocation().clone().toVector().add(new Vector(0, 1.5, 0)).subtract(loc.clone().toVector()).normalize();

					//thanks eliux
					BukkitRunnable runnable = new BukkitRunnable() {
						int mTicks = 0;
						Location mGroundLoc = loc.clone();

						final Location mLocation = loc.clone().add(0, 0.25, 0);
						final Vector mToPlayer = dirToPlayer.clone().multiply(p.SPEED);

						@Override
						public void run() {
							if (mTicks == 0) {
								p.SOUND_LAUNCH.play(mBoss.getLocation());
								p.PARTICLE_LAUNCH.spawn(mBoss, mBoss.getLocation().clone().add(0, 1, 0));
							}

							// move the projectile
							Vector angleDelta = mToPlayer.clone().crossProduct(mToPlayer.clone().crossProduct(player.getLocation().clone().toVector().add(new Vector(0, 1.5, 0)).subtract(mLocation.clone().toVector()))).normalize().multiply(p.TURN_SPEED);
							mToPlayer.add(angleDelta.multiply(-1)).normalize().multiply(p.SPEED);
							mLocation.add(mToPlayer);
							mGroundLoc = LocationUtils.fallToGround(mLocation, 0);

							if (mTicks % 2 == 0) {
								p.SOUND_SEEKER.play(mGroundLoc);
							}

							// check hitboxes
							Hitbox hitbox = new Hitbox.AABBHitbox(world, BoundingBox.of(mGroundLoc, 0.25, 0.25, 0.25));
							if (hitbox.getHitEntities((entity) -> !entity.equals(boss)).contains(player)) {
								p.PARTICLE_HIT.spawn(mBoss, player.getLocation());
								p.SOUND_HIT.play(player.getLocation());

								if (p.DAMAGE > 0) {
									BossUtils.blockableDamage(boss, player, DamageEvent.DamageType.MAGIC, p.DAMAGE, p.SPELL_NAME, loc, p.EFFECTS.mEffectList());
								}

								if (p.DAMAGE_PERCENTAGE > 0.0) {
									BossUtils.bossDamagePercent(mBoss, player, p.DAMAGE_PERCENTAGE, loc, p.SPELL_NAME, p.EFFECTS.mEffectList());
								}
								p.EFFECTS.apply(player, boss);

								if (!ZoneUtils.hasZoneProperty(player.getLocation(), ZoneUtils.ZoneProperty.NO_SUMMONS)) {
									Entity spawn = p.SPAWNED_MOB_POOL.spawn(player.getLocation());
									if (spawn != null) {
										summonPlugins(spawn);
									}
								}
								this.cancel();
							}

							p.PARTICLE_PROJECTILE.spawn(mBoss, mGroundLoc);

							mTicks++;
							if (mTicks > p.DURATION || mBoss.isDead()) {
								this.cancel();
							}
						}
					};
					mActiveRunnables.add(runnable);
					runnable.runTaskTimer(mPlugin, 0, 1);
				}
			}

			@Override
			public int cooldownTicks() {
				return p.COOLDOWN;
			}

			@Override
			public boolean canRun() {
				return !p.TARGETS.getTargetsList(mBoss).isEmpty();
			}
		};
		super.constructBoss(spell, p.DETECTION, null, p.DELAY);
	}

	public void summonPlugins(Entity summon) {

	}
}

package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class RiftBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_rift";

	public static class Parameters extends BossParameters {
		@BossParam(help = "detection radius")
		public int DETECTION = 40;
		@BossParam(help = "how often it can use the ability")
		public int COOLDOWN = 10 * 20;
		@BossParam(help = "delay before ability becomes online")
		public int DELAY = 2 * 20;
		@BossParam(help = "name of this spell")
		public String SPELL_NAME = "";
		@BossParam(help = "who will the mob create rifts towrads")
		public EntityTargets TARGETS = EntityTargets.GENERIC_PLAYER_TARGET.clone().setFilters(List.of(EntityTargets.PLAYERFILTER.NOT_STEALTHED));
		@BossParam(help = "duration the mob will charge the rift for")
		public int DURATION = 2 * 20;
		@BossParam(help = "whether the mob can move while charging up or not")
		public boolean CAN_MOVE = false;
		@BossParam(help = "max distance the rift will travel")
		public int MAX_RANGE = 50;
		@BossParam(help = "if lines > 1, will create multiple rifts at angles spread apart by splitangle")
		public int LINES = 1;
		@BossParam(help = "if lines > 1, the angle at which each line will be spread apart")
		public double SPLIT_ANGLE = 30;
		@BossParam(help = "tick period at which the rift will grow (higher = rift grows slower)")
		public int RIFT_PERIOD = 1;
		@BossParam(help = "how far in blocks the rift will step each tick (higher = sparser rift)")
		public double RIFT_STEP = 1.25;
		@BossParam(help = "y-offset from where the rift should start")
		public double HEIGHT_OFFSET = 0.5;
		@BossParam(help = "if true, the rift will stay horizontal and not attempt to climb up/down slopes, and will ignore walls")
		public boolean HORIZONTAL_LOCK = false;
		@BossParam(help = "damage the rift will deal on direct contact")
		public double DIRECT_DAMAGE = 0;
		@BossParam(help = "% damage the rift will deal on direct contact")
		public double DIRECT_DAMAGE_PERCENTAGE = 0;
		@BossParam(help = "damage the lingering rift will deal")
		public double LINGERING_DAMAGE = 0;
		@BossParam(help = "% damage the lingering rift will deal")
		public double LINGERING_DAMAGE_PERCENTAGE = 0;
		@BossParam(help = "duration the rift will linger")
		public int LINGERING_DURATION = 6 * 20;
		@BossParam(help = "effects applied on hit")
		public EffectsList EFFECTS = EffectsList.EMPTY;

		@BossParam(help = "block material of the rift")
		public Material MATERIAL = Material.CRYING_OBSIDIAN;

		@BossParam(help = "particles around the boss while it telegraphs")
		public ParticlesList PARTICLE_BOSS_CHARGE = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.CLOUD, 8, 1.0, 0.1, 1.0, 0.25))
			.add(new ParticlesList.CParticle(Particle.SMOKE_LARGE, 5, 1.0, 0.1, 1.0, 0.25))
			.build();
		@BossParam(help = "particles created in the telegraphed line")
		public ParticlesList PARTICLE_RIFT_CHARGE = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.SQUID_INK, 1, 0.25, 0.25, 0.25, 0.0))
			.build();
		@BossParam(help = "particles spawned as the rift is created")
		public ParticlesList PARTICLE_RIFT_GROW = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.CLOUD, 3, 0.5, 0.5, 0.5, 0.25))
			.add(new ParticlesList.CParticle(Particle.EXPLOSION_NORMAL, 3, 0.5, 0.5, 0.5, 0.125))
			.build();
		@BossParam(help = "particle above rift while it lingers")
		public ParticlesList PARTICLE_RIFT_LINGER = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.EXPLOSION_NORMAL, 1, 0.5, 0.5, 0.5, 0.1))
			.add(new ParticlesList.CParticle(Particle.DAMAGE_INDICATOR, 1, 0.5, 0.5, 0.5, 0.1))
			.build();

		@BossParam(help = "sound played when the mob starts telegraphing")
		public SoundsList SOUND_WARN = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_ENDER_DRAGON_GROWL, 3.0f, 0.5f))
			.build();
		@BossParam(help = "sound played while the lines are telegraphed")
		public SoundsList SOUND_CHARGE = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.BLOCK_END_PORTAL_FRAME_FILL, 1.25f, 1.0f))
			.add(new SoundsList.CSound(Sound.ENTITY_ENDER_DRAGON_HURT, 1.25f, 1.0f))
			.build();
		@BossParam(help = "sound played as the rift is created")
		public SoundsList SOUND_RIFT = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.BLOCK_CHAIN_BREAK, 1.0f, 0.85f))
			.build();

	}

	public RiftBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		List<Block> changedBlocks = new ArrayList<>();

		RiftBoss.Parameters p = RiftBoss.Parameters.getParameters(boss, identityTag, new RiftBoss.Parameters());

		Spell spell = new Spell() {
			private final Location mStartLoc = boss.getLocation();

			@Override
			public void run() {
				World world = mBoss.getWorld();
				p.SOUND_WARN.play(mStartLoc);

				List<? extends LivingEntity> players = PlayerUtils.playersInRange(mStartLoc, p.MAX_RANGE * 2, true);
				List<? extends LivingEntity> targets = p.TARGETS.getTargetsList(mBoss);
				List<Location> locs = new ArrayList<>();
				for (LivingEntity target : targets) {
					// treat everything as on the same y-plane for targeting
					Location flatLocation = target.getLocation();
					flatLocation.setY(mBoss.getLocation().getY());
					locs.add(flatLocation);
				}

				if (p.LINES > 1) {
					List<Location> extraLocs = new ArrayList<>();
					for (Location loc : locs) {
						Vector vector = loc.clone().toVector().subtract(mStartLoc.clone().toVector());
						for (int i = 0; i < p.LINES; i++) {
							double angleOffset = (p.SPLIT_ANGLE * Math.PI / 180) * (i - ((p.LINES - 1) / 2.0));
							extraLocs.add(mStartLoc.clone().add(vector.clone().rotateAroundY(angleOffset % (2 * Math.PI))));
						}
					}
					locs.clear();
					locs = extraLocs;
				}

				if (!p.CAN_MOVE) {
					EntityUtils.selfRoot(mBoss, p.DURATION);
				}

				List<Location> finalLocs = locs;
				new BukkitRunnable() {
					double mT = 0;
					float mPitch = 1;
					final Location mLoc = mBoss.getLocation().add(0, p.HEIGHT_OFFSET, 0);
					final double mBossX = mLoc.getX();
					final double mBossY = mLoc.getY();
					final double mBossZ = mLoc.getZ();

					@Override
					public void run() {
						mT += 2;
						mPitch += 0.025f;

						for (Location l : finalLocs) {
							Vector line = LocationUtils.getDirectionTo(l, mLoc).setY(0);
							double xloc = line.getX();
							double yloc = line.getY();
							double zloc = line.getZ();
							for (int i = 1; i < p.MAX_RANGE; i++) {
								Location newLoc = new Location(world, mBossX + (xloc * i), mBossY + (yloc * i), mBossZ + (zloc * i));

								if (newLoc.getBlock().getType() == Material.AIR) {
									p.PARTICLE_RIFT_CHARGE.spawn(mBoss, newLoc);
								} else {
									p.PARTICLE_RIFT_CHARGE.spawn(mBoss, newLoc.add(0, 0.5, 0));
								}
							}
						}

						p.SOUND_CHARGE.play(mLoc, 1.25f, mPitch);
						p.PARTICLE_BOSS_CHARGE.spawn(mBoss, mLoc);

						if (EntityUtils.shouldCancelSpells(mBoss)) {
							this.cancel();
							EntityUtils.cancelSelfRoot(mBoss);
						}

						if (mT >= p.DURATION) {
							this.cancel();

							for (Location l : finalLocs) {
								createRift(l, players);
							}
							EntityUtils.cancelSelfRoot(mBoss);
						}
					}
				}.runTaskTimer(mPlugin, 0, 2);
			}

			private void createRift(Location loc, List<? extends LivingEntity> players) {
				List<Location> locs = new ArrayList<>();

				BukkitRunnable runnable = new BukkitRunnable() {
					final Location mLoc = mBoss.getLocation().add(0, p.HEIGHT_OFFSET, 0);
					final Vector mDir = LocationUtils.getDirectionTo(loc, mLoc).setY(0).normalize();
					final BoundingBox mBox = BoundingBox.of(mLoc, 0.85, 1.2, 0.85);
					final Location mOgLoc = mLoc.clone();

					@Override
					public void run() {
						if (!Double.isFinite(mDir.getX())) {
							mDir.setX(1).setY(0).setZ(0);
						}
						mBox.shift(mDir.clone().multiply(p.RIFT_STEP));
						Location bLoc = mBox.getCenter().toLocation(mLoc.getWorld());

						//Allows the rift to climb up and down blocks
						if (!p.HORIZONTAL_LOCK) {
							if (bLoc.getBlock().getType().isSolid()) {
								bLoc.add(0, 1, 0);
								if (bLoc.getBlock().getType().isSolid()) {
									this.cancel();
									bLoc.subtract(0, 1, 0);
								}
							}

							if (!bLoc.subtract(0, 1, 0).getBlock().getType().isSolid()) {
								bLoc.subtract(0, 1, 0);
								if (!bLoc.getBlock().getType().isSolid()) {
									bLoc.subtract(0, 1, 0);
									if (!bLoc.getBlock().getType().isSolid()) {
										this.cancel();
									}
								}
							}
						}

						if (TemporaryBlockChangeManager.INSTANCE.changeBlock(bLoc.getBlock(), p.MATERIAL, p.LINGERING_DURATION)) {
							changedBlocks.add(bLoc.getBlock());
						}

						bLoc.add(0, 0.5, 0);

						locs.add(bLoc);
						if (bLoc.getBlock().getType() == Material.AIR) {
							p.PARTICLE_RIFT_GROW.spawn(mBoss, bLoc);
						} else {
							p.PARTICLE_RIFT_GROW.spawn(mBoss, bLoc.add(0, 0.5, 0));
						}
						p.SOUND_RIFT.play(bLoc);

						for (LivingEntity target : players) {
							if (target.getBoundingBox().overlaps(mBox)) {
								directHit(target, bLoc);
							}
						}
						if (bLoc.distance(mOgLoc) >= p.MAX_RANGE) {
							this.cancel();
						}
					}

				};

				runnable.runTaskTimer(mPlugin, 0, p.RIFT_PERIOD);
				mActiveRunnables.add(runnable);

				//If touching the "line" of particles, get debuffed and take damaged. Can be blocked over
				new BukkitRunnable() {
					int mT = 0;

					@Override
					public void run() {
						mT += 5;
						for (Location loc : locs) {
							if (loc.getBlock().getType() == Material.AIR) {
								p.PARTICLE_RIFT_LINGER.spawn(mBoss, loc);
							} else {
								p.PARTICLE_RIFT_LINGER.spawn(mBoss, loc.add(0, 0.5, 0));
							}
							BoundingBox box = BoundingBox.of(loc, 0.85, 1.2, 0.85);
							for (LivingEntity target : players) {
								if (target.getBoundingBox().overlaps(box)) {
									lingeringHit(target, loc);
								}
							}
						}

						if (!mBoss.isValid() || mT >= p.LINGERING_DURATION) {
							this.cancel();
						}
					}

				}.runTaskTimer(mPlugin, 0, 5);
			}

			private void directHit(LivingEntity target, Location loc) {
				if (target != null) {
					if (p.DIRECT_DAMAGE > 0) {
						BossUtils.blockableDamage(boss, target, DamageEvent.DamageType.MAGIC, p.DIRECT_DAMAGE, p.SPELL_NAME, loc, p.EFFECTS.mEffectList());
					}

					if (p.DIRECT_DAMAGE_PERCENTAGE > 0.0) {
						BossUtils.bossDamagePercent(mBoss, target, p.DIRECT_DAMAGE_PERCENTAGE, loc, p.SPELL_NAME, p.EFFECTS.mEffectList());
					}
					p.EFFECTS.apply(target, boss);
				}
			}

			private void lingeringHit(LivingEntity target, Location loc) {
				if (target != null) {
					if (p.LINGERING_DAMAGE > 0) {
						BossUtils.blockableDamage(boss, target, DamageEvent.DamageType.MAGIC, p.LINGERING_DAMAGE, p.SPELL_NAME, loc, p.EFFECTS.mEffectList());
					}

					if (p.LINGERING_DAMAGE_PERCENTAGE > 0.0) {
						BossUtils.bossDamagePercent(mBoss, target, p.LINGERING_DAMAGE_PERCENTAGE, loc, p.SPELL_NAME, p.EFFECTS.mEffectList());
					}
					p.EFFECTS.apply(target, boss);
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

			@Override
			public void cancel() {
				super.cancel();

				TemporaryBlockChangeManager.INSTANCE.revertChangedBlocks(changedBlocks, p.MATERIAL);
				changedBlocks.clear();
			}
		};

		super.constructBoss(spell, p.DETECTION, null, p.DELAY);
	}
}

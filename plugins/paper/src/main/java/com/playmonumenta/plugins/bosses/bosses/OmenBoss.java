package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class OmenBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_omen";

	public static class Parameters extends BossParameters {
		@BossParam(help = "spell name")
		public String SPELL_NAME = "Omen";
		@BossParam(help = "telegraph duration in ticks")
		public int TEL_DURATION = 20 * 2;
		@BossParam(help = "max range of the ground lines")
		public double MAX_RANGE = 24;
		@BossParam(help = "width of the omen")
		public int WIDTH = 5;
		@BossParam(help = "velocity of the ground lines")
		public double VELOCITY = 20;
		@BossParam(help = "")
		public int COOLDOWN = 150;
		@BossParam(help = "")
		public int DELAY = 40;
		@BossParam(help = "detection radius")
		public int DETECTION = 40;
		@BossParam(help = "degree offset of the blades")
		public int DEGREE_OFFSET = 0;
		@BossParam(help = "how many branches the omen will split into, all equal in angle in between each other (try not to go below 3 or so as theres no targeting param)")
		public int SPLITS = 4;
		@BossParam(help = "spell damage")
		public double DAMAGE = 20;
		@BossParam(help = "spell damage in %")
		public double DAMAGE_PERCENTAGE = 0;
		@BossParam(help = "The type of the damage dealt by the attack. Default: MAGIC")
		public DamageEvent.DamageType DAMAGE_TYPE = DamageEvent.DamageType.MAGIC;
		@BossParam(help = "effects given on hit")
		public EffectsList EFFECTS = EffectsList.EMPTY;
		@BossParam(help = "width of the gap between omen particles, across the omen horizontally")
		public double PARTICLE_GAP = 1.0;
		@BossParam(help = "whether boss can move during the cast")
		public boolean CAN_MOVE = true;
		@BossParam(help = "sound of omen launch")
		public SoundsList SOUND_LAUNCH = SoundsList.fromString("[(ENTITY_WITHER_SHOOT, 1, 0.75)]");
		@BossParam(help = "sound of getting hit")
		public SoundsList SOUND_HIT = SoundsList.fromString("[(ENTITY_GENERIC_EXTINGUISH_FIRE, 1, 2.0)]");
		@BossParam(help = "sound that plays throughout the telegraph's duration")
		public SoundsList SOUND_TEL = SoundsList.fromString("[(UI_TOAST_IN, 1.5, 1.9)]");
		@BossParam(help = "sound that plays at the start of the spell cast")
		public SoundsList SOUND_WARN = SoundsList.fromString("[(ENTITY_WITHER_AMBIENT, 1.0, 1.0)]");
		@BossParam(help = "particles of the omen telegraph")
		public ParticlesList PARTICLE_TEL_SWIRL = ParticlesList.fromString("[(SOUL_FIRE_FLAME, 1, 0.1, 0.1, 0.1, 0)]");
		@BossParam(help = "particles of the omen telegraph")
		public ParticlesList PARTICLE_TEL = ParticlesList.fromString("[(REDSTONE, 1, 0.1, 0.1, 0.1, 0, RED, 0.8)]");
		@BossParam(help = "particles of the omen")
		public ParticlesList PARTICLE_OMEN = ParticlesList.fromString("[(REDSTONE, 1, 0.1, 0.1, 0.1, 0, #c700ff, 1),(SOUL_FIRE_FLAME, 1, 0.1, 0.1, 0.1, 0)]");

	}

	public OmenBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		OmenBoss.Parameters p = OmenBoss.Parameters.getParameters(boss, identityTag, new OmenBoss.Parameters());

		List<Player> mDamaged = new ArrayList<>();

		Spell spell = new Spell() {
			@Override
			public void run() {
				p.SOUND_WARN.play(mBoss.getLocation());

				BukkitRunnable runA = new BukkitRunnable() {
					double mT = 0.0;
					final List<Vector> mBasevec = new ArrayList<>();

					@Override
					public void run() {
						if (mT == 0) {
							mBasevec.add(new Vector(0, 0, 1));
							mBasevec.add(new Vector(-4, 0, -2).normalize());
							mBasevec.add(new Vector(4, 0, -2).normalize());
							launchBlade(mBasevec, true);
						}
						mT++;
						//4 points swirl into center
						if (mT < p.TEL_DURATION) {
							Vector dir = new Vector(4, 0, 0);
							p.SOUND_TEL.play(mBoss.getLocation());
							for (int i = 0; i < p.SPLITS; i++) {
								Vector shape = VectorUtils.rotateYAxis(dir.clone(), (i * (double)360/p.SPLITS) + p.DEGREE_OFFSET);
								Vector shapeLength = shape.multiply(1 - mT / p.TEL_DURATION);
								Vector shapeDir = VectorUtils.rotateYAxis(shapeLength.clone(), ((double)360/p.SPLITS) / (mT / p.TEL_DURATION));
								Location l = mBoss.getLocation().add(shapeDir).add(0, 0.5, 0);
								p.PARTICLE_TEL_SWIRL.spawn(mBoss, l);
							}
						}

						//blade function
						if (mT >= p.TEL_DURATION) {
							//clear all entries before launching blade
							mDamaged.clear();

							p.SOUND_LAUNCH.play(mBoss.getLocation());

							launchBlade(mBasevec, false);
							this.cancel();

						}
						if (!p.CAN_MOVE) {
							EntityUtils.selfRoot(mBoss, p.TEL_DURATION);
						}
					}

				};
				runA.runTaskTimer(mPlugin, 0, 1);
				mActiveRunnables.add(runA);
			}

			public void launchBlade(List<Vector> basevec, boolean warning) {
				//loop for each direction, starting +Z, clockwise
				for (int i = 0; i <= p.SPLITS; i++) {
					List<Vector> vec = new ArrayList<>(basevec);

					//rotate vectors
					for (int j = 0; j < vec.size(); j++) {
						Vector v = vec.get(j);
						Vector v2 = VectorUtils.rotateYAxis(v, (i * (double)360/p.SPLITS) + p.DEGREE_OFFSET);
						vec.set(j, v2);
					}

					//spawn loc shift up 1 block
					Vector dir = vec.get(0);
					Location startLoc = mBoss.getLocation().add(0, 1, 0).add(dir);
					//launch blade tip
					BukkitRunnable runB = new BukkitRunnable() {
						int mT = 0;

						@Override
						public void run() {
							mT++;
							Location anchor;
							//iterate twice for higher accuracy
							for (int x = 0; x < 2; x++) {
								anchor = startLoc.clone().add(dir.clone().multiply(p.VELOCITY / 20 * (mT + 0.5 * x)));
								if (anchor.distance(startLoc) > p.MAX_RANGE) {
									this.cancel();
								}
									//construct blade
									createBlade(anchor, vec, warning);
							}
						}

					};
					runB.runTaskTimer(mPlugin, 0, 1);
					mActiveRunnables.add(runB);
				}
			}

			public void createBlade(Location startLoc, List<Vector> vec, boolean warning) {
				List<Location> locAll = new ArrayList<>();
				//construct blade
				for (int j = 1; j <= 2; j++) {
					Vector v = vec.get(j);
					//for 1 side, 5 locations
					for (int k = 0; k < p.WIDTH; k++) {
						if (k % p.PARTICLE_GAP == 0) {
							Location l = startLoc.clone();
							l.add(v.clone().multiply(0.25 * k));
							if (!locAll.contains(l)) {
								locAll.add(l);
							}
						}
					}
				}
				//spawn particle + check loc
				List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), p.MAX_RANGE, true);
				List<Player> damage = new ArrayList<>();
				for (Location l : locAll) {
						if (warning) {
							p.PARTICLE_TEL.spawn(mBoss, l);
						} else {
							p.PARTICLE_OMEN.spawn(mBoss, l);
							BoundingBox box = BoundingBox.of(l, 0.3, 0.3, 0.3);
							for (Player p : players) {
								if (p.getBoundingBox().overlaps(box) && !damage.contains(p)) {
									damage.add(p);
								}
							}
						}
				}
				//damage
				if (!warning) {
					for (Player player : damage) {
						if (!mDamaged.contains(player)) {
							mDamaged.add(player);
							p.SOUND_HIT.play(player.getLocation());
							if (p.DAMAGE > 0) {
								BossUtils.blockableDamage(mBoss, player, p.DAMAGE_TYPE, p.DAMAGE, p.SPELL_NAME, mBoss.getLocation());
							}

							if (p.DAMAGE_PERCENTAGE > 0.0) {
								BossUtils.bossDamagePercent(mBoss, player, p.DAMAGE_PERCENTAGE, mBoss.getLocation(), p.SPELL_NAME);
							}
							p.EFFECTS.apply(player, mBoss);
						}
					}
				}
			}

			@Override
			public int cooldownTicks() {
				return p.COOLDOWN;
			}
		};

		super.constructBoss(spell, p.DETECTION, null, p.DELAY);
	}
}
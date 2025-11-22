package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.AlocAcoc;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellPolarizingSlash extends Spell {
	public static final int INNER_COOLDOWN = (int) (7.5 * 20);
	public static final int OUTER_COOLDOWN = (int) (6.5 * 20);
	private static final int INNER_DURATION = 6 * 20;
	private static final int OUTER_DURATION = (int) (4.5 * 20);
	private static final int DAMAGE = 60;
	private static final Color PARTICLE_COLOR = Color.fromRGB(75, 145, 185);

	private final LivingEntity mBoss;
	private final Plugin mPlugin;
	private final PassivePolarAura mAura;
	private final AlocAcoc mAlocAcoc;

	public SpellPolarizingSlash(LivingEntity boss, Plugin plugin, AlocAcoc alocAcoc, PassivePolarAura aura) {
		mBoss = boss;
		mPlugin = plugin;
		mAlocAcoc = alocAcoc;
		mAura = aura;
	}

	private int getDuration() {
		return mAura.mInnerAura ? INNER_DURATION : OUTER_DURATION;
	}

	@Override
	public boolean canRun() {
		return mAura.timeUntilFinishedNextSwap() > getDuration() && mAlocAcoc.canRunSpell(this);
	}

	@Override
	public void run() {
		EntityUtils.selfRoot(mBoss, getDuration());

		if (!mAura.mInnerAura) {
			runInnerOuterSlashes();
		} else {
			runInnerSlashes();
		}
	}

	@Override
	public int cooldownTicks() {
		return mAura.mInnerAura ? INNER_COOLDOWN : OUTER_COOLDOWN;
	}

	private void runInnerOuterSlashes() {
		new BukkitRunnable() {
			int mTicks = 0;
			List<BoundingBox> mBoxes = new ArrayList<>();

			@Override
			public void run() {
				if (mTicks % 30 == 0) {
					doDamage(mBoxes);
					mBoxes = new ArrayList<>();
					if (mTicks != OUTER_DURATION) {
						generateInnerOuterSlash(mTicks % 60 != 0, mBoxes, true);
					}
				} else {
					if (mTicks >= 30 && mTicks < 60 && mTicks % 10 == 0) {
						generateInnerOuterSlash(true, mBoxes, false);
					} else if (mTicks % 10 == 0) {
						generateInnerOuterSlash(false, mBoxes, false);
					}
				}

				if (mBoss.isDead() || mTicks >= OUTER_DURATION) {
					EntityUtils.cancelSelfRoot(mBoss);
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void runInnerSlashes() {
		new BukkitRunnable() {
			int mTicks = 0;
			List<BoundingBox> mBoxes = new ArrayList<>();

			@Override
			public void run() {
				if (mTicks % 20 == 0) {
					doDamage(mBoxes);
					mBoxes = new ArrayList<>();
					if (mTicks != INNER_DURATION) {
						generateInnerSlash(mTicks / 20 * 120, mBoxes, true);
					}
				} else if (mTicks % 5 == 0) {
					generateInnerSlash(mTicks / 20 * 120, mBoxes, false);
				}

				if (mBoss.isDead() || mTicks >= INNER_DURATION) {
					EntityUtils.cancelSelfRoot(mBoss);
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void doDamage(List<BoundingBox> boxes) {
		if (boxes.isEmpty()) {
			return;
		}

		World world = mBoss.getWorld();
		Hitbox hitbox = Hitbox.unionOfAABB(boxes, world);
		for (Player p : hitbox.getHitPlayers(true)) {
			DamageUtils.damage(mBoss, p, DamageEvent.DamageType.MELEE, DAMAGE, null, false, false, "Polarizing Slash");
			mAura.addFrostbite(p, 0.125f);
		}

		world.playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 3, 0.5f);
		int count = 0;
		for (BoundingBox box : boxes) {
			if (count++ % 2 == 0) {
				new PartialParticle(Particle.SNOWFLAKE, box.getCenter().toLocation(world)).count(1).delta(0.15).spawnAsBoss();
			}
		}
	}

	private void generateInnerOuterSlash(Boolean startInner, List<BoundingBox> boxes, boolean makeBoxes) {
		new PPParametric(Particle.REDSTONE, mBoss.getLocation(),
			(t, builder) -> {
				Location finalLocation = LocationUtils.fallToGround(
					mBoss.getLocation().add((PassivePolarAura.INNER_RADIUS + 0.3f) * FastUtils.cosDeg(t * 360), 3, PassivePolarAura.INNER_RADIUS * FastUtils.sinDeg(t * 360)), mBoss.getLocation().getY() - 5);
				builder.location(finalLocation);
			}).count(144).delta(0.15).data(new Particle.DustOptions(Color.fromRGB(3, 53, 252), 1.25f)).spawnAsEntityActive(mBoss);

		new PPParametric(Particle.REDSTONE, mBoss.getLocation(),
			(t, builder) -> {
				Location finalLocation = LocationUtils.fallToGround(
					mBoss.getLocation().add((PassivePolarAura.OUTER_RADIUS + 0.3f) * FastUtils.cosDeg(t * 360), 3, PassivePolarAura.OUTER_RADIUS * FastUtils.sinDeg(t * 360)), mBoss.getLocation().getY() - 5);
				builder.location(finalLocation);
			}).count(144).delta(0.15).data(new Particle.DustOptions(Color.fromRGB(3, 53, 252), 1.25f)).spawnAsEntityActive(mBoss);

		if (startInner) {
			Vector vec;
			for (int deg = 0; deg < 360; deg += 5) {
				boolean bold = deg % 60 == 0 || (deg + 5) % 60 == 0;
				if (deg < 60 || (deg >= 120 && deg < 180) || (deg >= 240 && deg < 300)) {
					for (double radius = 0; radius < PassivePolarAura.INNER_RADIUS; radius += 0.5) {
						double radian = Math.toRadians(deg);
						vec = new Vector(FastUtils.cos(radian) * radius, 0.1, FastUtils.sin(radian) * radius);
						Location l = mBoss.getLocation().clone().add(vec);
						l = LocationUtils.fallToGround(l.add(0, 5, 0), mBoss.getLocation().getY() - 10);
						if (makeBoxes) {
							BoundingBox box = BoundingBox.of(l, 0.5, 3, 0.5);
							boxes.add(box);
						}
						new PartialParticle(Particle.REDSTONE, l, 2, 0.1, 0, 0.1).data(new Particle.DustOptions(PARTICLE_COLOR, bold ? 2f : 1.25f)).spawnAsEntityActive(mBoss);
					}
				} else {
					for (double radius = PassivePolarAura.INNER_RADIUS; radius < PassivePolarAura.OUTER_RADIUS; radius += 0.5) {
						double radian = Math.toRadians(deg);
						vec = new Vector(FastUtils.cos(radian) * radius, 0.1, FastUtils.sin(radian) * radius);
						Location l = mBoss.getLocation().clone().add(vec);
						l = LocationUtils.fallToGround(l.add(0, 5, 0), mBoss.getLocation().getY() - 10);
						if (makeBoxes) {
							BoundingBox box = BoundingBox.of(l, 0.5, 3, 0.5);
							boxes.add(box);
						}
						new PartialParticle(Particle.REDSTONE, l, 2, 0.1, 0, 0.1, 0, 0).data(new Particle.DustOptions(PARTICLE_COLOR, bold ? 2f : 1.25f)).spawnAsEntityActive(mBoss);
					}
				}

			}
		} else {
			Vector vec;
			for (int deg = 0; deg < 360; deg += 5) {
				boolean bold = deg % 60 == 0 || (deg + 5) % 60 == 0;
				if (deg < 60 || (deg >= 120 && deg < 180) || (deg >= 240 && deg < 300)) {
					for (double radius = PassivePolarAura.INNER_RADIUS; radius < PassivePolarAura.OUTER_RADIUS; radius += 0.5) {
						double radian = Math.toRadians(deg);
						vec = new Vector(FastUtils.cos(radian) * radius, 0, FastUtils.sin(radian) * radius);
						Location l = mBoss.getLocation().clone().add(vec);
						l = LocationUtils.fallToGround(l.add(0, 5, 0), mBoss.getLocation().getY() - 10);
						if (makeBoxes) {
							BoundingBox box = BoundingBox.of(l, 0.5, 3, 0.5);
							boxes.add(box);
						}
						new PartialParticle(Particle.REDSTONE, l, 2, 0.1, 0, 0.1, 0).data(new Particle.DustOptions(PARTICLE_COLOR, bold ? 2f : 1.25f)).spawnAsEntityActive(mBoss);
					}
				} else {
					for (double radius = 0; radius < PassivePolarAura.INNER_RADIUS; radius += 0.5) {
						double radian = Math.toRadians(deg);
						vec = new Vector(FastUtils.cos(radian) * radius, 0, FastUtils.sin(radian) * radius);
						Location l = mBoss.getLocation().clone().add(vec);
						l = LocationUtils.fallToGround(l.add(0, 5, 0), mBoss.getLocation().getY() - 10);
						if (makeBoxes) {
							BoundingBox box = BoundingBox.of(l, 0.5, 3, 0.5);
							boxes.add(box);
						}
						new PartialParticle(Particle.REDSTONE, l, 2, 0.1, 0, 0.1, 0).data(new Particle.DustOptions(PARTICLE_COLOR, bold ? 2f : 1.25f)).spawnAsEntityActive(mBoss);
					}
				}

			}
		}
	}

	private void generateInnerSlash(int degree, List<BoundingBox> boxes, boolean makeBoxes) {
		new PPParametric(Particle.REDSTONE, mBoss.getLocation(),
			(t, builder) -> {
				Location finalLocation = LocationUtils.fallToGround(
					mBoss.getLocation().add((PassivePolarAura.INNER_RADIUS + 0.3f) * FastUtils.cosDeg(t * 360), 3, PassivePolarAura.INNER_RADIUS * FastUtils.sinDeg(t * 360)), mBoss.getLocation().getY() - 5);
				builder.location(finalLocation);
			}).count(168).data(new Particle.DustOptions(Color.fromRGB(30, 64, 252), 1.5f)).spawnAsEntityActive(mBoss);

		Vector vec;
		for (int deg = degree; deg < degree + 120; deg += 5) {
			boolean bold = deg == degree || deg == degree + 115;
			for (double radius = 0; radius < PassivePolarAura.INNER_RADIUS; radius += 0.5) {
				double radian = Math.toRadians(deg);
				vec = new Vector(FastUtils.cos(radian) * radius, 0, FastUtils.sin(radian) * radius);
				Location l = mBoss.getLocation().clone().add(vec);
				l = LocationUtils.fallToGround(l.add(0, 5, 0), mBoss.getLocation().getY() - 10);
				if (makeBoxes) {
					BoundingBox box = BoundingBox.of(l, 0.5, 3, 0.5);
					boxes.add(box);
				}
				new PartialParticle(Particle.REDSTONE, l, 2, 0.1, 0, 0.1, 0).data(new Particle.DustOptions(PARTICLE_COLOR, bold ? 2f : 1.25f)).spawnAsEntityActive(mBoss);
			}
		}
	}
}

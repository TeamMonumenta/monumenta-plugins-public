package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class ShockwaveBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_shockwave";

	public static class Parameters extends BossParameters {
		@BossParam(help = "detection radius")
		public int DETECTION = 20;
		@BossParam(help = "time between casts")
		public int COOLDOWN = 10 * 20;
		@BossParam(help = "")
		public int DELAY = 2 * 20;
		@BossParam(help = "name of this spell")
		public String SPELL_NAME = "Shockwave";
		@BossParam(help = "duration of the wind-up")
		public int DURATION = 40;
		@BossParam(help = "duration of the wave")
		public int WAVE_DURATION = 40;
		@BossParam(help = "rate of wave expansion")
		public double SPEED = 0.45;
		@BossParam(help = "how often the wave will expand, in ticks")
		public int FREQUENCY = 2;
		@BossParam(help = "whether the mob can move while charging up or not")
		public boolean CAN_MOVE = false;
		@BossParam(help = "damage on contact")
		public double DAMAGE = 3;
		@BossParam(help = "number of points around the circle that will be generated")
		public int POINT_COUNT = 48;
		@BossParam(help = "whether the shockwave ignores i-frames")
		public boolean IGNORE_IFRAMES = false;
		@BossParam(help = "damage type")
		public DamageEvent.DamageType DAMAGE_TYPE = DamageEvent.DamageType.MAGIC;
		@BossParam(help = "horizontal knockback velocity")
		public float KB_X = 0.6f;
		@BossParam(help = "vertical knockback velocity")
		public float KB_Y = 0.8f;
		@BossParam(help = "effects applied on hit")
		public EffectsList EFFECTS = EffectsList.EMPTY;
		@BossParam(help = "particles around the boss while it telegraphs")
		public ParticlesList PARTICLE_BOSS_CHARGE = ParticlesList.fromString("[(ELECTRIC_SPARK,8,1,0.1,1,0.01)]");
		@BossParam(help = "particle of the shockwave")
		public ParticlesList PARTICLE_RELEASE = ParticlesList.fromString("[(CRIT,1,0.0,0.0,0.0,0)]");
		@BossParam(help = "sound played while the boss charges up")
		public SoundsList SOUND_CHARGE = SoundsList.fromString("[(ENTITY_FIREWORK_ROCKET_TWINKLE,0.5,1.7)]");
		@BossParam(help = "sound played when the shockwave is released")
		public SoundsList SOUND_RELEASE = SoundsList.fromString("[(ENTITY_WITHER_SHOOT,1,0.85),(ENTITY_FIREWORK_ROCKET_TWINKLE,1,2.5)]");

	}

	public ShockwaveBoss(Plugin plugin, LivingEntity boss) {

		super(plugin, identityTag, boss);

		ShockwaveBoss.Parameters p = ShockwaveBoss.Parameters.getParameters(boss, identityTag, new ShockwaveBoss.Parameters());

		Spell spell = new Spell() {

			@Override
			public void run() {
				World world = mBoss.getWorld();
				Location centerLoc = mBoss.getLocation();
				if (!p.CAN_MOVE) {
					EntityUtils.selfRoot(mBoss, p.DURATION);
				}
				new BukkitRunnable() {
					double mT = 0;
					@Override
					public void run() {
						if (mT <= p.DURATION) {
							mT += 2;
							if (mT % 2 == 0) {
								p.SOUND_CHARGE.play(mBoss.getLocation());
							}
							p.PARTICLE_BOSS_CHARGE.spawn(mBoss, mBoss.getLocation());
						} else {
							this.cancel();
							p.SOUND_RELEASE.play(mBoss.getLocation());
							Location loc = mBoss.getLocation().add(0, 0.25, 0);
							for (int i = 0; i < p.POINT_COUNT; i++) {
								int j = i;
								new BukkitRunnable() {
									final BoundingBox mBox = BoundingBox.of(loc, 0.75, 0.4, 0.75);
									final double mRadian1 = Math.toRadians((360.0 / p.POINT_COUNT * j));
									final Location mPoint = loc.clone().add(FastUtils.cos(mRadian1) * 0.5, 0, FastUtils.sin(mRadian1) * 0.5);
									final Vector mDir = LocationUtils.getDirectionTo(mPoint, loc);
									int mTicks = 0;

									@Override
									public void run() {
										mTicks++;
										mBox.shift(mDir.clone().multiply(p.SPEED));
										Location bLoc = mBox.getCenter().toLocation(world);
										p.PARTICLE_RELEASE.spawn(mBoss, bLoc);
										for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), p.DETECTION, true)) {
											if (player.getBoundingBox().overlaps(mBox)) {
												DamageUtils.damage(mBoss, player, p.DAMAGE_TYPE, p.DAMAGE, null, p.IGNORE_IFRAMES, true, p.SPELL_NAME);
												MovementUtils.knockAway(centerLoc, player, p.KB_X, p.KB_Y);
												p.EFFECTS.apply(player, boss);

											}
										}
										if (mTicks >= p.WAVE_DURATION) {
											this.cancel();
										}
									}

									@Override
									public synchronized void cancel() {
										mActiveRunnables.remove(this);
										super.cancel();
									}
								}.runTaskTimer(mPlugin, 0, p.FREQUENCY);
							}
						}
					}
				}.runTaskTimer(mPlugin, 0, 2);
			}

			@Override
			public int cooldownTicks() {
				return p.COOLDOWN;
			}
		};
		super.constructBoss(spell, p.DETECTION, null, p.DELAY);
	}
}

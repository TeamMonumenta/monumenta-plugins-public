package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.*;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSeekingAoE;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import org.apache.commons.math3.util.FastMath;
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
import org.bukkit.util.Vector;

public final class SlamAttackBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_slamattack";

	private final Color TWIST_COLOR_BASE = Color.fromRGB(130, 66, 66);
	private final Color TWIST_COLOR_TIP = Color.fromRGB(127, 0, 0);
	private final Color COLO_COLOR_BASE = Color.fromRGB(186, 140, 22);
	private final Color COLO_COLOR_TIP = Color.fromRGB(252, 217, 129);
	private final double[] ANGLES = {240, 290, 270};
	private final float[] PITCHES = {0.85f, 1.15f, 0.55f};
	private final float[] GOLEM_PITCHES = {0.6f, 0.775f, 0.5f};

	public static class Parameters extends BossParameters {
		@BossParam(help = "not written")
		public int RADIUS = 3;
		@BossParam(help = "not written")
		public int DAMAGE = 20;
		@BossParam(help = "Time it takes for the spell to charge")
		public int DELAY = 10;
		@BossParam(help = "not written")
		public int COOLDOWN = 8 * 20;
		@BossParam(help = "Whether or not the nova attack can be blocked by a shield. (Default = true)")
		public boolean CAN_BLOCK = true;
		@BossParam(help = "Amount of consecutive slams done")
		public int COMBOS = 1;
		@BossParam(help = "Delay in ticks between each slam in the combo")
		public int COMBO_DELAY = 10;
		@BossParam(help = "not written")
		public double DAMAGE_PERCENTAGE = 0.0;
		@BossParam(help = "Effect applied to players hit by the nova")
		public EffectsList EFFECTS = EffectsList.EMPTY;
		@BossParam(help = "The spell name shown when a player is killed by this skill")
		public String SPELL_NAME = "";
		@BossParam(help = "not written")
		public EntityTargets TARGETS = EntityTargets.GENERIC_PLAYER_TARGET;
		@BossParam(help = "If the boss can move while charging the attack")
		public boolean CAN_MOVE_WHEN_CHARGING = false;
		@BossParam(help = "If the boss can move while unleashing the attack")
		public boolean CAN_MOVE_WHEN_CASTING = false;
		@BossParam(help = "Sound played while charging")
		public SoundsList CHARGE_SOUND = SoundsList.fromString("[(ENTITY_BLAZE_BURN,0.5,0.2)]");
	}

	public SlamAttackBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		SlamAttackBoss.Parameters p = BossParameters.getParameters(boss, identityTag, new SlamAttackBoss.Parameters());

		Spell spell = new SpellBaseSeekingAoE(plugin, boss, (int) p.TARGETS.getRange(), (int) p.TARGETS.getRange(), p.DELAY, 0, p.COOLDOWN,
			p.RADIUS, p.COMBOS, p.COMBO_DELAY, p.CAN_MOVE_WHEN_CHARGING, p.CAN_MOVE_WHEN_CASTING, false, false, true,
			//Player filter
			(player) -> p.TARGETS.getTargetsList(boss).contains(player),
			//While Charging
			(Location location, int ticks) -> {

				p.CHARGE_SOUND.play(location);

				final Location mL = location.clone();
				final double RADIUS = Math.max(p.RADIUS, 0.5);
				double r = RADIUS * (1 - ((double) ticks / p.DELAY));

				if (!(ticks % (int) (p.DELAY * 0.5 / RADIUS) == 0)) {
					return;
				}

				if (r >= RADIUS) {
					return;
				}

				for (int degree = 0; degree < 360; degree += 5) {
					double radian = FastMath.toRadians(degree);
					Vector vec = new Vector(FastUtils.cos(radian) * r, 0,
						FastUtils.sin(radian) * r);
					Location loc = mL.clone().add(vec);
					new PartialParticle(Particle.DUST_COLOR_TRANSITION, loc, 1, 0, 0, 0, 0,
						new Particle.DustTransition(
							ParticleUtils.getTransition(COLO_COLOR_BASE, COLO_COLOR_TIP, r / RADIUS),
							ParticleUtils.getTransition(TWIST_COLOR_BASE, TWIST_COLOR_TIP, r / RADIUS),
							1f
						))
						.spawnAsBoss();
				}

			},
			//On Cast
			(Location location, int spellCount) -> { },
			//On Trigger
			(Location location, int spellCount) -> {

				int combo = (spellCount - 1) % 3;
				Location pLoc = location.clone();

				World world = boss.getWorld();

				world.playSound(pLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.75f, 0.75f);
				world.playSound(pLoc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 0.75f, PITCHES[combo]);
				world.playSound(pLoc, Sound.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.PLAYERS, 1f, 0.65f);

				Location bossLoc = boss.getLocation().add(0, 1, 0);
				Vector dir = LocationUtils.getDirectionTo(pLoc, bossLoc).multiply(2.15);
				pLoc.setDirection(dir);
				ParticleUtils.drawHalfArc(pLoc.clone().subtract(dir), 2.15, ANGLES[combo], -40, 140, 8, 0.2,
					(Location l, int ring) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0,
						new Particle.DustOptions(
							ParticleUtils.getTransition(COLO_COLOR_BASE, COLO_COLOR_TIP, ring / 8D),
							0.6f + (ring * 0.1f)
						))
						.spawnAsBoss());
				world.playSound(pLoc, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, 1f, GOLEM_PITCHES[combo]);
				world.playSound(pLoc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1f, 0.65f);
				world.playSound(pLoc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 1f, 0.75f);
				if (combo == 2) {
					world.playSound(pLoc, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 1f, 0.55f);
					new PartialParticle(Particle.SMOKE_LARGE, pLoc, 16, 0, 0, 0, 0.125)
						.spawnAsBoss();
					new PartialParticle(Particle.SMOKE_NORMAL, pLoc, 40, 0, 0, 0, 0.15)
						.spawnAsBoss();
				}
				new PartialParticle(Particle.SMOKE_NORMAL, pLoc, 55, 0, 0, 0, 0.15)
					.spawnAsBoss();
				new PartialParticle(Particle.CRIT, pLoc, 50, 0, 0, 0, 0.75)
					.spawnAsBoss();

				new BukkitRunnable() {

					double mRadius = 0;
					final Location mL = location.clone();
					final double RADIUS = Math.max(p.RADIUS, 0.5);

					@Override
					public void run() {

						for (int i = 0; i < 2; i++) {
							mRadius += 0.3;
							for (int degree = 0; degree < 360; degree += 5) {
								double radian = FastMath.toRadians(degree);
								Vector vec = new Vector(FastUtils.cos(radian) * mRadius, 0,
									FastUtils.sin(radian) * mRadius);
								Location loc = mL.clone().add(vec);
								new PartialParticle(Particle.DUST_COLOR_TRANSITION, loc, 1, 0, 0, 0, 0,
									new Particle.DustTransition(
										ParticleUtils.getTransition(COLO_COLOR_BASE, COLO_COLOR_TIP, mRadius / RADIUS),
										ParticleUtils.getTransition(TWIST_COLOR_BASE, TWIST_COLOR_TIP, mRadius / RADIUS),
										0.8f
									))
									.spawnAsBoss();
							}
						}

						if (mRadius >= RADIUS) {
							this.cancel();
						}
					}

				}.runTaskTimer(plugin, 0, 1);
			},
			//Hit players
			(Player player) -> {
				if (p.DAMAGE > 0) {
					if (p.CAN_BLOCK) {
						BossUtils.blockableDamage(boss, player, DamageEvent.DamageType.MELEE, p.DAMAGE, p.SPELL_NAME, mBoss.getLocation());
					} else {
						DamageUtils.damage(boss, player, DamageEvent.DamageType.MELEE, p.DAMAGE, null, false, true, p.SPELL_NAME);
					}
				}

				if (p.DAMAGE_PERCENTAGE > 0.0) {
					BossUtils.bossDamagePercent(mBoss, player, p.DAMAGE_PERCENTAGE, p.CAN_BLOCK ? mBoss.getLocation() : null, p.SPELL_NAME);
				}
				p.EFFECTS.apply(player, mBoss);
			}
		);

		super.constructBoss(spell, (int) p.TARGETS.getRange(), null, 0);
	}
}

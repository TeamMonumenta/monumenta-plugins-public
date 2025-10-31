package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
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

public class SpellColossalBruteForce extends SpellBaseSeekingAoE {

	private static final Color TWIST_COLOR_BASE = Color.fromRGB(130, 66, 66);
	private static final Color TWIST_COLOR_TIP = Color.fromRGB(127, 0, 0);
	private static final Color COLO_COLOR_BASE = Color.fromRGB(186, 140, 22);
	private static final Color COLO_COLOR_TIP = Color.fromRGB(252, 217, 129);
	private static final double[] ANGLES = {240, 290, 270};
	private static final float[] PITCHES = {0.85f, 1.15f, 0.55f};
	private static final float[] GOLEM_PITCHES = {0.6f, 0.775f, 0.5f};
	private static final String SPELL_NAME = "Colossal Shockwave";

	private final int mDamage;
	private final double mDamagePercentage;
	private final boolean mBlockable;
	private final EffectsList mEffects;
	private final SoundsList mChargeSound;

	/**
	 * @param plugin               Plugin
	 * @param caster               The boss casting the spell
	 * @param targets              Targets
	 * @param delay                Time it takes for the spell to trigger after it has finished charging and become stationary
	 * @param cooldown             Cooldown
	 * @param radius               Radius
	 * @param count                Amount of spells per player
	 * @param incrementDelay       Time between each consecutive spell when count is above 1
	 * @param damage               Damage
	 * @param damagePercentage     Percentage damage
	 * @param canMoveWhileCharging If the boss can move while in the charge up phase
	 * @param canMoveWhileCasting  If the boss can move in the period between the charge up is finished and the spell is triggered
	 */
	public SpellColossalBruteForce(Plugin plugin, LivingEntity caster, EntityTargets targets, int delay, int cooldown, int radius,
	                               int count, int incrementDelay, int damage, double damagePercentage, boolean blockable, EffectsList effects,
	                               boolean canMoveWhileCharging, boolean canMoveWhileCasting, SoundsList chargeSound) {

		super(plugin, caster, (int) targets.getRange(), (int) targets.getRange(), delay, 0, cooldown, radius,
			count, incrementDelay, canMoveWhileCharging, canMoveWhileCasting, false, false,
			true, (Player player) -> targets.getTargetsList(caster).contains(player));

		mDamage = damage;
		mDamagePercentage = damagePercentage;
		mBlockable = blockable;
		mEffects = effects;
		mChargeSound = chargeSound;
	}

	@Override
	protected void chargeAction(Location location, int ticks) {
		mChargeSound.play(location);

		final Location mL = location.clone();
		final double RADIUS = Math.max(mRadius, 0.5);
		double r = RADIUS * (1 - ((double) ticks / mChargeUp));

		if (!(ticks % (int) (mChargeUp * 0.5 / RADIUS) == 0)) {
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
	}

	@Override
	protected void castAction(Location location, int spellCount) {
	}

	@Override
	protected void outburstAction(Location location, int spellCount) {

		int combo = (spellCount - 1) % 3;
		Location pLoc = location.clone();

		World world = mCaster.getWorld();

		world.playSound(pLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.75f, 0.75f);
		world.playSound(pLoc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 0.75f, PITCHES[combo]);
		world.playSound(pLoc, Sound.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.PLAYERS, 1f, 0.65f);

		Location bossLoc = mCaster.getLocation().add(0, 1, 0);
		Vector dir = LocationUtils.getDirectionTo(pLoc, bossLoc).multiply(2.15);
		pLoc.setDirection(dir);
		ParticleUtils.drawHalfArc(pLoc.clone().subtract(dir), 2.15, ANGLES[combo], -40, 140, 8, 0.2,
			(Location l, int ring, double angleProgress) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0,
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
			final double RADIUS = Math.max(mRadius, 0.5);

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

		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	protected void hitAction(Player player) {
		if (mDamage > 0) {
			if (mBlockable) {
				BossUtils.blockableDamage(mCaster, player, DamageEvent.DamageType.MELEE, mDamage, SPELL_NAME, mCaster.getLocation());
			} else {
				DamageUtils.damage(mCaster, player, DamageEvent.DamageType.MELEE, mDamage, null, false, true, SPELL_NAME);
			}
		}

		if (mDamagePercentage > 0.0) {
			BossUtils.bossDamagePercent(mCaster, player, mDamagePercentage, mBlockable ? mCaster.getLocation() : null, SPELL_NAME);
		}
		mEffects.apply(player, mCaster);
	}
}

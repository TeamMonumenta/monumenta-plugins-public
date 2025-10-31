package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.CoreElemental;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellCoreExpansion extends Spell implements CoreElemental.CoreElementalBase {
	// Size
	private static final int RADIUS = 5;
	// Knock Velocity
	private static final float KNOCK_VELOCITY = 2;
	// Time needed to charge/ telegraph time
	private static final int CHARGE_TIME = 50;
	// Area damage
	private static final int AOE_DAMAGE = 70;
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final CoreElemental mQuarry;
	private final World mWorld;

	public SpellCoreExpansion(Plugin plugin, LivingEntity boss, CoreElemental quarry) {
		mPlugin = plugin;
		mBoss = boss;
		mQuarry = quarry;
		mWorld = mBoss.getWorld();
	}

	@Override
	public void run() {
		EntityUtils.selfRoot(mBoss, CHARGE_TIME);

		// Charging
		BukkitRunnable runnable = new BukkitRunnable() {
			double mRadius = 5;
			int mT = 0;

			@Override
			public void run() {
				// Telegraph
				new PPCircle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), mRadius)
					.count(30)
					.delta(0.25)
					.spawnAsEntityActive(mBoss);
				mRadius -= (double) 5 / CHARGE_TIME;
				mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, SoundCategory.HOSTILE, 0.7f, 0.5f + (float) mT++ / CHARGE_TIME);
				if (mRadius <= 0) {
					this.cancel();
					mActiveRunnables.remove(this);
					expand();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	private void expand() {
		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 2f, 1);

		new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getEyeLocation())
			.count(40)
			.delta(0.2)
			.extra(0.4)
			.spawnAsBoss();

		// Spawn fire
		for (int i = 0; i < 4 * mQuarry.getPlayers().size(); i++) {
			FallingBlock block = mWorld.spawn(mBoss.getEyeLocation(), FallingBlock.class, b -> b.setBlockData(Bukkit.createBlockData(Material.FIRE)));
			block.setVelocity(VectorUtils.randomUnitVector().setY(0.2));
		}

		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), RADIUS, false)) {
			DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, AOE_DAMAGE, null, false, false, getSpellName());
			MovementUtils.knockAway(mBoss, player, KNOCK_VELOCITY);
			EntityUtils.applyFire(com.playmonumenta.plugins.Plugin.getInstance(), 20 * 10, player, mBoss);
		}

	}

	@Override
	public int cooldownTicks() {
		return 20 * 6;
	}

	@Override
	public String getSpellName() {
		return "Core Expansion";
	}

	@Override
	public String getSpellChargePrefix() {
		return "Unleashing";
	}

	@Override
	public int getChargeDuration() {
		return CHARGE_TIME;
	}

	@Override
	public int getSpellDuration() {
		// Players should be able to damage the boss while the boss is expanded
		return 0;
	}

	@Override
	public boolean canRun() {
		return !mQuarry.mIsCastingBanish;
	}
}

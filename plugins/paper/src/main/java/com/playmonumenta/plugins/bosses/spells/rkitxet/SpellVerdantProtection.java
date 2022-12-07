package com.playmonumenta.plugins.bosses.spells.rkitxet;

import com.playmonumenta.plugins.bosses.bosses.RKitxet;
import com.playmonumenta.plugins.bosses.spells.SpellBaseAoE;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class SpellVerdantProtection extends SpellBaseAoE {

	private static final int RADIUS = 5;
	private static final int DURATION = 4 * 20;
	private static final int DAMAGE = 20;
	private static final Particle.DustOptions VERDANT_PROTECTION_COLOR = new Particle.DustOptions(Color.fromRGB(20, 200, 20), 1f);

	private final RKitxet mRKitxet;

	public SpellVerdantProtection(Plugin plugin, LivingEntity launcher, int radius, int time, int cooldown, RKitxet rKitxet) {
		super(plugin, launcher, radius, time, cooldown, false, Sound.BLOCK_ROOTS_BREAK);
		mRKitxet = rKitxet;
	}

	public SpellVerdantProtection(Plugin plugin, LivingEntity launcher, int cooldown, RKitxet rKitxet) {
		this(plugin, launcher, RADIUS, DURATION, cooldown, rKitxet);
	}

	@Override
	protected void chargeAuraAction(Location loc) {
		new PartialParticle(Particle.CRIMSON_SPORE, loc, 1, mRadius / 2.0, mRadius / 2.0, mRadius / 2.0).spawnAsEntityActive(mLauncher);
		new PartialParticle(Particle.COMPOSTER, loc, 1, mRadius / 2.0, mRadius / 2.0, mRadius / 2.0, 0.05).spawnAsEntityActive(mLauncher);
	}

	@Override
	protected void chargeCircleAction(Location loc) {
		new PartialParticle(Particle.REDSTONE, loc, 1, 0.25, 0.25, 0.25, VERDANT_PROTECTION_COLOR).spawnAsEntityActive(mLauncher);
		new PartialParticle(Particle.REDSTONE, loc.clone().add(0, 2, 0), 1, 0.25, 0.25, 0.25, VERDANT_PROTECTION_COLOR).spawnAsEntityActive(mLauncher);
		new PartialParticle(Particle.COMPOSTER, loc, 1, 0.25, 0.25, 0.25, 0.1).spawnAsEntityActive(mLauncher);
	}

	@Override
	protected void outburstAction(Location loc) {
		World world = loc.getWorld();
		world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.65F);
	}

	@Override
	protected void circleOutburstAction(Location loc) {
		new PartialParticle(Particle.COMPOSTER, loc, 1, 0.1, 0.1, 0.1, 0.3).spawnAsEntityActive(mLauncher);
		new PartialParticle(Particle.REDSTONE, loc, 1, 0.25, 0.25, 0.25, 0.1, VERDANT_PROTECTION_COLOR).spawnAsEntityActive(mLauncher);
	}

	@Override
	protected void dealDamageAction(Location loc) {
		mRKitxet.useSpell("Verdant Protection");

		boolean hasHit = false;
		for (Player player : PlayerUtils.playersInRange(mLauncher.getLocation(), mRadius, true)) {
			BossUtils.blockableDamage(mLauncher, player, DamageType.MAGIC, DAMAGE, "Verdant Protection", mLauncher.getLocation());

			double distance = player.getLocation().distance(loc);
			if (distance < mRadius / 3.0) {
				MovementUtils.knockAway(mLauncher, player, 2.25f);
			} else if (distance < (mRadius * 2.0) / 3.0) {
				MovementUtils.knockAway(mLauncher, player, 1.575f);
			} else if (distance < mRadius) {
				MovementUtils.knockAway(mLauncher, player, 0.9f);
			}

			hasHit = true;
		}

		if (hasHit) {
			mRKitxet.mShieldSpell.applyShield(true);
		}
	}

	@Override
	public boolean canRun() {
		return mRKitxet.canUseSpell("Verdant Protection");
	}
}

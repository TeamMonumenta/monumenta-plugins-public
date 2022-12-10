package com.playmonumenta.plugins.depths.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.SpellBaseAoE;
import com.playmonumenta.plugins.effects.AbilitySilence;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class SpellVoidBlast extends SpellBaseAoE {

	public static final int CAST_DURATION = 3 * 20;
	public static final int SILENCE_DURATION = 5 * 20;
	public static final int RADIUS = 5;
	public static final int MIN_DAMAGE = 25;
	public static final int MAX_DAMAGE = 40;

	private final float mMinDamage;
	private final float mMaxDamage;

	public SpellVoidBlast(Plugin plugin, LivingEntity launcher, int radius, float minDamage, float maxDamage, int duration, int cooldown) {
		super(plugin, launcher, radius, duration, cooldown, false, Sound.ENTITY_VEX_CHARGE);
		mMinDamage = minDamage;
		mMaxDamage = maxDamage;
	}

	public SpellVoidBlast(Plugin plugin, LivingEntity launcher, int cooldownTicks) {
		this(plugin, launcher, RADIUS, MIN_DAMAGE, MAX_DAMAGE, CAST_DURATION, cooldownTicks);
	}

	@Override
	protected void chargeAuraAction(Location loc) {
		new PartialParticle(Particle.CLOUD, loc, 3, mRadius / 2.0, mRadius / 2.0, mRadius / 2.0, 0.05).spawnAsEntityActive(mLauncher);
	}

	@Override
	protected void chargeCircleAction(Location loc) {
		new PartialParticle(Particle.SPELL_WITCH, loc, 1, 0.25, 0.25, 0.25, 0).minimumMultiplier(false).spawnAsEntityActive(mLauncher);
	}

	@Override
	protected void outburstAction(Location loc) {
		World world = loc.getWorld();
		world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.65F);
	}

	@Override
	protected void circleOutburstAction(Location loc) {
		new PartialParticle(Particle.CLOUD, loc, 1, 0.1, 0.1, 0.1, 0.2).minimumMultiplier(false).spawnAsEntityActive(mLauncher);
		new PartialParticle(Particle.SPELL_WITCH, loc, 1, 0.25, 0.25, 0.25, 0).minimumMultiplier(false).spawnAsEntityActive(mLauncher);
	}

	@Override
	protected void dealDamageAction(Location loc) {
		for (Player player : PlayerUtils.playersInRange(mLauncher.getLocation(), mRadius, true)) {
			double distance = player.getLocation().distance(mLauncher.getLocation());
			BossUtils.blockableDamage(mLauncher, player, DamageType.MAGIC, ((mMaxDamage - mMinDamage) * ((mRadius - distance) / mRadius)) + mMinDamage, "Void Blast", mLauncher.getLocation());
			com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(player, "Void Blast", new AbilitySilence(SILENCE_DURATION));
		}
	}

}

package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SpellFrostNova extends SpellBaseAoE {

	private final float mMinDamage;
	private final float mMaxDamage;

	public SpellFrostNova(Plugin plugin, LivingEntity launcher, int radius, float minDamage, float maxDamage, int duration, int cooldown) {
		super(plugin, launcher, radius, duration, cooldown, false, Sound.ENTITY_SNOWBALL_THROW);
		mMinDamage = minDamage;
		mMaxDamage = maxDamage;
	}

	public SpellFrostNova(Plugin plugin, LivingEntity launcher, int radius, float minDamage, float maxDamage) {
		this(plugin, launcher, radius, minDamage, maxDamage, 80, 160);
	}

	@Override
	protected void chargeAuraAction(Location loc) {
		new PartialParticle(Particle.CLOUD, loc, 7, mRadius / 2.0, mRadius / 2.0, mRadius / 2.0, 0.05).spawnAsEntityActive(mLauncher);
	}

	@Override
	protected void chargeCircleAction(Location loc) {
		new PartialParticle(Particle.SNOWBALL, loc, 1, 0.25, 0.25, 0.25, 0).spawnAsEntityActive(mLauncher);
	}

	@Override
	protected void outburstAction(Location loc) {
		World world = loc.getWorld();
		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1.5f, 0.77F);
		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1.5f, 0.5F);
		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1.5f, 0.65F);
	}

	@Override
	protected void circleOutburstAction(Location loc) {
		new PartialParticle(Particle.CLOUD, loc, 2, 0.1, 0.1, 0.1, 0.2).spawnAsEntityActive(mLauncher);
		new PartialParticle(Particle.SNOWBALL, loc, 1, 0.25, 0.25, 0.25, 0).spawnAsEntityActive(mLauncher);
	}

	@Override
	protected void dealDamageAction(Location loc) {
		for (Player player : PlayerUtils.playersInRange(mLauncher.getLocation(), mRadius, true)) {
			double distance = player.getLocation().distance(mLauncher.getLocation());
			BossUtils.blockableDamage(mLauncher, player, DamageType.MAGIC, ((mMaxDamage - mMinDamage) * ((mRadius - distance) / mRadius)) + mMinDamage);
			player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 50, 3));
		}
	}

}

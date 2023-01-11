package com.playmonumenta.plugins.depths.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.SpellBaseAoE;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Color;
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

public class SpellLeafNova extends SpellBaseAoE {

	private static final Particle.DustOptions LEAF_COLOR = new Particle.DustOptions(Color.fromRGB(14, 123, 8), 1.0f);
	private static final int RADIUS = 5;
	private static final int DURATION = 4 * 20;
	private static final int DAMAGE = 50;

	private int mCooldownTicks;

	public SpellLeafNova(Plugin plugin, LivingEntity launcher, int radius, int time, int cooldown) {
		super(plugin, launcher, radius, time, cooldown, false, Sound.BLOCK_BAMBOO_BREAK);
	}

	public SpellLeafNova(Plugin plugin, LivingEntity launcher, int cooldown) {
		this(plugin, launcher, RADIUS, DURATION, cooldown);
		mCooldownTicks = cooldown;
	}

	@Override
	protected void chargeAuraAction(Location loc) {
		new PartialParticle(Particle.REDSTONE, loc, 1, mRadius / 2.0, mRadius / 2.0, mRadius / 2.0, LEAF_COLOR).minimumMultiplier(false).spawnAsEntityActive(mLauncher);
		new PartialParticle(Particle.COMPOSTER, loc, 1, mRadius / 2.0, mRadius / 2.0, mRadius / 2.0, 0.05).minimumMultiplier(false).spawnAsEntityActive(mLauncher);
	}

	@Override
	protected void chargeCircleAction(Location loc) {
		new PartialParticle(Particle.REDSTONE, loc, 1, 0.25, 0.25, 0.25, LEAF_COLOR).minimumMultiplier(false).spawnAsEntityActive(mLauncher);
		new PartialParticle(Particle.REDSTONE, loc.clone().add(0, 2, 0), 1, 0.25, 0.25, 0.25, LEAF_COLOR).minimumMultiplier(false).spawnAsEntityActive(mLauncher);
		new PartialParticle(Particle.COMPOSTER, loc, 1, 0.25, 0.25, 0.25, 0.1).minimumMultiplier(false).spawnAsEntityActive(mLauncher);
	}

	@Override
	protected void outburstAction(Location loc) {
		World world = loc.getWorld();
		world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 2.0f, 0.65F);
	}

	@Override
	protected void circleOutburstAction(Location loc) {
		new PartialParticle(Particle.COMPOSTER, loc, 1, 0.1, 0.1, 0.1, 0.3).minimumMultiplier(false).spawnAsEntityActive(mLauncher);
		new PartialParticle(Particle.SLIME, loc, 2, 0.25, 0.25, 0.25, 0.1).minimumMultiplier(false).spawnAsEntityActive(mLauncher);
	}

	@Override
	protected void dealDamageAction(Location loc) {
		for (Player player : PlayerUtils.playersInRange(mLauncher.getLocation(), mRadius, true)) {
			DamageUtils.damage(mLauncher, player, DamageType.MAGIC, DAMAGE, null, false, true, "Leaf Nova");
			player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 20 * 6, 4));
		}
	}

	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}
}

package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.abilities.alchemist.UnstableAmalgam;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.util.Vector;

public class UnstableAmalgamEnhancementEffect extends Effect {

	private final Player mPlayer;
	private final UnstableAmalgam mUnstableAmalgam;
	private final ItemStatManager.PlayerItemStats mStats;

	public UnstableAmalgamEnhancementEffect(int duration, Player alchemist, UnstableAmalgam unstableAmalgam, ItemStatManager.PlayerItemStats stats) {
		super(duration);
		mPlayer = alchemist;
		mUnstableAmalgam = unstableAmalgam;
		mStats = stats;
	}

	@Override
	public void onDamage(LivingEntity entity, DamageEvent event, LivingEntity enemy) {
		Location eyeLoc = entity.getEyeLocation();
		Location enemyEyeLoc = enemy.getEyeLocation();
		Vector dir = enemyEyeLoc.clone().toVector().subtract(eyeLoc.toVector()).normalize();
		ThrownPotion potion = mPlayer.launchProjectile(ThrownPotion.class);
		potion.teleport(eyeLoc);
		potion.setVelocity(dir);
		//we may need to multiply the velocity a bit more?
		mUnstableAmalgam.setEnhancementThrownPotion(potion, mStats);

		clearEffect();
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		new PartialParticle(Particle.REDSTONE, mPlayer.getEyeLocation(), 12, 0.5, 0.5, 0.5, 0.3, new Particle.DustOptions(Color.WHITE, 0.8f)).spawnAsPlayerPassive(mPlayer);
	}

	@Override public String toString() {
		return String.format("UnstableAmalgamEnhancement duration:%d from:%s", this.getDuration(), mPlayer.getName());
	}
}

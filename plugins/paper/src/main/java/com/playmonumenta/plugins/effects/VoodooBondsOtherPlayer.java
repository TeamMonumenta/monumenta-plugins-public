package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class VoodooBondsOtherPlayer extends Effect {
	public static final String effectID = "VoodooBondsOtherPlayer";

	private static final String SEND_EFFECT_NAME = "VoodooBondsDamageTaken";

	private final Player mPlayer;
	private final Plugin mPlugin;

	int mRotation = 0;
	private int mTransferDuration;
	private boolean mTriggerTickParticle = false;

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(13, 13, 13), 1.0f);

	public VoodooBondsOtherPlayer(int duration, int transferDuration, Player player, Plugin plugin) {
		super(duration, effectID);
		mPlayer = player;
		mPlugin = plugin;
		mTransferDuration = transferDuration;
	}

	@Override
	public void onHurt(LivingEntity entity, DamageEvent event) {
		if (event.isBlocked() || event.getType() == DamageType.TRUE) {
			return;
		}

		int duration = mTransferDuration;
		double damage = event.getFinalDamage(true);
		double maxHealth = EntityUtils.getMaxHealth(entity);
		double percentDamage = damage / maxHealth;

		LivingEntity source = event.getSource();
		if (source != null) {
			if (EntityUtils.isBoss(source)) {
				event.setDamage(event.getDamage() / 2);
			} else {
				event.setDamage(0);
				if (event.getDamager() instanceof LivingEntity le) {
					MovementUtils.knockAway(mPlayer.getLocation(), le, 0.3f, 0.15f, true);
				}
				event.setCancelled(true);
			}
		} else {
			// Ignore DoT damage
			if (event.getType() == DamageType.FIRE || event.getType() == DamageType.AILMENT) {
				return;
			}
			event.setDamage(0);
			event.setCancelled(true);
		}

		mTriggerTickParticle = true;
		// Add this effect immediately afterwards to avoid causing a ConcurrentModificationException
		Bukkit.getScheduler().runTask(mPlugin, () -> {
			mPlugin.mEffectManager.addEffect(mPlayer, SEND_EFFECT_NAME, new VoodooBondsReaper(duration, mPlayer, damage, percentDamage, mPlugin));
		});

		Location loc = entity.getLocation();
		World world = loc.getWorld();
		new PartialParticle(Particle.SPELL_WITCH, loc, 65, 1, 0.5, 1, 0.001).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, loc, 65, 1, 0.5, 1, 0, COLOR).spawnAsPlayerActive(mPlayer);
		world.playSound(loc, Sound.BLOCK_CHAIN_BREAK, 2f, 0.75f);
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (entity instanceof LivingEntity le) {
			mRotation += 20;
			Location loc = le.getLocation();
			World world = loc.getWorld();
			//replace with halo effect
			double angle = Math.toRadians(mRotation);
			loc.add(FastUtils.cos(angle) * 0.5, le.getHeight(), FastUtils.sin(angle) * 0.5);
			new PartialParticle(Particle.REDSTONE, loc, 5, 0, 0, 0, COLOR).spawnAsEnemy();
			if (mTriggerTickParticle) {
				setDuration(0);
				mTriggerTickParticle = false;
				//replace with better particles
				new PartialParticle(Particle.REDSTONE, loc, 60, 3, 3, 3, 0.075, COLOR).spawnAsEnemy();
				world.playSound(loc, Sound.BLOCK_ANVIL_FALL, SoundCategory.PLAYERS, 2f, 1.0f);
			}
		}
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return "Voodoo Bonds";
	}

	@Override
	public String toString() {
		return String.format("VoodooBondsOtherPlayer duration:%d", this.getDuration());
	}
}

package com.playmonumenta.plugins.effects;

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
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.warlock.reaper.VoodooBonds;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;

public class VoodooBondsOtherPlayer extends Effect {

	private static final String SEND_EFFECT_NAME = "VoodooBondsDamageTaken";
	private static final int DURATION_1 = 20 * 5;
	private static final int DURATION_2 = 20 * 7;

	private final Player mPlayer;
	private final Plugin mPlugin;

	private @Nullable VoodooBonds mVoodooBonds;
	private int mScore;
	int mRotation = 0;
	private boolean mTriggerTickParticle = false;

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(13, 13, 13), 1.0f);

	public VoodooBondsOtherPlayer(int duration, Player player, Plugin plugin) {
		super(duration);
		mPlayer = player;
		mPlugin = plugin;

		if (player != null) {
			Bukkit.getScheduler().runTask(mPlugin, () -> {
				mVoodooBonds = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, VoodooBonds.class);
				mScore = mVoodooBonds.getAbilityScore();
			});
		}
	}

	@Override
	public void onHurt(LivingEntity entity, DamageEvent event) {
		int duration = mScore == 1 ? DURATION_1 : DURATION_2;
		double damage = event.getDamage();
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
			}
		} else {
			// Ignore DoT damage
			if (event.getType() == DamageType.FIRE || event.getType() == DamageType.AILMENT) {
				return;
			}
			event.setDamage(0);
		}

		mTriggerTickParticle = true;
		// Add this effect immediately afterwards to avoid causing a ConcurrentModificationException
		Bukkit.getScheduler().runTask(mPlugin, () -> {
			mPlugin.mEffectManager.addEffect(mPlayer, SEND_EFFECT_NAME, new VoodooBondsReaper(duration, mPlayer, event.getDamage(), percentDamage, mPlugin));
		});

		Location loc = entity.getLocation();
		World world = loc.getWorld();
		world.spawnParticle(Particle.SPELL_WITCH, loc, 65, 1, 0.5, 1, 0.001);
		world.spawnParticle(Particle.REDSTONE, loc, 65, 1, 0.5, 1, 0, COLOR);
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
			world.spawnParticle(Particle.REDSTONE, loc, 5, 0, 0, 0, COLOR);
			if (mTriggerTickParticle) {
				setDuration(0);
				mTriggerTickParticle = false;
				//replace with better particles
				world.spawnParticle(Particle.REDSTONE, loc, 60, 3, 3, 3, 0.075, COLOR);
				world.playSound(loc, Sound.BLOCK_ANVIL_FALL, SoundCategory.PLAYERS, 2f, 1.0f);
			}
		}
	}

	@Override
	public String toString() {
		return String.format("VoodooBondsOtherPlayer duration:%d", this.getDuration());
	}
}

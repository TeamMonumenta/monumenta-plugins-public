package com.playmonumenta.plugins.effects;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.Particle;
import org.bukkit.Color;
import org.bukkit.entity.LivingEntity;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;

import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.warlock.reaper.VoodooBonds;

public class VoodooBondsOtherPlayer extends Effect {

	private static final String SEND_EFFECT_NAME = "VoodooBondsDamageTaken";
	private static final int DURATION_1 = 20 * 5;
	private static final int DURATION_2 = 20 * 7;

	private final Player mPlayer;
	private final Plugin mPlugin;

	private VoodooBonds mVoodooBonds;
	private int mScore;
	int mRotation = 0;
	private boolean mTriggerTickParticle = false;

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(13, 13, 13), 1.0f);

	public VoodooBondsOtherPlayer(int duration, Player player, Plugin plugin) {
		super(duration);
		mPlayer = player;
		mPlugin = plugin;

		Bukkit.getScheduler().runTask(mPlugin, () -> {
			if (mPlayer != null) {
				mVoodooBonds = AbilityManager.getManager().getPlayerAbility(mPlayer, VoodooBonds.class);
				mScore = mVoodooBonds.getAbilityScore();
			}
		});
	}

	@Override
	public boolean entityReceiveDamageEvent(EntityDamageEvent event) {
		int duration = mScore == 1 ? DURATION_1 : DURATION_2;
		Player p = (Player) event.getEntity();
		double damage = event.getFinalDamage();
		double maxHealth = p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
		double percentDamage = damage / maxHealth;

		mPlugin.mEffectManager.addEffect(mPlayer, SEND_EFFECT_NAME, new VoodooBondsReaper(duration, mPlayer, event.getDamage(), percentDamage, mPlugin));

		if (event instanceof EntityDamageByEntityEvent) {
			EntityDamageByEntityEvent entityEvent = (EntityDamageByEntityEvent) event;
			if (EntityUtils.isBoss(entityEvent.getDamager())) {
				event.setDamage(event.getDamage() / 2);
			} else {
				event.setDamage(0);
				if (entityEvent.getDamager() instanceof LivingEntity) {
					MovementUtils.knockAway(mPlayer.getLocation(), (LivingEntity) entityEvent.getDamager(), 0.3f, 0.15f);
				}
			}
		} else {
			event.setDamage(0);
		}

		mTriggerTickParticle = true;

		Location loc = event.getEntity().getLocation();
		World world = loc.getWorld();
		world.spawnParticle(Particle.SPELL_WITCH, loc, 65, 1, 0.5, 1, 0.001);
		world.spawnParticle(Particle.REDSTONE, loc, 65, 1, 0.5, 1, 0, COLOR);
		world.playSound(loc, Sound.BLOCK_CHAIN_BREAK, 2f, 0.75f);
		return true;
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (entity instanceof LivingEntity) {
			mRotation += 20;
			LivingEntity le = (LivingEntity) entity;
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

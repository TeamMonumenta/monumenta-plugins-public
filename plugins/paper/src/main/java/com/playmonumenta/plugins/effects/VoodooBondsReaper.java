package com.playmonumenta.plugins.effects;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.warlock.reaper.VoodooBonds;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class VoodooBondsReaper extends Effect {

	private static final double PERCENT_1 = 0.33;
	private static final double PERCENT_2 = 0.67;
	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(13, 13, 13), 1.0f);
	private static final String MESSAGE = "You feel a pull on your soul...";

	private final Player mPlayer;
	private final double mDamageTaken;
	private final double mDamagePercent;
	private final Plugin mPlugin;

	private @Nullable VoodooBonds mVoodooBonds;
	private int mScore;
	private boolean mDone = false;

	public VoodooBondsReaper(int duration, Player player, double damageTaken, double damagePercent, Plugin plugin) {
		super(duration);
		mPlayer = player;
		mDamageTaken = damageTaken;
		mDamagePercent = damagePercent;
		mPlugin = plugin;

		if (mPlayer != null) {
			Bukkit.getScheduler().runTask(mPlugin, () -> {
				mVoodooBonds = AbilityManager.getManager().getPlayerAbility(mPlayer, VoodooBonds.class);
				mScore = mVoodooBonds != null ? mVoodooBonds.getAbilityScore() : 0;
			});
		}
	}

	@Override
	public boolean entityDealDamageEvent(EntityDamageByEntityEvent event) {
		if (mScore == 0) {
			return true;
		}
		double percent = mScore == 1 ? PERCENT_1 : PERCENT_2;
		if (!EntityUtils.isBoss(event.getEntity())) {
			event.setDamage(event.getDamage() + mDamageTaken * percent);
		}
		Location loc = event.getEntity().getLocation();
		World world = loc.getWorld();
		//replace with better particles
		world.spawnParticle(Particle.SPELL_WITCH, loc, 65, 1, 0.5, 1, 0.001);
		world.spawnParticle(Particle.REDSTONE, loc, 65, 1, 0.5, 1, 0, COLOR);
		world.playSound(loc, Sound.BLOCK_CHAIN_BREAK, 2f, 0.75f);
		mDone = true;
		setDuration(0);
		return true;
	}

	@Override
	public void entityGainEffect(Entity entity) {
		MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, MESSAGE);
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (!mDone) {
			double absorbHealth = AbsorptionUtils.getAbsorption(mPlayer);
			if (absorbHealth <= 0) {
				mPlayer.setHealth(Math.max(Math.min(mPlayer.getHealth() - mPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * mDamagePercent, mPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()), 1));
			} else {
				if (mPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * mDamagePercent >= absorbHealth) {
					double leftoverHealth = mPlayer.getHealth() + absorbHealth - mPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * mDamagePercent;
					AbsorptionUtils.subtractAbsorption(mPlayer, absorbHealth);
					mPlayer.setHealth(Math.max(leftoverHealth, 1));
				} else {
					AbsorptionUtils.subtractAbsorption(mPlayer, absorbHealth);
				}
			}

			Location loc = mPlayer.getLocation();
			World world = loc.getWorld();
			world.spawnParticle(Particle.SPELL_WITCH, loc, 60, 0.5, 0.5, 0.5, 0.001);
			world.spawnParticle(Particle.REDSTONE, loc, 60, 0.5, 0.5, 0.5, 0, COLOR);
			world.playSound(loc, Sound.ENTITY_WITHER_SKELETON_HURT, 1f, 0.75f);
			world.playSound(loc, Sound.ENTITY_WITHER_SKELETON_HURT, 1f, 0.6f);
			world.playSound(loc, Sound.ENTITY_WITHER_SKELETON_HURT, 1f, 0.5f);
		}
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (oneHertz) {
			mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 0.5f);
		}
		World world = mPlayer.getLocation().getWorld();
		Location rightHand = PlayerUtils.getRightSide(mPlayer.getEyeLocation(), 0.45).subtract(0, .8, 0);
		Location leftHand = PlayerUtils.getRightSide(mPlayer.getEyeLocation(), -0.45).subtract(0, .8, 0);
		world.spawnParticle(Particle.REDSTONE, leftHand, 2, 0.05f, 0.05f, 0.05f, 0, COLOR);
		world.spawnParticle(Particle.SPELL_WITCH, leftHand, 1, 0.05, 0.05, 0.05, 0);
		world.spawnParticle(Particle.REDSTONE, rightHand, 2, 0.05f, 0.05f, 0.05f, 0, COLOR);
		world.spawnParticle(Particle.SPELL_WITCH, rightHand, 1, 0.05, 0.05, 0.05, 0);
	}

	@Override
	public String toString() {
		return String.format("VoodooBondsReaper duration:%d", this.getDuration());
	}
}

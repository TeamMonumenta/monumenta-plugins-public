package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TemporalFlux extends Effect {

	//private int mCurrentDuration;
	public static final String GENERIC_NAME = "Paradox";
	public static final String effectID = "TemporalFlux";
	public static final int MAX_TIME = 30 * 20;

	private BossBar mBossBar;

	public TemporalFlux(int duration) {
		super(duration, effectID);
		mBossBar = Bukkit.getServer().createBossBar(null, BarColor.BLUE, BarStyle.SOLID, BarFlag.PLAY_BOSS_MUSIC);
		mBossBar.setTitle(ChatColor.BLUE + "Paradox expires in " + (duration / 20) + "seconds!");
		mBossBar.setVisible(true);
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (oneHertz) {
			/*
			if (entity instanceof Player player && getDuration() <= 10) {
				if (player.getGameMode() == GameMode.SURVIVAL) {
					player.setHealth(0);
					player.addPotionEffect(new PotionEffect(PotionEffectType.HARM, 20, 100));
				}
			}
			 */
			entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_HIT, SoundCategory.HOSTILE, 30, 1);
		}

		if (fourHertz) {
			double progress = ((double) getDuration() / (double) MAX_TIME);
			mBossBar.setProgress(progress);
			mBossBar.setTitle(ChatColor.BLUE + "Paradox expires in " + (getDuration()/ 20) + " seconds!");
			if (progress <= 0.01) {
				com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(entity, "Stasis");
				PlayerUtils.executeCommandOnNearbyPlayers(entity.getLocation(), 30, "kill " + entity.getName());
				DamageUtils.damage(null, (LivingEntity) entity, DamageEvent.DamageType.OTHER, 999999999, null, true, false, "Temporal Flux");
				mBossBar.setVisible(false);
				mBossBar.removeAll();
			}
			if (progress <= 0.25) {
				mBossBar.setColor(BarColor.RED);
			} else if (progress <= 0.5) {
				mBossBar.setColor(BarColor.YELLOW);
			} else if (progress > 0.5) {
				mBossBar.setColor(BarColor.BLUE);
			}
			if (getDuration() % (20 * 10) == 0) {
				entity.sendMessage(ChatColor.RED + "Paradox has " + ChatColor.BOLD + getDuration()/20 + ChatColor.RESET + ChatColor.RED + " seconds remaining!");
			}
			new PPCircle(Particle.SOUL_FIRE_FLAME, entity.getLocation(), 1).ringMode(true)
				.count(20).delta(0.25, 0.1, 0.25).spawnAsBoss();
		}
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return ChatColor.RED + "Paradox \u2620";
	}

	@Override
	public void entityGainEffect(Entity entity) {
		if (entity instanceof Player player) {
			for (Player p : PlayerUtils.playersInRange(entity.getLocation(), 50, true)) {
				p.sendMessage(ChatColor.BLUE + "" + ChatColor.BOLD + "" + entity.getName() + ChatColor.RESET + " " + ChatColor.BLUE + "has been given the Paradox effect!");
			}
			player.sendMessage(ChatColor.RED + "You have been inflicted with Paradox! Quickly transfer " +
				"it using the " + ChatColor.GOLD + "Temporal Exchanger" + ChatColor.WHITE + "!");
			player.playSound(entity.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 20, 1);
			mBossBar.addPlayer((Player) entity);
		}
	}

	@Override
	public void onDeath(EntityDeathEvent event) {
		mBossBar.setVisible(false);
		mBossBar.removeAll();
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		mBossBar.setVisible(false);
		mBossBar.removeAll();
		entity.sendMessage(ChatColor.GRAY + "You are no longer inflicted with Paradox, you are safe for now.");
	}

	@Override
	public String toString() {
		return String.format(GENERIC_NAME + ":%d", this.getDuration());
	}

}

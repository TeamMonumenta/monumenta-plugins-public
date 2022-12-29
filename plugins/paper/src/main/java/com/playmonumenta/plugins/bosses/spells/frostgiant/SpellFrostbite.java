package com.playmonumenta.plugins.bosses.spells.frostgiant;

import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
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
import org.bukkit.scheduler.BukkitRunnable;

/*
 Hailstorm - Creates a snowstorm in a circle that is 18 blocks and beyond that passively
 deals 5% max health damage every half second to players are in it and giving them slowness
 3 for 2 seconds.
 */
public class SpellFrostbite extends Spell {

	private Location mStartLoc;
	private LivingEntity mBoss;
	private Plugin mPlugin;
	private boolean mAttack = false;
	private List<Player> mWarned = new ArrayList<Player>();

	public SpellFrostbite(Plugin plugin, LivingEntity boss, Location loc) {
		mPlugin = plugin;
		mBoss = boss;
		mStartLoc = loc;

		new BukkitRunnable() {
			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}
				mWarned.clear();
			}
		}.runTaskTimer(mPlugin, 0, 20 * 10);
	}

	@Override
	public void run() {
		mAttack = false;
		World world = mBoss.getWorld();
		for (Player player : PlayerUtils.playersInRange(mStartLoc, 70, true)) {
			Location playerLoc = player.getLocation();

			if (player.getGameMode() == GameMode.CREATIVE) {
				continue;
			}

			PotionEffect effect = player.getPotionEffect(PotionEffectType.JUMP);

			boolean damage = false;
			if (playerLoc.getY() - mStartLoc.getY() >= 4 && (effect == null || effect.getAmplifier() < 3) && (player.getGameMode() == GameMode.SURVIVAL || player.getLocation().distance(mStartLoc) < FrostGiant.fighterRange) && player.getLocation().getY() - mStartLoc.getY() <= 45) {
				damage = true;
				if (!mWarned.contains(player)) {
					player.sendMessage(ChatColor.RED + "The upper air is freezing!");
					mWarned.add(player);
				}
			} else if (playerLoc.getY() - mStartLoc.getY() <= -4) {
				damage = true;
				if (!mWarned.contains(player)) {
					player.sendMessage(ChatColor.RED + "The lower air is freezing!");
					mWarned.add(player);
				}
			}

			if (damage) {
				BossUtils.bossDamagePercent(mBoss, player, 0.15, "Frostbite");

				world.playSound(playerLoc, Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1, 1);
				new PartialParticle(Particle.FIREWORKS_SPARK, playerLoc.add(0, 1, 0), 15, 0.4, 0.4, 0.4, 0.15).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.SPIT, playerLoc, 6, 0.4, 0.4, 0.4, 0.2).spawnAsEntityActive(mBoss);
			}
		}
	}

	@Override
	public boolean canRun() {
		if (!mAttack) {
			mAttack = true;
			return false;
		}
		return mAttack;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

}

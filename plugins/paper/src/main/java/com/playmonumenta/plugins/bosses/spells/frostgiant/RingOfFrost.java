package com.playmonumenta.plugins.bosses.spells.frostgiant;

import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.ChatColor;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/*
 Ring of Frost: Fast(er) cast speed. All players 18 blocks away or
 greater take 28 damage and are thrown towards the Frost Giant. Afterwards
 they are given slowness 3 for 8 seconds. (lowish CD)
 */

public class RingOfFrost extends Spell {

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final double mRadius;
	private final Location mStartLoc;
	private final PPCircle mInnerCircle;
	private final PPCircle mOuterCircle1;
	private final PPCircle mOuterCircle2;
	private final PPCircle mExplodeCircle;

	public RingOfFrost(Plugin plugin, LivingEntity boss, double radius, Location loc) {
		mPlugin = plugin;
		mBoss = boss;
		mRadius = radius;
		mStartLoc = loc;

		mInnerCircle = new PPCircle(Particle.REDSTONE, loc, radius).ringMode(true).count(40).delta(0.05).extra(0.05).data(GREEN_COLOR);
		mOuterCircle1 = new PPCircle(Particle.DAMAGE_INDICATOR, loc, radius).ringMode(true).count(20).delta(1, 0.1, 1).extra(0.05);
		mOuterCircle2 = new PPCircle(Particle.DRAGON_BREATH, loc, radius).ringMode(true).count(20).delta(1, 0.1, 1).extra(0.05);
		mExplodeCircle = new PPCircle(Particle.EXPLOSION_NORMAL, loc, radius).ringMode(true).count(150).delta(1, 3, 1).extra(0.45);
	}

	private static final Particle.DustOptions GREEN_COLOR = new Particle.DustOptions(Color.fromRGB(0, 255, 0), 1.0f);

	@Override
	public void run() {
		FrostGiant.freezeGolems(mBoss);
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 3, 1.5f);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_POLAR_BEAR_WARNING, SoundCategory.HOSTILE, 5, 0);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 3, 0);
		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), FrostGiant.detectionRange, true)) {
			player.sendMessage(ChatColor.DARK_PURPLE + "The air away from the giant starts to freeze!");
		}
		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;
			float mPitch = 0.5f;

			@Override
			public void run() {
				Location loc = mBoss.getLocation().add(0, 0.1, 0);
				mTicks += 2;

				mInnerCircle.location(loc);
				mOuterCircle1.location(loc);
				mOuterCircle2.location(loc);

				mInnerCircle.spawnAsBoss();
				for (int r = 24; r > mRadius + 1; r -= 4) {
					mOuterCircle1.radius(r);
					mOuterCircle2.radius(r);
					mOuterCircle1.spawnAsBoss();
					mOuterCircle2.spawnAsBoss();
				}

				world.playSound(mBoss.getLocation(), Sound.ENTITY_SNOW_GOLEM_SHOOT, SoundCategory.HOSTILE, 3, mPitch);
				mPitch += 0.025f;

				world.spawnParticle(Particle.VILLAGER_HAPPY, loc.clone().add(0, 3, 0), 20, 2, 4, 2, 0);

				//After 4 seconds, damage everyone outside the green circle and pull them in
				if (mTicks >= 20 * 4) {
					FrostGiant.unfreezeGolems(mBoss);
					this.cancel();
					mExplodeCircle.location(mBoss.getLocation().add(0, 2, 0));
					mExplodeCircle.spawnAsBoss();
					world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 3, 1.5f);
					world.playSound(mBoss.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 3, 0.65f);
					Vector vec = loc.toVector();
					for (Player player : PlayerUtils.playersInRange(loc, 50, true)) {
						if (!player.getLocation().toVector().isInSphere(vec, mRadius) && mStartLoc.distance(player.getLocation()) <= FrostGiant.fighterRange) {
							DamageUtils.damage(mBoss, player, DamageType.MAGIC, 40, null, false, true, "Ring of Frost");
							world.spawnParticle(Particle.EXPLOSION_NORMAL, player.getLocation().add(0, 1, 0), 30, 0.25, 0.45, 0.25, 0.2);
							MovementUtils.knockAway(loc, player, -2.75f, 0.5f, false);
							player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 10, 3));
						}
					}
				}
			}

		};
		runnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int cooldownTicks() {
		return 20 * 6;
	}
}

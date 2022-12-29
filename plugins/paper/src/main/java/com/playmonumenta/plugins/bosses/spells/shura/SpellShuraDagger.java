package com.playmonumenta.plugins.bosses.spells.shura;

import com.playmonumenta.plugins.bosses.bosses.CShura;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSeekingProjectile;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
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

public class SpellShuraDagger extends Spell {

	private final LivingEntity mBoss;
	private final Plugin mPlugin;
	private final SpellBaseSeekingProjectile mMissile;

	private static final boolean SINGLE_TARGET = true;
	private static final boolean LAUNCH_TRACKING = false;
	private static final int COOLDOWN = 20 * 6;
	private static final int DELAY = 20 * 1;
	private static final double SPEED = 0.8;
	private static final double TURN_RADIUS = 0;
	private static final int DISTANCE = 32;
	private static final int LIFETIME_TICKS = (int) (DISTANCE / SPEED);
	private static final double HITBOX_LENGTH = 0.5;
	private static final boolean COLLIDES_WITH_BLOCKS = false;
	private static final boolean LINGERS = true;
	private static final int DAMAGE = 20;

	public SpellShuraDagger(LivingEntity boss, Plugin plugin) {
		mBoss = boss;
		mPlugin = plugin;

		mMissile = new SpellBaseSeekingProjectile(plugin, boss, CShura.detectionRange, SINGLE_TARGET, LAUNCH_TRACKING, COOLDOWN, DELAY,
			SPEED, TURN_RADIUS, LIFETIME_TICKS, HITBOX_LENGTH, COLLIDES_WITH_BLOCKS, LINGERS,
			// Initiate Aesthetic
			(World world, Location loc, int ticks) -> {
				PotionUtils.applyPotion(null, mBoss, new PotionEffect(PotionEffectType.GLOWING, DELAY, 0));

				if (ticks % 4 == 0) {
					new PartialParticle(Particle.SMOKE_NORMAL, loc, 8, 0.5, 0.5, 0.5, 0.2).spawnAsEntityActive(mBoss);
				}
			},
			// Launch Aesthetic
			(World world, Location loc, int ticks) -> {
				world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 3, 1.2f);
			},
			// Projectile Aesthetic
			(World world, Location loc, int ticks) -> {
				new PartialParticle(Particle.FIREWORKS_SPARK, loc, 4, 0.1, 0.1, 0.1, 0.05).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.SMOKE_NORMAL, loc, 4, 0.25, 0.25, 0.25, 0.05).spawnAsEntityActive(mBoss);
			},
			// Hit Action
			(World world, LivingEntity le, Location loc, Location prevLoc) -> {
				loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.HOSTILE, 1, 1);
				new PartialParticle(Particle.FIREWORKS_SPARK, loc, 20, 0.5, 0.5, 0.5, 0.5).spawnAsEntityActive(mBoss);
				if (le instanceof Player player) {
					BossUtils.blockableDamage(boss, player, DamageEvent.DamageType.MAGIC, DAMAGE, "Arcane Dagger", prevLoc);
					PotionUtils.applyPotion(com.playmonumenta.plugins.Plugin.getInstance(), player, new PotionEffect(PotionEffectType.POISON, 6 * 20, 0));
					AbilityUtils.increaseHealingPlayer(player, 6 * 20, -0.5, "Shura");
				}
			});
	}

	@Override
	public void run() {
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), CShura.detectionRange, true);
		Collections.shuffle(players);
		if (players.size() == 0 || mBoss.getTargetBlock(CShura.detectionRange) == null) {
			return;
		}

		//Used for the launch method, does not actually target this player
		Player player = players.get(FastUtils.RANDOM.nextInt(players.size()));

		new BukkitRunnable() {
			private int mTicks = 0;

			@Override
			public void run() {
				if (mTicks >= DELAY) {

					mMissile.launch(player, mBoss.getEyeLocation().add(0, 0, 1));
					mMissile.launch(player, mBoss.getEyeLocation().add(0, 0, -1));
					mMissile.launch(player, mBoss.getEyeLocation().add(-1, 0, 0));
					mMissile.launch(player, mBoss.getEyeLocation().add(1, 0, 0));

					mMissile.launch(player, mBoss.getEyeLocation().add(1, 0, 1));
					mMissile.launch(player, mBoss.getEyeLocation().add(-1, 0, -1));
					mMissile.launch(player, mBoss.getEyeLocation().add(-1, 0, 1));
					mMissile.launch(player, mBoss.getEyeLocation().add(1, 0, -1));

					Runnable runnable = () -> players.forEach(p -> mMissile.launch(p, p.getEyeLocation()));
					Bukkit.getScheduler().runTaskLater(mPlugin, runnable, 10);
					Bukkit.getScheduler().runTaskLater(mPlugin, runnable, 20);

					this.cancel();
				}

				PotionUtils.applyPotion(null, mBoss, new PotionEffect(PotionEffectType.GLOWING, DELAY, 0));

				if (mTicks % 4 == 0) {
					new PartialParticle(Particle.SMOKE_NORMAL, mBoss.getLocation(), 4, 0.5, 0.5, 0.5, 0.2).spawnAsEntityActive(mBoss);
				}

				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int cooldownTicks() {
		return 6 * 20;
	}
}

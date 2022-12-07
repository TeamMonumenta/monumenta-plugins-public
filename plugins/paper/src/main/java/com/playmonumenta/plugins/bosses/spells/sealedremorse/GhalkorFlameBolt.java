package com.playmonumenta.plugins.bosses.spells.sealedremorse;

import com.playmonumenta.plugins.bosses.bosses.Ghalkor;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSeekingProjectile;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.List;
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

public class GhalkorFlameBolt extends Spell {

	private SpellBaseSeekingProjectile mMissile;

	private LivingEntity mBoss;
	private Plugin mPlugin;
	private Ghalkor mBossClass;

	private static final boolean SINGLE_TARGET = true;
	private static final boolean LAUNCH_TRACKING = false;
	private static final int COOLDOWN = 20 * 8;
	private static final int DELAY = 20 * 1;
	private static final double SPEED = 0.8;
	private static final double TURN_RADIUS = 0;
	private static final int DISTANCE = 32;
	private static final int LIFETIME_TICKS = (int) (DISTANCE / SPEED);
	private static final double HITBOX_LENGTH = 0.5;
	private static final boolean COLLIDES_WITH_BLOCKS = false;
	private static final boolean LINGERS = true;
	private static final int DAMAGE = 26;

	public GhalkorFlameBolt(LivingEntity boss, Plugin plugin, Ghalkor bossClass) {
		mBoss = boss;
		mPlugin = plugin;
		mBossClass = bossClass;

		mMissile = new SpellBaseSeekingProjectile(plugin, boss, Ghalkor.detectionRange, SINGLE_TARGET, LAUNCH_TRACKING, COOLDOWN, DELAY,
			SPEED, TURN_RADIUS, LIFETIME_TICKS, HITBOX_LENGTH, COLLIDES_WITH_BLOCKS, LINGERS,
			// Initiate Aesthetic
			(World world, Location loc, int ticks) -> {
				PotionUtils.applyPotion(null, mBoss, new PotionEffect(PotionEffectType.GLOWING, DELAY, 0));

				if (ticks % 4 == 0) {
					new PartialParticle(Particle.SOUL_FIRE_FLAME, loc, 8, 0.5, 0.5, 0.5, 0.2).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.FLAME, loc, 8, 0.5, 0.5, 0.5, 0.2).spawnAsEntityActive(mBoss);
				}
			},
			// Launch Aesthetic
			(World world, Location loc, int ticks) -> {
				world.playSound(loc, Sound.ENTITY_BLAZE_BURN, SoundCategory.HOSTILE, 1, 2);
				world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 3, 1.2f);
			},
			// Projectile Aesthetic
			(World world, Location loc, int ticks) -> {
				new PartialParticle(Particle.SOUL_FIRE_FLAME, loc, 4, 0.1, 0.1, 0.1, 0.05).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.FLAME, loc, 4, 0.25, 0.25, 0.25, 0.05).spawnAsEntityActive(mBoss);
			},
			// Hit Action
			(World world, LivingEntity player, Location loc) -> {
				loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_DEATH, SoundCategory.HOSTILE, 1, 2);
				new PartialParticle(Particle.FLAME, loc, 20, 0.5, 0.5, 0.5, 0.5).spawnAsEntityActive(mBoss);
				if (player != null) {
					BossUtils.blockableDamage(boss, player, DamageType.MAGIC, DAMAGE, "Flame Bolt", boss.getLocation());
					EntityUtils.applyFire(com.playmonumenta.plugins.Plugin.getInstance(), 4 * 20, player, boss);
				}
			});
	}

	@Override
	public void run() {
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), Ghalkor.detectionRange, true);
		if (players.size() == 0 || mBoss.getTargetBlock(Ghalkor.detectionRange) == null) {
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

					for (Player p : players) {
						mMissile.launch(p, p.getEyeLocation());
					}

					this.cancel();
				}

				PotionUtils.applyPotion(null, mBoss, new PotionEffect(PotionEffectType.GLOWING, DELAY, 0));

				if (mTicks % 4 == 0) {
					new PartialParticle(Particle.SOUL_FIRE_FLAME, mBoss.getLocation(), 4, 0.5, 0.5, 0.5, 0.2).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.FLAME, mBoss.getLocation(), 4, 0.5, 0.5, 0.5, 0.2).spawnAsEntityActive(mBoss);
				}

				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int cooldownTicks() {
		return (int) (7 * 20 * mBossClass.mCastSpeed);
	}
}

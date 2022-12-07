package com.playmonumenta.plugins.bosses.spells.falsespirit;

import com.playmonumenta.plugins.bosses.bosses.FalseSpirit;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSeekingProjectile;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class NothingnessSeeker extends SpellBaseSeekingProjectile {

	private static final boolean SINGLE_TARGET = true;
	private static final boolean LAUNCH_TRACKING = false;
	private static final int COOLDOWN = 20 * 6;
	private static final int DELAY = 20 * 1;
	private static final double TURN_RADIUS = Math.PI / 90;
	private static final int LIFETIME_TICKS = 20 * 8;
	private static final double HITBOX_LENGTH = 0.3;
	private static final boolean COLLIDES_WITH_BLOCKS = true;
	private static final boolean LINGERS = true;

	private LivingEntity mBoss;
	private Plugin mPlugin;
	private int mTimer;
	private int mCooldown = 0;
	private List<Player> mNoTarget = new ArrayList<>();

	private static final Particle.DustOptions GREEN_COLOR = new Particle.DustOptions(Color.fromRGB(0, 255, 0), 1.0f);

	public NothingnessSeeker(Plugin plugin, LivingEntity boss, int timer, double speed, boolean delve) {
		super(plugin, boss, FalseSpirit.detectionRange, SINGLE_TARGET, LAUNCH_TRACKING, COOLDOWN, DELAY,
			speed, TURN_RADIUS, LIFETIME_TICKS, HITBOX_LENGTH, COLLIDES_WITH_BLOCKS, LINGERS,
			// Initiate Aesthetic
			(World world, Location loc, int ticks) -> {
				world.playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.HOSTILE, 1f, 0.5f);
			},
			// Launch Aesthetic
			(World world, Location loc, int ticks) -> {
				new PartialParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 0).spawnAsEntityActive(boss);
				world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 3f, 2);
			},
			// Projectile Aesthetic
			(World world, Location loc, int ticks) -> {
				new PartialParticle(Particle.REDSTONE, loc, 6, 0.6, 0.6, 0.6, 0.1, GREEN_COLOR).spawnAsEntityActive(boss);
				new PartialParticle(Particle.SMOKE_LARGE, loc, 2, 0.25, 0.25, 0.25, 0).spawnAsEntityActive(boss);
				if (ticks % 2 == 0) {
					world.playSound(loc, Sound.ENTITY_ARROW_SHOOT, 0.1f, 0.6f);
				}
			},
			// Hit Action
			(World world, LivingEntity player, Location loc) -> {
				world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 0.5f, 0.5f);
				new PartialParticle(Particle.REDSTONE, loc, 50, 0.5, 0.5, 0.5, 0.25, GREEN_COLOR).spawnAsEntityActive(boss);
				if (player != null) {
					if (delve) {
						BossUtils.blockableDamage(boss, player, DamageType.MAGIC, 30, "Nothingness Seeker", boss.getLocation());
					} else {
						BossUtils.blockableDamage(boss, player, DamageType.MAGIC, 25, "Nothingness Seeker", boss.getLocation());
					}
				}
			});
		mBoss = boss;
		mPlugin = plugin;
		mTimer = timer;
	}

	@Override
	public void run() {
		mCooldown -= 5;
		if (mCooldown <= 0) {
			mCooldown = mTimer;

			//List is sorted with nearest players earlier in the list, and farthest players at the end
			List<Player> players = EntityUtils.getNearestPlayers(mBoss.getLocation(), FalseSpirit.detectionRange);

			new PartialParticle(Particle.VILLAGER_ANGRY, mBoss.getLocation(), 20, 1, 1, 1, 0).spawnAsEntityActive(mBoss);

			for (Player player : players) {
				mBoss.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_HIT, SoundCategory.HOSTILE, 1f, 0.5f);
				new PartialParticle(Particle.VILLAGER_ANGRY, player.getLocation(), 25, 0.5, 0.5, 0.5, 0).spawnAsEntityActive(mBoss);
			}

			new BukkitRunnable() {
				@Override
				public void run() {
					if (players.size() > 0) {
						if (players.size() > 1) {
							Player playerTwo = players.get(players.size() - 2);
							if (!mNoTarget.contains(playerTwo)) {
								launch(playerTwo, playerTwo.getEyeLocation());
							}
						}
						Player playerOne = players.get(players.size() - 1);
						if (!mNoTarget.contains(playerOne)) {
							launch(playerOne, playerOne.getEyeLocation());
						}
					}
				}
			}.runTaskLater(mPlugin, 20);
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		//If player dies, no targetting for 60 seconds
		if (damagee instanceof Player player && event.getFinalDamage(true) > player.getHealth()) {
			mNoTarget.add(player);
			new BukkitRunnable() {
				@Override
				public void run() {
					mNoTarget.remove(player);
				}
			}.runTaskLater(mPlugin, 20 * 60);
		}
	}
}

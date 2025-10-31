package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.TheImpenetrable;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.Collection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class TransientCrystals extends Spell {
	private static final int ATTACK_DAMAGE = 75;

	private static final double LINE_LENGTH = 5;
	private static final double LINE_HITBOX_SIZE = 0.25;

	private static final int FOLLOW_DURATION = 17;
	private static final int WINDUP_DURATION = 8;

	private static final int BONUS_SLASH_DELAY = 28;

	private final int mSlashes;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;

	public TransientCrystals(Plugin plugin, LivingEntity boss, int slashes) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = boss.getWorld();
		mSlashes = slashes;
	}

	@Override
	public void run() {
		for (int i = 0; i < mSlashes; i++) {
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				Collection<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), TheImpenetrable.OUTER_RADIUS, true);
				for (Player player : players) {
					createSlash(player);
				}
			}, (long) i * BONUS_SLASH_DELAY);
		}

		// Boss telegraph effects
		mWorld.playSound(mBoss.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_2, SoundCategory.HOSTILE, 5f, 0.5f);

		for (int j = 0; j < 4; j++) {
			Vector telegraphDir = VectorUtils.rotateYAxis(VectorUtils.rotateXAxis(new Vector(0, 3, 0), FastUtils.randomDoubleInRange(0, 60)), FastUtils.randomDoubleInRange(0, 360));
			new PPLine(Particle.DRAGON_BREATH, mBoss.getLocation().clone().add(telegraphDir), mBoss.getLocation())
				.countPerMeter(15)
				.spawnAsBoss();
		}
	}

	private void createSlash(Player player) {
		if (mBoss.isDead()) {
			return;
		}

		player.playSound(player.getLocation(), Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.HOSTILE, 1f, 0.5f);

		double theta = FastUtils.randomDoubleInRange(0, Math.PI * 2);
		Vector direction = new Vector(FastUtils.cos(theta) * LINE_LENGTH / 2, 0, FastUtils.sin(theta) * LINE_LENGTH / 2);
		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;
			@Nullable Location mFinalLocation = null;

			@Override
			public void run() {
				if (mTicks < FOLLOW_DURATION) {
					Location targetCenter = player.getLocation().clone().add(0, 0.75, 0);
					new PPLine(Particle.ELECTRIC_SPARK, targetCenter.clone().add(direction), targetCenter.clone().subtract(direction))
						.countPerMeter(10)
						.spawnForPlayer(ParticleCategory.BOSS, player);

					if (mTicks % 4 == 0) {
						player.playSound(player.getLocation(), Sound.BLOCK_CHAIN_BREAK, SoundCategory.HOSTILE, 0.3f, 0.5f + (float) mTicks / FOLLOW_DURATION);
						player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, SoundCategory.HOSTILE, 0.5f, 1.2f + (float) mTicks / FOLLOW_DURATION);
					}
				} else if (mTicks < FOLLOW_DURATION + WINDUP_DURATION) {
					if (mFinalLocation == null) {
						mFinalLocation = player.getLocation();

						player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, SoundCategory.HOSTILE, 1, 0.65f);
						player.playSound(player.getLocation(), Sound.ITEM_LODESTONE_COMPASS_LOCK, SoundCategory.HOSTILE, 1, 0.5f);
					}
					Location targetCenter = mFinalLocation.clone().add(0, 0.75, 0);
					new PPLine(Particle.ELECTRIC_SPARK, targetCenter.clone().add(direction), targetCenter.clone().subtract(direction))
						.countPerMeter(10)
						.spawnForPlayer(ParticleCategory.BOSS, player);
				} else if (mTicks == FOLLOW_DURATION + WINDUP_DURATION) {
					if (mFinalLocation != null) {
						Location targetCenter = mFinalLocation.clone().add(0, 0.75, 0);
						new PPLine(Particle.SPELL_WITCH, targetCenter.clone().add(direction), targetCenter.clone().subtract(direction))
							.countPerMeter(10)
							.spawnForPlayer(ParticleCategory.BOSS, player);

						player.playSound(player.getLocation(), Sound.BLOCK_BEEHIVE_SHEAR, SoundCategory.HOSTILE, 1, 0.55f);
						player.playSound(player.getLocation(), Sound.BLOCK_BEEHIVE_SHEAR, SoundCategory.HOSTILE, 1, 0.55f);
						player.playSound(player.getLocation(), Sound.BLOCK_BEEHIVE_SHEAR, SoundCategory.HOSTILE, 1, 0.55f);

						Location start = mFinalLocation.clone().subtract(direction);
						Vector step = direction.clone().normalize().multiply(LINE_HITBOX_SIZE);
						for (double i = 0; i < 1; i += LINE_HITBOX_SIZE / LINE_LENGTH / 2) {
							Hitbox hitbox = new Hitbox.AABBHitbox(mWorld, new BoundingBox(start.clone().getX() - LINE_HITBOX_SIZE, start.clone().getY() - 10, start.clone().getZ() - LINE_HITBOX_SIZE,
								start.clone().getX() + LINE_HITBOX_SIZE, start.clone().getY() + 10, start.clone().getZ() + LINE_HITBOX_SIZE));
							if (hitbox.getHitPlayers(true).contains(player)) {
								DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, ATTACK_DAMAGE, null, true, true, "Transient Crystals");
								player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.HOSTILE, 1, 0.7f);
								break;
							}

							start.add(step);
						}
					}

					this.cancel();
					return;
				}

				mTicks++;
				if (mBoss.isDead()) {
					this.cancel();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int cooldownTicks() {
		return FOLLOW_DURATION + WINDUP_DURATION + 20;
	}
}

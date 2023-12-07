package com.playmonumenta.plugins.depths.bosses.spells.vesperidys;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.AbilitySilence;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellShadowCrystalVoidGrenades extends Spell {
	private static final Particle.DustOptions BLACK_COLOR = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.0f);
	private static final Particle.DustOptions DARK_PURPLE_COLOR = new Particle.DustOptions(Color.fromRGB(97, 5, 105), 1.0f);
	private static final int DURATION = 8 * 20;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final double mRange;
	private final int mCooldownTicks;

	private static final int BLAST_DAMAGE = 55;
	private static final int LINGERING_DAMAGE = 10;
	private static final int SILENCE_DURATION = 5 * 20;

	public SpellShadowCrystalVoidGrenades(Plugin plugin, LivingEntity entity, double range, int cooldown) {
		mPlugin = plugin;
		mBoss = entity;
		mRange = range;
		mCooldownTicks = cooldown;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		// Choose random player within range that has line of sight to boss
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), mRange, false);
		players.removeIf(player -> player.getLocation().distance(mBoss.getLocation()) <= 3);
		if (players.isEmpty()) {
			return;
		}

		open();
		Bukkit.getScheduler().runTaskLater(mPlugin, this::close, mCooldownTicks / 2);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITCH_CELEBRATE, SoundCategory.HOSTILE, 3, 1.5f);

		new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				mTicks++;

				new PartialParticle(Particle.SOUL, mBoss.getLocation(), 40, 0, 0, 0, 0.1).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.SMOKE_NORMAL, mBoss.getLocation(), 35, 0, 0, 0, 0.1).spawnAsEntityActive(mBoss);
				world.playSound(mBoss.getLocation(), Sound.BLOCK_DISPENSER_LAUNCH, SoundCategory.HOSTILE, 3, 0.75f);
				Collections.shuffle(players);
				for (Player player : players) {
					if (LocationUtils.hasLineOfSight(mBoss, player)) {
						launch(player);
						break;
					}
				}
				if (mTicks >= 3) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 20, 20);
	}

	public void launch(Player target) {
		Location sLoc = mBoss.getLocation();
		sLoc.setY(sLoc.getY() + 1.7f);
		try {
			FallingBlock fallingBlock = sLoc.getWorld().spawnFallingBlock(sLoc, Material.NETHERITE_BLOCK.createBlockData());
			fallingBlock.setDropItem(false);
			EntityUtils.disableBlockPlacement(fallingBlock);

			Location pLoc = target.getLocation();
			Location tLoc = fallingBlock.getLocation();
			Vector vect = new Vector(pLoc.getX() - tLoc.getX(), 0, pLoc.getZ() - tLoc.getZ());
			vect.normalize().multiply(pLoc.distance(tLoc) / 20).setY(0.7f);
			fallingBlock.setVelocity(vect);

			PartialParticle flameTrail = new PartialParticle(Particle.SOUL, fallingBlock.getLocation(), 3, 0.25, .25, .25, 0.025);
			PartialParticle smokeTrail = new PartialParticle(Particle.SPELL_WITCH, fallingBlock.getLocation(), 2, 0.25, .25, .25, 0.025);

			new BukkitRunnable() {
				final World mWorld = mBoss.getWorld();
				@Override
				public void run() {
					// Particles while flying through the air
					Location particleLoc = fallingBlock.getLocation().add(0, fallingBlock.getHeight() / 2, 0);
					flameTrail.location(particleLoc).spawnAsBoss();
					smokeTrail.location(particleLoc).spawnAsBoss();

					if (fallingBlock.isOnGround() || !fallingBlock.isValid()) {
						// Landed on ground
						fallingBlock.remove();
						Location loc = fallingBlock.getLocation();

						new PartialParticle(Particle.SOUL, loc, 150, 0, 0, 0, 0.165).spawnAsBoss();
						new PartialParticle(Particle.SPELL_WITCH, loc, 65, 0, 0, 0, 0.1).spawnAsBoss();
						new PartialParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 0).minimumCount(1).spawnAsBoss();
						mWorld.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2, 0.85f);

						for (Player player : PlayerUtils.playersInRange(loc, 3, true)) {
							DamageUtils.damage(mBoss, player, DamageType.BLAST, BLAST_DAMAGE, null, false, true, "Void Grenades");
							if (!mPlugin.mEffectManager.hasEffect(player, "VoidGrenadeSilence")) {
								player.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 0.5f);
								mPlugin.mEffectManager.addEffect(player, "VoidGrenadeSilence", new AbilitySilence(SILENCE_DURATION));
							}
						}

						Location alternateHeight = loc.clone();
						alternateHeight.setY(loc.getY() + 0.5);
						PartialParticle marker1 = new PartialParticle(Particle.SOUL, alternateHeight, 4, 1, 0.15, 1, 0.025);
						PartialParticle marker2 = new PartialParticle(Particle.REDSTONE, alternateHeight, 10, 1, 1, 1, 0.15, BLACK_COLOR);
						PPCircle circle = new PPCircle(Particle.REDSTONE, alternateHeight, 3).count(15).data(DARK_PURPLE_COLOR);

						new BukkitRunnable() {
							int mTicks = 0;
							@Override
							public void run() {
								mTicks += 2;
								marker1.spawnAsBoss();
								marker2.spawnAsBoss();
								circle.spawnAsBoss();

								if (mTicks % 10 == 0) {
									for (Player player : PlayerUtils.playersInRange(loc, 3, true)) {
										DamageUtils.damage(mBoss, player, DamageType.MAGIC, LINGERING_DAMAGE, null, true, true, "Void Grenades");
										if (!mPlugin.mEffectManager.hasEffect(player, "VoidGrenadeSilence")) {
											player.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 0.5f);
											mPlugin.mEffectManager.addEffect(player, "VoidGrenadeSilence", new AbilitySilence(SILENCE_DURATION));
										}
									}
								}

								if (mBoss.isDead() || mTicks >= DURATION) {
									this.cancel();
								}
							}

						}.runTaskTimer(mPlugin, 10, 2);
						this.cancel();
					}
				}

			}.runTaskTimer(mPlugin, 0, 1);
		} catch (Exception e) {
			MMLog.warning("Failed to summon grenade for hellzone grenade toss: " + e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}

	public void open() {
		if (mBoss instanceof Shulker shulker) {
			shulker.setPeek(1.0f);
		}
	}

	public void close() {
		if (mBoss instanceof Shulker shulker) {
			shulker.setPeek(0.0f);
		}
	}
}

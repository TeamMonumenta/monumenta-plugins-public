package com.playmonumenta.plugins.bosses.spells.frostgiant;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.ArrayList;
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
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public final class SpellFrostRift extends Spell {
	public static final Material RIFT_BLOCK_TYPE = Material.BLACKSTONE;

	private static final String SPELL_NAME = "Frost Rift";
	private static final String SLOWNESS_SRC = "FrostRiftSlowness";
	private static final int DEBUFF_DURATION = Constants.TICKS_PER_SECOND * 6;
	private static final int CHARGE_DURATION = Constants.TICKS_PER_SECOND * 2;
	private static final int LINGER_DURATION = Constants.TICKS_PER_SECOND * 18;
	private static final Particle.DustOptions BLACK_COLOR = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.0f);

	private final Plugin mPlugin;
	private final FrostGiant mFrostGiant;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final List<Block> mChangedBlocks = new ArrayList<>();
	private final ChargeUpManager mChargeManager;

	private boolean mCooldown = false;

	public SpellFrostRift(final Plugin plugin, final FrostGiant frostGiant) {
		mPlugin = plugin;
		mFrostGiant = frostGiant;
		mBoss = mFrostGiant.mBoss;
		mWorld = mBoss.getWorld();
		mChargeManager = FrostGiant.defaultChargeUp(mBoss, CHARGE_DURATION, "Charging " + SPELL_NAME + "...");
	}

	@Override
	public void run() {
		mCooldown = true;
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mCooldown = false, Constants.TICKS_PER_SECOND * 25);

		final List<Player> players = (List<Player>) mFrostGiant.getArenaParticipants();
		if (players.isEmpty()) {
			return;
		}

		final int cap = Math.min(players.size() / 2, 3);
		final List<Location> playerAttackLocs = new ArrayList<>();
		Collections.shuffle(players);

		if (players.size() > 1) {
			for (int i = 0; i < cap; i++) {
				playerAttackLocs.add(players.get(i).getLocation());
			}
		} else {
			playerAttackLocs.add(players.get(0).getLocation());
		}
		mBoss.setAI(false);

		final int chargeRunnablePeriod = 2;

		new BukkitRunnable() {
			float mPitch = 1;
			final Location mLoc = mBoss.getLocation().add(0, 0.5, 0);

			@Override
			public void run() {
				playerAttackLocs.forEach(loc -> new PPLine(Particle.SQUID_INK, mLoc, LocationUtils.getDirectionTo(loc, mLoc).setY(0), 30)
					.countPerMeter(2).delta(0.25).extra(0).spawnAsEntityActive(mBoss));
				mWorld.playSound(mLoc, Sound.BLOCK_ANVIL_LAND, SoundCategory.HOSTILE, 0.5f, mPitch);
				new PartialParticle(Particle.CLOUD, mLoc, 6, 1, 0.1, 1, 0.25).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.SMOKE_LARGE, mLoc, 5, 1, 0.1, 1, 0.25).spawnAsEntityActive(mBoss);
				mPitch += 0.025f;

				if (mChargeManager.nextTick(chargeRunnablePeriod)) {
					mChargeManager.reset();
					playerAttackLocs.forEach(location -> createRift(location, mLoc, players));
					mBoss.setAI(true);
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, chargeRunnablePeriod);
	}

	/* TODO: Use custom hitboxes to reduce the amount of checks this needs to do */
	private void createRift(final Location playerAttackLoc, final Location initialBossLoc, final List<Player> players) {
		/* Each location is centered on a block and while the rift lingers there is a hitbox on it */
		final List<Location> locs = new ArrayList<>();

		final BukkitRunnable travelAcrossArena = new BukkitRunnable() {
			final Location mLoc = initialBossLoc.clone().add(0, 0.5, 0);
			final Vector mDir = LocationUtils.getDirectionTo(playerAttackLoc, mLoc).setY(0).normalize();
			final BoundingBox mBox = BoundingBox.of(mLoc, 0.85, 0.35, 0.85);

			@Override
			public void run() {
				mBox.shift(mDir.clone().multiply(1.25));
				final Location bLoc = mBox.getCenter().toLocation(mWorld);

				//Allows the rift to climb up and down blocks
				if (bLoc.getBlock().getType().isSolid()) {
					bLoc.add(0, 1, 0);
					if (bLoc.getBlock().getType().isSolid()) {
						this.cancel();
						bLoc.subtract(0, 1, 0);
					}
				}

				if (!bLoc.subtract(0, 1, 0).getBlock().getType().isSolid()) {
					bLoc.subtract(0, 1, 0);
					if (!bLoc.getBlock().getType().isSolid()) {
						bLoc.subtract(0, 1, 0);
						if (!bLoc.getBlock().getType().isSolid()) {
							this.cancel();
						}
					}
				}

				if (TemporaryBlockChangeManager.INSTANCE.changeBlock(bLoc.getBlock(), RIFT_BLOCK_TYPE, LINGER_DURATION)) {
					mChangedBlocks.add(bLoc.getBlock());
				}

				bLoc.add(0, 0.5, 0);
				locs.add(bLoc);
				new PartialParticle(Particle.CLOUD, bLoc, 3, 0.5, 0.5, 0.5, 0.25).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.EXPLOSION_NORMAL, bLoc, 3, 0.5, 0.5, 0.5, 0.125).spawnAsEntityActive(mBoss);
				mWorld.playSound(bLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.HOSTILE, 1, 0.85f);

				for (final Player player : players) {
					if (player.getBoundingBox().overlaps(mBox)) {
						DamageUtils.damage(mBoss, player, DamageType.MAGIC, 30, null, false, true, SPELL_NAME);
					}
				}

				/* Safeguard in case the runnable isn't canceled by any of the previous checks */
				if (bLoc.distanceSquared(initialBossLoc) >= FrostGiant.ARENA_LENGTH * FrostGiant.ARENA_LENGTH) {
					this.cancel();
				}
			}

			@Override
			public synchronized void cancel() {
				super.cancel();

				if (mBoss.isDead() || !mBoss.isValid()) {
					TemporaryBlockChangeManager.INSTANCE.revertChangedBlocks(mChangedBlocks, RIFT_BLOCK_TYPE);
				}
			}
		};
		travelAcrossArena.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(travelAcrossArena);

		final BukkitRunnable debuffUponRiftContact = new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				mT += 5;
				for (final Location loc : locs) {
					new PartialParticle(Particle.CLOUD, loc, 1, 0.5, 0.5, 0.5, 0.075).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.CRIT, loc, 1, 0.5, 0.5, 0.5, 0.075).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.REDSTONE, loc, 1, 0.5, 0.5, 0.5, 0.075, BLACK_COLOR).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 1, 0.5, 0.5, 0.5, 0.1).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.DAMAGE_INDICATOR, loc, 1, 0.5, 0.5, 0.5, 0.1).spawnAsEntityActive(mBoss);
					final BoundingBox box = BoundingBox.of(loc, 0.85, 1.2, 0.85);
					for (final Player player : players) {
						if (player.getBoundingBox().overlaps(box)) {
							DamageUtils.damage(mBoss, player, DamageType.MAGIC, 20, null, false, true, SPELL_NAME);
							com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(player, SLOWNESS_SRC,
								new PercentSpeed(DEBUFF_DURATION, -0.5, SLOWNESS_SRC));
							player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, DEBUFF_DURATION, 49));
						}
					}
				}

				if (mT >= LINGER_DURATION || mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}
			}
		};
		debuffUponRiftContact.runTaskTimer(mPlugin, 0, 5);
		mActiveRunnables.add(debuffUponRiftContact);
	}

	@Override
	public void cancel() {
		super.cancel();

		TemporaryBlockChangeManager.INSTANCE.revertChangedBlocks(mChangedBlocks, RIFT_BLOCK_TYPE);
		mChangedBlocks.clear();
	}

	@Override
	public boolean canRun() {
		return !mCooldown;
	}

	@Override
	public int cooldownTicks() {
		return Constants.TICKS_PER_SECOND * 7;
	}
}

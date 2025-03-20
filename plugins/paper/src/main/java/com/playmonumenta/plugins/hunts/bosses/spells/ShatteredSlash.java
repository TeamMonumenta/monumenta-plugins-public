package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellSlashAttack;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.TheImpenetrable;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ShatteredSlash extends Spell {
	// Lifetime of the windup portion of the spell in ticks
	private static final int WINDUP_DURATION = 25;

	private static final int ATTACK_DAMAGE = 90;
	private static final int FRAGMENT_ATTACK_DAMAGE = 65;

	private static final int EXTRA_LOCATIONS = 5;
	private static final int EXTRA_RADIUS_MIN = 7;
	private static final int EXTRA_RADIUS_MAX = 14;

	private final boolean mEnhanced;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;

	public ShatteredSlash(Plugin plugin, LivingEntity boss, boolean enhanced) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = boss.getWorld();

		mEnhanced = enhanced;
	}

	@Override
	public void run() {
		if (mEnhanced) {
			runEnhanced();
		}

		final double CIRCLE_SIZE = 3.0;

		// Circle telegraph
		new PPParametric(Particle.WAX_OFF, mBoss.getLocation().clone().add(0, 0.2, 0),
			(param, builder) -> {
				double theta = param * Math.PI * 2;

				Vector pos = new Vector(FastUtils.cos(theta) * CIRCLE_SIZE, 0, FastUtils.sin(theta) * CIRCLE_SIZE);

				builder.offset(-pos.getX(), 0, -pos.getZ());
				builder.location(mBoss.getLocation().clone().add(pos));
			})
			.count(160)
			.directionalMode(true)
			.extra(2.4)
			.spawnAsBoss();

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks == 0) {
					mWorld.playSound(mBoss.getLocation(), Sound.BLOCK_SMALL_AMETHYST_BUD_BREAK, SoundCategory.HOSTILE, 1.2f, 0.75f);
				}
				if (mTicks == 4) {
					mWorld.playSound(mBoss.getLocation(), Sound.BLOCK_SMALL_AMETHYST_BUD_BREAK, SoundCategory.HOSTILE, 1.2f, 1f);
				}

				if (mTicks == WINDUP_DURATION) {
					SpellSlashAttack slash = new SpellSlashAttack(mPlugin, mBoss, 0, ATTACK_DAMAGE, 0, 1.2, FastUtils.randomDoubleInRange(-15, 15), FastUtils.randomDoubleInRange(-15, 15), "Shattered Slash", 7, -40, 380, 0.4, "38175C", "8344C9", "B8A6E3", "false", "false", new Vector(3, 0.3, 3), "true", 0.2, "false", 0.3, 1.0, DamageEvent.DamageType.MELEE, SoundsList.EMPTY, SoundsList.fromString("[(ENTITY_GLOW_SQUID_SQUIRT,0.8,1.7)]"), SoundsList.EMPTY, SoundsList.EMPTY, false, 8, true);
					slash.run();

					mWorld.playSound(mBoss.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_FALL, SoundCategory.HOSTILE, 5.0f, 0.81f);
					mWorld.playSound(mBoss.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_FALL, SoundCategory.HOSTILE, 5.0f, 1.51f);
				}

				mTicks++;

				if (mTicks > WINDUP_DURATION || mBoss.isDead()) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

	}

	private void runEnhanced() {
		// Choose extra locations for fragmented slashes
		List<Location> extraLocs = new ArrayList<>();
		for (int i = 0; i < EXTRA_LOCATIONS; i++) {
			double r = FastUtils.randomDoubleInRange(EXTRA_RADIUS_MIN, EXTRA_RADIUS_MAX);
			double theta = FastUtils.randomDoubleInRange(0, Math.PI * 2);

			Location tLoc = mBoss.getLocation().clone().add(FastUtils.cos(theta) * r, 4, FastUtils.sin(theta) * 4);
			Location gLoc = TheImpenetrable.getOnNearestGround(tLoc, 8);
			if (gLoc == null) {
				continue;
			}
			boolean tooClose = false;

			for (Location l : extraLocs) {
				if (gLoc.distance(l) < 3) {
					tooClose = true;
					break;
				}
			}

			if (!tooClose && gLoc.getBlock().getType() == Material.AIR) {
				extraLocs.add(gLoc);
			}
		}

		new BukkitRunnable() {
			int mTicks = 0;
			final List<Entity> mExtraSlashes = new ArrayList<>();

			@Override
			public void run() {
				if (mTicks < WINDUP_DURATION) {
					extraLocs.forEach(l ->
						new PartialParticle(Particle.SPELL_WITCH, l, 8)
							.delta(0, 0.4, 0)
							.count(6)
							.spawnAsBoss());
				}

				if (mTicks == WINDUP_DURATION) {
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ITEM_AXE_SCRAPE, SoundCategory.HOSTILE, 4.5f, 0.6f);

					for (Location extraLocation : extraLocs) {
						ArmorStand tempMarker = mWorld.spawn(extraLocation, ArmorStand.class);
						SpellSlashAttack slash = new SpellSlashAttack(mPlugin, tempMarker, 0, FRAGMENT_ATTACK_DAMAGE, 0, 0.1, FastUtils.randomDoubleInRange(-15, 15), FastUtils.randomDoubleInRange(-15, 15), "Shattered Slash", 4, -40, 380, 0.4, "38175C", "8344C9", "B8A6E3", "false", "false", new Vector(3, 0.3, 3), "true", 0.4, "false", 0.3, 0.75, DamageEvent.DamageType.MELEE, SoundsList.EMPTY, SoundsList.fromString("[(ENTITY_GLOW_SQUID_SQUIRT,0.8,1.7)]"), SoundsList.EMPTY, SoundsList.EMPTY, false, 8, true);
						slash.run();
						tempMarker.remove();
					}
				}

				mTicks++;

				if (mTicks > WINDUP_DURATION) {
					this.cancel();
				}
				if (mBoss.isDead()) {
					mExtraSlashes.forEach(Entity::remove);
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int cooldownTicks() {
		return 80;
	}
}

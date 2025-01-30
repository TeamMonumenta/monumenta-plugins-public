package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.Constants.HALF_TICKS_PER_SECOND;
import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;

public final class CrownbearerBoss extends SerializedLocationBossAbilityGroup {
	public static final String identityTag = "boss_crownbearer";
	public static final int detectionRange = 60;

	private static final int SUMMON_RADIUS = 5;

	private double mDamageReduction = 1;

	public CrownbearerBoss(final Plugin plugin, final LivingEntity boss, final Location spawnLoc, final Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc);
		final World world = mSpawnLoc.getWorld();
		mBoss.addScoreboardTag("Boss");
		mBoss.setRemoveWhenFarAway(false);

		final Map<Integer, BossHealthAction> events = new HashMap<>();
		events.put(100, mBoss ->
			sendDialogue("So my identity has been revealed? No matter, I'll take out you and the King in one fell swoop!"));

		events.put(75, mBoss ->
			sendDialogue("Don't underestimate me! After you fall, so will the King and all of the Narsens!"));

		events.put(50, mBoss -> {
			sendDialogue("Sons of the Forest, come to me! Let us conquer this place once and for all!");

			new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					mTicks++;
					final Location loc = mBoss.getLocation().add(FastUtils.RANDOM.nextInt(SUMMON_RADIUS), 1.5, FastUtils.RANDOM.nextInt(SUMMON_RADIUS));
					LibraryOfSoulsIntegration.summon(loc, "SonOfTheForest");
					new PartialParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 50, 0.25, 0.45, 0.25).extra(0.175).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 10, 0, 0.45, 0).extra(0.15).spawnAsEntityActive(mBoss);
					world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1, 0.75f);
					if (mTicks >= 4) {
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, (int) (TICKS_PER_SECOND * 1.5), HALF_TICKS_PER_SECOND);
		});

		events.put(30, mBoss -> {
			sendDialogue("Agh! This battle ends here and now! I will not let you stall this any longer!");
			knockback(6);
			changePhase(SpellManager.EMPTY, Collections.emptyList(), null);
		});

		events.put(20, mBoss -> {
			sendDialogue("My allies, aid me! Let us finish this fight!");

			new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					mTicks++;
					final Location loc = mBoss.getLocation().add(FastUtils.RANDOM.nextInt(SUMMON_RADIUS), 1.5, FastUtils.RANDOM.nextInt(SUMMON_RADIUS));
					LibraryOfSoulsIntegration.summon(loc, "SonOfTheForest");
					new PartialParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 50, 0.25, 0.45, 0.25).extra(0.175).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 10, 0, 0.45, 0).extra(0.15).spawnAsEntityActive(mBoss);
					world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1, 0.75f);
					if (mTicks >= 5) {
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 15, 7);
		});

		final BossBarManager bossBar = new BossBarManager(mBoss, detectionRange, BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_10, events);
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, bossBar);
	}

	private void knockback(final double r) {
		final World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2, 1);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2, 0.5f);
		for (final Player player : PlayerUtils.playersInRange(mBoss.getLocation(), r, true)) {
			MovementUtils.knockAway(mBoss.getLocation(), player, 0.45f, false);
		}
		new BukkitRunnable() {
			double mRotation = 0;
			final Location mLoc = mBoss.getLocation();
			double mRadius = 0;
			double mY = 2.5;
			double mYMinus = 0.35;

			@Override
			public void run() {

				mRadius += 1;
				for (int i = 0; i < 15; i += 1) {
					mRotation += 24;
					final double radian1 = Math.toRadians(mRotation);
					mLoc.add(FastUtils.cos(radian1) * mRadius, mY, FastUtils.sin(radian1) * mRadius);
					new PartialParticle(Particle.SWEEP_ATTACK, mLoc).count(1).delta(0.1).extra(0).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.EXPLOSION_NORMAL, mLoc).count(3).delta(0.1).extra(0.1).spawnAsEntityActive(mBoss);
					mLoc.subtract(FastUtils.cos(radian1) * mRadius, mY, FastUtils.sin(radian1) * mRadius);
				}
				mY -= mY * mYMinus;
				mYMinus += 0.02;
				if (mYMinus >= 1) {
					mYMinus = 1;
				}
				if (mRadius >= r) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void sendDialogue(final String msg) {
		PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true).forEach(player ->
			com.playmonumenta.scriptedquests.utils.MessagingUtils.sendNPCMessage(player, "Onyx Crownbearer", msg));
	}

	@Override
	public void onHurt(final DamageEvent event) {
		event.setFlatDamage(event.getFlatDamage() / mDamageReduction);
	}

	@Override
	public void death(@Nullable final EntityDeathEvent event) {
		for (final Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
			player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, SoundCategory.HOSTILE, 5, 0.8f);
		}
		sendDialogue("Damn you... the King... must meet... his...");
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
	}

	@Override
	public void init() {
		final List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true);
		final int baseHealth = 512;
		mDamageReduction = BossUtils.healthScalingCoef(players.size(), 0.5, 0.5);
		EntityUtils.setMaxHealthAndHealth(mBoss, baseHealth);

		for (final Player player : players) {
			MessagingUtils.sendBoldTitle(player, Component.text("Onyx Crownbearer", NamedTextColor.GOLD),
				Component.text("The King Assassinator", NamedTextColor.DARK_RED));
			player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 5, 1.25f);
		}
	}
}

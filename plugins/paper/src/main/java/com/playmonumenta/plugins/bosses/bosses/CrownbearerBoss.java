package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class CrownbearerBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_crownbearer";
	public static final int detectionRange = 60;
	private static final int SUMMON_RADIUS = 5;

	private final Location mSpawnLoc;
	private final Location mEndLoc;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new CrownbearerBoss(plugin, boss, spawnLoc, endLoc);
		});
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}

	public CrownbearerBoss(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss);
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;
		World world = mSpawnLoc.getWorld();
		mBoss.addScoreboardTag("Boss");
		mBoss.setRemoveWhenFarAway(false);

		Map<Integer, BossHealthAction> events = new HashMap<Integer, BossHealthAction>();
		events.put(100, mBoss -> {
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Onyx Crownbearer] \",\"color\":\"gold\"},{\"text\":\"So my identity has been revealed? No matter, I'll take out you and the King in one fell swoop!\",\"color\":\"white\"}]");
		});
		events.put(75, mBoss -> {
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Onyx Crownbearer] \",\"color\":\"gold\"},{\"text\":\"Don't underestimate me! After you fall, so will the King, and all of Sierhaven with him!\",\"color\":\"white\"}]");
		});
		events.put(50, mBoss -> {
			new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					mTicks++;
					Location loc = mBoss.getLocation().add(FastUtils.RANDOM.nextInt(SUMMON_RADIUS), 1.5, FastUtils.RANDOM.nextInt(SUMMON_RADIUS));
					summonSOTF(loc);
					new PartialParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 50, 0.25, 0.45, 0.25, 0.175).spawnAsEntityActive(boss);
					new PartialParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 10, 0, 0.45, 0, 0.15).spawnAsEntityActive(boss);
					world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 0.75f);
					if (mTicks >= 4) {
						this.cancel();
					}
				}

			}.runTaskTimer(plugin, 30, 10);
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Onyx Crownbearer] \",\"color\":\"gold\"},{\"text\":\"Sons of the Forest, come to me! Let us conquer this place once and for all!\",\"color\":\"white\"}]");
		});
		events.put(30, mBoss -> {
			knockback(plugin, 6);
			changePhase(SpellManager.EMPTY, Collections.emptyList(), null);
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Onyx Crownbearer] \",\"color\":\"gold\"},{\"text\":\"Agh! This battle ends here and now! I will not let you stall this any longer!\",\"color\":\"white\"}]");
		});
		events.put(20, mBoss -> {
			new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					mTicks++;
					Location loc = mBoss.getLocation().add(FastUtils.RANDOM.nextInt(SUMMON_RADIUS), 1.5, FastUtils.RANDOM.nextInt(SUMMON_RADIUS));
					summonSOTF(loc);
					new PartialParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 50, 0.25, 0.45, 0.25, 0.175).spawnAsEntityActive(boss);
					new PartialParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 10, 0, 0.45, 0, 0.15).spawnAsEntityActive(boss);
					world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 0.75f);
					if (mTicks >= 5) {
						this.cancel();
					}
				}

			}.runTaskTimer(plugin, 15, 7);
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Onyx Crownbearer] \",\"color\":\"gold\"},{\"text\":\"My allies, aid me! Let us finish this fight!\",\"color\":\"white\"}]");
		});

		BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.GREEN, BarStyle.SEGMENTED_10, events);

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, bossBar);
	}

	private void summonSOTF(Location loc) {
		LibraryOfSoulsIntegration.summon(loc, "SonOfTheForest");
	}

	private void knockback(Plugin plugin, double r) {
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2, 1);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2, 0.5f);
		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), r, true)) {
			MovementUtils.knockAway(mBoss.getLocation(), player, 0.45f, false);
		}
		new BukkitRunnable() {
			double mRotation = 0;
			Location mLoc = mBoss.getLocation();
			double mRadius = 0;
			double mY = 2.5;
			double mYMinus = 0.35;

			@Override
			public void run() {

				mRadius += 1;
				for (int i = 0; i < 15; i += 1) {
					mRotation += 24;
					double radian1 = Math.toRadians(mRotation);
					mLoc.add(FastUtils.cos(radian1) * mRadius, mY, FastUtils.sin(radian1) * mRadius);
					new PartialParticle(Particle.SWEEP_ATTACK, mLoc, 1, 0.1, 0.1, 0.1, 0).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.EXPLOSION_NORMAL, mLoc, 3, 0.1, 0.1, 0.1, 0.1).spawnAsEntityActive(mBoss);
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

		}.runTaskTimer(plugin, 0, 1);
	}

	@Override
	public void death(EntityDeathEvent event) {
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound minecraft:entity.wither.death master @s ~ ~ ~ 100 0.8");
		PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Onyx Crownbearer] \",\"color\":\"gold\"},{\"text\":\"Damn you... The King... Must meet... His...\",\"color\":\"white\"}]");
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
	}

	@Override
	public void init() {
		int bossTargetHp = 0;
		int playerCount = BossUtils.getPlayersInRangeForHealthScaling(mBoss, detectionRange);
		int hpDelta = 512;
		while (playerCount > 0) {
			bossTargetHp = bossTargetHp + hpDelta;
			hpDelta = hpDelta / 2;
			playerCount--;
		}
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, bossTargetHp);
		mBoss.setHealth(bossTargetHp);

		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
			MessagingUtils.sendBoldTitle(player, ChatColor.GOLD + "Onyx Crownbearer", ChatColor.DARK_RED + "The King's Assassinator");
			player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 10, 1.25f);
		}
	}

}

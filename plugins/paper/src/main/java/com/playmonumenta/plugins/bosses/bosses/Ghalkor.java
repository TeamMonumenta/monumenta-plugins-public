package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.bosses.spells.sealedremorse.GhalkorFlameBolt;
import com.playmonumenta.plugins.bosses.spells.sealedremorse.GhalkorFlamingCharge;
import com.playmonumenta.plugins.bosses.spells.sealedremorse.GhalkorForwardSweep;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public final class Ghalkor extends BossAbilityGroup {

	public static final String identityTag = "boss_ghalkor";
	public static final int detectionRange = 75;

	private static final int BASE_HEALTH = 2000;
	private static final String duoTag = "svalgotthevoidwalker";

	private final Location mSpawnLoc;
	private final Location mEndLoc;
	private final Location mMiddleLoc;

	private @Nullable LivingEntity mSvalgot;
	private @Nullable Svalgot mSvalgotBoss;

	//True when the final boss has been called from death
	boolean mSummonedFinalBoss = false;

	//Lower number = faster cast speed
	//0.5 is double casting speed
	public double mCastSpeed = 1;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new Ghalkor(plugin, boss, spawnLoc, endLoc);
		});
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}

	public Ghalkor(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss);

		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;

		mMiddleLoc = mSpawnLoc.add(-2, 0, 0);

		SpellManager normalSpells = new SpellManager(Arrays.asList(
			new GhalkorFlamingCharge(mPlugin, mBoss, this),
			new GhalkorFlameBolt(mBoss, mPlugin, this),
			new GhalkorForwardSweep(mPlugin, mBoss, this)
		));


		List<Spell> passiveNormalSpells = Arrays.asList(
			new SpellBlockBreak(boss, 2, 3, 2)
		);

		Map<Integer, BossHealthAction> events = new HashMap<Integer, BossHealthAction>();

		events.put(50, mBoss -> {
			//Cast faster
			mCastSpeed = .5;

			World world = mBoss.getWorld();
			Location loc = mBoss.getLocation();
			world.playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.HOSTILE, 1f, 0.6f);
			world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 1.5f, 1.5f);
			new PartialParticle(Particle.SPELL_WITCH, loc.add(0, mBoss.getHeight() / 2, 0), 15, 0.25, 0.45, 0.25, 1).spawnAsEntityActive(boss);
			new PartialParticle(Particle.VILLAGER_ANGRY, loc.add(0, mBoss.getHeight() / 2, 0), 5, 0.35, 0.5, 0.35, 0).spawnAsEntityActive(boss);

			PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Ghalkor]\",\"color\":\"gold\"},{\"text\":\" This pain... My blood boils!\",\"color\":\"dark_gray\"}]");
		});


		//TODO: Find better way to get boss duo
		for (LivingEntity e : EntityUtils.getNearbyMobs(spawnLoc, 75)) {
			if (e.getScoreboardTags().contains(duoTag)) {
				mSvalgot = e;
				break;
			}
		}

		if (mSvalgot != null) {
			new BukkitRunnable() {
				@Override
				public void run() {
					mSvalgotBoss = mSvalgot != null ? BossManager.getInstance().getBoss(mSvalgot, Svalgot.class) : null;
				}
			}.runTaskLater(mPlugin, 1);
		} else {
			mPlugin.getLogger().warning("Svalgot was not found by Ghalkor!");
		}

		new BukkitRunnable() {
			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				} else if (mSvalgot == null || mSvalgot.isDead() || !mSvalgot.isValid()) {
					//changePhase to increased pace
					EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MOVEMENT_SPEED, EntityUtils.getAttributeBaseOrDefault(mBoss, Attribute.GENERIC_MOVEMENT_SPEED, 0) * 1.05);
					EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_ATTACK_DAMAGE, EntityUtils.getAttributeBaseOrDefault(mBoss, Attribute.GENERIC_ATTACK_DAMAGE, 0) * 1.25);

					PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Ghalkor]\",\"color\":\"gold\"},{\"text\":\" Broer, for you and for the Blackflame, I will devour them!\",\"color\":\"dark_gray\"}]");
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_VINDICATOR_DEATH, SoundCategory.HOSTILE, 3, 0);

					World world = mBoss.getWorld();
					Location loc = mBoss.getLocation();
					world.playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.HOSTILE, 1f, 0.6f);
					world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 1.5f, 1.5f);
					new PartialParticle(Particle.SPELL_WITCH, loc.add(0, mBoss.getHeight() / 2, 0), 15, 0.25, 0.45, 0.25, 1).spawnAsEntityActive(boss);
					new PartialParticle(Particle.VILLAGER_ANGRY, loc.add(0, mBoss.getHeight() / 2, 0), 5, 0.35, 0.5, 0.35, 0).spawnAsEntityActive(boss);

					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 5);

		new BukkitRunnable() {
			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}

				//If below 3 blocks, teleport
				if (mSpawnLoc.getY() - mBoss.getLocation().getY() >= 3) {
					teleport(mSpawnLoc);
				}

				//If player too far from arena center or below 4 blocks or too high and is on a block, damage them
				for (Player p : PlayerUtils.playersInRange(mMiddleLoc, detectionRange, true)) {
					if ((mMiddleLoc.distance(p.getLocation()) > 22
						     || mMiddleLoc.getY() - p.getLocation().getY() >= 3
						     || (mMiddleLoc.getY() - p.getLocation().getY() <= -2 && p.isOnGround()))
						    && p.getGameMode() != GameMode.CREATIVE) {
						Vector vel = p.getVelocity();
						BossUtils.bossDamagePercent(mBoss, p, 0.1);
						p.setVelocity(vel);

						p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.PLAYERS, 1, 0f);
						new PartialParticle(Particle.FLAME, p.getLocation(), 10, 0.5, 0.25, 0.5, 0.2).spawnAsEntityActive(boss);
					}
				}
			}
		}.runTaskTimer(mPlugin, 0, 10);

		mBoss.setAI(false);
		mBoss.setGravity(false);
		mBoss.setInvulnerable(true);

		PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Ghalkor]\",\"color\":\"gold\"},{\"text\":\" Hael Broer, the invaders approach.\",\"color\":\"dark_gray\"}]");
		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_VINDICATOR_AMBIENT, SoundCategory.HOSTILE, 5, 0.8f);

		new BukkitRunnable() {
			private int mTicks = 0;

			@Override
			public void run() {
				if (mTicks == 20 * 2) {
					PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Svalgot]\",\"color\":\"gold\"},{\"text\":\" Yes Ghalkor! Fear not. The power of the Blackflame is absolute.\",\"color\":\"dark_gray\"}]");
					if (mSvalgot != null) {
						mBoss.getWorld().playSound(mSvalgot.getLocation(), Sound.ENTITY_VINDICATOR_AMBIENT, SoundCategory.HOSTILE, 5, 1f);
					}
				} else if (mTicks == 20 * 4) {
					PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Ghalkor]\",\"color\":\"gold\"},{\"text\":\" Yah Broer, the nonbelievers draw close.\",\"color\":\"dark_gray\"}]");
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_VINDICATOR_AMBIENT, SoundCategory.HOSTILE, 5, 0.8f);
				} else if (mTicks == 20 * 6) {
					new PartialParticle(Particle.FLAME, mBoss.getLocation(), 200, 0.1, 0.1, 0.1, 0.3).spawnAsEntityActive(boss);
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 5, 2);
				} else if (mTicks == 20 * 6 + 10) {
					this.cancel();

					mBoss.setAI(true);
					mBoss.setGravity(true);
					mBoss.setInvulnerable(false);

					BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.RED, BarStyle.SEGMENTED_10, events);
					constructBoss(normalSpells, passiveNormalSpells, detectionRange, bossBar, 20 * 10);

					PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Ghalkor and Svalgot]\",\"color\":\"gold\"},{\"text\":\" BY THE BLACKFLAME, YOU WILL FALL.\",\"color\":\"dark_gray\",\"bold\":true}]");

					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_VINDICATOR_CELEBRATE, SoundCategory.HOSTILE, 5, 0.8f);
					if (mSvalgot != null) {
						mBoss.getWorld().playSound(mSvalgot.getLocation(), Sound.ENTITY_VINDICATOR_CELEBRATE, SoundCategory.HOSTILE, 5, 1f);
					}

					for (Player player : PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true)) {
						MessagingUtils.sendBoldTitle(player, ChatColor.DARK_GRAY + "Ghalkor & Svalgot", ChatColor.GRAY + "Speakers of Remorse");
						player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10, 0.75f);
					}
				}

				mTicks += 10;
			}
		}.runTaskTimer(mPlugin, 0, 10);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		if ((mSvalgot == null || mSvalgot.isDead() || !mSvalgot.isValid()) && (mSvalgotBoss == null || !mSvalgotBoss.mSummonedFinalBoss)) {
			mSummonedFinalBoss = true;

			PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Ghalkor]\",\"color\":\"gold\"},{\"text\":\" With mine Laastasem...  My lifeblood fuels the ritual... Come forth o Beast!\",\"color\":\"dark_gray\"}]");

			Entity beast = LibraryOfSoulsIntegration.summon(mSpawnLoc.add(-2, -3, 0), BeastOfTheBlackFlame.losName);
			if (beast != null && beast instanceof LivingEntity leBeast) {
				try {
					BossManager.createBoss(null, leBeast, BeastOfTheBlackFlame.identityTag, mEndLoc);
				} catch (Exception e) {
					mPlugin.getLogger().warning("Failed to create boss BeastOfTheBlackFlame: " + e.getMessage());
					e.printStackTrace();
				}
			} else {
				mPlugin.getLogger().warning("Failed to summon BeastOfTheBlackFlame");
			}
		}
	}

	@Override
	public void init() {
		int playerCount = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true).size();
		double bossTargetHp = BASE_HEALTH * BossUtils.healthScalingCoef(playerCount, 0.5, 0.5);

		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, bossTargetHp);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_FOLLOW_RANGE, detectionRange);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 1);
		mBoss.setHealth(bossTargetHp);

		mBoss.setPersistent(true);

	}

	//Teleport with special effects
	private void teleport(Location loc) {
		World world = loc.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 0f);
		new PartialParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.CLOUD, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1).spawnAsEntityActive(mBoss);
		mBoss.teleport(loc);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 0f);
		new PartialParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1).spawnAsEntityActive(mBoss);
	}
}

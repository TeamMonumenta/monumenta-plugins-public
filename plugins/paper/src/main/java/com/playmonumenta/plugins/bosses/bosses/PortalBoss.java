package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.portalboss.SpellKnockup;
import com.playmonumenta.plugins.bosses.spells.portalboss.SpellPortalBullet;
import com.playmonumenta.plugins.bosses.spells.portalboss.SpellPortalPassiveLava;
import com.playmonumenta.plugins.bosses.spells.portalboss.SpellPortalSummons;
import com.playmonumenta.plugins.bosses.spells.portalboss.SpellRisingCircles;
import com.playmonumenta.plugins.bosses.spells.portalboss.SpellUltimateShulkerMania;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.portals.PortalManager;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public final class PortalBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_portalfight";
	public static final int detectionRange = 50;
	public static final int BASE_HEALTH = 5000;
	public static final String CUBE_TAG = "boss_portal";
	public static final String ELITE_LOS = "FlameConstruct";
	public static final String CUBE_FUNCTION = "function monumenta:quests/r2/quest149/cube/spawning/force_clear_boss";

	public static final String MUSIC_TITLE = "epic:music.misanthropic_circuitry";
	private static final int MUSIC_DURATION = 233; //seconds

	public final Location mSpawnLoc;
	public final Location mEndLoc;

	public int mCooldownTicks;
	public boolean mIsHidden;
	public int mCubesDropped;
	public int mPhase = 1;
	public List<Location> mReplaceBlocks;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new PortalBoss(plugin, boss, spawnLoc, endLoc);
		});
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}

	public PortalBoss(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss);
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;
		mReplaceBlocks = new ArrayList<>();
		mBoss.setRemoveWhenFarAway(false);
		mBoss.addScoreboardTag("Boss");

		mCooldownTicks = 8 * 20;

		//Spell setup
		SpellManager phase1Spells = new SpellManager(Arrays.asList(
			new SpellKnockup(mBoss, plugin, mCooldownTicks - 30),
			new SpellPortalSummons(mBoss, mCooldownTicks, mSpawnLoc, this),
			new SpellPortalBullet(plugin, mBoss, mCooldownTicks, this),
			new SpellRisingCircles(plugin, mBoss, mSpawnLoc, mCooldownTicks)
		));
		List<Spell> phase1Passives = Arrays.asList(
			new SpellPortalPassiveLava(mBoss, mSpawnLoc, this)
		);

		SpellManager phase2Spells = new SpellManager(Arrays.asList(
			new SpellUltimateShulkerMania(plugin, mBoss, mSpawnLoc, mCooldownTicks),
			new SpellKnockup(mBoss, plugin, mCooldownTicks - 30),
			new SpellPortalSummons(mBoss, mCooldownTicks, mSpawnLoc, this),
			new SpellPortalBullet(plugin, mBoss, mCooldownTicks, this),
			new SpellRisingCircles(plugin, mBoss, mSpawnLoc, mCooldownTicks)
		));
		List<Spell> phase2Passives = Arrays.asList(
			new SpellPortalPassiveLava(mBoss, mSpawnLoc, this)
		);

		SpellManager phase3Spells = new SpellManager(Arrays.asList(
			new SpellUltimateShulkerMania(plugin, mBoss, mSpawnLoc, mCooldownTicks),
			new SpellKnockup(mBoss, plugin, mCooldownTicks - 30),
			new SpellPortalSummons(mBoss, mCooldownTicks, mSpawnLoc, this),
			new SpellPortalBullet(plugin, mBoss, mCooldownTicks, this),
			new SpellRisingCircles(plugin, mBoss, mSpawnLoc, mCooldownTicks)
		));
		List<Spell> phase3Passives = Arrays.asList(
			new SpellPortalPassiveLava(mBoss, mSpawnLoc, this)
		);

		Map<Integer, BossHealthAction> events = new HashMap<Integer, BossHealthAction>();

		events.put(66, (mBoss) -> {
			mCooldownTicks -= 30;
			changePhase(phase2Spells, phase2Passives, null);
			PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "tellraw @s [\"\",{\"text\":\"[Iota]\", \"color\":\"gold\"},{\"text\":\" DAMAGE SUSTAINED. OVERCLOCKING… POWER LEVEL RAISED. INTRUDER - DESTRUCTION IS ASSURED. \",\"color\":\"red\"}]");
			mPhase = 2;
			hide();
		});
		events.put(50, (mBoss) -> {
			PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "tellraw @s [\"\",{\"text\":\"[Iota]\",\"color\":\"gold\"},{\"text\":\" THIS MACHINE FAILS. NO MATTER. THIS LAB IS VAST. I AM…INF\",\"color\":\"red\"},{\"text\":\"69\",\"obfuscated\":\"true\",\"color\":\"red\"},{\"text\":\"IN\",\"color\":\"red\"},{\"text\":\"  8\",\"obfuscated\":\"true\",\"color\":\"red\"},{\"text\":\"ITE.\",\"color\":\"red\"}]");
			LibraryOfSoulsIntegration.summon(spawnLoc.clone().add(new Vector(10, 1, 10)), ELITE_LOS);
			LibraryOfSoulsIntegration.summon(spawnLoc.clone().add(new Vector(-10, 1, -10)), ELITE_LOS);

		});
		events.put(25, (mBoss) -> {
			mCooldownTicks -= 30;
			changePhase(phase3Spells, phase3Passives, null);
			PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "tellraw @s [\"\",{\"text\":\"[Iota]\",\"color\":\"gold\"},{\"text\":\" DAMA\",\"color\":\"red\"},{\"text\":\"94\",\"obfuscated\":\"true\",\"color\":\"red\"},{\"text\":\" SUSTA\",\"color\":\"red\"},{\"text\":\"  32\",\"obfuscated\":\"true\",\"color\":\"red\"},{\"text\":\"ED...\",\"color\":\"red\"},{\"text\":\" Bermuda? Wher9… am I? The rift…\",\"obfuscated\":\"false\",\"color\":\"blue\"},{\"text\":\" 43267\",\"obfuscated\":\"true\",\"color\":\"red\"},{\"text\":\"THOUGHT PROTOCOL OVERRIDDEN. RIFT PROTECTION RESUMED. INTRUDERS WILL BE EXPUNGED.\",\"obfuscated\":\"false\",\"color\":\"red\"}]");
			mPhase = 3;
			//Clear portals
			for (Player p : PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true)) {
				PortalManager.clearPortal(p, 1);
				PortalManager.clearPortal(p, 2);
			}
			World world = mBoss.getWorld();
			world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
			world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0f);
			hide();
			honeyify();
		});
		BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.RED, BarStyle.SEGMENTED_10, events);
		super.constructBoss(phase1Spells, phase1Passives, detectionRange, bossBar, 12 * 20);

		hide();
	}

	public void expose() {
		mBoss.setGlowing(true);
		mBoss.teleport(mSpawnLoc.clone().add(0, 2, 0));
		mBoss.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
		mBoss.removePotionEffect(PotionEffectType.INVISIBILITY);
		mIsHidden = false;

		new BukkitRunnable() {

			@Override
			public void run() {
				hide();
			}

		}.runTaskLater(mPlugin, 20 * 20);
	}

	public void hide() {
		if (mBoss.isDead() || mIsHidden) {
			return;
		}

		// Kill any active cubes
		Bukkit.dispatchCommand(mBoss, CUBE_FUNCTION);

		mBoss.setGlowing(false);
		mBoss.teleport(mSpawnLoc.clone().add(0, -15, 0));
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 1000000, 4));
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 1000000, 0));
		mCubesDropped = 0;
		mIsHidden = true;

		new BukkitRunnable() {
			@Override
			public void run() {
				if (!mIsHidden) {
					this.cancel();
				} else if (mBoss.isGlowing()) {
					mBoss.setGlowing(false);
				}
			}
		}.runTaskTimer(mPlugin, 5, 5);
	}

	public void cubeBrought() {
		mCubesDropped++;
		//Expose the boss if cubes dropped is above threshold
		if ((mCubesDropped >= 1 && mPhase == 1) || (mCubesDropped >= 2 && mPhase >= 2)) {
			//Expose the boss
			PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "tellraw @s [\"\",{\"text\":\"[Iota]\",\"color\":\"gold\"},{\"text\":\" I AM INDESTRUCT\",\"color\":\"red\"},{\"text\":\"698765\",\"obfuscated\":\"true\",\"color\":\"red\"},{\"text\":\"  The core… destroy it… \",\"color\":\"blue\"},{\"text\":\"  44546\",\"obfuscated\":\"true\",\"color\":\"red\"},{\"text\":\"MY DEMISE IS IMPOSSIBLE\",\"color\":\"red\"}]");
			expose();
		} else {
			PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "tellraw @s [\"\",{\"text\":\"[Iota]\",\"color\":\"gold\"},{\"text\":\" NO...\",\"color\":\"red\"},{\"text\":\"945\",\"obfuscated\":\"true\",\"color\":\"red\"},{\"text\":\"  YOU CANN\",\"color\":\"red\"},{\"text\":\"  32...\",\"obfuscated\":\"true\",\"color\":\"red\"},{\"text\":\" It's working.. keep going... \",\"color\":\"blue\"},{\"text\":\"945\",\"obfuscated\":\"true\",\"color\":\"red\"},{\"text\":\" I AM IMMORTAL\",\"bold\":\"true\",\"color\":\"red\"}]");
		}
	}

	public void honeyify() {
		new BukkitRunnable() {
			@Override
			public void run() {
				for (int x = -30; x < 30; x++) {
					for (int y = -3; y < 20; y++) {
						for (int z = -30; z < 30; z++) {
							Location loc = new Location(mBoss.getWorld(), mSpawnLoc.getX() + x, mSpawnLoc.getY() + y, mSpawnLoc.getZ() + z);
							if (mBoss.getWorld().getBlockAt(loc).getType() == Material.SMOOTH_STONE && FastUtils.RANDOM.nextBoolean() && FastUtils.RANDOM.nextBoolean()) {
								mBoss.getWorld().getBlockAt(loc).setType(Material.HONEY_BLOCK);
								mBoss.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, loc, 1, 0, 0, 0, 0);
								mReplaceBlocks.add(loc);
							}
						}
					}
				}
			}
		}.runTask(mPlugin);
	}

	@Override
	public void init() {
		mBoss.setAI(false);

		int playerCount = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true).size();
		double bossTargetHp = BASE_HEALTH * BossUtils.healthScalingCoef(playerCount, 0.5, 0.5);

		mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(bossTargetHp);
		mBoss.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(detectionRange);
		mBoss.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1);
		mBoss.setHealth(bossTargetHp);

		mBoss.setPersistent(true);

		//Clear portals
		for (Player p : PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true)) {
			PortalManager.clearPortal(p, 1);
			PortalManager.clearPortal(p, 2);
		}

		new BukkitRunnable() {

			int mTicks = 0;

			@Override
			public void run() {
				mTicks += 5;
				mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 20.0f, 0.5f + (mTicks / 25));

				//launch event related spawn commands
				if (mTicks >= 6 * 20) {
					this.cancel();
					for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
						MessagingUtils.sendBoldTitle(player, ChatColor.DARK_RED + "Iota", ChatColor.DARK_RED + "Corrupted Construct");
						PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "tellraw @s [\"\",{\"text\":\"[Iota]\", \"color\":\"gold\"},{\"text\":\" INTRUSION DETECTED -- INTRUDERS ENTERING INNER CHAMBER. RIFT PROXIMITY… 100%\",\"color\":\"red\"}]");
						PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "tellraw @s [\"\",{\"text\":\"[Iota]\", \"color\":\"gold\"},{\"text\":\" DELETION PROTOCOL COMMENCING. INTRUDERS ARE FRAIL: CHANCE OF SURVIVAL….. 0.00001%.\",\"color\":\"red\"}]");
						PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "tellraw @s [\"\",{\"text\":\"[Iota]\", \"color\":\"gold\"},{\"text\":\" BRING IT ON.\",\"bold\":\"true\",\"color\":\"red\"}]");

						player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 2, false, true, true));
						player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 10, 0.7f);
					}
					mMusicRunnable.runTaskTimer(mPlugin, 0, MUSIC_DURATION * 20 + 20);
				}

			}

		}.runTaskTimer(mPlugin, 0, 5);


	}

	@Override
	public void death(EntityDeathEvent event) {
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound minecraft:entity.enderdragon.death master @s ~ ~ ~ 100 0.8");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "tellraw @s [\"\",{\"text\":\"[Iota]\",\"color\":\"gold\"},{\"text\":\" DESTR\",\"color\":\"red\"},{\"text\":\"6\",\"obfuscated\":\"true\",\"color\":\"red\"},{\"text\":\"Y... INTRU\",\"color\":\"red\"},{\"text\":\"4\",\"obfuscated\":\"true\",\"color\":\"red\"},{\"text\":\"DER...\",\"color\":\"red\"},{\"text\":\"65789\",\"obfuscated\":\"true\",\"color\":\"red\"},{\"text\":\" Thank… you… tell Bermuda… that thing… it broke me… I didn’t mean to…\",\"color\":\"blue\"}]");
		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 2));
			player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 10, 2));
		}

		// Fix misplaced blocks
		for (Location loc : mReplaceBlocks) {
			loc.getBlock().setType(Material.SMOOTH_STONE);
		}
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);

		//Kill nearby mobs
		for (LivingEntity e : EntityUtils.getNearbyMobs(mBoss.getLocation(), 40.0)) {
			e.damage(10000);
		}

		DepthsUtils.animate(mBoss.getLocation());
	}

	BukkitRunnable mMusicRunnable = new BukkitRunnable() {
		@Override
		public void run() {
			if (mBoss.isDead()) {
				this.cancel();
			}
			PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound " + MUSIC_TITLE + " record @s ~ ~ ~ 2");
		}
	};
}

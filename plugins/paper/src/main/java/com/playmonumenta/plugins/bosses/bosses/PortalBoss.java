package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellMusic;
import com.playmonumenta.plugins.bosses.spells.portalboss.SpellKnockup;
import com.playmonumenta.plugins.bosses.spells.portalboss.SpellPortalBullet;
import com.playmonumenta.plugins.bosses.spells.portalboss.SpellPortalPassiveLava;
import com.playmonumenta.plugins.bosses.spells.portalboss.SpellPortalSummons;
import com.playmonumenta.plugins.bosses.spells.portalboss.SpellRisingCircles;
import com.playmonumenta.plugins.bosses.spells.portalboss.SpellUltimateShulkerMania;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

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
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) ->
			new PortalBoss(plugin, boss, spawnLoc, endLoc));
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

		SpellMusic music = new SpellMusic(mBoss, MUSIC_TITLE, MUSIC_DURATION * 20, 2.0f, 6 * 20, detectionRange, detectionRange, false, 0, true);

		//Spell setup
		SpellManager phase1Spells = new SpellManager(Arrays.asList(
			new SpellKnockup(mBoss, plugin, mCooldownTicks - 30),
			new SpellPortalSummons(mBoss, mCooldownTicks, mSpawnLoc, this),
			new SpellPortalBullet(plugin, mBoss, mCooldownTicks, this),
			new SpellRisingCircles(plugin, mBoss, mSpawnLoc, mCooldownTicks)
		));
		List<Spell> phase1Passives = Arrays.asList(
			new SpellPortalPassiveLava(mBoss, mSpawnLoc, this),
			music
		);

		SpellManager phase2Spells = new SpellManager(Arrays.asList(
			new SpellUltimateShulkerMania(plugin, mBoss, mSpawnLoc, mCooldownTicks),
			new SpellKnockup(mBoss, plugin, mCooldownTicks - 30),
			new SpellPortalSummons(mBoss, mCooldownTicks, mSpawnLoc, this),
			new SpellPortalBullet(plugin, mBoss, mCooldownTicks, this),
			new SpellRisingCircles(plugin, mBoss, mSpawnLoc, mCooldownTicks)
		));
		List<Spell> phase2Passives = Arrays.asList(
			new SpellPortalPassiveLava(mBoss, mSpawnLoc, this),
			music
		);

		SpellManager phase3Spells = new SpellManager(Arrays.asList(
			new SpellUltimateShulkerMania(plugin, mBoss, mSpawnLoc, mCooldownTicks),
			new SpellKnockup(mBoss, plugin, mCooldownTicks - 30),
			new SpellPortalSummons(mBoss, mCooldownTicks, mSpawnLoc, this),
			new SpellPortalBullet(plugin, mBoss, mCooldownTicks, this),
			new SpellRisingCircles(plugin, mBoss, mSpawnLoc, mCooldownTicks)
		));
		List<Spell> phase3Passives = Arrays.asList(
			new SpellPortalPassiveLava(mBoss, mSpawnLoc, this),
			music
		);

		Map<Integer, BossHealthAction> events = new HashMap<>();

		events.put(66, (mBoss) -> {
			mCooldownTicks -= 30;
			changePhase(phase2Spells, phase2Passives, null);
			PlayerUtils.nearbyPlayersAudience(mBoss.getLocation(), detectionRange)
				.sendMessage(Component.text("", NamedTextColor.RED)
					.append(Component.text("[Iota]", NamedTextColor.GOLD))
					.append(Component.text(" DAMAGE SUSTAINED. OVERCLOCKING… POWER LEVEL RAISED. INTRUDER - DESTRUCTION IS ASSURED.")));
			mPhase = 2;
			hide();
		});
		events.put(50, (mBoss) -> {
			PlayerUtils.nearbyPlayersAudience(mBoss.getLocation(), detectionRange)
				.sendMessage(Component.text("", NamedTextColor.RED)
					.append(Component.text("[Iota]", NamedTextColor.GOLD))
					.append(Component.text(" THIS MACHINE FAILS. NO MATTER. THIS LAB IS VAST. I AM…INF"))
					.append(Component.text("69").decoration(TextDecoration.OBFUSCATED, true))
					.append(Component.text("IN"))
					.append(Component.text("  8").decoration(TextDecoration.OBFUSCATED, true))
					.append(Component.text("ITE.")));
			LibraryOfSoulsIntegration.summon(spawnLoc.clone().add(10, 1, 10), ELITE_LOS);
			LibraryOfSoulsIntegration.summon(spawnLoc.clone().add(-10, 1, -10), ELITE_LOS);

		});
		events.put(25, (mBoss) -> {
			mCooldownTicks -= 30;
			changePhase(phase3Spells, phase3Passives, null);
			PlayerUtils.nearbyPlayersAudience(mBoss.getLocation(), detectionRange)
				.sendMessage(Component.text("", NamedTextColor.RED)
					.append(Component.text("[Iota]", NamedTextColor.GOLD))
					.append(Component.text(" DAMA"))
					.append(Component.text("94").decoration(TextDecoration.OBFUSCATED, true))
					.append(Component.text(" SUSTA"))
					.append(Component.text("  32").decoration(TextDecoration.OBFUSCATED, true))
					.append(Component.text("ED..."))
					.append(Component.text(" Bermuda? Wher9… am I? The rift…", NamedTextColor.BLUE))
					.append(Component.text(" 43267").decoration(TextDecoration.OBFUSCATED, true))
					.append(Component.text("THOUGHT PROTOCOL OVERRIDDEN. RIFT PROTECTION RESUMED. INTRUDERS WILL BE EXPUNGED.")));
			mPhase = 3;
			//Clear portals
			for (Player p : PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true)) {
				PortalManager.clearAllPortals(p);
			}
			World world = mBoss.getWorld();
			world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2.0f, 1.0f);
			world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 2.0f, 0f);
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

		Bukkit.getScheduler().runTaskLater(mPlugin, this::hide, 20 * 20);
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
			PlayerUtils.nearbyPlayersAudience(mBoss.getLocation(), detectionRange)
				.sendMessage(Component.text("", NamedTextColor.RED)
					.append(Component.text("[Iota]", NamedTextColor.GOLD))
					.append(Component.text(" I AM INDESTRUCT"))
					.append(Component.text("698765").decoration(TextDecoration.OBFUSCATED, true))
					.append(Component.text("  The core… destroy it… ", NamedTextColor.BLUE))
					.append(Component.text("  44546").decoration(TextDecoration.OBFUSCATED, true))
					.append(Component.text("MY DEMISE IS IMPOSSIBLE")));
			expose();
		} else {
			PlayerUtils.nearbyPlayersAudience(mBoss.getLocation(), detectionRange)
				.sendMessage(Component.text("", NamedTextColor.RED)
					.append(Component.text("[Iota]", NamedTextColor.GOLD))
					.append(Component.text(" NO..."))
					.append(Component.text("945").decoration(TextDecoration.OBFUSCATED, true))
					.append(Component.text("  YOU CANN"))
					.append(Component.text("  32...").decoration(TextDecoration.OBFUSCATED, true))
					.append(Component.text(" It's working.. keep going... ", NamedTextColor.BLUE))
					.append(Component.text("945").decoration(TextDecoration.OBFUSCATED, true))
					.append(Component.text(" I AM IMMORTAL").decoration(TextDecoration.BOLD, true)));
		}
	}

	public void honeyify() {
		Bukkit.getScheduler().runTask(mPlugin, () -> {
			for (int x = -30; x < 30; x++) {
				for (int y = -3; y < 20; y++) {
					for (int z = -30; z < 30; z++) {
						Location loc = mSpawnLoc.clone().add(x, y, z);
						Block block = loc.getBlock();
						if (block.getType() == Material.SMOOTH_STONE && FastUtils.RANDOM.nextBoolean() && FastUtils.RANDOM.nextBoolean()) {
							block.setType(Material.HONEY_BLOCK);
							new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 1, 0, 0, 0, 0).spawnAsEntityActive(mBoss);
							mReplaceBlocks.add(loc);
						}
					}
				}
			}
		});
	}

	@Override
	public void init() {
		mBoss.setAI(false);

		List<Player> players = PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true);
		int playerCount = players.size();
		double bossTargetHp = BASE_HEALTH * BossUtils.healthScalingCoef(playerCount, 0.5, 0.5);

		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, bossTargetHp);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_FOLLOW_RANGE, detectionRange);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 1);
		mBoss.setHealth(bossTargetHp);

		mBoss.setPersistent(true);

		//Clear portals
		players.forEach(PortalManager::clearAllPortals);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks += 5;
				mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.HOSTILE, 20.0f, 0.5f + (mTicks / 25.0f));

				//launch event related spawn commands
				if (mTicks >= 6 * 20) {
					this.cancel();
					for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
						MessagingUtils.sendBoldTitle(player, ChatColor.DARK_RED + "Iota", ChatColor.DARK_RED + "Corrupted Construct");
						player.sendMessage(ChatColor.GOLD + "[Iota]" + ChatColor.RED + " INTRUSION DETECTED -- INTRUDERS ENTERING INNER CHAMBER. RIFT PROXIMITY… 100%");
						player.sendMessage(ChatColor.GOLD + "[Iota]" + ChatColor.RED + " DELETION PROTOCOL COMMENCING. INTRUDERS ARE FRAIL: CHANCE OF SURVIVAL….. 0.00001%.");
						player.sendMessage(ChatColor.GOLD + "[Iota]" + ChatColor.RED + ChatColor.BOLD + " BRING IT ON.");

						player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 2, false, true, true));
						player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10, 0.7f);
					}
				}

			}

		}.runTaskTimer(mPlugin, 0, 5);

	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
			player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, SoundCategory.HOSTILE, 100.0f, 0.8f);
			player.sendMessage(Component.text("", NamedTextColor.RED)
				.append(Component.text("[Iota]", NamedTextColor.GOLD))
				.append(Component.text(" DESTR"))
				.append(Component.text("6")).decoration(TextDecoration.OBFUSCATED, true)
				.append(Component.text("Y... INTRU"))
				.append(Component.text("4").decoration(TextDecoration.OBFUSCATED, true))
				.append(Component.text("DER..."))
				.append(Component.text("65789").decoration(TextDecoration.OBFUSCATED, true))
				.append(Component.text(" Thank… you… tell Bermuda… that thing… it broke me… I didn't mean to…", NamedTextColor.BLUE)));
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

		//kill all nearby ShulkerBullet
		for (ShulkerBullet sb : mBoss.getLocation().getNearbyEntitiesByType(ShulkerBullet.class, 40)) {
			sb.remove();
		}

		EntityUtils.fireworkAnimation(mBoss);
	}
}

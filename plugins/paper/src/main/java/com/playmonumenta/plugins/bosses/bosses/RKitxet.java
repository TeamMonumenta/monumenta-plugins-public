package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellMusic;
import com.playmonumenta.plugins.bosses.spells.SpellShieldStun;
import com.playmonumenta.plugins.bosses.spells.rkitxet.SpellEndlessAgony;
import com.playmonumenta.plugins.bosses.spells.rkitxet.SpellEndlessAgonyDamage;
import com.playmonumenta.plugins.bosses.spells.rkitxet.SpellForsakenLeap;
import com.playmonumenta.plugins.bosses.spells.rkitxet.SpellKaulsFury;
import com.playmonumenta.plugins.bosses.spells.rkitxet.SpellRKitxetSummon;
import com.playmonumenta.plugins.bosses.spells.rkitxet.SpellShardShield;
import com.playmonumenta.plugins.bosses.spells.rkitxet.SpellVerdantProtection;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class RKitxet extends BossAbilityGroup {
	public static final String identityTag = "boss_rkitxet";
	public static final int detectionRange = 50;
	public static final int RKITXET_HEALTH = 1400;
	public static final int SWAP_TARGET_SECONDS = 15;

	private static final int MUSIC_DURATION = 3 * 60 + 39; //seconds
	private static final String GOLEM_TAG = "Golem";
	private static final int COOLDOWN_TICKS_1 = 10 * 20;
	private static final int COOLDOWN_TICKS_2 = 7 * 20;

	private final Location mSpawnLoc;
	private final Location mEndLoc;

	public SpellShardShield mShieldSpell;
	public List<Location> mAgonyLocations;

	private @Nullable Player mAgonyTarget;
	private @Nullable Player mFuryTarget;

	private @Nullable String mLastUsedSpell;
	private boolean mSameSpellUsedTwice;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new RKitxet(plugin, boss, spawnLoc, endLoc);
		});
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}

	public RKitxet(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss);
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;
		mAgonyLocations = new ArrayList<>();

		mLastUsedSpell = null;
		mSameSpellUsedTwice = false;

		mBoss.setRemoveWhenFarAway(false);
		mBoss.addScoreboardTag("Boss");

		new BukkitRunnable() {
			@Override
			public void run() {
				if (!mBoss.isValid() || mBoss.isDead()) {
					this.cancel();
					return;
				}

				List<Player> players = PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true);
				Collections.shuffle(players);
				if (!players.isEmpty() && mBoss instanceof Mob mob) {
					mob.setTarget(players.get(0));
				}
			}
		}.runTaskTimer(mPlugin, 0, SWAP_TARGET_SECONDS * 20);

		//Anticheese runnable
		new BukkitRunnable() {
			@Override
			public void run() {
				if (!mBoss.isValid() || mBoss.isDead()) {
					this.cancel();
					return;
				}

				for (Player player : PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true)) {
					if (player.getLocation().distance(mSpawnLoc) > 30 || (player.getLocation().getY() > mSpawnLoc.getY() + 4 && player.isOnGround())) {
						//Give 10 seconds at the beginning of the fight before actually damaging
						if (mBoss.getTicksLived() >= 200) {
							PotionUtils.applyPotion(mBoss, player, new PotionEffect(PotionEffectType.POISON, 30 * 20, 2));
							BossUtils.bossDamagePercent(mBoss, player, 0.1);
						}
					} else if (player.isInWaterOrBubbleColumn()) {
						PotionUtils.applyPotion(mBoss, player, new PotionEffect(PotionEffectType.POISON, 25 * 20, 2));
					}
				}

				if (mBoss.isInWaterOrBubbleColumn()) {
					mBoss.teleport(mSpawnLoc);
				}
			}
		}.runTaskTimer(mPlugin, 0, 5);

		//The SpellShardShield class stores the information about the shield and handles adding/removing the shield
		mShieldSpell = new SpellShardShield(mBoss);

		SpellMusic music = new SpellMusic(mBoss, "epic:music.kaul", MUSIC_DURATION * 20, 3 * 20, 0, detectionRange, detectionRange, false, 0);

		//Only change between phase 1 and 2 is the cooldowns of the spells
		SpellManager phase1Actives = new SpellManager(Arrays.asList(
			new SpellEndlessAgony(mPlugin, this, mSpawnLoc, detectionRange, COOLDOWN_TICKS_1),
			new SpellForsakenLeap(mPlugin, mBoss, COOLDOWN_TICKS_1 - 1 * 20, this),
			new SpellVerdantProtection(mPlugin, mBoss, COOLDOWN_TICKS_1 - 1 * 20, this),
			new SpellRKitxetSummon(mPlugin, this, boss, COOLDOWN_TICKS_1 - 2 * 20)
		));
		SpellManager phase2Actives = new SpellManager(Arrays.asList(
			new SpellEndlessAgony(mPlugin, this, mSpawnLoc, detectionRange, COOLDOWN_TICKS_2),
			new SpellForsakenLeap(mPlugin, mBoss, COOLDOWN_TICKS_2, this),
			new SpellVerdantProtection(mPlugin, mBoss, COOLDOWN_TICKS_2 - 1 * 20, this),
			new SpellRKitxetSummon(mPlugin, this, boss, COOLDOWN_TICKS_2 - 1 * 20)
		));

		List<Spell> phase1Passives = Arrays.asList(
			new SpellKaulsFury(mPlugin, mBoss, this, 13 * 20, 5 * 20, 15, 10 * 20),
			mShieldSpell,
			new SpellBlockBreak(mBoss),
			new SpellShieldStun(10 * 20),
			new SpellEndlessAgonyDamage(mBoss, this),
			music
		);
		List<Spell> phase2Passives = Arrays.asList(
			new SpellKaulsFury(mPlugin, mBoss, this, 7 * 20, 3 * 20, 10, 7 * 20),
			mShieldSpell,
			new SpellBlockBreak(mBoss),
			new SpellShieldStun(10 * 20),
			new SpellEndlessAgonyDamage(mBoss, this),
			music
		);

		Map<Integer, BossHealthAction> events = new HashMap<>();
		events.put(50, (mBoss) -> {

			Collection<ArmorStand> nearbyStands = mBoss.getWorld().getNearbyEntitiesByType(ArmorStand.class, mBoss.getLocation(), detectionRange);
			for (ArmorStand stand : nearbyStands) {
				if (stand.getScoreboardTags().contains(GOLEM_TAG)) {
					Entity summon = LibraryOfSoulsIntegration.summon(stand.getLocation(), "DistortedHulk");
					if (summon instanceof LivingEntity golem) {
						golem.setPersistent(true);
						golem.setAI(false);
						Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
							if (golem.isValid() && !golem.isDead()) {
								golem.setAI(true);
							}
						}, 10);
					}
				}
			}

			for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
				player.sendMessage(ChatColor.GREEN + "I have seen something, something terrifying...");
			}
		});
		events.put(25, (mBoss) -> {
			//Phase 2
			//Active spells cast sooner
			//Shield regenerates much sooner and is less limited
			//Kaul's Fury casts much sooner and falls quicker
			changePhase(phase2Actives, phase2Passives, null);
			mShieldSpell.activatePhase2();

			for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
				player.sendMessage(ChatColor.GREEN + "Please, end this.");
			}
		});

		BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.RED, BarStyle.SEGMENTED_10, events);
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, bossBar, 7 * 20);
		mBoss.setInvulnerable(true);
		mBoss.setAI(false);

		new BukkitRunnable() {
			int mCount = 0;

			@Override
			public void run() {
				String message;
				if (mCount == 0) {
					message = "You must leave...";
				} else if (mCount == 1) {
					message = "I... cannot control it.";
				} else if (mCount == 2) {
					message = "I'm sorry...";
				} else if (mCount == 3) {
					message = ChatColor.DARK_GREEN + "THIS PLACE BELONGS TO THE JUNGLE ALONE. YOU ALL SHALL PAY FOR YOUR TRANSGRESSIONS.";
				} else {
					// Do health scaling here because players might not have been teleported in yet when init() would be run
					List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true);
					int playerCount = players.size();
					double hp = RKITXET_HEALTH * BossUtils.healthScalingCoef(playerCount, 0.5, 0.35);
					EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, hp);
					mBoss.setHealth(hp);
					mBoss.setInvulnerable(false);
					mBoss.setAI(true);
					EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_FOLLOW_RANGE, detectionRange);

					for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
						MessagingUtils.sendBoldTitle(player, ChatColor.DARK_GREEN + "R'Kitxet", ChatColor.GREEN + "Forsaken Elder");
						player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 2, false, true, true));
						player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10, 0.7f);
					}

					changePhase(phase1Actives, phase1Passives, null);
					this.cancel();
					return;
				}
				for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
					player.sendMessage(ChatColor.GREEN + message);
				}
				mCount++;
			}
		}.runTaskTimer(mPlugin, 0, 20);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		changePhase(SpellManager.EMPTY, Collections.emptyList(), null);

		for (LivingEntity e : EntityUtils.getNearbyMobs(mBoss.getLocation(), detectionRange)) {
			e.damage(10000);
		}

		new BukkitRunnable() {
			int mCount = 0;

			@Override
			public void run() {
				String message;
				if (mCount == 0) {
					message = "Every shard taken brings it closer...";
				} else if (mCount == 1) {
					message = "He... He is breaking it apart...";
				} else if (mCount == 2) {
					message = "I hope you can stop it, adventurer.";
				} else if (mCount == 3) {
					message = "Go now, before this wretched sanctum claims you too.";
				} else if (mCount == 4) {
					message = "Thank you...";
				} else {
					mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
					PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "stopsound @p");
					this.cancel();
					return;
				}
				for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
					player.sendMessage(ChatColor.GREEN + message);
				}
				mCount++;
			}
		}.runTaskTimer(mPlugin, 0, 20);
	}

	@Override
	public void onHurt(DamageEvent event) {
		if (mShieldSpell.isShielded()) {
			event.setCancelled(true);
			if (event.getDamager() instanceof Arrow arrow && arrow.hasCustomEffect(PotionEffectType.SLOW)) {
				arrow.removeCustomEffect(PotionEffectType.SLOW);
			}
			if (event.getSource() instanceof Player player) {
				player.sendMessage(ChatColor.AQUA + "The Elder's shield absorbs your attack.");
				player.playSound(mBoss.getLocation(), Sound.ITEM_SHIELD_BLOCK, SoundCategory.HOSTILE, 1, 1);
			}
		}
	}

	@Override
	public void bossChangedTarget(EntityTargetEvent event) {
		if (!(event.getTarget() instanceof Player)) {
			event.setCancelled(true);
		}
	}

	public LivingEntity getEntity() {
		return mBoss;
	}

	public Location getBossLocation() {
		return mBoss.getLocation();
	}

	public Location getSpawnLocation() {
		return mSpawnLoc;
	}

	// Cannot use any active spell 3 times in a row (bad/good luck protection)
	public boolean canUseSpell(String spell) {
		return !spell.equals(mLastUsedSpell) || !mSameSpellUsedTwice;
	}

	public void useSpell(String spell) {
		if (spell.equals(mLastUsedSpell)) {
			mSameSpellUsedTwice = true;
		} else {
			mLastUsedSpell = spell;
			mSameSpellUsedTwice = false;
		}
	}

	public @Nullable Player getAgonyTarget() {
		return mAgonyTarget;
	}

	public @Nullable Player getFuryTarget() {
		return mFuryTarget;
	}

	public void setAgonyTarget(@Nullable Player player) {
		mAgonyTarget = player;
	}

	public void setFuryTarget(@Nullable Player player) {
		mFuryTarget = player;
	}
}

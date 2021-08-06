package com.playmonumenta.plugins.depths.bosses;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockFace;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.bosses.spells.SpellPassiveExplosions;
import com.playmonumenta.plugins.depths.bosses.spells.SpellPassiveEyes;
import com.playmonumenta.plugins.depths.bosses.spells.SpellPassiveSummons;
import com.playmonumenta.plugins.depths.bosses.spells.SpellRisingTides;
import com.playmonumenta.plugins.depths.bosses.spells.SpellSurroundingDeath;
import com.playmonumenta.plugins.depths.bosses.spells.SpellTectonicDevastation;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;

import net.md_5.bungee.api.ChatColor;

public class Nucleus extends BossAbilityGroup {
	public static final String identityTag = "boss_nucleus";
	public static final int detectionRange = 50;
	public static final String DOOR_FILL_TAG = "Door";
	public static final int NUCLEUS_HEALTH = 7000;
	public static final int SWAP_TARGET_SECONDS = 15;
	public static final String EYE_STAND_TAG = "Plant";
	public static final String EYE_LOS = "GyrhaeddantEye";
	public static final int EYE_KILL_COUNT = 4;

	private static final int MUSIC_DURATION = 152; //seconds

	public final Location mSpawnLoc;
	public final Location mEndLoc;

	public int mCooldownTicks;
	public List<Location> mEyeSpawns;
	public Map<Location, LivingEntity> mEyes;
	public int mEyesKilled = 0;
	public boolean mIsHidden;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new Nucleus(plugin, boss, spawnLoc, endLoc);
		});
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}

	public Nucleus(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss);
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;
		mEyeSpawns = new ArrayList<>();
		mEyes = new HashMap<>();

		mBoss.setRemoveWhenFarAway(false);
		mBoss.addScoreboardTag("Boss");

		//Set/remove blocks
		if (spawnLoc.getBlock().getType() == Material.STONE_BUTTON) {
			spawnLoc.getBlock().setType(Material.AIR);
		}
		if (spawnLoc.getBlock().getRelative(BlockFace.DOWN).getType() == Material.BEDROCK) {
			spawnLoc.getBlock().getRelative(BlockFace.DOWN).setType(Material.SHROOMLIGHT);
		}

		//Switch mCooldownTicks depending on floor of party
		DepthsParty party = DepthsUtils.getPartyFromNearbyPlayers(mSpawnLoc);
		if (party == null || party.getFloor() == 3) {
			mCooldownTicks = 8 * 20;
		} else if (party.getFloor() == 6) {
			mCooldownTicks = 7 * 20;
		} else if (party.getFloor() % 3 == 0) {
			mCooldownTicks = 6 * 20;
		} else {
			mCooldownTicks = 8 * 20;
		}

		new BukkitRunnable() {
			Mob mTendrils = (Mob) mBoss;
			@Override
			public void run() {
				if (!mBoss.isValid() || mBoss.isDead()) {
					this.cancel();
					return;
				}

				List<Player> players = PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true);
				if (players != null && players.size() > 0) {
					Collections.shuffle(players);
					mTendrils.setTarget(players.get(0));
				}
			}
		}.runTaskTimer(mPlugin, 0, SWAP_TARGET_SECONDS * 20);

		Collection<ArmorStand> nearbyStands = mBoss.getWorld().getNearbyEntitiesByType(ArmorStand.class, mBoss.getLocation(), 40.0);
		for (ArmorStand stand : nearbyStands) {

			if (stand.getName().contains(EYE_STAND_TAG)) {
				Location loc = stand.getLocation();
				mEyeSpawns.add(loc);
				stand.remove();
				loc.getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.UP).setType(Material.AIR);
			}

			//Set bedrock behind boss room
			if (stand.getName().contains(DOOR_FILL_TAG)) {
				Location baseLoc = stand.getLocation().getBlock().getLocation();
				stand.remove();
				Location p1 = baseLoc.clone().add(0, -6, -6);
				Location p2 = baseLoc.clone().add(0, 6, 6);
				Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "fill " + (int) p1.getX() + " " + (int) p1.getY() + " " + (int) p1.getZ() + " " + (int) p2.getX() + " " + (int) p2.getY() + " " + (int) p2.getZ() + " bedrock");
				p1 = p1.clone().add(1, 0, 0);
				p2 = p2.clone().add(1, 0, 0);
				Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "fill " + (int) p1.getX() + " " + (int) p1.getY() + " " + (int) p1.getZ() + " " + (int) p2.getX() + " " + (int) p2.getY() + " " + (int) p2.getZ() + " black_concrete");
			}
		}

		//Spell setup
		SpellManager phase1Spells = new SpellManager(Arrays.asList(
				new SpellSurroundingDeath(plugin, mBoss, mSpawnLoc, mCooldownTicks, this),
				new SpellRisingTides(plugin, mBoss, mSpawnLoc, mCooldownTicks, this)
			));
		List<Spell> phase1Passives = Arrays.asList(
				new SpellBlockBreak(mBoss, 2, 3, 2),
				new SpellPassiveEyes(mBoss, this, spawnLoc),
				new SpellPassiveSummons(plugin, mBoss, 30.0, 15, mSpawnLoc.getY(), mSpawnLoc)
			);

		SpellManager phase2Spells = new SpellManager(Arrays.asList(
				new SpellTectonicDevastation(mPlugin, mBoss, mSpawnLoc, mCooldownTicks, this),
				new SpellSurroundingDeath(plugin, mBoss, mSpawnLoc, mCooldownTicks, this),
				new SpellRisingTides(plugin, mBoss, mSpawnLoc, mCooldownTicks, this)
			));
		List<Spell> phase2Passives = Arrays.asList(
			new SpellBlockBreak(mBoss, 2, 3, 2),
			new SpellPassiveEyes(mBoss, this, spawnLoc),
			new SpellPassiveSummons(plugin, mBoss, 30.0, 15, mSpawnLoc.getY(), mSpawnLoc)
		);

		SpellManager phase3Spells = new SpellManager(Arrays.asList(
			new SpellTectonicDevastation(mPlugin, mBoss, mSpawnLoc, mCooldownTicks, this),
			new SpellSurroundingDeath(plugin, mBoss, mSpawnLoc, mCooldownTicks, this),
			new SpellRisingTides(plugin, mBoss, mSpawnLoc, mCooldownTicks, this)
			//new SpellTentacleCrawl(plugin, mBoss, mSpawnLoc, mCooldownTicks, this)
		));
		List<Spell> phase3Passives = Arrays.asList(
			new SpellBlockBreak(mBoss, 2, 3, 2),
			new SpellPassiveExplosions(mBoss, mSpawnLoc),
			new SpellPassiveEyes(mBoss, this, spawnLoc),
			new SpellPassiveSummons(plugin, mBoss, 30.0, 15, mSpawnLoc.getY(), mSpawnLoc)
		);

		Map<Integer, BossHealthAction> events = new HashMap<Integer, BossHealthAction>();

		events.put(60, (mBoss) -> {
			mCooldownTicks -= 30;
			changePhase(phase2Spells, phase2Passives, null);
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Gyrheaddant Nucleus]\",\"color\":\"gold\"},{\"text\":\" Beyond... I \",\"color\":\"red\"},{\"text\":\"nb\",\"obfuscated\":\"true\",\"color\":\"red\"},{\"text\":\" push further into \",\"color\":\"red\"},{\"text\":\"nbff\",\"obfuscated\":\"true\",\"color\":\"red\"},{\"text\":\"this reality... Quickness... Yes... \",\"color\":\"red\"},{\"text\":\"hggghg\",\"obfuscated\":\"true\",\"color\":\"red\"},{\"text\":\" Sink...\",\"color\":\"red\"}]");
			forceCastSpell(SpellTectonicDevastation.class);
			hide();
		});
		events.put(20, (mBoss) -> {
			mCooldownTicks -= 30;
			changePhase(phase3Spells, phase3Passives, null);
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Gyrheaddant Nucleus]\",\"color\":\"gold\"},{\"text\":\" This \",\"color\":\"red\"},{\"text\":\"ygg\",\"obfuscated\":\"true\",\"color\":\"red\"},{\"text\":\"Void sustains me... Faster now... \",\"color\":\"red\"},{\"text\":\"hfhu\",\"obfuscated\":\"true\",\"color\":\"red\"},{\"text\":\"Faster...\",\"color\":\"red\"}]");
			forceCastSpell(SpellTectonicDevastation.class);
			hide();
		});
		BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.RED, BarStyle.SEGMENTED_10, events);
		super.constructBoss(phase1Spells, phase1Passives, detectionRange, bossBar, 12 * 20);

		hide();
	}

	public void killedEye() {
		mEyesKilled++;

		if (mEyesKilled >= EYE_KILL_COUNT) {
			PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"The nucleus is exposed!\",\"color\":\"red\"}]");
			expose();
		} else {
			for (Player p : PlayerUtils.playersInRange(mBoss.getLocation(), 50, true)) {
				p.sendMessage(ChatColor.RED + "You killed an eye! You need to take down " + (EYE_KILL_COUNT - mEyesKilled) + " more!");
			}
		}
	}

	public void expose() {
		mBoss.setGlowing(true);
		mBoss.teleport(mSpawnLoc.clone().add(0, 2, 0));
		mBoss.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
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
		mBoss.setGlowing(false);
		mBoss.teleport(mSpawnLoc.clone().add(0, 15, 0));
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 1000000, 4));
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 1000000, 0));
		mEyesKilled = 0;
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

	public void updateEyes() {
		for (Location l : mEyeSpawns) {
			if (mEyes.get(l) == null) {
				continue;
			}
			if (mEyes.get(l).isDead()) {
				mEyes.remove(l);
				killedEye();
				l.getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.UP).setType(Material.AIR);
			}
		}
	}

	public void spawnEye() {
		//Get an open location
		Collections.shuffle(mEyeSpawns);
		for (Location loc : mEyeSpawns) {
			List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(loc, 1.0);
			if (mEyes.get(loc) == null && nearbyMobs.size() == 0) {
				//Summon a new plant here
				String plant = EYE_LOS;

				LivingEntity newPlant = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, plant);
				mEyes.put(loc, newPlant);
				newPlant.setAI(false);
				newPlant.setGlowing(true);
				loc.getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.UP).setType(Material.FIRE);

				mBoss.getWorld().playSound(loc, Sound.ENTITY_WITHER_BREAK_BLOCK, 20.0f, 1.0f);
				mBoss.getWorld().playSound(loc, Sound.BLOCK_GRASS_PLACE, 20.0f, 1.0f);

				new BukkitRunnable() {

					@Override
					public void run() {
						mEyes.remove(loc, newPlant);
						newPlant.remove();
						loc.getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.UP).setType(Material.AIR);
					}

				}.runTaskLater(mPlugin, 20 * 9);

				break;
			}
		}
	}


	@Override
	public void init() {
		mBoss.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(0);
		mBoss.setAI(false);

		// Health is scaled by 1.5 times each time you fight the boss
		DepthsParty party = DepthsUtils.getPartyFromNearbyPlayers(mSpawnLoc);
		int modifiedHealth = (int) (NUCLEUS_HEALTH * Math.pow(1.25, (party.getFloor() - 1) / 3));
		mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(modifiedHealth);
		mBoss.setHealth(modifiedHealth);

		Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "growable grow " + (int) (mSpawnLoc.getX() - 1) + " " + (int) (mSpawnLoc.getY() + 21) + " " + (int) (mSpawnLoc.getZ() - 1) + " jellyfish 1 20 true");

		new BukkitRunnable() {

			int mTicks = 0;

			@Override
			public void run() {
				mTicks += 5;
				mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 20.0f, 0.5f + (mTicks / 25));

				//launch event related spawn commands
				if (mTicks >= 6 * 20) {
					this.cancel();
					PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "effect give @s minecraft:blindness 2 2");
					PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s title [\"\",{\"text\":\"Gyrhaeddant\",\"color\":\"dark_red\",\"bold\":true}]");
					PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s subtitle [\"\",{\"text\":\"The Nucleus\",\"color\":\"dark_red\",\"bold\":true}]");
					PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound minecraft:entity.wither.spawn master @s ~ ~ ~ 10 0.7");
					mMusicRunnable.runTaskTimer(mPlugin, 0, MUSIC_DURATION * 20 + 20);
				}

			}

		}.runTaskTimer(mPlugin, 0, 5);


	}

	@Override
	public void death(EntityDeathEvent event) {
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound minecraft:entity.enderdragon.death master @s ~ ~ ~ 100 0.8");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "tellraw @s [\"\",{\"text\":\"[Gyrheaddant Nucleus]\",\"color\":\"gold\"},{\"text\":\" B\",\"color\":\"red\"},{\"text\":\"ngrbgg\",\"obfuscated\":\"true\",\"color\":\"red\"},{\"text\":\"A\",\"color\":\"red\"},{\"text\":\"gbg\",\"obfuscated\":\"true\",\"color\":\"red\"},{\"text\":\"C\",\"color\":\"red\"},{\"text\":\"bggbg\",\"obfuscated\":\"true\",\"color\":\"red\"},{\"text\":\"K!!! AWAY!!! This world... \",\"color\":\"red\"},{\"text\":\"hhgg\",\"obfuscated\":\"true\",\"color\":\"red\"},{\"text\":\"is poison...\",\"color\":\"red\"}]");
		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 2));
			player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 10, 2));
		}

		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);

		//Kill nearby mobs
		for (LivingEntity e : EntityUtils.getNearbyMobs(mBoss.getLocation(), 40.0)) {
			e.damage(10000);
		}

		DepthsUtils.animate(mBoss.getLocation());
		//Send players
		new BukkitRunnable() {

			@Override
			public void run() {
				PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "effect give @s minecraft:blindness 2 2");
				PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "stopsound @p");
				mMusicRunnable.cancel();
			}

		}.runTaskLater(mPlugin, 60);

		new BukkitRunnable() {

			@Override
			public void run() {
				DepthsManager.getInstance().goToNextFloor(PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true).get(0));
			}

		}.runTaskLater(mPlugin, 80);
	}

	@Override
	public void bossDamagedEntity(EntityDamageByEntityEvent event) {
		//Slow on hit
		if (event.getEntity() instanceof Player) {
			((Player) event.getEntity()).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 1));
		}
	}

	BukkitRunnable mMusicRunnable = new BukkitRunnable() {
		@Override
		public void run() {
			if (mBoss == null || mBoss.getHealth() <= 0) {
				this.cancel();
			}
			PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound epic:music.nucleus record @s ~ ~ ~ 2");
		}
	};
}

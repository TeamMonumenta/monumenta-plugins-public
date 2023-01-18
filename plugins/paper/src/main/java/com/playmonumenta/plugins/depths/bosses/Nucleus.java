package com.playmonumenta.plugins.depths.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellMusic;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.bosses.spells.SpellPassiveEyes;
import com.playmonumenta.plugins.depths.bosses.spells.SpellPassiveSummons;
import com.playmonumenta.plugins.depths.bosses.spells.SpellRisingTides;
import com.playmonumenta.plugins.depths.bosses.spells.SpellSurroundingDeath;
import com.playmonumenta.plugins.depths.bosses.spells.SpellTectonicDevastation;
import com.playmonumenta.plugins.depths.bosses.spells.SpellVolcanicDeepmise;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockFace;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public final class Nucleus extends BossAbilityGroup {
	public static final String identityTag = "boss_nucleus";
	public static final int detectionRange = 50;
	public static final String DOOR_FILL_TAG = "Door";
	public static final int NUCLEUS_HEALTH = 8000;
	public static final int SWAP_TARGET_SECONDS = 15;
	public static final String EYE_STAND_TAG = "Plant";
	public static final String EYE_LOS = "GyrhaeddantEye";
	public static final int EYE_KILL_COUNT = 4;

	public static final String MUSIC_TITLE = "epic:music.nucleus";
	private static final int MUSIC_DURATION = 152; //seconds

	public final Location mSpawnLoc;
	public final Location mEndLoc;

	public int mCooldownTicks;
	public List<Location> mEyeSpawns;
	public Map<Location, LivingEntity> mEyes;
	public int mEyesKilled = 0;
	public boolean mIsHidden;
	public boolean mCanSpawnMobs = true;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) ->
			new Nucleus(plugin, boss, spawnLoc, endLoc));
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
		if (spawnLoc.isChunkLoaded()) {
			if (spawnLoc.getBlock().getType() == Material.STONE_BUTTON) {
				spawnLoc.getBlock().setType(Material.AIR);
			}
			if (spawnLoc.getBlock().getRelative(BlockFace.DOWN).getType() == Material.BEDROCK) {
				spawnLoc.getBlock().getRelative(BlockFace.DOWN).setType(Material.SHROOMLIGHT);
			}
		}

		//Switch mCooldownTicks depending on floor of party
		DepthsParty party = DepthsUtils.getPartyFromNearbyPlayers(mSpawnLoc);
		int surroundingDeathCooldown = 14 * 20;
		if (party == null || party.getFloor() == 3) {
			mCooldownTicks = 8 * 20;
			//Disable passive mob spawning until 90% hp if fighting for the first time
			mCanSpawnMobs = false;
		} else if (party.getFloor() == 6) {
			mCooldownTicks = 7 * 20;
			surroundingDeathCooldown = 10 * 20;
		} else if (party.getFloor() % 3 == 0) {
			mCooldownTicks = 6 * 20;
			if (party.getFloor() == 9) {
				surroundingDeathCooldown = 6 * 20;
			} else {
				surroundingDeathCooldown = 4 * 20;
			}
		} else {
			mCooldownTicks = 8 * 20;
		}

		new BukkitRunnable() {
			final Mob mTendrils = (Mob) mBoss;
			@Override
			public void run() {
				if (!mBoss.isValid() || mBoss.isDead()) {
					this.cancel();
					return;
				}

				List<Player> players = PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true);
				if (!players.isEmpty()) {
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
				LocationUtils.fillBlocks(p1, p2, Material.BEDROCK);
				p1 = p1.clone().add(1, 0, 0);
				p2 = p2.clone().add(1, 0, 0);
				LocationUtils.fillBlocks(p1, p2, Material.BLACK_CONCRETE);
			}
		}

		SpellMusic music = new SpellMusic(mBoss, MUSIC_TITLE, MUSIC_DURATION * 20, 2.0f, 0, detectionRange, detectionRange, false, 0, true);

		//Spell setup
		SpellManager phase1Spells = new SpellManager(Arrays.asList(
				new SpellSurroundingDeath(plugin, mBoss, mSpawnLoc, surroundingDeathCooldown, this),
				new SpellRisingTides(plugin, mBoss, mSpawnLoc, mCooldownTicks, this)
			));
		List<Spell> phase1Passives = Arrays.asList(
			new SpellBlockBreak(mBoss, 2, 3, 2),
			new SpellPassiveEyes(mBoss, this, spawnLoc),
			new SpellPassiveSummons(plugin, mBoss, 30.0, 15, mSpawnLoc.getY(), mSpawnLoc, party == null ? 1 : ((party.getFloor() - 1) / 3) + 1, this),
			music
		);

		SpellManager phase2Spells = new SpellManager(Arrays.asList(
				new SpellTectonicDevastation(mPlugin, mBoss, mSpawnLoc, mCooldownTicks, this),
				new SpellSurroundingDeath(plugin, mBoss, mSpawnLoc, surroundingDeathCooldown, this),
				new SpellRisingTides(plugin, mBoss, mSpawnLoc, mCooldownTicks, this)
			));
		List<Spell> phase2Passives = Arrays.asList(
			new SpellBlockBreak(mBoss, 2, 3, 2),
			new SpellPassiveEyes(mBoss, this, spawnLoc),
			new SpellPassiveSummons(plugin, mBoss, 30.0, 15, mSpawnLoc.getY(), mSpawnLoc, party == null ? 1 : ((party.getFloor() - 1) / 3) + 1, this),
			music
		);

		SpellManager phase3Spells = new SpellManager(Arrays.asList(
				new SpellTectonicDevastation(mPlugin, mBoss, mSpawnLoc, mCooldownTicks, this),
				new SpellSurroundingDeath(plugin, mBoss, mSpawnLoc, surroundingDeathCooldown, this),
				new SpellRisingTides(plugin, mBoss, mSpawnLoc, mCooldownTicks, this)
			));
		List<Spell> phase3Passives = Arrays.asList(
			new SpellBlockBreak(mBoss, 2, 3, 2),
			new SpellVolcanicDeepmise(mBoss, mSpawnLoc),
			new SpellPassiveEyes(mBoss, this, spawnLoc),
			new SpellPassiveSummons(plugin, mBoss, 30.0, 15, mSpawnLoc.getY(), mSpawnLoc, party == null ? 1 : ((party.getFloor() - 1) / 3) + 1, this),
			music
		);

		Map<Integer, BossHealthAction> events = new HashMap<>();
		events.put(90, (mBoss) -> mCanSpawnMobs = true);
		events.put(60, (mBoss) -> {
			mCooldownTicks -= 30;
			changePhase(phase2Spells, phase2Passives, null);
			PlayerUtils.nearbyPlayersAudience(mBoss.getLocation(), detectionRange)
				.sendMessage(Component.text("", NamedTextColor.RED)
					.append(Component.text("[Gyrhaeddant Nucleus]", NamedTextColor.GOLD))
					.append(Component.text(" Beyond... I "))
					.append(Component.text("nb").decoration(TextDecoration.OBFUSCATED, true))
					.append(Component.text(" push further into "))
					.append(Component.text("nbff").decoration(TextDecoration.OBFUSCATED, true))
					.append(Component.text("this reality... Quickness... Yes... "))
					.append(Component.text("hggghg").decoration(TextDecoration.OBFUSCATED, true))
					.append(Component.text(" Sink...")));
			forceCastSpell(SpellTectonicDevastation.class);
			hide();
		});
		events.put(20, (mBoss) -> {
			mCooldownTicks -= 30;
			changePhase(phase3Spells, phase3Passives, null);
			PlayerUtils.nearbyPlayersAudience(mBoss.getLocation(), detectionRange)
				.sendMessage(Component.text("", NamedTextColor.RED)
					.append(Component.text("[Gyrhaeddant Nucleus]", NamedTextColor.GOLD))
					.append(Component.text(" This "))
					.append(Component.text("ygg").decoration(TextDecoration.OBFUSCATED, true))
					.append(Component.text("Void sustains me... Faster now... "))
					.append(Component.text("hfhu").decoration(TextDecoration.OBFUSCATED, true))
					.append(Component.text("Faster...")));
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
			PlayerUtils.nearbyPlayersAudience(mBoss.getLocation(), detectionRange)
				.sendMessage(Component.text("The nucleus is exposed!", NamedTextColor.RED));
			expose();
		} else {
			PlayerUtils.nearbyPlayersAudience(mBoss.getLocation(), detectionRange)
				.sendMessage(Component.text("You killed an eye! You need to take down " + (EYE_KILL_COUNT - mEyesKilled) + " more!", NamedTextColor.RED));
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
				//Summon a new eye here
				LivingEntity newEye = Objects.requireNonNull((LivingEntity) LibraryOfSoulsIntegration.summon(loc, EYE_LOS));
				mEyes.put(loc, newEye);
				newEye.setAI(false);
				newEye.setGlowing(true);
				loc.getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.UP).setType(Material.FIRE);

				mBoss.getWorld().playSound(loc, Sound.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.HOSTILE, 20.0f, 1.0f);
				mBoss.getWorld().playSound(loc, Sound.BLOCK_GRASS_PLACE, SoundCategory.HOSTILE, 20.0f, 1.0f);

				new BukkitRunnable() {

					@Override
					public void run() {
						mEyes.remove(loc, newEye);
						newEye.remove();
						loc.getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.UP).setType(Material.AIR);
					}

				}.runTaskLater(mPlugin, 20 * 9);

				break;
			}
		}
	}


	@Override
	public void init() {
		mBoss.setAI(false);

		// Health is scaled by 1.15 times each time you fight the boss
		DepthsParty party = DepthsUtils.getPartyFromNearbyPlayers(mSpawnLoc);
		int modifiedHealth = (int) (NUCLEUS_HEALTH * Math.pow(1.15, party == null ? 0 : (party.getFloor() - 1) / 3.0));
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, modifiedHealth);
		mBoss.setHealth(modifiedHealth);

		Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "execute at " + mBoss.getUniqueId() + " run growable grow " + (int) (mSpawnLoc.getX() - 1) + " " + (int) (mSpawnLoc.getY() + 21) + " " + (int) (mSpawnLoc.getZ() - 1) + " jellyfish 1 20 true");

		new BukkitRunnable() {

			int mTicks = 0;

			@Override
			public void run() {
				mTicks += 5;
				mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, SoundCategory.HOSTILE, 20.0f, 0.5f + (mTicks / 25.0f));

				//launch event related spawn commands
				if (mTicks >= 6 * 20) {
					this.cancel();
					for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
						MessagingUtils.sendBoldTitle(player, ChatColor.DARK_RED + "Gyrhaeddant", ChatColor.DARK_RED + "The Nucleus");
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
			player.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, SoundCategory.HOSTILE, 100.0f, 0.8f);
			player.sendMessage(Component.text("", NamedTextColor.RED)
				.append(Component.text("[Gyrhaeddant Nucleus]", NamedTextColor.GOLD))
				.append(Component.text(" B"))
				.append(Component.text("ngrbgg").decoration(TextDecoration.OBFUSCATED, true))
				.append(Component.text("A"))
				.append(Component.text("gbg").decoration(TextDecoration.OBFUSCATED, true))
				.append(Component.text("C"))
				.append(Component.text("bggbg").decoration(TextDecoration.OBFUSCATED, true))
				.append(Component.text("K!!! AWAY!!! This world... "))
				.append(Component.text("hhgg").decoration(TextDecoration.OBFUSCATED, true))
				.append(Component.text("is poison...")));
			player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 2));
			player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 10, 2));
		}

		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);

		//Kill nearby mobs
		for (LivingEntity e : EntityUtils.getNearbyMobs(mBoss.getLocation(), 40.0)) {
			e.damage(10000);
		}

		EntityUtils.fireworkAnimation(mBoss);

		new BukkitRunnable() {

			@Override
			public void run() {
				Player nearestPlayer = EntityUtils.getNearestPlayer(mBoss.getLocation(), detectionRange);
				if (nearestPlayer != null) {
					DepthsManager.getInstance().goToNextFloor(nearestPlayer);
				}
			}

		}.runTaskLater(mPlugin, 20);
	}
}

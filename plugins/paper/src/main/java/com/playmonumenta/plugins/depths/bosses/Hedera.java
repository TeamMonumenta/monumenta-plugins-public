package com.playmonumenta.plugins.depths.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.bosses.spells.SpellEndlessHederaSummons;
import com.playmonumenta.plugins.depths.bosses.spells.SpellEvolutionSeeds;
import com.playmonumenta.plugins.depths.bosses.spells.SpellHederaAnticheese;
import com.playmonumenta.plugins.depths.bosses.spells.SpellIvyGarden;
import com.playmonumenta.plugins.depths.bosses.spells.SpellLeafNova;
import com.playmonumenta.plugins.depths.bosses.spells.SpellPassiveGarden;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;
import com.playmonumenta.scriptedquests.managers.SongManager;
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
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
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
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class Hedera extends BossAbilityGroup {
	public static final String identityTag = "boss_hedera";
	public static final int detectionRange = 70;
	public static final String PLANT_STAND_TAG = "Plant";
	public static final String DOOR_FILL_TAG = "Door";
	public static final double PLANT_CONSUME_PERCENT = 0.05;
	public static final int HEDERA_HEALTH = 4000;
	public static final int SWAP_TARGET_SECONDS = 15;

	public static final String MUSIC_TITLE = "epic:music.hedera";
	private static final int MUSIC_DURATION = 202; //seconds

	private final Location mSpawnLoc;
	private final Location mEndLoc;

	public List<Location> mPlantSpawns;
	public Map<Location, LivingEntity> mPlants;
	public Map<Location, String> mPlantTypes;
	public int mCooldownTicks;
	public int mTimesHealed = 0;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) ->
			new Hedera(plugin, boss, spawnLoc, endLoc));
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}

	public Hedera(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss);
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;

		mBoss.setRemoveWhenFarAway(false);
		mBoss.addScoreboardTag("Boss");

		mPlantSpawns = new ArrayList<>();
		mPlants = new HashMap<>();
		mPlantTypes = new HashMap<>();


		//Switch mCooldownTicks depending on floor of party
		DepthsParty party = DepthsUtils.getPartyFromNearbyPlayers(mSpawnLoc);
		if (party == null || party.getFloor() == 1) {
			mCooldownTicks = 8 * 20;
		} else if (party.getFloor() == 4) {
			mCooldownTicks = 6 * 20;
		} else if (party.getFloor() % 3 == 1) {
			mCooldownTicks = 5 * 20;
		} else {
			mCooldownTicks = 8 * 20;
		}

		//Set/remove blocks
		if (spawnLoc.isChunkLoaded() && spawnLoc.getBlock().getType() == Material.STONE_BUTTON) {
			spawnLoc.getBlock().setType(Material.AIR);
		}

		new BukkitRunnable() {
			final Mob mWitch = (Mob) mBoss;
			@Override
			public void run() {
				if (!mBoss.isValid() || mBoss.isDead()) {
					this.cancel();
					return;
				}

				List<Player> players = PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true);
				if (players.size() > 0) {
					Collections.shuffle(players);
					mWitch.setTarget(players.get(0));
				}
			}
		}.runTaskTimer(mPlugin, 0, SWAP_TARGET_SECONDS * 20);

		Collection<ArmorStand> nearbyStands = mBoss.getWorld().getNearbyEntitiesByType(ArmorStand.class, mBoss.getLocation(), 30.0);
		for (ArmorStand stand : nearbyStands) {
			if (stand.getName().contains(PLANT_STAND_TAG)) {
				mPlantSpawns.add(stand.getLocation());
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


		SpellManager activeSpells = new SpellManager(Arrays.asList(
			//new SpellEarthshake(plugin, mBoss, 5, 80),
			new SpellLeafNova(plugin, mBoss, mCooldownTicks),
			new SpellIvyGarden(plugin, mCooldownTicks, mPlants),
			new SpellEvolutionSeeds(plugin, mCooldownTicks, mPlants, mPlantTypes)
		));
		//Extra summon ability if fighting on f4 or higher
		if (party != null && party.getFloor() != 1) {
			activeSpells = new SpellManager(Arrays.asList(
					//new SpellEarthshake(plugin, mBoss, 5, 80),
					new SpellLeafNova(plugin, mBoss, mCooldownTicks),
					new SpellIvyGarden(plugin, mCooldownTicks, mPlants),
					new SpellEvolutionSeeds(plugin, mCooldownTicks, mPlants, mPlantTypes),
					new SpellEndlessHederaSummons(mBoss, mCooldownTicks, ((party.getFloor() - 1) / 3) + 1)
			));
		}

		List<Spell> passiveSpells = Arrays.asList(
			new SpellBlockBreak(mBoss, 2, 3, 2, true, Material.AIR),
			new SpellHederaAnticheese(mBoss, mSpawnLoc),
			new SpellPassiveGarden(mBoss, mPlantSpawns, mPlants, mPlantTypes, mSpawnLoc)
		);

		Map<Integer, BossHealthAction> events = new HashMap<>();
		BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.RED, BarStyle.SEGMENTED_10, events);
		super.constructBoss(activeSpells, passiveSpells, detectionRange, bossBar);
	}

	@Override
	public void init() {
		// Health is scaled by 1.15 times each time you fight the boss
		DepthsParty party = DepthsUtils.getPartyFromNearbyPlayers(mSpawnLoc);
		int modifiedHealth = (int) (HEDERA_HEALTH * Math.pow(1.15, party == null ? 0.0 : party.getFloor() / 3.0));
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, modifiedHealth);
		mBoss.setHealth(modifiedHealth);

		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true);
		SongManager.playBossSong(players, new SongManager.Song(MUSIC_TITLE, SoundCategory.RECORDS, MUSIC_DURATION, true, 2.0f, 1.0f, true), true, mBoss, true, 0, 5);

		for (Player player : players) {
			MessagingUtils.sendBoldTitle(player, ChatColor.DARK_GRAY + "Hedera", ChatColor.GRAY + "Venom of the Waves");
			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 2, false, true, true));
			player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10, 0.7f);
		}
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
			player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, SoundCategory.HOSTILE, 100.0f, 0.8f);
			player.sendMessage(Component.text("", NamedTextColor.DARK_GREEN)
				.append(Component.text("[Hedera]", NamedTextColor.GOLD))
				.append(Component.text(" No! No! This cannot be! The Broken Beyond must let me flee!")));
			player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 2));
			player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 10, 2));
			PotionEffect poisonEffect = player.getPotionEffect(PotionEffectType.POISON);
			if (poisonEffect != null) {
				player.removePotionEffect(PotionEffectType.POISON);
			}
		}
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);

		// Remove the plant armor stands
		for (ArmorStand stand : mBoss.getWorld().getNearbyEntitiesByType(ArmorStand.class, mBoss.getLocation(), detectionRange)) {
			if (stand.getName().contains(PLANT_STAND_TAG)) {
				stand.remove();
			}
		}

		//Kill nearby mobs
		for (LivingEntity e : EntityUtils.getNearbyMobs(mBoss.getLocation(), detectionRange)) {
			e.damage(10000);
		}

		//Finish animation
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

	@Override
	public void onHurt(DamageEvent event) {
		boolean plantsAlive = !mPlants.isEmpty();

		//Prevents bypassing plants if a single high-damage hit is done
		if (event.getFinalDamage(true) >= mBoss.getHealth() && plantsAlive) {
			event.setDamage(1);
			mBoss.setHealth(1);
		}

		//Consume random plant if there are any
		if (mBoss.getHealth() < PLANT_CONSUME_PERCENT * EntityUtils.getMaxHealth(mBoss) && plantsAlive) {

			//If not being damaged from a source, don't consume plants but don't take damage
			if (event.getSource() == null) {
				event.setDamage(1);
			}

			Collection<Location> plantsCollection = mPlants.keySet();
			List<Location> plants = new ArrayList<>(plantsCollection);
			Collections.shuffle(plants);
			Location unluckyPlant = plants.get(0);

			Objects.requireNonNull(mPlants.get(unluckyPlant)).damage(10000);
			mPlants.remove(unluckyPlant);
			mPlantTypes.remove(unluckyPlant);
			PlayerUtils.nearbyPlayersAudience(mBoss.getLocation(), detectionRange)
					.sendMessage(Component.text("", NamedTextColor.DARK_GREEN)
						.append(Component.text("[Hedera]", NamedTextColor.GOLD))
						.append(Component.text(" Consumeth me mine hydrophytes, the vines begone now give me life!")));
			//Heal Hedera
			double amountToHeal = Math.max(.05, .25 - (mTimesHealed * .05));
			mBoss.setHealth(mBoss.getHealth() + (EntityUtils.getMaxHealth(mBoss) * amountToHeal));
			mTimesHealed++;

			//Particles from consumed plant to Hedera
			new BukkitRunnable() {
				int mCount = 0;
				final int mMaxCount = Math.max(15, (int) mBoss.getEyeLocation().distance(unluckyPlant));
				@Override
				public void run() {
					if (mCount >= mMaxCount) {
						this.cancel();
					}

					Location bossEyeLoc = mBoss.getEyeLocation();
					Vector particleVector = bossEyeLoc.subtract(unluckyPlant).toVector().multiply(((double) mCount) / mMaxCount);
					new PartialParticle(Particle.VILLAGER_HAPPY, unluckyPlant.add(particleVector), 5).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.SPELL_WITCH, unluckyPlant.add(particleVector), 5).spawnAsEntityActive(mBoss);

					mCount++;
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
	}
}

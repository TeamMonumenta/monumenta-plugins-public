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
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
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
import org.bukkit.util.Vector;

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
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;

public class Hedera extends BossAbilityGroup {
	public static final String identityTag = "boss_hedera";
	public static final int detectionRange = 70;
	public static final String PLANT_STAND_TAG = "Plant";
	public static final String DOOR_FILL_TAG = "Door";
	public static final int PLANT_CONSUME_HP = 150;
	public static final int HEDERA_HEALTH = 4000;
	public static final int SWAP_TARGET_SECONDS = 15;

	private static final int MUSIC_DURATION = 202; //seconds

	private final Location mSpawnLoc;
	private final Location mEndLoc;

	public List<Location> mPlantSpawns;
	public Map<Location, LivingEntity> mPlants;
	public Map<Location, String> mPlantTypes;
	public int mCooldownTicks;
	public int mTimesHealed = 0;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new Hedera(plugin, boss, spawnLoc, endLoc);
		});
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
		if (spawnLoc.getBlock().getType() == Material.STONE_BUTTON) {
			spawnLoc.getBlock().setType(Material.AIR);
		}

		new BukkitRunnable() {
			Mob mWitch = (Mob) mBoss;
			@Override
			public void run() {
				if (!mBoss.isValid() || mBoss.isDead()) {
					this.cancel();
					return;
				}

				List<Player> players = PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true);
				if (players != null && players.size() > 0) {
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
				Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "fill " + (int) p1.getX() + " " + (int) p1.getY() + " " + (int) p1.getZ() + " " + (int) p2.getX() + " " + (int) p2.getY() + " " + (int) p2.getZ() + " bedrock");
				p1 = p1.clone().add(1, 0, 0);
				p2 = p2.clone().add(1, 0, 0);
				Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "fill " + (int) p1.getX() + " " + (int) p1.getY() + " " + (int) p1.getZ() + " " + (int) p2.getX() + " " + (int) p2.getY() + " " + (int) p2.getZ() + " black_concrete");
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

		Map<Integer, BossHealthAction> events = new HashMap<Integer, BossHealthAction>();
		BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.RED, BarStyle.SEGMENTED_10, events);
		super.constructBoss(activeSpells, passiveSpells, detectionRange, bossBar);
	}

	@Override
	public void init() {
		// Health is scaled by 1.15 times each time you fight the boss
		DepthsParty party = DepthsUtils.getPartyFromNearbyPlayers(mSpawnLoc);
		int modifiedHealth = (int) (HEDERA_HEALTH * Math.pow(1.15, party == null ? 0 : party.getFloor() / 3));
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, modifiedHealth);
		mBoss.setHealth(modifiedHealth);

		//launch event related spawn commands
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "effect give @s minecraft:blindness 2 2");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s title [\"\",{\"text\":\"Hedera\",\"color\":\"dark_gray\",\"bold\":true}]");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s subtitle [\"\",{\"text\":\"Venom of the Waves\",\"color\":\"gray\",\"bold\":true}]");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound minecraft:entity.wither.spawn master @s ~ ~ ~ 10 0.7");
		mMusicRunnable.runTaskTimer(mPlugin, 0, MUSIC_DURATION * 20 + 20);
	}

	@Override
	public void death(EntityDeathEvent event) {
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound minecraft:entity.enderdragon.death master @s ~ ~ ~ 100 0.8");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "tellraw @s [\"\",{\"text\":\"[Hedera]\",\"color\":\"gold\"},{\"text\":\" No! No! This cannot be! The Broken Beyond must let me flee!\",\"color\":\"dark_green\"}]");
		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 2));
			player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 10, 2));
			PotionEffect poisonEffect = player.getPotionEffect(PotionEffectType.POISON);
			if (poisonEffect != null) {
				player.removePotionEffect(PotionEffectType.POISON);
			}
		}
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);

		// Remove the plant armorstands
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
		DepthsUtils.animate(mBoss.getLocation());
		//Send players
		new BukkitRunnable() {

			@Override
			public void run() {
				PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "effect give @s minecraft:blindness 2 2");
				PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "stopsound @p");
				if (!mMusicRunnable.isCancelled()) {
					mMusicRunnable.cancel();
				}
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
	public void bossDamagedByEntity(EntityDamageByEntityEvent event) {
		boolean plantsAlive = mPlants.values().size() > 0;

		//Prevents bypassing plants if a single high damage hit is done
		if (event.getFinalDamage() >= mBoss.getHealth() && plantsAlive) {
			event.setDamage(1);
			mBoss.setHealth(1);
		}

		//Consume random plant if there are any
		if (mBoss.getHealth() < PLANT_CONSUME_HP && plantsAlive) {
			Collection<Location> plantsCollection = mPlants.keySet();
			List<Location> plants = new ArrayList<>();
			for (Location plant : plantsCollection) {
				plants.add(plant);
			}
			Collections.shuffle(plants);
			Location unluckyPlant = plants.get(0);

			mPlants.get(unluckyPlant).damage(10000);
			mPlants.remove(unluckyPlant);
			mPlantTypes.remove(unluckyPlant);
			PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "tellraw @s [\"\",{\"text\":\"[Hedera]\",\"color\":\"gold\"},{\"text\":\" Consumeth me mine hydrophytes, the vines begone now give me life!\",\"color\":\"dark_green\"}]");
			//Heal Hedera
			double amountToHeal = Math.max(.05, .25 - (mTimesHealed * .05));
			mBoss.setHealth(mBoss.getHealth() + (EntityUtils.getMaxHealth(mBoss) * amountToHeal));
			mTimesHealed++;

			//Particles from consumed plant to Hedera
			World world = mBoss.getWorld();
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
					world.spawnParticle(Particle.VILLAGER_HAPPY, unluckyPlant.add(particleVector), 5);
					world.spawnParticle(Particle.SPELL_WITCH, unluckyPlant.add(particleVector), 5);

					mCount++;
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	BukkitRunnable mMusicRunnable = new BukkitRunnable() {
		@Override
		public void run() {
			if (mBoss == null || mBoss.getHealth() <= 0) {
				this.cancel();
			}
			PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound epic:music.hedera record @s ~ ~ ~ 2");
		}
	};
}

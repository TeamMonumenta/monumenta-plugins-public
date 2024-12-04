package com.playmonumenta.plugins.depths.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.SerializedLocationBossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.bosses.spells.hedera.SpellEndlessHederaSummons;
import com.playmonumenta.plugins.depths.bosses.spells.hedera.SpellEvolutionSeeds;
import com.playmonumenta.plugins.depths.bosses.spells.hedera.SpellHederaAnticheese;
import com.playmonumenta.plugins.depths.bosses.spells.hedera.SpellIvyGarden;
import com.playmonumenta.plugins.depths.bosses.spells.hedera.SpellLeafNova;
import com.playmonumenta.plugins.depths.bosses.spells.hedera.SpellPassiveGarden;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
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

public class Hedera extends SerializedLocationBossAbilityGroup {
	public static final String identityTag = "boss_hedera";
	public static final String DOOR_FILL_TAG = "Door"; // Used by Callicarpa for some reason
	public static final int detectionRange = 70;

	private static final int HEDERA_HEALTH = 4000;
	private static final int SWAP_TARGET_SECONDS = 15;
	private static final String PLANT_STAND_TAG = "Plant";
	private static final double PLANT_CONSUME_PERCENT = 0.05;
	public static final String MUSIC_TITLE = "epic:music.hedera";
	public static final int MUSIC_DURATION = 202; //seconds

	private final List<Location> mPlantSpawns = new ArrayList<>();
	private final Map<Location, LivingEntity> mPlants = new HashMap<>();
	private final Map<Location, String> mPlantTypes = new HashMap<>();

	private int mTimesHealed = 0;
	private final @Nullable DepthsParty mParty;

	public Hedera(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc);
		mBoss.setRemoveWhenFarAway(false);
		mBoss.addScoreboardTag("Boss");

		// Switch cooldownTicks depending on floor of party
		int cooldownTicks;
		mParty = DepthsUtils.getPartyFromNearbyPlayers(mSpawnLoc);
		int floor = mParty != null ? mParty.getFloor() : 1;
		if (floor == 1) {
			cooldownTicks = 8 * 20;
		} else if (floor == 4) {
			cooldownTicks = 6 * 20;
		} else if (floor % 3 == 1) {
			cooldownTicks = 5 * 20;
		} else {
			cooldownTicks = 8 * 20;
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
			new SpellLeafNova(plugin, mBoss, cooldownTicks),
			new SpellIvyGarden(plugin, cooldownTicks, mPlants),
			new SpellEvolutionSeeds(plugin, cooldownTicks, mPlants, mPlantTypes)
		));
		//Extra summon ability if fighting on f4 or higher
		if (floor != 1) {
			activeSpells = new SpellManager(Arrays.asList(
					//new SpellEarthshake(plugin, mBoss, 5, 80),
					new SpellLeafNova(plugin, mBoss, cooldownTicks),
					new SpellIvyGarden(plugin, cooldownTicks, mPlants),
					new SpellEvolutionSeeds(plugin, cooldownTicks, mPlants, mPlantTypes),
					new SpellEndlessHederaSummons(mBoss, cooldownTicks, ((floor - 1) / 3) + 1)
			));
		}

		List<Spell> passiveSpells = Arrays.asList(
			new SpellBlockBreak(mBoss, true, true),
			new SpellHederaAnticheese(mBoss, mSpawnLoc),
			new SpellPassiveGarden(mBoss, mPlantSpawns, mPlants, mPlantTypes, mSpawnLoc)
		);

		Map<Integer, BossHealthAction> events = new HashMap<>();
		BossBarManager bossBar = new BossBarManager(boss, detectionRange, BossBar.Color.RED, BossBar.Overlay.NOTCHED_10, events);
		super.constructBoss(activeSpells, passiveSpells, detectionRange, bossBar);
	}

	@Override
	public void init() {
		// Health is scaled by 1.15 times each time you fight the boss
		int modifiedHealth = (int) (HEDERA_HEALTH * Math.pow(1.15, mParty == null ? 0.0 : mParty.getFloor() / 3.0));
		EntityUtils.setMaxHealthAndHealth(mBoss, modifiedHealth);

		if (mParty != null) {
			mParty.playBossSong(MUSIC_TITLE, MUSIC_DURATION, mBoss);
		}

		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true);
		for (Player player : players) {
			MessagingUtils.sendBoldTitle(player, Component.text("Hedera", NamedTextColor.DARK_GRAY), Component.text("Venom of the Waves", NamedTextColor.GRAY));
			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 2, false, true, true));
			player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10, 0.7f);
		}
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		Location loc = mBoss.getLocation();
		List<Player> players = PlayerUtils.playersInRange(loc, detectionRange, true);

		BossUtils.endBossFightEffects(players);
		for (Player player : players) {
			player.sendMessage(Component.text("", NamedTextColor.DARK_GREEN)
				.append(Component.text("[Hedera]", NamedTextColor.GOLD))
				.append(Component.text(" No! No! This cannot be! The Broken Beyond must let me flee!")));
			PotionEffect poisonEffect = player.getPotionEffect(PotionEffectType.POISON);
			if (poisonEffect != null) {
				player.removePotionEffect(PotionEffectType.POISON);
			}
		}
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);

		// Remove the plant armor stands
		for (ArmorStand stand : mBoss.getWorld().getNearbyEntitiesByType(ArmorStand.class, loc, detectionRange)) {
			if (stand.getName().contains(PLANT_STAND_TAG)) {
				stand.remove();
			}
		}

		//Kill nearby mobs
		for (LivingEntity e : EntityUtils.getNearbyMobs(loc, detectionRange)) {
			e.damage(10000);
		}

		//Finish animation
		EntityUtils.fireworkAnimation(mBoss);
		DepthsManager.getInstance().bossDefeated(loc, detectionRange);
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
			// If not being damaged from a source OR If damage is a form of DoT, don't consume plants and don't take damage
			if (event.getSource() == null ||
				event.getType() == DamageEvent.DamageType.AILMENT || event.getType() == DamageEvent.DamageType.FIRE) {
				event.setDamage(1);
			} else {
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
}

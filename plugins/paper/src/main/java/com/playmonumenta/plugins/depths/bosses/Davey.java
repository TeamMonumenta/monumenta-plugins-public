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

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.bosses.spells.SpellAbyssalCharge;
import com.playmonumenta.plugins.depths.bosses.spells.SpellAbyssalLeap;
import com.playmonumenta.plugins.depths.bosses.spells.SpellAbyssalSpawnPassive;
import com.playmonumenta.plugins.depths.bosses.spells.SpellDaveyAnticheese;
import com.playmonumenta.plugins.depths.bosses.spells.SpellLinkBeyondLife;
import com.playmonumenta.plugins.depths.bosses.spells.SpellVoidBlast;
import com.playmonumenta.plugins.depths.bosses.spells.SpellVoidGrenades;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;

public class Davey extends BossAbilityGroup {
	public static final String identityTag = "boss_davey";
	public static final int detectionRange = 50;
	public static final String DOOR_FILL_TAG = "Door";
	public static final int DAVEY_HEALTH = 3250;
	public static final String VEX_LOS = "AbyssalSpawn";
	public static final int SWAP_TARGET_SECONDS = 15;

	private final Location mSpawnLoc;
	private final Location mEndLoc;

	//Two vexes Davey controls
	private List<LivingEntity> mVexes;

	public int mCooldownTicks;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new Davey(plugin, boss, spawnLoc, endLoc);
		});
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}

	public Davey(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss);
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;

		mBoss.setRemoveWhenFarAway(false);
		mBoss.addScoreboardTag("Boss");

		//Set/remove blocks
		if (spawnLoc.getBlock().getType() == Material.STONE_BUTTON) {
			spawnLoc.getBlock().setType(Material.AIR);
		}

		DepthsParty party = DepthsUtils.getPartyFromNearbyPlayers(mSpawnLoc);
		if (party == null || party.getFloor() == 2) {
			mCooldownTicks = 7 * 20;
		} else if (party.getFloor() == 5) {
			mCooldownTicks = 6 * 20;
		} else if (party.getFloor() % 3 == 2) {
			mCooldownTicks = 4 * 20;
		} else {
			mCooldownTicks = 7 * 20;
		}

		new BukkitRunnable() {
			Mob mDavey = (Mob) mBoss;
			@Override
			public void run() {
				if (!mBoss.isValid() || mBoss.isDead()) {
					this.cancel();
					return;
				}

				List<Player> players = PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true);
				if (players != null && players.size() > 0) {
					Collections.shuffle(players);
					mDavey.setTarget(players.get(0));
				}
			}
		}.runTaskTimer(mPlugin, 0, SWAP_TARGET_SECONDS * 20);

		Collection<ArmorStand> nearbyStands = mBoss.getWorld().getNearbyEntitiesByType(ArmorStand.class, mBoss.getLocation(), 50.0);
		for (ArmorStand stand : nearbyStands) {

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

		//Summon vexes
		mVexes = new ArrayList<>();
		mVexes.add((LivingEntity) LibraryOfSoulsIntegration.summon(spawnLoc.clone().add(2, 2, 2), VEX_LOS));
		mVexes.add((LivingEntity) LibraryOfSoulsIntegration.summon(spawnLoc.clone().add(-2, 2, -2), VEX_LOS));

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellLinkBeyondLife(mBoss, mCooldownTicks),
			new SpellVoidBlast(plugin, mVexes.get(0), mCooldownTicks / 2),
			new SpellVoidBlast(plugin, mVexes.get(1), mCooldownTicks / 2),
			new SpellAbyssalLeap(plugin, mBoss, mCooldownTicks),
			new SpellAbyssalCharge(mBoss, mCooldownTicks),
			new SpellVoidGrenades(mPlugin, mBoss, detectionRange, mCooldownTicks)
		));
		List<Spell> passiveSpells = Arrays.asList(
			new SpellBlockBreak(mBoss, 2, 3, 2),
			new SpellDaveyAnticheese(mBoss, mSpawnLoc),
			new SpellAbyssalSpawnPassive(mBoss, mVexes)
		);

		Map<Integer, BossHealthAction> events = new HashMap<Integer, BossHealthAction>();
		BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.RED, BarStyle.SEGMENTED_10, events);
		super.constructBoss(activeSpells, passiveSpells, detectionRange, bossBar);
	}

	@Override
	public void init() {
		int bossTargetHp = 0;
		int playerCount = BossUtils.getPlayersInRangeForHealthScaling(mBoss, detectionRange);
		int hpDelta = 512;
		int armor = (int)(Math.sqrt(playerCount * 2) - 1);
		while (playerCount > 0) {
			bossTargetHp = bossTargetHp + hpDelta;
			hpDelta = hpDelta / 2;
			playerCount--;
		}
		mBoss.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(armor);
		mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(DAVEY_HEALTH);
		mBoss.setHealth(DAVEY_HEALTH);

		DepthsParty party = DepthsUtils.getPartyFromNearbyPlayers(mSpawnLoc);
		if (party.getFloor() == 5) {
			//Set health higher
			mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(DAVEY_HEALTH * 1.5);
			mBoss.setHealth(DAVEY_HEALTH * 1.5);
		} else if (party.getFloor() % 3 == 2) {
			//Set health higher
			mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(DAVEY_HEALTH * 2.0);
			mBoss.setHealth(DAVEY_HEALTH * 2.0);
		}

		//launch event related spawn commands
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound epic:music.varcosa record @s ~ ~ ~ 2");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "effect give @s minecraft:blindness 2 2");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s title [\"\",{\"text\":\"Lieutenant Davey\",\"color\":\"dark_gray\",\"bold\":true}]");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s subtitle [\"\",{\"text\":\"Void Herald\",\"color\":\"gray\",\"bold\":true}]");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound minecraft:entity.wither.spawn master @s ~ ~ ~ 10 0.7");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "tellraw @s [\"\",{\"text\":\"[Davey]\", \"color\":\"gold\"},{\"text\":\" Ahoy! Ye have the stink of the Veil upon ye. She won't be likin' this... Sink!\",\"color\":\"blue\"}]");
	}

	@Override
	public void death(EntityDeathEvent event) {
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound minecraft:entity.enderdragon.death master @s ~ ~ ~ 100 0.8");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "tellraw @s [\"\",{\"text\":\"[Davey]\",\"color\":\"gold\"},{\"text\":\" Nay... I'll sink to ye, God of the Deep. I become a great part of ye ferever...\",\"color\":\"blue\"}]");
		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 2));
			player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 10, 2));
		}
		for (LivingEntity vex : mVexes) {
			if (vex != null && !vex.isDead()) {
				vex.remove();
			}
		}

		//Kill nearby mobs
		for (LivingEntity e : EntityUtils.getNearbyMobs(mBoss.getLocation(), 40.0)) {
			e.damage(10000);
		}

		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);

		DepthsUtils.animate(mBoss.getLocation());
		//Send players
		new BukkitRunnable() {

			@Override
			public void run() {
				PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "effect give @s minecraft:blindness 2 2");
				PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "stopsound @p");
			}

		}.runTaskLater(mPlugin, 60);

		new BukkitRunnable() {

			@Override
			public void run() {
				DepthsManager.getInstance().goToNextFloor(PlayerUtils.playersInRange(mBoss.getLocation(), 20.0, true).get(0));
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
}

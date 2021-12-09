package com.playmonumenta.plugins.bosses.bosses;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBossBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellPlayerAction;
import com.playmonumenta.plugins.bosses.spells.SpellPurgeNegatives;
import com.playmonumenta.plugins.bosses.spells.varcosamist.ForcefulGrip;
import com.playmonumenta.plugins.bosses.spells.varcosamist.SpellActions;
import com.playmonumenta.plugins.bosses.spells.varcosamist.SpellDeathlyCharge;
import com.playmonumenta.plugins.bosses.spells.varcosamist.SpellGhostlyCannons;
import com.playmonumenta.plugins.bosses.spells.varcosamist.SpellJibberJabber;
import com.playmonumenta.plugins.bosses.spells.varcosamist.SpellPurgeGlowing;
import com.playmonumenta.plugins.bosses.spells.varcosamist.SpellSummonConstantly;
import com.playmonumenta.plugins.bosses.spells.varcosamist.SpellSwitcheroo;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;

public final class VarcosaLingeringWillBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_varcosa_will";
	public static final int detectionRange = 50;
	private final Location mSpawnLoc;
	private final Location mEndLoc;
	private Location mCenter;
	private String[] mSpeak = {"The cold beyond be takin' me. It'll be takin' ye too...", "The veil be partin'... I won't go... not without me treasure..."};

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new VarcosaLingeringWillBoss(plugin, boss, spawnLoc, endLoc);
		});
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}

	public VarcosaLingeringWillBoss(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss);
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;
		mBoss.setRemoveWhenFarAway(false);

		List<String> mSummonableMobs = new ArrayList<>();
		mSummonableMobs.add("SeaWolf");
		mSummonableMobs.add("PirateGunner");
		mSummonableMobs.add("DrownedCrewman");

		for (LivingEntity e : mBoss.getLocation().getNearbyEntitiesByType(ArmorStand.class, detectionRange, detectionRange, detectionRange)) {
			if (e.getScoreboardTags().contains("varcosa_center")) {
				mCenter = e.getLocation();
				break;
			}
		}

		if (mCenter == null) {
			//This should be the same spot as the armor stand - but it is really bad to do things like this, and should only be the fallback
			mCenter = mSpawnLoc.clone().subtract(0, 0.5, 0);
		}

		SpellManager spells = new SpellManager(
				Arrays.asList(
						new SpellDeathlyCharge(mPlugin, mBoss, 20 * 15, "The Mist is callin' ye still. Let it in...!"),
						new SpellGhostlyCannons(mPlugin, mBoss, 22, mCenter, true, "Call down the cannons mateys, right quick!"),
						new SpellSwitcheroo(mPlugin, mBoss, 20 * 10, 30, "A trap be set now. Get ye into it!"),
						new ForcefulGrip(mPlugin, mBoss, 20 * 10, "Yarr, get ye over here! I'll deal with ye myself.") //change later lmao
				));

		//Passive Spells
		SpellPlayerAction action = SpellActions.getTooLowAction(mBoss, mCenter);

		SpellPlayerAction tooHighAction = SpellActions.getTooHighAction(mBoss, mCenter);

		BukkitRunnable runnable = SpellActions.getTeleportEntityRunnable(mBoss, mCenter);

		runnable.runTaskTimer(plugin, 20, 20 * 2);
		List<Spell> passiveSpells = Arrays.asList(
			new SpellSummonConstantly(mSummonableMobs, 20 * 16, 50, 4, 2, mCenter, this),
			new SpellJibberJabber(mBoss, mSpeak, detectionRange),
			new SpellPurgeNegatives(mBoss, 2),
			new SpellPurgeGlowing(mBoss, 20 * 15),
			new SpellBossBlockBreak(mBoss, 175, 1, 3, 1, true, true),
			action, tooHighAction
		);

		Map<Integer, BossHealthAction> events = new HashMap<>();
		events.put(10, mBoss -> forceCastSpell(SpellGhostlyCannons.class));
		BossBarManager bossBar = new BossBarManager(mPlugin, mBoss, detectionRange + 20, BarColor.RED, BarStyle.SEGMENTED_10, events);

		super.constructBoss(spells, passiveSpells, detectionRange, bossBar);
	}

	@Override
	public void bossDamagedEntity(EntityDamageByEntityEvent event) {
		//If we hit a player
		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			//Set all nearby mobs to target them
			for (LivingEntity mob : EntityUtils.getNearbyMobs(mBoss.getLocation(), detectionRange)) {
				if (mob instanceof Mob) {
					((Mob) mob).setTarget(player);
				}
			}
			//Let the players know something happened
			player.playSound(player.getLocation(), Sound.BLOCK_BELL_RESONATE, 0.3f, 0.9f);
			player.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, player.getLocation(), 25, 1.5, 1.5, 1.5);
		}

		if (event.getEntity() instanceof Player && event.getCause().equals(DamageCause.ENTITY_ATTACK)) {
			Player player = (Player) event.getEntity();
			if (player.isBlocking()) {
				player.setCooldown(Material.SHIELD, 20 * 6);
			}
		}
	}

	@Override
	public void death(EntityDeathEvent event) {
		String dio = "I feel it... partin'... the beyond calls... and I answer...";
		PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, 50, "tellraw @s [\"\",{\"text\":\"" + dio + "\",\"color\":\"red\"}]");
		new BukkitRunnable() {
			@Override
			public void run() {
				if (!PlayerUtils.playersInRange(mCenter, detectionRange, true).isEmpty()) {
					mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
				}
			}
		}.runTaskLater(mPlugin, 20 * 3);
	}

	@Override
	public void init() {
		mBoss.teleport(mSpawnLoc);
		int bossTargetHp = 0;
		int bossHpDelta = 1000;
		int playersInRange = BossUtils.getPlayersInRangeForHealthScaling(mBoss, detectionRange);
		while (playersInRange > 0) {
			bossTargetHp += bossHpDelta;
			bossHpDelta = (int) Math.floor(bossHpDelta / 1.8 + 100);
			playersInRange--;
		}

		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, bossTargetHp);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_FOLLOW_RANGE, detectionRange);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 1);
		mBoss.setHealth(bossTargetHp);

		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "effect give @s minecraft:blindness 2 2");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s title [\"\",{\"text\":\"Lingering Will\",\"color\":\"dark_red\",\"bold\":true}]");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound minecraft:entity.wither.spawn master @s ~ ~ ~ 10 0.7");

		summonArmorStandIfNoneAreThere(mCenter.clone().add(0, 0, 11.5));
		summonArmorStandIfNoneAreThere(mCenter.clone().add(0, 0, -11.5));
		summonArmorStandIfNoneAreThere(mCenter.clone().add(11.5, 0, 0));
		summonArmorStandIfNoneAreThere(mCenter.clone().add(-11.5, 0, 0));

		mBoss.setAI(true);

		new BukkitRunnable() {
			int mCount = 0;
			@Override
			public void run() {
				String[] dio = {"The light... it burns...", "That lantern be shinin' through the beyond..."};
				PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, 50, "tellraw @s [\"\",{\"text\":\"" + dio[mCount] + "\",\"color\":\"red\"}]");
				mCount++;
				if (mCount == dio.length) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 20 * 1, 20 * 2);
	}

	private void summonArmorStandIfNoneAreThere(Location loc) {
		if (loc.getNearbyEntitiesByType(ArmorStand.class, 2, 2, 2).isEmpty()) {
			ArmorStand as = (ArmorStand) mBoss.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
			as.setInvisible(true);
			as.setInvulnerable(true);
			as.setMarker(true);
			as.addScoreboardTag("summon_constantly_stand");
		}
	}
}

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
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellConditionalTeleport;
import com.playmonumenta.plugins.bosses.spells.SpellPlayerAction;
import com.playmonumenta.plugins.bosses.spells.SpellPurgeNegatives;
import com.playmonumenta.plugins.bosses.spells.varcosamist.SpellActions;
import com.playmonumenta.plugins.bosses.spells.varcosamist.SpellDeathlyCharge;
import com.playmonumenta.plugins.bosses.spells.varcosamist.SpellGhostlyCannons;
import com.playmonumenta.plugins.bosses.spells.varcosamist.SpellJibberJabber;
import com.playmonumenta.plugins.bosses.spells.varcosamist.SpellSummonConstantly;
import com.playmonumenta.plugins.bosses.spells.varcosamist.SpellSwitcheroo;
import com.playmonumenta.plugins.bosses.spells.varcosamist.SpellVarcosaHook;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;

public class VarcosasLastBreathBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_varcosa_breath";
	public static final int detectionRange = 50;
	private static String[] mSpeak = {"Arr, I be killin' ye myself then. I shan't be stopped twice...", "The air be growin' stale. I shan't let me end be this!"};
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mSpawnLoc;
	private final Location mEndLoc;
	private Location mCenter = null;
	private List<String> mSummonableMobs = new ArrayList<>();

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new VarcosasLastBreathBoss(plugin, boss, spawnLoc, endLoc);
		});
	}

	public VarcosasLastBreathBoss(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		mPlugin = plugin;
		mBoss = boss;
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;
		mBoss.setRemoveWhenFarAway(false);
		//Possible summons
		mSummonableMobs.add("SeaWolf");
		mSummonableMobs.add("PirateGunner");
		mSummonableMobs.add("DrownedCrewman");

		for (LivingEntity e : mBoss.getLocation().getNearbyEntitiesByType(ArmorStand.class, detectionRange, detectionRange, detectionRange)) {
			if (e.getScoreboardTags() != null && !e.getScoreboardTags().isEmpty() && e.getScoreboardTags().contains("varcosa_center")) {
				mCenter = e.getLocation();
				break;
			}
		}

		if (mCenter == null) {
			//This should be the same spot as the armor stand - but it is really bad to do things like this, and should only be the fallback
			mCenter = mSpawnLoc.clone().subtract(0, 0.5, 0);
		}

		//Active Spells
		SpellManager spells = new SpellManager(
				Arrays.asList(
						new SpellDeathlyCharge(mPlugin, mBoss, 20 * 20, "It be time. The beyond is callin' for ye, thief. The Mist will take ye to yer grave..."),
						new SpellGhostlyCannons(mPlugin, mBoss, 22, mCenter, false, "Call down the cannons mateys!"),
						new SpellSwitcheroo(mPlugin, mBoss, 20 * 16, 50, "A trap be set now. Get ye into it!"),
						new SpellVarcosaHook(mPlugin, mBoss, 20 * 10, "Yarr, get ye over here! I'll deal with ye myself.")
				));
		//Passive Spells
		SpellPlayerAction action = SpellActions.getTooLowAction(mBoss, mCenter);

		SpellPlayerAction tooHighAction = SpellActions.getTooHighAction(mBoss, mCenter);
		//Passives

		List<Spell> passiveSpells = Arrays.asList(
				new SpellSummonConstantly(mSummonableMobs, 20 * 16, 50, 2, 2, mCenter, mBoss, this),
				new SpellConditionalTeleport(mBoss, mSpawnLoc, b -> b.getLocation().getBlock().getType() == Material.WATER),
				new SpellJibberJabber(mBoss, mSpeak, detectionRange), new SpellPurgeNegatives(mBoss, 20 * 3), new SpellBlockBreak(mBoss),
				action, tooHighAction
				);

		BukkitRunnable runnable = SpellActions.getTeleportEntityRunnable(mBoss, mCenter);
		runnable.runTaskTimer(plugin, 20, 20 * 2);

		Map<Integer, BossHealthAction> events = new HashMap<Integer, BossHealthAction>();
		events.put(10, mBoss -> forceCastSpell(SpellGhostlyCannons.class));
		BossBarManager bossBar = new BossBarManager(mPlugin, mBoss, detectionRange + 20, BarColor.RED, BarStyle.SEGMENTED_10, events);

		super.constructBoss(plugin, identityTag, boss, spells, passiveSpells, detectionRange, bossBar);
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
            player.playSound(player.getLocation(), Sound.BLOCK_BELL_RESONATE, SoundCategory.HOSTILE, 0.3f, 0.9f);
            player.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, player.getLocation(), 25, 1.5, 1.5, 1.5);
        }
	}

	@Override
	public void init() {
		mBoss.teleport(mCenter.clone().add(0, 1, 0));
		int bossTargetHp = 0;
		int bossHpDelta = 1400;
		int playersInRange = BossUtils.getPlayersInRangeForHealthScaling(mBoss, detectionRange);
		int armor = (int)(Math.sqrt(playersInRange * 2) - 1);
		while (playersInRange > 0) {
			bossTargetHp += bossHpDelta;
			bossHpDelta = (int)Math.floor(bossHpDelta / 1.5 + 100);
			playersInRange--;
		}

		mBoss.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(armor);
		mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(bossTargetHp);
		mBoss.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(detectionRange);
		mBoss.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1);
		mBoss.setHealth(bossTargetHp);

		summonArmorStandIfNoneAreThere(mCenter.clone().add(0, 0, 11.5));
		summonArmorStandIfNoneAreThere(mCenter.clone().add(0, 0, -11.5));
		summonArmorStandIfNoneAreThere(mCenter.clone().add(11.5, 0, 0));
		summonArmorStandIfNoneAreThere(mCenter.clone().add(-11.5, 0, 0));

		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "effect give @s minecraft:blindness 2 2");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s title [\"\",{\"text\":\"Varcosa's\",\"color\":\"red\",\"bold\":true}]");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s subtitle [\"\",{\"text\":\"Last Breath\",\"color\":\"dark_red\",\"bold\":true}]");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound minecraft:entity.wither.spawn master @s ~ ~ ~ 10 0.7");
		mBoss.setAI(true);
	}

	@Override
	public void death(EntityDeathEvent event) {
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange);

		if (players.size() <= 0) {
			return;
		}

		changePhase(null, null, null);
		String dio = "Yarr... why be this hurtin’? I shan’t go!";
		PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"" + dio + "\",\"color\":\"red\"}]");

		new BukkitRunnable() {
			@Override
			public void run() {
				mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
			}
		}.runTaskLater(mPlugin, 20 * 1);

	}

	private void summonArmorStandIfNoneAreThere(Location loc) {
		if (loc.getNearbyEntitiesByType(ArmorStand.class, 2, 2, 2).isEmpty()) {
			EntityUtils.summonEntityAt(loc, EntityType.ARMOR_STAND, "{Pose:{},Marker:1b,Tags:[\"summon_constantly_stand\"],Invisible:1b,Invulnerable:1b}");
		}
	}
}

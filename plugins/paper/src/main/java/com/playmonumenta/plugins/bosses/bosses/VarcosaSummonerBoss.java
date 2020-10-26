package com.playmonumenta.plugins.bosses.bosses;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellPlayerAction;
import com.playmonumenta.plugins.bosses.spells.SpellPurgeNegatives;
import com.playmonumenta.plugins.bosses.spells.varcosamist.SpellActions;
import com.playmonumenta.plugins.bosses.spells.varcosamist.SpellJibberJabber;
import com.playmonumenta.plugins.bosses.spells.varcosamist.SpellSummonConstantly;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;


public class VarcosaSummonerBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_varcosa_summoner";
	public static final int detectionRange = 50;
	public static int mSummonPeriod = 20 * 5;
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private List<String> mSummonableMobs = new ArrayList<>();
	private String[] mSpeak = new String[3];
	private final int mRadius = 50;
	private Location mCenter = null;
	private boolean mActive;
	private Location mEndLoc;
	private Location mSpawnLoc;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new VarcosaSummonerBoss(plugin, boss, spawnLoc, endLoc);
		});
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}

	public VarcosaSummonerBoss(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		mPlugin = plugin;
		mBoss = boss;
		mEndLoc = endLoc;
		mSpawnLoc = spawnLoc;

		mSummonableMobs.add("SeaWolf");
		mSummonableMobs.add("PirateGunner");
		mSummonableMobs.add("DrownedCrewman");

		mSpeak[0] = "Yarr! It be you again? This shan't end the way it did before, matey!";
		mSpeak[1] = "Ye took me treasure, ye stole me fleece. Now I be takin' ye to the beyond!";
		mSpeak[2] = "Yarr! Me ghostly crew rides forth!";

		for (LivingEntity e : mBoss.getLocation().getNearbyEntitiesByType(ArmorStand.class, mRadius, mRadius, mRadius)) {
			if (e.getScoreboardTags() != null && !e.getScoreboardTags().isEmpty() && e.getScoreboardTags().contains("varcosa_center")) {
				mCenter = e.getLocation();
				break;
			}
		}

		if (mCenter == null) {
			mCenter = mSpawnLoc.clone().subtract(0, 0.5, 0);
		}

		SpellPlayerAction action = SpellActions.getTooLowAction(mBoss, mCenter);

		SpellPlayerAction tooHighAction = SpellActions.getTooHighAction(mBoss, mCenter);

		BukkitRunnable runnable = SpellActions.getTeleportEntityRunnable(mBoss, mCenter);
		runnable.runTaskTimer(plugin, 20, 20 * 2);

		summonArmorStandIfNoneAreThere(mCenter.clone().add(0, 0, 11.5));
		summonArmorStandIfNoneAreThere(mCenter.clone().add(0, 0, -11.5));
		summonArmorStandIfNoneAreThere(mCenter.clone().add(11.5, 0, 0));
		summonArmorStandIfNoneAreThere(mCenter.clone().add(-11.5, 0, 0));

		List<Spell> passiveSpells = Arrays.asList(new SpellSummonConstantly(mSummonableMobs, mSummonPeriod, 50, 5, 2, mCenter, mBoss, this), new SpellJibberJabber(mBoss, mSpeak, mRadius), action, tooHighAction);
		SpellManager activeSpells = new SpellManager(Arrays.asList(new SpellPurgeNegatives(mBoss, 100)));

		Map<Integer, BossHealthAction> events = new HashMap<Integer, BossHealthAction>();

		BossBarManager bossBar = new BossBarManager(mPlugin, mBoss, detectionRange + 20, BarColor.RED, BarStyle.SEGMENTED_10, events);

		super.constructBoss(plugin, identityTag, boss, activeSpells, passiveSpells, detectionRange, bossBar);
	}

	@Override
	public void death(EntityDeathEvent event) {
		mActive = false;
		changePhase(null, null, null);

		String dio = "Fine! Ye make me do this meself!";
		PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"" + dio + "\",\"color\":\"red\"}]");

		new BukkitRunnable() {
			@Override
			public void run() {
				if (!PlayerUtils.playersInRange(mCenter, detectionRange, true).isEmpty()) {
					mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
				}
			}
		}.runTaskLater(mPlugin, 20 * 1);

	}

	@Override
	public void init() {
		int bossTargetHp = 0;
		int hpDelta = 650;
		int playerCount = BossUtils.getPlayersInRangeForHealthScaling(mBoss, detectionRange);
		while (playerCount > 0) {
			bossTargetHp = bossTargetHp + hpDelta;
			hpDelta = hpDelta / 2 + 75;
			playerCount--;
		}

		mBoss.setInvulnerable(true);
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 99, 9999));
		mBoss.setAI(false);

		mBoss.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(0);
		mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(bossTargetHp);
		mBoss.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(detectionRange);
		mBoss.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1);

		mBoss.setHealth(bossTargetHp);

		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s title [\"\",{\"text\":\"Varcosa\",\"color\":\"red\",\"bold\":true}]");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s subtitle [\"\",{\"text\":\"Mighty Pirate Captain\",\"color\":\"dark_red\",\"bold\":true}]");
		mActive = true;
	}

	public void onSummonKilled() {
		if (mActive) {
			mBoss.setHealth(mBoss.getHealth() - 20 < 0 ? 0 : mBoss.getHealth() - 20);
		}
	}

	private void summonArmorStandIfNoneAreThere(Location loc) {
		if (loc.getNearbyEntitiesByType(ArmorStand.class, 2, 2, 2).isEmpty()) {
			EntityUtils.summonEntityAt(loc, EntityType.ARMOR_STAND, "{Pose:{},Marker:1b,Tags:[\"summon_constantly_stand\"],Invisible:1b,Invulnerable:1b}");
		}
	}
}

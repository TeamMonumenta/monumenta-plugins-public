package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellBombToss;
import com.playmonumenta.plugins.bosses.spells.oldslabsbos.SpellBash;
import com.playmonumenta.plugins.bosses.spells.oldslabsbos.SpellWhirlwind;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class OldLabsBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_oldlabs";
	public static final int detectionRange = 32;

	private final Location mSpawnLoc;
	private final Location mEndLoc;

	private String[] mDio = new String[] {
		"Well, this is very peculiar...",
		"The rats causing such a ruckus down here are mere commoners? How feeble are those bandits?",
		"Now as a noble, I'm supposed to take pity on you. Where's the fun in that when I can cut you down instead?",
		"Your intrusion on my plans ends here! Have at ye, commoners!"
	};

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new OldLabsBoss(plugin, boss, spawnLoc, endLoc, false);
		});
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}

	public OldLabsBoss(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		this(plugin, boss, spawnLoc, endLoc, true);
	}

	public OldLabsBoss(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc, boolean newBoss) {
		super(plugin, identityTag, boss);
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;

		mBoss.setRemoveWhenFarAway(false);
		mBoss.addScoreboardTag("Boss");

		if (!newBoss) {
			resumeBossFight(plugin, boss);
		} else {
			mBoss.setGravity(false);
			mBoss.setInvulnerable(true);
			mBoss.setAI(false);
			Location tempLoc = mSpawnLoc.clone();
			tempLoc.setY(300);
			mBoss.teleport(tempLoc);

			PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "playsound minecraft:entity.witch.ambient master @s ~ ~ ~ 10 0.6");
			new BukkitRunnable() {
				int mIdx = 0;

				@Override
				public void run() {
					String line = mDio[mIdx];

					if (mIdx < 3) {
						PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[???] \",\"color\":\"gold\"},{\"text\":\"" + line + "\",\"color\":\"white\"}]");
						mIdx += 1;
					} else {
						this.cancel();
						Location loc = mSpawnLoc.clone().add(0, 1, 0);
						PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Elcard the Ignoble] \",\"color\":\"gold\"},{\"text\":\"" + line + "\",\"color\":\"white\"}]");

						mBoss.teleport(mSpawnLoc);
						mBoss.setAI(true);
						mBoss.setInvulnerable(false);
						mBoss.setGravity(true);

						new PartialParticle(Particle.CLOUD, loc, 10, 0.2, 0.45, 0.2, 0.125).spawnAsEntityActive(boss);
						new PartialParticle(Particle.SMOKE_NORMAL, loc, 75, 0.2, 0.45, 0.2, 0.2).spawnAsEntityActive(boss);
						new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 35, 0.2, 0.45, 0.2, 0.15).spawnAsEntityActive(boss);
						for (Player player : PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true)) {
							MessagingUtils.sendBoldTitle(player, ChatColor.GOLD + "Elcard", ChatColor.RED + "The Ignoble");
							player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 10, 1.65f);
							player.playSound(player.getLocation(), Sound.ENTITY_WITCH_AMBIENT, 10, 0.6f);
						}

						resumeBossFight(plugin, boss);
					}
				}

			}.runTaskTimer(plugin, 0, 20 * 5);
		}
	}

	/* This is called either when the boss chunk loads OR when he is first created */
	private void resumeBossFight(Plugin plugin, LivingEntity boss) {
		SpellManager phase1Spells = new SpellManager(Arrays.asList(
			new SpellBombToss(plugin, mBoss, 20, 2, 1, 100, false, true),
			new SpellBash(plugin, mBoss)
		));

		SpellManager phase2Spells = new SpellManager(Arrays.asList(
			new SpellWhirlwind(plugin, mBoss),
			new SpellBash(plugin, mBoss)
		));

		Map<Integer, BossHealthAction> events = new HashMap<Integer, BossHealthAction>();
		events.put(75, mBoss -> {
			PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Elcard the Ignoble] \",\"color\":\"gold\"},{\"text\":\"Do not interfere with my affairs! I will see that crown-head fall and assert myself as King!\",\"color\":\"white\"}]");
		});
		events.put(50, mBoss -> {
			changePhase(phase2Spells, Collections.emptyList(), null);
			PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Elcard the Ignoble] \",\"color\":\"gold\"},{\"text\":\"Agh! You think you're so strong? Let me show you true swordsmanship!\",\"color\":\"white\"}]");
		});

		events.put(35, mBoss -> {
			PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Elcard the Ignoble] \",\"color\":\"gold\"},{\"text\":\"Even if you stop this, city leeches like you will never step foot outside of Sierhaven! Where you do you think you'll go? Back to the slums!?\",\"color\":\"white\"}]");
		});

		events.put(20, mBoss -> {
			PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Elcard the Ignoble] \",\"color\":\"gold\"},{\"text\":\"Ugh, looks like I might need help from those bandits after all...\",\"color\":\"white\"}]");
			Location spawnLoc = mSpawnLoc.clone().add(-1, -1, 13);
			try {
				new PartialParticle(Particle.SMOKE_LARGE, spawnLoc, 15, 0.2, 0.45, 0.2, 0.2).spawnAsEntityActive(boss);
				Entity mob = LibraryOfSoulsIntegration.summon(spawnLoc, "RebelSoldier");
				if (mob instanceof LivingEntity) {
					((LivingEntity) mob).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 2, 4));
				}

				spawnLoc = spawnLoc.add(2, 0, 0);
				new PartialParticle(Particle.SMOKE_LARGE, spawnLoc, 15, 0.2, 0.45, 0.2, 0.2).spawnAsEntityActive(boss);
				mob = LibraryOfSoulsIntegration.summon(spawnLoc, "RebelSlinger");
				if (mob instanceof LivingEntity) {
					((LivingEntity) mob).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 2, 4));
				}
			} catch (Exception ex) {
				mPlugin.getLogger().warning("Failed to spawn labs boss summons");
				ex.printStackTrace();
			}
		});

		BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.BLUE, BarStyle.SEGMENTED_10, events);
		constructBoss(phase1Spells, Collections.emptyList(), detectionRange, bossBar);
	}

	@Override
	public void death(EntityDeathEvent event) {
		PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "playsound minecraft:entity.wither.death master @s ~ ~ ~ 100 0.8");
		PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Elcard The Ignoble] \",\"color\":\"gold\"},{\"text\":\"You are no commoner... Who... Are you...?\",\"color\":\"white\"}]");
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
	}

	@Override
	public void init() {
		int bossTargetHp = 0;
		int playerCount = BossUtils.getPlayersInRangeForHealthScaling(mSpawnLoc, detectionRange);
		int hpDelta = 160;
		while (playerCount > 0) {
			bossTargetHp = bossTargetHp + hpDelta;
			hpDelta = hpDelta / 2;
			playerCount--;
		}
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, bossTargetHp);
		mBoss.setHealth(bossTargetHp);
	}
}

package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellConditionalTeleport;
import com.playmonumenta.plugins.bosses.spells.urik.SpellTwistedShot;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;

public class Urik extends BossAbilityGroup {
	public static final String identityTag = "boss_urik";
	public static final int detectionRange = 110;

	private final LivingEntity mBoss;
	private final Location mSpawnLoc;
	private final Location mEndLoc;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new CAxtal(plugin, boss, spawnLoc, endLoc);
		});
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}

	public Urik(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		mBoss = boss;
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;
		mBoss.setRemoveWhenFarAway(false);

		mBoss.addScoreboardTag("Boss");

		SpellManager activeSpells = new SpellManager(Arrays.asList(
				//new SpellSpiralVolley(plugin, mBoss, 20*10, 20*6, 20*2,50),
				//new SpellPiercingRain(plugin, mBoss, 20*2, 20*10, 50),
				new SpellTwistedShot(plugin, mBoss, 20 * 8, 20*2, 20*1, 50)
				//new SpellGroundingShot(plugin, mBoss, 20*2, 50)
		));
		List<Spell> passiveSpells = Arrays.asList(
			//new SpellBlockBreak(mBoss),
			// Teleport the boss to spawnLoc if he gets too far away from where he spawned
			new SpellConditionalTeleport(mBoss, spawnLoc, b -> spawnLoc.distance(b.getLocation()) > 40),
			// Teleport the boss to spawnLoc if he is stuck in bedrock
			new SpellConditionalTeleport(mBoss, spawnLoc, b -> b.getLocation().getBlock().getType() == Material.BEDROCK ||
			                                                   b.getLocation().add(0, 1, 0).getBlock().getType() == Material.BEDROCK ||
			                                                   b.getLocation().getBlock().getType() == Material.LAVA)
		);

		Map<Integer, BossHealthAction> events = new HashMap<Integer, BossHealthAction>();
		events.put(100, mBoss -> {
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"i am mega scout fear me.\",\"color\":\"dark_red\"}]");
		});
		events.put(50, (mBoss) -> {
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"pew pew mega scout.\",\"color\":\"dark_red\"}]");
		});
		events.put(25, (mBoss) -> {
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"brrrrrr\",\"color\":\"dark_red\"}]");
		});
		events.put(10, (mBoss) -> {
			PlayerUtils.executeCommandOnNearbyPlayers(spawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"BRRRRRRRRRRRRRRRRR\",\"color\":\"dark_green\"}]");
		});
		BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.BLUE, BarStyle.SEGMENTED_6, events);

		super.constructBoss(plugin, identityTag, mBoss, activeSpells, passiveSpells, detectionRange, bossBar);
	}

	@Override
	public void init() {
		int bossTargetHp = 0;
		int playerCount = BossUtils.getPlayersInRangeForHealthScaling(mBoss, detectionRange);
		int hpDelta = 1024;
		int armor = (int)(Math.sqrt(playerCount * 2) - 1);
		while (playerCount > 0) {
			bossTargetHp = bossTargetHp + hpDelta;
			hpDelta = hpDelta / 2;
			playerCount--;
		}
		mBoss.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(armor);
		mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(bossTargetHp);
		mBoss.setHealth(bossTargetHp);

		//launch event related spawn commands
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "effect give @s minecraft:blindness 2 2");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s title [\"\",{\"text\":\"Urik\",\"color\":\"blue\",\"bold\":true}]");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s subtitle [\"\",{\"text\":\"Forgotten Hunter\",\"color\":\"aqua\",\"bold\":true}]");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound minecraft:entity.wither.spawn master @s ~ ~ ~ 10 0.7");
	}

	@Override
	public void death(EntityDeathEvent event) {
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound minecraft:entity.enderdragon.death master @s ~ ~ ~ 100 0.8");
		PlayerUtils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "tellraw @s [\"\",{\"text\":\"It ends at last... Is this what freedom feels like?..\",\"color\":\"dark_red\"}]");
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
	}
}

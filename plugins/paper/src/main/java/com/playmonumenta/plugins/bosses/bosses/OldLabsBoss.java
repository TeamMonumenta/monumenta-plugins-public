package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellBombToss;
import com.playmonumenta.plugins.bosses.spells.spells_oldslabsbos.SpellBash;
import com.playmonumenta.plugins.bosses.spells.spells_oldslabsbos.SpellWhirlwind;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SerializationUtils;

public class OldLabsBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_oldlabs";
	public static final int detectionRange = 32;

	private final LivingEntity mBoss;
	private final Location mSpawnLoc;
	private final Location mEndLoc;

	private String[] dio = new String[] {
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

		mBoss = boss;
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;

		if (!newBoss) {
			resumeBossFight(plugin, boss);
		} else {
			mBoss.setRemoveWhenFarAway(false);

			mBoss.setGravity(false);
			mBoss.setInvulnerable(true);
			mBoss.setAI(false);
			Location tempLoc = mSpawnLoc.clone();
			tempLoc.setY(300);
			mBoss.teleport(tempLoc);

			PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "playsound minecraft:entity.witch.ambient master @s ~ ~ ~ 10 0.6");
			new BukkitRunnable() {
				int index = 0;
				@Override
				public void run() {
					String line = dio[index];

					if (index < 3) {
						PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[???] \",\"color\":\"gold\"},{\"text\":\"" + line + "\",\"color\":\"white\"}]");
						index += 1;
					} else {
						this.cancel();
						Location loc = mSpawnLoc.clone().add(0, 1, 0);
						PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Elcard the Ignoble] \",\"color\":\"gold\"},{\"text\":\"" + line + "\",\"color\":\"white\"}]");

						mBoss.teleport(mSpawnLoc);
						mBoss.setAI(true);
						mBoss.setInvulnerable(false);
						mBoss.setGravity(true);

						mBoss.getWorld().spawnParticle(Particle.CLOUD, loc, 10, 0.2, 0.45, 0.2, 0.125);
						mBoss.getWorld().spawnParticle(Particle.SMOKE_NORMAL, loc, 75, 0.2, 0.45, 0.2, 0.2);
						mBoss.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, loc, 35, 0.2, 0.45, 0.2, 0.15);
						PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "title @s title [\"\",{\"text\":\"Elcard\",\"color\":\"gold\",\"bold\":true}]");
						PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "title @s subtitle [\"\",{\"text\":\"The Ignoble\",\"color\":\"red\",\"bold\":true}]");
						PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "playsound minecraft:entity.blaze.shoot master @s ~ ~ ~ 10 1.65");
						PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "playsound minecraft:entity.witch.ambient master @s ~ ~ ~ 10 0.6");

						resumeBossFight(plugin, boss);
					}
				}

			}.runTaskTimer(plugin, 0, 20 * 5);
		}
	}

	/* This is called either when the boss chunk loads OR when he is first created */
	private void resumeBossFight(Plugin plugin, LivingEntity boss) {
		SpellManager phase1Spells = new SpellManager(Arrays.asList(
				new SpellBombToss(plugin, mBoss, 20, 2, 1, 100),
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
			changePhase(phase2Spells, null, null);
			PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Elcard the Ignoble] \",\"color\":\"gold\"},{\"text\":\"Agh! You think you're so strong? Let me show you true swordsmanship!\",\"color\":\"white\"}]");
		});

		events.put(35, mBoss -> {
			PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Elcard the Ignoble] \",\"color\":\"gold\"},{\"text\":\"Even if you stop this, city leeches like you will never step foot outside of Sierhaven! Where you do you think you'll go? Back to the slums!?\",\"color\":\"white\"}]");
		});

		events.put(20, mBoss -> {
			PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Elcard the Ignoble] \",\"color\":\"gold\"},{\"text\":\"Ugh, looks like I might need help from those bandits after all...\",\"color\":\"white\"}]");
			Location spawnLoc = mSpawnLoc.clone().add(-1, -1, 13);
			try {
				spawnLoc.getWorld().spawnParticle(Particle.SMOKE_LARGE, spawnLoc, 15, 0.2, 0.45, 0.2, 0.2);
				Entity mob = EntityUtils.summonEntityAt(spawnLoc, EntityType.ZOMBIE, "{CustomName:\"[{\\\"text\\\":\\\"Rebel Grunt\\\"}]\",ArmorItems:[{id:\"minecraft:leather_boots\",Count:1b,tag:{display:{color:3290681,Name:\"{\\\"text\\\":\\\"§fLeather Boots\\\"}\"},AttributeModifiers:[{UUIDMost:8547382388562740130L,UUIDLeast:-5611961115798238564L,Amount:0.0d,Slot:\"feet\",AttributeName:\"generic.armor\",Operation:0,Name:\"Modifier\"}]}},{id:\"minecraft:leather_leggings\",Count:1b,tag:{display:{color:4673362,Name:\"{\\\"text\\\":\\\"§fLeather Pants\\\"}\"},AttributeModifiers:[{UUIDMost:-4153258568067363572L,UUIDLeast:-8303070907437016335L,Amount:0.0d,Slot:\"legs\",AttributeName:\"generic.armor\",Operation:0,Name:\"Modifier\"}]}},{id:\"minecraft:leather_chestplate\",Count:1b,tag:{display:{color:6825251,Name:\"{\\\"text\\\":\\\"§fLeather Tunic\\\"}\"},AttributeModifiers:[{UUIDMost:4241443962897256113L,UUIDLeast:-6496831972651984062L,Amount:0.0d,Slot:\"chest\",AttributeName:\"generic.armor\",Operation:0,Name:\"Modifier\"}]}},{id:\"minecraft:player_head\",Count:1b,tag:{SkullOwner:{Id:\"56a75ceb-3e56-4e35-9921-22cd0fd80ad1\",Properties:{textures:[{Value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWRmMzU4NjlhMDcwZjE3YzEzZmU1YjgxYjlkODVjNjgzM2FjNmFiOTdiZjFkZjNjOGViZjY4YmZhNzM3YzQifX19\"}]}},display:{Name:\"{\\\"text\\\":\\\"Bandit\\\"}\"}}}],HandItems:[{id:\"minecraft:wooden_sword\",Count:1b,tag:{AttributeModifiers:[{UUIDMost:-4964101247198805690L,UUIDLeast:-8453403592897431934L,Amount:1.0d,Slot:\"mainhand\",AttributeName:\"generic.attackDamage\",Operation:0,Name:\"Modifier\"}]}},{}]}");
				if (mob instanceof LivingEntity) {
					((LivingEntity)mob).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 2, 4));
				}

				spawnLoc = spawnLoc.add(2, 0, 0);
				spawnLoc.getWorld().spawnParticle(Particle.SMOKE_LARGE, spawnLoc, 15, 0.2, 0.45, 0.2, 0.2);
				mob = EntityUtils.summonEntityAt(spawnLoc, EntityType.SKELETON, "{CustomName:\"{\\\"text\\\":\\\"Rebel Archer\\\"}\",Health:15.0f,ArmorItems:[{id:\"minecraft:leather_boots\",Count:1b,tag:{display:{color:3290681,Name:\"{\\\"text\\\":\\\"§fLeather Boots\\\"}\"},AttributeModifiers:[{UUIDMost:8547382388562740130L,UUIDLeast:-5611961115798238564L,Amount:0.0d,Slot:\"feet\",AttributeName:\"generic.armor\",Operation:0,Name:\"Modifier\"}]}},{id:\"minecraft:leather_leggings\",Count:1b,tag:{display:{color:4673362,Name:\"{\\\"text\\\":\\\"§fLeather Pants\\\"}\"},AttributeModifiers:[{UUIDMost:-4153258568067363572L,UUIDLeast:-8303070907437016335L,Amount:0.0d,Slot:\"legs\",AttributeName:\"generic.armor\",Operation:0,Name:\"Modifier\"}]}},{id:\"minecraft:leather_chestplate\",Count:1b,tag:{display:{color:6825251,Name:\"{\\\"text\\\":\\\"§fLeather Tunic\\\"}\"},AttributeModifiers:[{UUIDMost:4241443962897256113L,UUIDLeast:-6496831972651984062L,Amount:0.0d,Slot:\"chest\",AttributeName:\"generic.armor\",Operation:0,Name:\"Modifier\"},{UUIDMost:-463034444199736113L,UUIDLeast:-8777613331715170401L,Amount:-0.08d,Slot:\"chest\",AttributeName:\"generic.movementSpeed\",Operation:1,Name:\"Modifier\"}]}},{id:\"minecraft:player_head\",Count:1b,tag:{SkullOwner:{Id:\"56a75ceb-3e56-4e35-9921-22cd0fd80ad1\",Properties:{textures:[{Value:\"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWRmMzU4NjlhMDcwZjE3YzEzZmU1YjgxYjlkODVjNjgzM2FjNmFiOTdiZjFkZjNjOGViZjY4YmZhNzM3YzQifX19\"}]}},display:{Name:\"{\\\"text\\\":\\\"Bandit\\\"}\"}}}],Attributes:[{Base:15.0d,Name:\"generic.maxHealth\"}],HandItems:[{id:\"minecraft:bow\",Count:1b},{}]}");
				if (mob instanceof LivingEntity) {
					((LivingEntity)mob).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 2, 4));
				}
			} catch (Exception ex) {
				mPlugin.getLogger().warning("Failed to spawn labs boss summons");
				ex.printStackTrace();
			}
		});

		BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.BLUE, BarStyle.SEGMENTED_10, events);
		constructBoss(plugin, identityTag, mBoss, phase1Spells, null, detectionRange, bossBar);
	}

	@Override
	public void death() {
		PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "playsound minecraft:entity.wither.death master @s ~ ~ ~ 100 0.8");
		PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Elcard The Ignoble] \",\"color\":\"gold\"},{\"text\":\"You are no commoner... Who... Are you...?\",\"color\":\"white\"}]");
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
	}

	@Override
	public void init() {
		int bossTargetHp = 0;
		int player_count = PlayerUtils.playersInRange(mSpawnLoc, detectionRange).size();
		int hp_del = 160;
		int armor = (int)(Math.sqrt(player_count * 2) - 1);
		while (player_count > 0) {
			bossTargetHp = bossTargetHp + hp_del;
			hp_del = hp_del / 2;
			player_count--;
		}
		mBoss.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(armor);
		mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(bossTargetHp);
		mBoss.setHealth(bossTargetHp);
	}
}

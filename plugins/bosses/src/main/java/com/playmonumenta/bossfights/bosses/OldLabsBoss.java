package com.playmonumenta.bossfights.bosses;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.bossfights.BossBarManager;
import com.playmonumenta.bossfights.BossBarManager.BossHealthAction;
import com.playmonumenta.bossfights.SpellManager;
import com.playmonumenta.bossfights.spells.SpellBombToss;
import com.playmonumenta.bossfights.spells.spells_oldslabsbos.SpellBash;
import com.playmonumenta.bossfights.spells.spells_oldslabsbos.SpellDashAttack;
import com.playmonumenta.bossfights.spells.spells_oldslabsbos.SpellWhirlwind;
import com.playmonumenta.bossfights.utils.SerializationUtils;
import com.playmonumenta.bossfights.utils.Utils;

public class OldLabsBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_oldlabs";
	public static final int detectionRange = 32;

	private final LivingEntity mBoss;
	private final Location mSpawnLoc;
	private final Location mEndLoc;

	private EntityEquipment equips;
	private ItemStack[] armorc;
	private ItemStack m;
	private ItemStack o;
	private String[] dio = new String[] {
		"Well, this is very peculiar...",
		"The rats that are causing such a ruckus down here are mere commoners? How feeble are those Bandits?",
		"Now as a noble, I'm supposed to take pity on you, but where's the fun in that when I can cut you instead?",
		"Your intrusion on my plans ends here! Have at ye, commoners!"
	};

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new OldLabsBoss(plugin, boss, spawnLoc, endLoc);
		});
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}

	public OldLabsBoss(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		mBoss = boss;
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;
		equips = mBoss.getEquipment();
		armorc = equips.getArmorContents();
		m = equips.getItemInMainHand();
		o = equips.getItemInOffHand();

		mBoss.getEquipment().clear();
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 999, 0));
		mBoss.setAI(false);
		mBoss.setInvulnerable(true);
		mBoss.setRemoveWhenFarAway(false);

		SpellManager phase1Spells = new SpellManager(Arrays.asList(
				new SpellBombToss(mPlugin, mBoss, 20, 3, 2),
				new SpellBash(mPlugin, mBoss)
			));

		SpellManager phase2Spells = new SpellManager(Arrays.asList(
				new SpellDashAttack(mPlugin, mBoss, 24, 6),
				new SpellWhirlwind(mPlugin, mBoss)
			));

		Map<Integer, BossHealthAction> events = new HashMap<Integer, BossHealthAction>();
		events.put(75, mBoss -> {
			Utils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "tellraw @s [\"\",{\"text\":\"[Elcard the Ignoble] \",\"color\":\"gold\"},{\"text\":\"Do not interfere in my affairs! I will see that crown-head fall and assert myself as King!\",\"color\":\"white\"}]");
		});
		events.put(50, mBoss -> {
			changePhase(phase2Spells, null, null);
			Utils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "tellraw @s [\"\",{\"text\":\"[Elcard the Ignoble] \",\"color\":\"gold\"},{\"text\":\"Agh! You think you're so strong? Let me show you true swordsmanship!\",\"color\":\"white\"}]");
		});

		events.put(25, mBoss -> {
			Utils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "tellraw @s [\"\",{\"text\":\"[Elcard the Ignoble] \",\"color\":\"gold\"},{\"text\":\"Even if you stop this, city leeches like you will never step foot outside of Sierhaven! Where you do you think you'll go? Back to the slums!? \",\"color\":\"white\"}]");
		});

		Utils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound minecraft:entity.witch.ambient master @s ~ ~ ~ 10 0.6");
		new BukkitRunnable() {
			int index = 0;
			@Override
			public void run() {
				String line = dio[index];

				if (index < 3) {
					Utils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "tellraw @s [\"\",{\"text\":\"[???] \",\"color\":\"gold\"},{\"text\":\"" + line + "\",\"color\":\"white\"}]");
				} else {
					this.cancel();
					Location loc = mBoss.getLocation().add(0, 1, 0);
					Utils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "tellraw @s [\"\",{\"text\":\"[Elcard the Ignoble] \",\"color\":\"gold\"},{\"text\":\"" + line + "\",\"color\":\"white\"}]");
					BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.BLUE, BarStyle.SEGMENTED_10, events);

					constructBoss(plugin, identityTag, mBoss, phase1Spells, null, detectionRange, bossBar);
					mBoss.getEquipment().setArmorContents(armorc);
					mBoss.getEquipment().setItemInMainHand(m);
					mBoss.getEquipment().setItemInOffHand(o);
					mBoss.removePotionEffect(PotionEffectType.INVISIBILITY);
					mBoss.setAI(true);
					mBoss.setInvulnerable(false);

					mBoss.getWorld().spawnParticle(Particle.CLOUD, loc, 10, 0.2, 0.45, 0.2, 0.125);
					mBoss.getWorld().spawnParticle(Particle.SMOKE_NORMAL, loc, 75, 0.2, 0.45, 0.2, 0.2);
					mBoss.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, loc, 35, 0.2, 0.45, 0.2, 0.15);
					Utils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s title [\"\",{\"text\":\"Elcard\",\"color\":\"gold\",\"bold\":true}]");
					Utils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "title @s subtitle [\"\",{\"text\":\"The Ignoble\",\"color\":\"red\",\"bold\":true}]");
					Utils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound minecraft:entity.blaze.shoot master @s ~ ~ ~ 10 1.65");
					Utils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound minecraft:entity.witch.ambient master @s ~ ~ ~ 10 0.6");
				}
			}

		}.runTaskTimer(mPlugin, 0, 20 * 5);

	}

	@Override
	public void death() {
		Utils.executeCommandOnNearbyPlayers(mBoss.getLocation(), detectionRange, "playsound minecraft:entity.wither.death master @s ~ ~ ~ 100 0.8");
		Utils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"[Elcard The Ignoble] \",\"color\":\"gold\"},{\"text\":\"You are no commoner... Who... Are you...?\",\"color\":\"white\"}]");
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
	}

	@Override
	public void init() {
		int bossTargetHp = 0;
		int player_count = Utils.playersInRange(mBoss.getLocation(), detectionRange).size();
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

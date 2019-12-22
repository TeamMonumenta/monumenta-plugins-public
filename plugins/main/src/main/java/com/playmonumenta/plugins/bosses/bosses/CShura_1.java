package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellConditionalTeleport;
import com.playmonumenta.plugins.bosses.spells.SpellTpBehindRandomPlayer;
import com.playmonumenta.plugins.utils.SerializationUtils;
import com.playmonumenta.plugins.bosses.utils.Utils;

public class CShura_1 extends BossAbilityGroup {
	public static final String identityTag = "boss_cshura_1";
	public static final int detectionRange = 50;
	private final Random mRand = new Random();

	private final LivingEntity mBoss;
	private final Location mSpawnLoc;
	private final Location mEndLoc;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new CShura_1(plugin, boss, spawnLoc, endLoc);
		});
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}

	public CShura_1(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		mBoss = boss;
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;
		mBoss.setRemoveWhenFarAway(false);

		mBoss.addScoreboardTag("Boss");

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellTpBehindRandomPlayer(plugin, mBoss, 160)
		));
		List<Spell> passiveSpells = Arrays.asList(
			new SpellBlockBreak(mBoss),
			new SpellConditionalTeleport(mBoss, spawnLoc, b -> spawnLoc.distance(b.getLocation()) > 35)
		);

		BossBarManager bossBar = new BossBarManager(plugin, mBoss, detectionRange, BarColor.RED, BarStyle.SOLID, null);

		super.constructBoss(plugin, identityTag, mBoss, activeSpells, passiveSpells, detectionRange, bossBar);
	}

	@Override
	public void init() {
		int bossTargetHp = 0;
		int player_count = Utils.playersInRange(mBoss.getLocation(), detectionRange).size();
		int hp_del = 256;
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

	public void bossDamagedEntity(EntityDamageByEntityEvent event) {
		int rand = mRand.nextInt(4);
		LivingEntity target = (LivingEntity) event.getEntity();
		if (rand == 0) {
			target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 0, false, true));
		} else if (rand == 1) {
			target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, false, true));
		} else if (rand == 2) {
			target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0, false, true));
		} else if (rand == 3) {
			target.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 60, 0, false, true));
		}
	}

	@Override
	public void death() {
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
		mBoss.teleport(mBoss.getLocation().add(0, -300, 0));
	}
}

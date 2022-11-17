package com.playmonumenta.plugins.bosses.bosses.bluestrike;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRunAction;
import com.playmonumenta.plugins.bosses.spells.SpellTargetVisiblePlayer;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;


public class DropShardBoss extends BossAbilityGroup {
	public static String identityTag = "boss_drop_shard";
	private static final int detectionRange = 100;
	private final LootTable mTable = Bukkit.getLootTable(NamespacedKey.fromString("epic:r3/dungeons/bluestrike/boss/blackblood_shard"));
	private final Random mRand;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new DropShardBoss(plugin, boss);
	}

	public DropShardBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mRand = new Random();
		List<Spell> passiveSpells = Arrays.asList(
			new SpellTargetVisiblePlayer((Mob) boss, detectionRange, 60, 160),
			new SpellRunAction(() -> {
				if (boss.hasPotionEffect(PotionEffectType.GLOWING)) {
					boss.removePotionEffect(PotionEffectType.GLOWING);
				}
			})
		);

		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null);
	}

	@Override
	public void death(EntityDeathEvent event) {
		double rand = FastUtils.randomDoubleInRange(0, 1.0);
		if (EntityUtils.isElite(mBoss) || rand > 0.5) {
			LootContext cont = new LootContext.Builder(mBoss.getLocation()).build();
			ItemStack item = mTable.populateLoot(mRand, cont).stream().toList().get(0);
			mBoss.getWorld().dropItem(mBoss.getLocation(), item).setGlowing(true);
		}

		event.setDroppedExp(0);
		event.getDrops().clear();
	}

	@Override
	public void bossChangedTarget(EntityTargetEvent event) {
		if (event.getEntity().getType() == EntityType.VILLAGER) {
			event.setCancelled(true);
		}
	}
}

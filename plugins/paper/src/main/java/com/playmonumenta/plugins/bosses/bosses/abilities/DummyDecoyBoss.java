package com.playmonumenta.plugins.bosses.bosses.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellDrawAggro;
import com.playmonumenta.plugins.depths.abilities.shadow.DummyDecoy;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;

public class DummyDecoyBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_dummydecoy";
	public static final int detectionRange = 30;

	private int mStunTime = 0;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new DummyDecoyBoss(plugin, boss);
	}

	public DummyDecoyBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		List<Spell> passives = new ArrayList<>();
		passives.add(new SpellDrawAggro(boss, DummyDecoy.AGGRO_RADIUS));

		super.constructBoss(SpellManager.EMPTY, passives, detectionRange, null);
	}

	@Override
	public void death(EntityDeathEvent event) {
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.7f);
		world.spawnParticle(Particle.EXPLOSION_LARGE, mBoss.getLocation(), 10, 0.5, 1, 0.5, 0.05);
		List<LivingEntity> mobsToStun = EntityUtils.getNearbyMobs(mBoss.getLocation(), DummyDecoy.STUN_RADIUS);
		for (LivingEntity le : mobsToStun) {
			EntityUtils.applyStun(Plugin.getInstance(), mStunTime, le);
		}
	}

	public void spawn(int stunTime) {
		mStunTime = stunTime;
	}
}

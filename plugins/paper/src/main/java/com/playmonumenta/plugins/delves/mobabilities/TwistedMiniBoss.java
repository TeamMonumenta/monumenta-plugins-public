package com.playmonumenta.plugins.delves.mobabilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.delves.abilities.Twisted;
import com.playmonumenta.plugins.events.DamageEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class TwistedMiniBoss extends BossAbilityGroup {
	public static final String identityTag = "TwistedBoss";

	private final Map<UUID, Integer> mCounterMap = new HashMap<>();

	public TwistedMiniBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		boss.addScoreboardTag(Twisted.TWISTED_MINIBOSS_TAG);

		Spell spell = new Spell() {
			int mTimer = 0;
			@Override public void run() {
				mTimer += 5;

				if (mTimer >= 20 * 120 && mBoss.isValid() && !mBoss.isDead()) {
					Twisted.despawnTwistedMiniBoss(mBoss);
					mBoss.remove();
				}
			}

			@Override public int cooldownTicks() {
				return 5;
			}

			@Override public void onDamage(DamageEvent event, LivingEntity damagee) {
				mTimer = 0;
			}

			@Override public void onHurtByEntity(DamageEvent event, Entity damager) {
				if (damager instanceof Player) {
					mTimer = 0;
				}
			}

			@Override public void onHurtByEntityWithSource(DamageEvent event, Entity damager, LivingEntity source) {
				if (source instanceof Player) {
					mTimer = 0;
				}
			}
		};

		super.constructBoss(SpellManager.EMPTY, List.of(spell), -1, null);
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new TwistedMiniBoss(plugin, boss);
	}

	public void playerDeath(Player player) {
		int oldValue = mCounterMap.getOrDefault(player.getUniqueId(), 0);
		mCounterMap.put(player.getUniqueId(), oldValue + 1);
		if (oldValue + 1 >= 5) {
			Twisted.despawnTwistedMiniBoss(mBoss);
			mBoss.remove();
		}
	}
}

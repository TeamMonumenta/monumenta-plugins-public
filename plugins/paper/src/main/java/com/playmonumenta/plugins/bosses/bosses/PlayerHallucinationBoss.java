package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class PlayerHallucinationBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_playerhallucination";
	public static final int detectionRange = 50;
	private final Player mPlayer;

	public PlayerHallucinationBoss(Plugin plugin, LivingEntity boss) {
		this(plugin, boss, null);
	}

	public PlayerHallucinationBoss(Plugin plugin, LivingEntity boss, @Nullable Player player) {
		super(plugin, identityTag, boss);
		if (player == null) {
			List<Player> players = PlayerUtils.playersInRange(boss.getLocation(), detectionRange, true);
			Collections.shuffle(players);
			mPlayer = players.getFirst();
		} else {
			mPlayer = player;
		}
		ArrayList<LivingEntity> mSelf = new ArrayList<>();
		mSelf.add(boss);

		getAllPassengers(boss, mSelf);

		HashMap<LivingEntity, Boolean> map = new HashMap<>();
		for (LivingEntity e : mSelf) {
			boolean bool = e.isVisibleByDefault();
			map.put(e, bool);
			makeMobHallucination(e, mPlayer);
		}

		for (LivingEntity e : mSelf) {
			e.addScoreboardTag("taunt_ignore");
		}

		Spell mModSafetySpell = new Spell() {
			@Override
			public void run() {
				for (Map.Entry<LivingEntity, Boolean> entry : map.entrySet()) {
					for (Player p : mBoss.getWorld().getNearbyPlayers(mBoss.getLocation(), detectionRange)) {
						if (p.getGameMode() == GameMode.SPECTATOR || p.getGameMode() == GameMode.CREATIVE) {
							p.showEntity(plugin, entry.getKey());
						} else {
							if (p != mPlayer) {
								p.hideEntity(plugin, entry.getKey());
							} else {
								p.showEntity(plugin, entry.getKey());
							}
						}
					}
				}
			}

			@Override
			public int cooldownTicks() {
				return 10;
			}
		};
		super.constructBoss(mModSafetySpell, detectionRange, null, 0);
	}

	private void getAllPassengers(LivingEntity e, List<LivingEntity> list) {
		for (Entity entity : e.getPassengers()) {
			if (entity instanceof LivingEntity add) {
				list.add(add);
				getAllPassengers(add, list);
			}
		}
	}

	private void makeMobHallucination(LivingEntity boss, Player player) {
		boolean invisible = boss.getScoreboardTags().contains("boss_invisible");
		boss.setVisibleByDefault(false);
		if (invisible) {
			boss.setInvisible(true);
		}
		player.showEntity(mPlugin, player);
		BossManager.getInstance().manuallyRegisterBoss(boss, new CrowdControlImmunityBoss(mPlugin, boss));
		EntityUtils.setAttributeBase(boss, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 1);
		if (boss instanceof Mob mob) {
			mob.setTarget(player);
		}
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		super.onDamage(event, damagee);
		if (!mPlayer.equals(damagee)) {
			event.setCancelled(true);
		}
	}

	@Override
	public void onHurt(DamageEvent event) {
		super.onHurt(event);
		if (!mPlayer.equals(event.getSource())) {
			event.setCancelled(true);
		}
	}

	@Override
	public void bossChangedTarget(EntityTargetEvent event) {
		super.bossChangedTarget(event);
		if (mBoss.getTargetEntity(100) == mPlayer) {
			event.setCancelled(true);
		} else {
			event.setTarget(mPlayer);
		}
	}
}

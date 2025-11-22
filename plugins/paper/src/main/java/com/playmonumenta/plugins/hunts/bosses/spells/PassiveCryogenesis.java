package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.Cryogenesis;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.AlocAcoc;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class PassiveCryogenesis extends Spell {
	private static final int ABOVE_Y = 3;

	private final LivingEntity mBoss;
	private int mT;

	public PassiveCryogenesis(LivingEntity boss) {
		mBoss = boss;
		mT = 0;
	}

	@Override
	public void run() {
		// every second
		if (mT++ % 20 == 0) {
			Location bossLoc = mBoss.getLocation();
			for (Player p : PlayerUtils.playersInXZRange(bossLoc, AlocAcoc.OUTER_RADIUS, true)) {
				Location playerLoc = p.getLocation();
				double amount = getCryogenesis(p);
				if (playerLoc.getY() > bossLoc.getY() + ABOVE_Y) {
					amount += 0.1;
					p.playSound(p, Sound.BLOCK_GLASS_BREAK, 1f, 2f);
				} else {
					amount -= 0.025;
				}
				if (amount > 1) {
					amount = 1;
					DamageUtils.damage(mBoss, p, DamageEvent.DamageType.AILMENT, 2, null, true, false, Cryogenesis.GENERIC_NAME);
					p.playSound(p, Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 0.6f);
				}
				setCryogenesis(p, Math.max(0, amount));
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	private double getCryogenesis(Player player) {
		Cryogenesis cryogenesis = Plugin.getInstance().mEffectManager.getActiveEffect(player, Cryogenesis.class);
		return cryogenesis == null ? 0 : cryogenesis.getMagnitude();
	}

	private void setCryogenesis(Player player, double amount) {
		EffectManager effectManager = Plugin.getInstance().mEffectManager;
		if (amount == 0) {
			player.setFreezeTicks(0);
			effectManager.clearEffects(player, Cryogenesis.GENERIC_NAME);
			return;
		}
		if (!effectManager.hasEffect(player, Cryogenesis.class)) {
			player.sendMessage(Component.text("You feel the cold air pierce your skin.", AlocAcoc.COLOR));
			player.playSound(player, Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, 1f, 0.75f);
			player.playSound(player, Sound.ENTITY_HORSE_BREATHE, 1f, 0.5f);
		}
		player.setFreezeTicks((int) ((player.getMaxFreezeTicks() - 1) * amount));
		effectManager.clearEffects(player, Cryogenesis.GENERIC_NAME);
		effectManager.addEffect(player, Cryogenesis.GENERIC_NAME, new Cryogenesis(amount).displaysTime(false));
	}
}

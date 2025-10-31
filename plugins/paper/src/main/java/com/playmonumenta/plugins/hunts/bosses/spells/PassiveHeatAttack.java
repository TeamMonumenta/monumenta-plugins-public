package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PassiveHeatAttack extends Spell {
	public static final String SLOWNESS_SOURCE = "FireProximitySlow";
	public static final String VULNERABILITY_SOURCE = "FireProximityVuln";
	public static final int FIRE_DETECT_RADIUS = 20;
	public static final int FIRE_THRESHOLD = 15;
	private final LivingEntity mBoss;
	int mT = 0;

	// Miscellaneous functions to make the magma cube's attacks more aggressive
	public PassiveHeatAttack(LivingEntity boss) {
		mBoss = boss;
	}

	@Override
	public void run() {
		if (mT++ % 20 == 0) {
			List<Block> fire = LocationUtils.getNearbyBlocks(mBoss.getLocation().getBlock(), FIRE_DETECT_RADIUS);
			fire.removeIf(block -> block.getType() != Material.FIRE);
			if (fire.size() >= FIRE_THRESHOLD) {
				for (Block block : fire) {
					new PartialParticle(Particle.REDSTONE, block.getLocation())
						.count(20)
						.delta(0.5)
						.data(new Particle.DustOptions(Color.BLACK, 1f))
						.spawnAsBoss();

					// Chance to be removed
					if (FastUtils.randomDoubleInRange(0, 1) <= 0.1 && block.getType() == Material.FIRE) {
						block.setType(Material.AIR);
					}
				}
			}

			// Has fire nearby, apply DOT to players
			PlayerUtils.playersInRange(mBoss.getLocation(), 20, true).forEach(player -> {
				if (fire.size() >= FIRE_THRESHOLD) {
					if (!EffectManager.getInstance().hasEffect(player, SLOWNESS_SOURCE)) {
						player.sendMessage(Component.text("The sulphur is being burnt, and toxic gas is released...", NamedTextColor.DARK_RED));
					}

					double amount = Math.max(-0.5, -((double) (fire.size() - FIRE_THRESHOLD) * 0.05));
					com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(player, SLOWNESS_SOURCE,
						new PercentSpeed(100, amount, SLOWNESS_SOURCE));
					com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(player, VULNERABILITY_SOURCE,
						new PercentDamageReceived(100, -amount / 2));
				}
			});

			if (mBoss.isOnGround() && !EffectManager.getInstance().hasEffect(mBoss, "SelfRoot")) {
				mBoss.setJumping(true);
				mBoss.setVelocity(mBoss.getLocation().getDirection().setY(0.4));
			}
		}
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (event.getType() == DamageEvent.DamageType.MELEE && damagee instanceof Player p && !event.isBlocked()) {
			for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), 3, false)) {
				EntityUtils.applyFire(com.playmonumenta.plugins.Plugin.getInstance(), 20 * 2, player, mBoss);
				MovementUtils.knockAway(mBoss, player, 0.2f, true);
			}
			PotionUtils.applyPotion(mBoss, p, new PotionEffect(PotionEffectType.LEVITATION, 15, 3, false, false, false));
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}

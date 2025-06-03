package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.Uamiel;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Tremor extends Spell {
	// the delay between each check, in ticks
	private static final int DELAY = 20;

	// the damage of the attack, in percent of max health
	private static final double ATTACK_DAMAGE = 0.15;

	// the distance above and below the spawn location of the boss for the attack to trigger
	private static final double ABOVE_Y = 3;
	private static final double BELOW_Y = 2;

	private final LivingEntity mBoss;
	private final World mWorld;
	private final Uamiel mUamiel;

	private int mTicks = 0;

	public Tremor(LivingEntity boss, Uamiel uamiel) {
		mBoss = boss;
		mWorld = boss.getWorld();
		mUamiel = uamiel;
	}

	@Override
	public void run() {
		mTicks++;
		if (mTicks >= DELAY) {
			mTicks = 0;
			for (Player player : PlayerUtils.playersInRange(mUamiel.mCenterLocation, 50, true)) {
				if ((player.getLocation().getY() > mUamiel.mCenterLocation.getY() + ABOVE_Y || player.getLocation().getY() < mUamiel.mCenterLocation.getY() - BELOW_Y) && !player.getLocation().clone().add(0, -0.5, 0).getBlock().isEmpty()) {
					double damage = EntityUtils.getMaxHealth(player) * ATTACK_DAMAGE;
					DamageUtils.damage(mBoss, player, DamageEvent.DamageType.TRUE, damage, null, false, true, "Tremor");
					player.sendMessage(Component.text("The ground shifts dangerously around you. You should return to the fight.", TextColor.color(56, 84, 60)));

					new PartialParticle(Particle.BLOCK_CRACK, player.getLocation().clone().add(0, 0.2, 0))
						.data(Material.DEEPSLATE.createBlockData())
						.count(30)
						.delta(0.4, 0.1, 0.4)
						.extra(0.05)
						.spawnAsBoss();

					mWorld.playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.HOSTILE, 1.5f, 0.6f);
				}
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}

package com.playmonumenta.plugins.bosses.spells.lich;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SpellMiasma extends Spell {
	private static final String WEAKNESS_SRC = "MiasmaWeakness";
	private final LivingEntity mBoss;
	private final Location mCenter;
	private final double mDepth;
	private final double mRange;
	private final List<Player> mWarnedPlayers = new ArrayList<>();
	private int mT = 0;

	public SpellMiasma(LivingEntity boss, Location loc, double j, double r) {
		mBoss = boss;
		mCenter = loc;
		mDepth = j;
		mRange = r;
	}

	@Override
	public void run() {
		mT--;
		if (mT <= 0) {
			mT = 2;
			for (Player player : Lich.playersInRange(mCenter, mRange, true)) {
				if (player.getLocation().getY() > mDepth + 10 && player.getGameMode() == GameMode.SURVIVAL) {
					Location l = player.getEyeLocation();
					new PartialParticle(Particle.SQUID_INK, l, 10, 0.1, 0.1, 0.1, 0.25).spawnAsEntityActive(mBoss);

					DamageUtils.damage(mBoss, player, DamageType.MAGIC, 20, null, false, true, "Miasma");
					player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 2 * 20, 0));
					Plugin.getInstance().mEffectManager.addEffect(player, WEAKNESS_SRC,
						new PercentDamageDealt(6 * 20, 0.2));
					if (!mWarnedPlayers.contains(player)) {
						mWarnedPlayers.add(player);
						player.sendMessage(Component.text("BEGONE THEN! FLY AWAY LITTLE, BIRD!", NamedTextColor.LIGHT_PURPLE));
					}
				}
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

}

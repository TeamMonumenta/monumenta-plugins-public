package com.playmonumenta.plugins.bosses.spells.portalboss;

import com.playmonumenta.plugins.bosses.bosses.PortalBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.bosses.Hedera;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.Collection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import static com.playmonumenta.plugins.bosses.bosses.PortalBoss.CUBE_TAG;

public class SpellPortalPassiveLava extends Spell {

	public LivingEntity mBoss;
	public Location mStartLoc;
	public int mTicks = 0;
	public PortalBoss mPortalBoss;

	public SpellPortalPassiveLava(LivingEntity boss, Location startLoc, PortalBoss portalBoss) {
		mBoss = boss;
		mStartLoc = startLoc;
		mPortalBoss = portalBoss;
	}

	@Override
	public void run() {

		//Detect Cube near drop spot
		Collection<ArmorStand> nearbyStands = mBoss.getWorld().getNearbyEntitiesByType(ArmorStand.class, mStartLoc.clone().add(new Vector(0, -1, 0)), 2.0);
		for (ArmorStand stand : nearbyStands) {

			if (stand.getScoreboardTags().contains(CUBE_TAG)) {
				mBoss.getWorld().playSound(stand.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.HOSTILE, 5.0f, 1.0f);
				stand.remove();
				mPortalBoss.cubeBrought();
			}
		}

		//Anticheese
		for (Player p : PlayerUtils.playersInRange(mBoss.getLocation(), Hedera.detectionRange, true)) {
			if (p.getLocation().getY() < mStartLoc.getY() - 4 && p.isInLava()) {
				BossUtils.bossDamagePercent(mBoss, p, .35, "Iota's Domain");
				com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(p, "PortalLava", new PercentHeal(6 * 20, -0.50));
				MessagingUtils.sendActionBarMessage(p, "You have 50% reduced healing for 6s", NamedTextColor.RED);
				PotionUtils.applyPotion(com.playmonumenta.plugins.Plugin.getInstance(), p, new PotionEffect(PotionEffectType.BAD_OMEN, 6 * 20, 1));
				p.sendMessage(Component.text("You feel the pure, flowing energy infest you, then spit you out.", NamedTextColor.RED));
				p.teleport(mStartLoc.clone().add(new Vector(0, 5, 0)), PlayerTeleportEvent.TeleportCause.UNKNOWN);
				p.playSound(p.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, SoundCategory.HOSTILE, 1.0f, 0.6f);
			}
		}

	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}

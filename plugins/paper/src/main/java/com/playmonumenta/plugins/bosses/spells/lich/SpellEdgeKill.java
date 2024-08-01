package com.playmonumenta.plugins.bosses.spells.lich;

import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SpellEdgeKill extends Spell {
	private final LivingEntity mBoss;
	private final Location mCenter;
	private boolean mTrigger = false;
	private List<Player> mWarned = new ArrayList<>();

	private final EnumSet<Material> mIgnoredMats = EnumSet.of(
		Material.AIR,
		Material.CAVE_AIR,
		Material.VOID_AIR,
		Material.LAVA
	);

	public SpellEdgeKill(LivingEntity boss, Location loc) {
		mBoss = boss;
		mCenter = loc;
	}

	@Override
	public void run() {
		final int mInArena = 69;
		final double mMaxDistance = 40.5;
		//run every half second
		if (mTrigger) {
			mTrigger = false;
		} else {
			mTrigger = true;
			//get all players within arena
			Location edgeYLoc = mCenter.clone().add(0, 18, 0);
			List<Player> players = Lich.playersInRange(edgeYLoc, mInArena, true);
			players.removeIf(p -> SpellDimensionDoor.getShadowed().contains(p));
			for (Player p : players) {
				//check distance
				Location pLoc = p.getLocation();
				pLoc.setY(mCenter.getY());
				Location standLoc = p.getLocation();
				standLoc.subtract(0, 1, 0);
				if (pLoc.distance(mCenter) > mMaxDistance &&
					    (!mIgnoredMats.contains(p.getLocation().getBlock().getType()) || !mIgnoredMats.contains(standLoc.getBlock().getType()))) {
					//players are on the outer ring of the arena, do damage + massive knock back into arena
					BossUtils.bossDamagePercent(mBoss, p, 0.4);
					MovementUtils.knockAway(mCenter, p, -5, false);
					p.playSound(p.getLocation(), Sound.ENTITY_GHAST_SCREAM, SoundCategory.HOSTILE, 3, 0.5f);
					p.playSound(p.getLocation(), Sound.ENTITY_GHAST_HURT, SoundCategory.HOSTILE, 3, 0.8f);

					//warn player
					if (!mWarned.contains(p)) {
						p.sendMessage(Component.text("YES, YOU SHALL FLEE! BEGONE.", NamedTextColor.LIGHT_PURPLE));
						mWarned.add(p);
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

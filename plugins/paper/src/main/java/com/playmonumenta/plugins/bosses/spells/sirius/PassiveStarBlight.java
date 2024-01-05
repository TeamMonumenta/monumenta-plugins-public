package com.playmonumenta.plugins.bosses.spells.sirius;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.sirius.Sirius;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.FlatHealthBoost;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

public class PassiveStarBlight extends Spell {
	private final Sirius mSirius;
	private PassiveStarBlightConversion mConverter;
	public static final String STARBLIGHTAG = "Starblight";
	public static final double STARBLIGHTCHANGEAMOUNT = -2.0;
	public static final int TICKSBETWENSTARBLIGHT = 40;
	//10min duration
	public static final int STARBLIGHTDURATION = 10 * 60 * 20;


	public PassiveStarBlight(Sirius sirius, PassiveStarBlightConversion converter) {
		mSirius = sirius;
		mConverter = converter;
	}

	@Override
	public void run() {
		List<Player> mPlayers = mSirius.getPlayersInArena(false);
		for (Player p : mPlayers) {
			int x = (int) p.getLocation().getX();
			int z = (int) p.getLocation().getZ();
			double realX = Math.abs(mConverter.mCornerOne.x() - x);
			double realZ = Math.abs(mConverter.mCornerOne.z() - z);
			realX = Math.floor(realX);
			realZ = Math.floor(realZ);
			if (mConverter.mBlighted[(int) realX][(int) realZ]) {
				applyStarBlight(p);
				//already applied starblight to them this tick
				continue;
			}
			//stops people standing on top
			if (p.getLocation().clone().subtract(0, 1, 0).getBlock().getType().equals(Material.BARRIER)) {
				applyStarBlight(p);
			}
		}
	}

	public static void applyStarBlight(Player p) {
		if (p.isDead()) {
			return;
		}
		Effect blight = EffectManager.getInstance().getActiveEffect(p, STARBLIGHTAG);
		Effect fail = EffectManager.getInstance().getActiveEffect(p, Sirius.FAIL_PARTICIPATION_TAG);
		int blightmultiplier = 1;
		if (fail != null && fail.getMagnitude() >= 3) {
			//increase strength if you dont participate
			blightmultiplier = 2;
		}
		if (blight != null) {
			if (STARBLIGHTDURATION - blight.getDuration() > TICKSBETWENSTARBLIGHT) {
				playSound(p);
				if (EntityUtils.getMaxHealth(p) < 2) {
					//death
					Plugin.getInstance().mEffectManager.clearEffects(p, STARBLIGHTAG);
					DamageUtils.damage(null, p, DamageEvent.DamageType.TRUE, EntityUtils.getMaxHealth(p), null, true, false, "Starblighten");
				} else {
					Plugin.getInstance().mEffectManager.clearEffects(p, STARBLIGHTAG);
					Plugin.getInstance().mEffectManager.addEffect(p, STARBLIGHTAG, new FlatHealthBoost(STARBLIGHTDURATION, blightmultiplier * STARBLIGHTCHANGEAMOUNT - blight.getMagnitude(), STARBLIGHTAG).deleteOnLogout(true));
				}
			}
		} else if (EntityUtils.getMaxHealth(p) > 2) {
			playSound(p);
			Plugin.getInstance().mEffectManager.addEffect(p, STARBLIGHTAG, new FlatHealthBoost(STARBLIGHTDURATION, blightmultiplier * STARBLIGHTCHANGEAMOUNT, STARBLIGHTAG).deleteOnLogout(true));
		} else {
			playSound(p);
			//has less then 2 hp so starblight insta kills
			Plugin.getInstance().mEffectManager.clearEffects(p, STARBLIGHTAG);
			DamageUtils.damage(null, p, DamageEvent.DamageType.TRUE, p.getHealth(), null, true, false, "Starblighten");
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}


	private static void playSound(Player p) {
		p.playSound(p, Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, SoundCategory.HOSTILE, 0.5f, 2);
		p.playSound(p, Sound.ENTITY_POLAR_BEAR_WARNING, SoundCategory.HOSTILE, 0.4f, 0.8f);
		p.playSound(p, Sound.ENTITY_ENDERMAN_DEATH, SoundCategory.HOSTILE, 0.2f, 0.1f);
	}
}

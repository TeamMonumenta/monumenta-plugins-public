package com.playmonumenta.plugins.bosses.spells.sirius;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.sirius.Sirius;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.StarBlight;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.listeners.StasisListener;
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
		EffectManager manager = Plugin.getInstance().mEffectManager;
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
				continue;
			}
			//remove 0 magnitude effects from people who are not effected by it for 3 seconds.
			Effect effect = manager.getActiveEffect(p, STARBLIGHTAG);
			if (effect != null && effect.getMagnitude() == 0 && STARBLIGHTDURATION - effect.getDuration() > TICKSBETWENSTARBLIGHT) {
				manager.clearEffects(p, STARBLIGHTAG);
			}
		}
	}

	public static void applyStarBlightNoCoolodown(Player p) {
		EffectManager manager = Plugin.getInstance().mEffectManager;
		if (p.isDead() || StasisListener.isInStasis(p)) {
			return;
		}
		Effect blight = manager.getActiveEffect(p, STARBLIGHTAG);
		Effect fail = manager.getActiveEffect(p, Sirius.FAIL_PARTICIPATION_TAG);
		int blightmultiplier = 1;
		if (fail != null && fail.getMagnitude() >= 3) {
			//increase strength if you dont participate
			blightmultiplier = 2;
		}
		if (blight != null) {
			playSound(p);
			if (EntityUtils.getMaxHealth(p) < 2) {
				//death
				manager.clearEffects(p, STARBLIGHTAG);
				DamageUtils.damage(null, p, DamageEvent.DamageType.TRUE, EntityUtils.getMaxHealth(p), null, true, false, "Starblighten");
				p.setHealth(0);
			} else {
				manager.clearEffects(p, STARBLIGHTAG);
				manager.addEffect(p, STARBLIGHTAG, new StarBlight(STARBLIGHTDURATION, blightmultiplier * STARBLIGHTCHANGEAMOUNT - blight.getMagnitude(), STARBLIGHTAG).deleteOnLogout(true));
			}
		} else if (EntityUtils.getMaxHealth(p) > 2) {
			playSound(p);
			manager.addEffect(p, STARBLIGHTAG, new StarBlight(STARBLIGHTDURATION, STARBLIGHTCHANGEAMOUNT, STARBLIGHTAG).deleteOnLogout(true).displays(false));
		} else {
			playSound(p);
			//has less then 2 hp so starblight insta kills
			manager.clearEffects(p, STARBLIGHTAG);
			DamageUtils.damage(null, p, DamageEvent.DamageType.TRUE, p.getHealth(), null, true, false, "Starblighten");
			p.setHealth(0);
		}
	}

	public static void applyStarBlight(Player p) {
		EffectManager manager = Plugin.getInstance().mEffectManager;
		if (p.isDead() || StasisListener.isInStasis(p)) {
			return;
		}
		Effect blight = manager.getActiveEffect(p, STARBLIGHTAG);
		Effect fail = manager.getActiveEffect(p, Sirius.FAIL_PARTICIPATION_TAG);
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
					manager.clearEffects(p, STARBLIGHTAG);
					DamageUtils.damage(null, p, DamageEvent.DamageType.TRUE, EntityUtils.getMaxHealth(p), null, true, false, "Starblighten");
					p.setHealth(0);
				} else {
					manager.clearEffects(p, STARBLIGHTAG);
					manager.addEffect(p, STARBLIGHTAG, new StarBlight(STARBLIGHTDURATION, blightmultiplier * STARBLIGHTCHANGEAMOUNT - blight.getMagnitude(), STARBLIGHTAG).deleteOnLogout(true));
				}
			}
		} else if (EntityUtils.getMaxHealth(p) > 2) {
			playSound(p);
			manager.addEffect(p, STARBLIGHTAG, new StarBlight(STARBLIGHTDURATION, 0, STARBLIGHTAG).deleteOnLogout(true).displays(false));
		} else {
			playSound(p);
			//has less then 2 hp so starblight insta kills
			manager.clearEffects(p, STARBLIGHTAG);
			DamageUtils.damage(null, p, DamageEvent.DamageType.TRUE, p.getHealth(), null, true, false, "Starblighten");
			p.setHealth(0);
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

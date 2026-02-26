package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.SporousAmalgam;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SporeBeastArenaRange extends Spell {
	public static final int INSIDE_RANGE = SporousAmalgam.SPELL_INNER_RADIUS;
	public static final int DAMAGE_FREQUENCY = (int) (20 * 2.5);
	public static final int MESSAGE_FREQUENCY = 20 * 4;
	public static final float SPORE_INFLICTED = 1f;
	public static final double DAMAGE_INFLICTED = 5;

	private final SporousAmalgam mSporebeast;
	private final LivingEntity mBoss;

	private final PPCircle mArenaRing;
	private final Map<Player, Integer> mMessagedPlayers;
	private final Map<Player, Integer> mDamagedPlayers;
	private final List<Player> mValidPlayers;

	private int mTicks;

	public SporeBeastArenaRange(SporousAmalgam sporeBeast) {
		mSporebeast = sporeBeast;
		mBoss = sporeBeast.mBoss;
		mArenaRing = new PPCircle(Particle.SCRAPE, mBoss.getLocation().add(0, 1.5, 0), INSIDE_RANGE).count(10);
		mMessagedPlayers = new HashMap<>();
		mDamagedPlayers = new HashMap<>();
		mValidPlayers = new ArrayList<>();
		mSporebeast.getPlayersInInRange().forEach(this::addValidPlayer);
		mTicks = 0;
	}


	@Override
	public void run() {
		if (mTicks++ % 5 == 0) {
			if (mValidPlayers.isEmpty()) {
				return;
			}

			for (int i = 0; i < mValidPlayers.size(); i++) {
				if (mValidPlayers.get(i).getLocation().distanceSquared(mBoss.getLocation()) > SporousAmalgam.OUTER_RADIUS * SporousAmalgam.OUTER_RADIUS) {
					mValidPlayers.remove(i);
					i--;
				}
			}

			List<Player> players = mSporebeast.getPlayersInOutRange();
			for (Player p : players) {
				if (!mValidPlayers.contains(p)) {
					continue;
				}

				if (p.getLocation().distanceSquared(mBoss.getLocation()) >= INSIDE_RANGE * INSIDE_RANGE || (p.getLocation().getY() - mBoss.getLocation().getY() >= 4 && PlayerUtils.isOnGround(p))) {
					if (!mMessagedPlayers.containsKey(p) || !mDamagedPlayers.containsKey(p)) {
						sendMessage(p);
						mMessagedPlayers.put(p, mTicks);
						mDamagedPlayers.put(p, mTicks);
					} else {
						if (mTicks - mMessagedPlayers.get(p) >= MESSAGE_FREQUENCY) {
							sendMessage(p);
							mMessagedPlayers.put(p, mTicks);
						}
						if (mTicks - mDamagedPlayers.get(p) >= DAMAGE_FREQUENCY) {
							dealDamage(p);
							mDamagedPlayers.put(p, mTicks);
						}
					}
				} else {
					mMessagedPlayers.remove(p);
					mDamagedPlayers.remove(p);
				}
			}
			mArenaRing.spawnAsBoss();
		}
	}

	private void dealDamage(Player player) {
		DamageUtils.damage(mBoss, player, DamageEvent.DamageType.TRUE, DAMAGE_INFLICTED);
		mSporebeast.addSpores(player, SPORE_INFLICTED);
		player.setVelocity(LocationUtils.getDirectionTo(mBoss.getLocation(), player.getLocation()).multiply(2.5));
		player.playSound(player, Sound.ENTITY_GENERIC_WIND_BURST, 1f, 0.9f);
	}

	private void sendMessage(Player player) {
		player.sendMessage(Component.text("The spores here are too dense; you should head back to the fight!", SporousAmalgam.TEXT_COLOR));
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	public void addValidPlayer(Player player) {
		if (!mValidPlayers.contains(player)) {
			mValidPlayers.add(player);
		}
	}
}

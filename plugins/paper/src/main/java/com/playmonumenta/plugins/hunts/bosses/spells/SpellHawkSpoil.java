package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.SteelWingHawk;
import com.playmonumenta.plugins.particle.PartialParticle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SpellHawkSpoil extends Spell {

	public static final double MIN_MOVEMENT_SQUARED = 1.5;
	public static final int MIN_SIZE = 10; // 10 entries in array = 2.5 seconds * 4 entries per second

	public final LivingEntity mBoss;
	public final SteelWingHawk mHawk;

	public final Map<Player, List<Vector>> mTrackingMap = new HashMap<>();

	public SpellHawkSpoil(LivingEntity boss, SteelWingHawk hawk) {
		mBoss = boss;
		mHawk = hawk;

		// Relies on player list being populated before this spell is created
		for (Player player : mHawk.getPlayers()) {
			mTrackingMap.put(player, new ArrayList<>());
		}
	}

	@Override
	public void run() {
		for (Player player : new ArrayList<>(mTrackingMap.keySet())) {
			if (!mHawk.getPlayers().contains(player) || mHawk.getSpoiledPlayers().contains(player)) {
				mTrackingMap.remove(player);
				return;
			}

			List<Vector> locs = mTrackingMap.get(player);
			if (locs == null) {
				// impossible but reviewdog complains
				return;
			}
			Vector current = player.getLocation().toVector();
			while (!locs.isEmpty()) {
				if (locs.get(0).distanceSquared(current) >= MIN_MOVEMENT_SQUARED) {
					locs.remove(0);
				} else {
					break;
				}
			}
			locs.add(current);
			if (locs.size() >= MIN_SIZE) {
				player.playSound(player.getLocation(), Sound.BLOCK_FURNACE_FIRE_CRACKLE, SoundCategory.HOSTILE, 2.0f, 0.4f);
				new PartialParticle(Particle.CRIT_MAGIC, player.getLocation(), 4, 0.5, 1).spawnAsBoss();
			}
		}
	}

	public boolean isStill(Player player) {
		List<Vector> locs = mTrackingMap.get(player);
		if (locs == null) {
			return false;
		}
		return locs.size() >= MIN_SIZE;
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (!(damagee instanceof Player player && isStill(player)) || event.getBossSpellName() == null) {
			return;
		}
		event.setFlatDamage(event.getFlatDamage() * 1.25);
		if (mHawk.spoil(player)) {
			player.sendMessage(Component.text("You were an easy target, so the Hawk landed a critical hit with its plume, spoiling your loot.", SteelWingHawk.COLOR));
		}
	}

	@Override
	public int cooldownTicks() {
		return 5;
	}
}

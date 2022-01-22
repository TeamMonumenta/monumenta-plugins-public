package com.playmonumenta.plugins.bosses.bosses.lich;

import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.lich.undeadplayers.SpellCrystalParticle;
import com.playmonumenta.plugins.events.DamageEvent;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

public class LichShieldBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_lichshield";
	public static final int detectionRange = 55;
	private final Location mCenter = Lich.getLichSpawn();
	private final Location mSpawnLoc;
	private double mHp = 0;
	private LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new LichShieldBoss(plugin, boss);
	}

	public LichShieldBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mBoss = boss;

		mSpawnLoc = mBoss.getLocation();
		List<Spell> passiveSpells = Arrays.asList(
			new SpellCrystalParticle(mBoss, mSpawnLoc)
		);

		super.constructBoss(null, passiveSpells, detectionRange, null);

		int hpScaling = Lich.mShieldMin;
		List<Player> players = Lich.playersInRange(mCenter, detectionRange, true);
		if (players.size() >= hpScaling) {
			//20 hp crystals if there are more than 10 players to prevent aleph spam
			mHp = 16 * (1 + (1 - 1/Math.E) * Math.log(players.size() - hpScaling));
		}
		mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(mHp);
		mBoss.setHealth(mHp);
	}

	@Override
	public void onHurt(DamageEvent event) {
		Entity damager = event.getDamager();
		if (damager != null && damager instanceof AbstractArrow proj) {
			proj.remove();
		}
		if (event.getDamage() > 32) {
			event.setDamage(32);
		}
		if (mBoss.getHealth() - event.getDamage() <= 0) {
			event.setCancelled(true);
			World world = mBoss.getWorld();
			world.playSound(mBoss.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 5, 1.2f);
			mBoss.remove();
		}
	}
}

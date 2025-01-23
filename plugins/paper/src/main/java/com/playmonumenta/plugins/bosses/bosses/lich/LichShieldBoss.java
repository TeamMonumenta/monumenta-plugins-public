package com.playmonumenta.plugins.bosses.bosses.lich;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.lich.undeadplayers.SpellCrystalParticle;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.List;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class LichShieldBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_lichshield";
	public static final int detectionRange = 55;

	public LichShieldBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		List<Spell> passiveSpells = List.of(
			new SpellCrystalParticle(mBoss, mBoss.getLocation())
		);

		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null);

		int hpScaling = Lich.mShieldMin;
		List<Player> players = Lich.playersInRange(Lich.getLichSpawn(), detectionRange, true);
		double hp = 0;
		if (players.size() > hpScaling) {
			//14 hp crystals if there are more than 10 players to prevent aleph spam
			hp = 14 * (1 + (1 - 1 / Math.E) * Math.log(players.size() - hpScaling));
		}
		EntityUtils.setMaxHealthAndHealth(mBoss, hp);
	}

	@Override
	public void onHurt(DamageEvent event) {
		Entity damager = event.getDamager();
		if (damager instanceof AbstractArrow proj) {
			proj.remove();
		}
		if (event.getDamage() > 32) {
			event.setFlatDamage(32);
		}
		if (mBoss.getHealth() - event.getFinalDamage(true) <= 0) {
			event.setCancelled(true);
			World world = mBoss.getWorld();
			world.playSound(mBoss.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 5, 1.2f);
			mBoss.remove();
		}
	}
}

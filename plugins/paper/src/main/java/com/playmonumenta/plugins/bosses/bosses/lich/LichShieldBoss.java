package com.playmonumenta.plugins.bosses.bosses.lich;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.lich.undeadplayers.SpellCrystalParticle;

public class LichShieldBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_lichshield";
	public static final int detectionRange = 55;
	private final Location mCenter = Lich.getLichSpawn();
	private final Location mSpawnLoc;
	private int mHpScaling = 8;
	private double mHp = 0;
	static LivingEntity mBoss;

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

		List<Player> players = Lich.playersInRange(mCenter, detectionRange, true);
		if (players.size() >= mHpScaling) {
			//20 hp crystals if there are more than 10 players to prevent aleph spam
			mHp = 16 * (1 + (1 - 1/Math.E) * Math.log(players.size() - mHpScaling));
		}
		mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(mHp);
		mBoss.setHealth(mHp);

		new BukkitRunnable() {

			@Override
			public void run() {
				mBoss.setNoDamageTicks(0);
				if (mBoss.isDead()) {
					World world = mBoss.getWorld();
					Location loc = mBoss.getLocation();
					mBoss.remove();
					world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 5, 1.2f);
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public void bossDamagedByEntity(EntityDamageByEntityEvent event) {
		if (event.getEntity() instanceof AbstractArrow) {
			AbstractArrow proj = (AbstractArrow) event.getEntity();
			proj.remove();
		}
		if (event.getDamage() > 26) {
			event.setDamage(26);
		}
	}
}

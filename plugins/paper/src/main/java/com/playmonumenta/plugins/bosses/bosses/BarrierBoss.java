package com.playmonumenta.plugins.bosses.bosses;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBarrier;

public class BarrierBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_barrier";
	public static final int detectionRange = 100;

	public static final int RECHARGE_TIME = 20 * 5;
	public static final int HITS_TO_BREAK = 1;
	private static final Particle.DustOptions REDSTONE_COLOR = new Particle.DustOptions(Color.fromRGB(225, 15, 255), 1.0f);

	private LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new BarrierBoss(plugin, boss);
	}

	public BarrierBoss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;

		List<Spell> passives = new ArrayList<>(Arrays.asList(new SpellBarrier(plugin, mBoss, detectionRange, RECHARGE_TIME, HITS_TO_BREAK,
				(Location loc) -> {
					World world = loc.getWorld();
					world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 1, 1);
				}, (Location loc) -> {
					World world = loc.getWorld();
					world.spawnParticle(Particle.REDSTONE, loc, 4, 0, 1, 0, REDSTONE_COLOR);
				}, (Location loc) -> {
					World world = loc.getWorld();
					world.playSound(loc, Sound.ITEM_SHIELD_BREAK, SoundCategory.HOSTILE, 1, 1);
				})));
		super.constructBoss(plugin, identityTag, mBoss, null, passives, detectionRange, null);
	}

}

package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseParticleAura;
import com.playmonumenta.plugins.bosses.spells.SpellBossBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellConditionalTeleport;
import com.playmonumenta.plugins.bosses.spells.SpellShieldStun;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellEarthenRupture;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellPrimordialBolt;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellRaiseJungle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Summons a powerful Primordial Elemental that is invulnerable and immovable until out of the ground.
 * Players will have 15 seconds to prepare for the elemental’s arrival. Kaul will not be attacking or
 * casting any abilities (except for his passives) during this time. (512 health)

Elemental’s Abilities:
Normal Block break passive
Raise Jungle (Kaul’s ability), however the timer for raising them will be 30 seconds instead of 40.

Earthen Rupture: After charging for 2 seconds, the Elemental will cause a large rupture that
spans out 5 blocks, knocking back all players, dealing 18 damage, and applying Slowness II for 10 seconds.

Stone Blast: After 1 second, fires at all players a powerful block breaking bolt. Intersecting with
a player causes 15 damage and applies Weakness II and Slowness II. Intersecting with a block causes
a TNT explosion to happen instead. The bolt will stop traveling if it hits a player or a block.

Once the elemental is dead, Kaul returns to the fight. The elemental will meld into the ground for later return in Phase 3.5

 */
public final class PrimordialElementalKaulBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_kaulprimordial";
	public static final int detectionRange = 100;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new PrimordialElementalKaulBoss(plugin, boss);
	}

	public PrimordialElementalKaulBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mBoss.setRemoveWhenFarAway(false);
		Location spawnLoc = mBoss.getLocation();
		World world = mBoss.getWorld();
		int bossTargetHp = 0;
		int playerCount = BossUtils.getPlayersInRangeForHealthScaling(mBoss, detectionRange);
		int hpDelta = 768;
		while (playerCount > 0) {
			bossTargetHp = bossTargetHp + hpDelta;
			hpDelta = hpDelta / 2;
			playerCount--;
		}
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, bossTargetHp * 1.1);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_FOLLOW_RANGE, detectionRange);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 1);
		mBoss.setHealth(bossTargetHp * 1.1);
		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellRaiseJungle(plugin, mBoss, 10, detectionRange, 20 * 8, 20 * 20),
			new SpellEarthenRupture(plugin, mBoss),
			new SpellPrimordialBolt(plugin, mBoss)
		));

		List<Spell> passiveSpells = Arrays.asList(
			new SpellBossBlockBreak(mBoss, 8, 1, 3, 1, true, true),
			new SpellBaseParticleAura(boss, 1,
				(LivingEntity mBoss) -> {
					world.spawnParticle(Particle.FALLING_DUST, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 8, 0.35,
					                    0.4, 0.35, Material.BROWN_CONCRETE.createBlockData());
				}
			),
			new SpellConditionalTeleport(mBoss, spawnLoc, b -> b.getLocation().getBlock().getType() == Material.BEDROCK
														       || b.getLocation().add(0, 1, 0).getBlock().getType() == Material.BEDROCK
														       || b.getLocation().getBlock().getType() == Material.LAVA
														       || b.getLocation().getBlock().getType() == Material.WATER),
			new SpellShieldStun(30 * 20)
		);

		Map<Integer, BossHealthAction> events = new HashMap<Integer, BossHealthAction>();
		events.put(50, mBoss -> {
			super.forceCastSpell(SpellRaiseJungle.class);
		});


		BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.GREEN, BarStyle.SEGMENTED_10, events);

		super.constructBoss(activeSpells, passiveSpells, detectionRange, bossBar);
	}

	@Override
	public void bossCastAbility(SpellCastEvent event) {
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true);
		if (players.size() > 0 && mBoss instanceof Mob mob) {
			Player newTarget = players.get(FastUtils.RANDOM.nextInt(players.size()));
			mob.setTarget(newTarget);
		}
	}
}

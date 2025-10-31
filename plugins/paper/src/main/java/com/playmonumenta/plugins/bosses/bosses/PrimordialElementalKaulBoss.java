package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseParticleAura;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellConditionalTeleport;
import com.playmonumenta.plugins.bosses.spells.SpellShieldStun;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellEarthenRupture;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellPrimordialBolt;
import com.playmonumenta.plugins.bosses.spells.kaul.SpellRaiseJungle;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class PrimordialElementalKaulBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_kaulprimordial";
	public static final int detectionRange = 100;

	public PrimordialElementalKaulBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mBoss.setRemoveWhenFarAway(false);
		final Location spawnLoc = mBoss.getLocation();

		final int playerCount = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true).size();
		final int hpDelta = 768;
		final double bossTargetHp = 1.1 * hpDelta * BossUtils.healthScalingCoef(playerCount, 0.6, 0.35);

		EntityUtils.setMaxHealthAndHealth(mBoss, bossTargetHp);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_FOLLOW_RANGE, detectionRange);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 1);

		GlowingManager.startGlowing(mBoss, NamedTextColor.GOLD, -1, GlowingManager.BOSS_SPELL_PRIORITY - 1);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellRaiseJungle(plugin, mBoss, 10, detectionRange, 20 * 8, 20 * 20),
			new SpellEarthenRupture(plugin, mBoss),
			new SpellPrimordialBolt(plugin, mBoss)
		));

		List<Spell> passiveSpells = Arrays.asList(
			new SpellBlockBreak(mBoss, 1, 3, 1, 8, false, true, false),
			new SpellBaseParticleAura(boss, 1,
				(LivingEntity mBoss) -> new PartialParticle(Particle.FALLING_DUST, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 8, 0.35,
					0.4, 0.35, Material.BROWN_CONCRETE.createBlockData()).spawnAsEntityActive(boss)
			),
			new SpellConditionalTeleport(mBoss, spawnLoc, b -> b.getLocation().getBlock().getType() == Material.BEDROCK
				|| b.getLocation().add(0, 1, 0).getBlock().getType() == Material.BEDROCK
				|| b.getLocation().getBlock().getType() == Material.LAVA
				|| b.getLocation().getBlock().getType() == Material.WATER),
			new SpellShieldStun(30 * 20)
		);

		Map<Integer, BossHealthAction> events = new HashMap<>();
		events.put(50, mBoss -> forceCastSpell(SpellRaiseJungle.class));

		BossBarManager bossBar = new BossBarManager(boss, detectionRange, BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_10, events);

		super.constructBoss(activeSpells, passiveSpells, detectionRange, bossBar);
	}

	@Override
	public void bossCastAbility(SpellCastEvent event) {
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true);
		if (!players.isEmpty() && mBoss instanceof Mob mob) {
			Player newTarget = players.get(FastUtils.RANDOM.nextInt(players.size()));
			mob.setTarget(newTarget);
		}
	}
}

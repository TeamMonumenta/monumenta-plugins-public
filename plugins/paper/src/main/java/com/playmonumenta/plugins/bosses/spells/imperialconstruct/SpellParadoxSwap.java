package com.playmonumenta.plugins.bosses.spells.imperialconstruct;

import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.bosses.ImperialConstruct;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.TemporalFlux;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class SpellParadoxSwap extends Spell {

	private static final EnumSet<DamageEvent.DamageType> AFFECTED_TYPES = EnumSet.of(DamageEvent.DamageType.MELEE, DamageEvent.DamageType.MELEE_SKILL, DamageEvent.DamageType.MELEE_ENCH, DamageEvent.DamageType.PROJECTILE, DamageEvent.DamageType.PROJECTILE_SKILL, DamageEvent.DamageType.PROJECTILE_ENCH, DamageEvent.DamageType.MAGIC);
	private static final int COOLDOWN = 20 * 5;
	private final int mRange;
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final @Nullable ImperialConstruct mConstruct;
	private boolean mOnCooldown;

	public SpellParadoxSwap(Plugin plugin, int range, LivingEntity boss) {
		mPlugin = plugin;
		mRange = range;
		mBoss = boss;
		mConstruct = findConstruct();
		mOnCooldown = false;
	}

	@Override
	public void run() {
		mBoss.setGlowing(!mOnCooldown);
	}

	@Override
	public void onHurtByEntity(DamageEvent event, Entity damager) {
		if (mOnCooldown || !(damager instanceof Player player) || !AFFECTED_TYPES.contains(event.getType())) {
			return;
		}

		if (mConstruct == null || !mConstruct.isInArena(player)) {
			return;
		}

		List<Player> nearbyPlayers = EntityUtils.getNearestPlayers(mBoss.getLocation(), mRange);
		nearbyPlayers.remove(player);
		if (nearbyPlayers.isEmpty()) {
			return;
		}

		Collections.reverse(nearbyPlayers);
		Player swapToPlayer = nearbyPlayers.stream()
			.filter(mConstruct::isInArena)
			.filter(p -> !com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.hasEffect(p, TemporalFlux.class))
			.findFirst().orElse(null);
		if (swapToPlayer == null) {
			return;
		}

		Set<Effect> clearedEffects = com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(player, TemporalFlux.GENERIC_NAME);
		if (clearedEffects == null || clearedEffects.isEmpty()) {
			return;
		}

		mConstruct.sendMessage(Component.text("[Temporal Exchanger]", NamedTextColor.GOLD).append(Component.text(" TEMPORAL ANOMALY TRANSFERRED - ENTERING TEMPORARY ENERGY REGENERATION STATE", NamedTextColor.WHITE)));

		new PartialParticle(Particle.SOUL, mBoss.getLocation(), 20, 1, 1, 1).spawnAsEntityActive(mBoss);
		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, SoundCategory.HOSTILE, 30, 1);
		com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(swapToPlayer, TemporalFlux.GENERIC_NAME, new TemporalFlux(20 * 30));

		mOnCooldown = true;
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mOnCooldown = false, COOLDOWN);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	private @Nullable ImperialConstruct findConstruct() {
		for (IronGolem golem : mBoss.getWorld().getEntitiesByClass(IronGolem.class)) {
			if (!golem.isValid()) {
				continue;
			}
			ImperialConstruct construct = BossManager.getInstance().getBoss(golem, ImperialConstruct.class);
			if (construct != null) {
				return construct;
			}
		}
		MMLog.warning("Failed to find Construct in SpellParadoxSwap!");
		return null;
	}
}

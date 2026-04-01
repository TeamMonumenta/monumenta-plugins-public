package com.playmonumenta.plugins.abilities.snowperks;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.guis.SnowPerkGui;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.WHITE;

public class ShatterProofOrnament extends Ability {
	private static final String SCOREBOARD = "ShatterProofOrnament";
	private static final int POINT_COST = 5;
	private static final String USED_TAG = "OrnamentUsed";
	private static final double REVIVE_HP = 1.0;
	private static final int INVULN_DURATION = 5 * 20;

	public static final AbilityInfo<ShatterProofOrnament> INFO =
		new SnowPerkGui.SnowPerkInfo<>(ShatterProofOrnament.class, "Shatter-Proof Ornament", ShatterProofOrnament::new)
			.snowPointCost(POINT_COST)
			.scoreboardId(SCOREBOARD)
			.displayItem(Material.TOTEM_OF_UNDYING)
			.description(getDescription())
			.priorityAmount(10000);

	public ShatterProofOrnament(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public void onHurtFatal(DamageEvent event) {
		if (ScoreboardUtils.checkTag(mPlayer, USED_TAG) || event.isBlocked() || event.getType() == DamageEvent.DamageType.TRUE) {
			return;
		}

		mPlayer.getScoreboardTags().add(USED_TAG);

		sendActionBarMessage("Shatter-Proof Ornament has been activated!");
		event.setCancelled(true);

		Location loc = LocationUtils.getEntityCenter(mPlayer);
		World world = mPlayer.getWorld();

		mPlayer.setHealth(EntityUtils.getMaxHealth(mPlayer));
		mPlugin.mEffectManager.addEffect(mPlayer, "ShatterProofInvuln", new PercentDamageReceived(INVULN_DURATION, -1));

		new PartialParticle(Particle.FIREWORKS_SPARK, loc, 150, 0.2, 0.35, 0.2, 0.5).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SNOWFLAKE, loc, 150, 0.2, 0.35, 0.2, 0.5).spawnAsPlayerActive(mPlayer);
		world.playSound(loc, Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1f, 1.8f);
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 0.8f, 2.0f);
		world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 2.0f, 1.6f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.4f, 1.4f);
		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 2.0f, 0.5f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1.0f, 1.4f);
		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.0f, 0.1f);
	}

	@Override
	public @Nullable Component getHotbarMessage() {
		Component output = Component.text("[", NamedTextColor.YELLOW)
			.append(Component.text("SPO", INFO.getActionBarColor()))
			.append(Component.text("]", NamedTextColor.YELLOW))
			.append(Component.text(": ", NamedTextColor.WHITE));

		if (ScoreboardUtils.checkTag(mPlayer, USED_TAG)) {
			output = output.append(Component.text("✗", NamedTextColor.RED, TextDecoration.BOLD));
		} else {
			output = output.append(Component.text("✓", NamedTextColor.GREEN, TextDecoration.BOLD));
		}

		return output;
	}

	public static Description<ShatterProofOrnament> getDescription() {
		return new FormattedDescriptionBuilder<>(() -> INFO).arrowColor(SnowPerkGui.SNOW_ARROW_COLOR)
			.addDashedLine()
			.addLine("Once per game, you revive with %p *HP*").styles(WHITE).statValues(stat(REVIVE_HP))
			.addLine("and %t of invulnerability when you take").statValues(stat(INVULN_DURATION))
			.addLine("fatal damage.")
			.addLine()
			.addStat("Cost: %d Snow Points").statValues(stat(POINT_COST))
			.addDashedLine();
	}
}

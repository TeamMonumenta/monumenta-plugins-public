package com.playmonumenta.plugins.cosmetics.skills.cleric;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class DivineJusticeCS implements CosmeticSkill {

	private final float HEAL_PITCH_SELF = Constants.NotePitches.C18;
	private final float HEAL_PITCH_OTHER = Constants.NotePitches.E22;

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.DIVINE_JUSTICE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.IRON_SWORD;
	}

	public float getHealPitchSelf() {
		return HEAL_PITCH_SELF;
	}

	public float getHealPitchOther() {
		return HEAL_PITCH_OTHER;
	}

	public Material justiceAsh() {
		return Material.SUGAR;
	}

	public void justiceAshColor(Item item) {
		ScoreboardUtils.addEntityToTeam(item, "GlowingWhite", NamedTextColor.WHITE);
	}

	public String justiceAshName() {
		return "Purified Ash";
	}

	public void justiceAshPickUp(Player player, Location loc) {
		player.playSound(player.getLocation(), Sound.BLOCK_GRAVEL_STEP, SoundCategory.PLAYERS, 0.75f, 0.5f);
		player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.PLAYERS, 0.2f, 0.2f);

		Location particleLocation = loc.add(0, 0.2, 0);
		new PartialParticle(Particle.ASH, particleLocation, 50)
			.delta(0.15, 0.1, 0.15)
			.spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, particleLocation, 7)
			.delta(0.1, 0.1, 0.1)
			.data(new Particle.DustOptions(Color.fromBGR(100, 100, 100), 1))
			.spawnAsPlayerActive(player);
	}

	public void justiceOnDamage(Player player, LivingEntity enemy, World world, Location enemyLoc, double widerWidthDelta, int combo) {
		PartialParticle partialParticle = new PartialParticle(
			Particle.END_ROD,
			LocationUtils.getHalfHeightLocation(enemy),
			10,
			widerWidthDelta,
			PartialParticle.getHeightDelta(enemy),
			widerWidthDelta,
			0.05
		).spawnAsPlayerActive(player);
		partialParticle.mParticle = Particle.FLAME;
		partialParticle.spawnAsPlayerActive(player);

		world.playSound(enemyLoc, Sound.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.2f, 1.5f);
		world.playSound(enemyLoc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 0.5f, 2.0f);
		world.playSound(enemyLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0f, 0.8f);
		world.playSound(enemyLoc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 0.5f, 1.2f);
	}

	public void justiceKill(Player player, Location loc) {

	}

	public void justiceHealSound(List<Player> players, float pitch) {
		for (Player healedPlayer : players) {
			healedPlayer.playSound(
				healedPlayer.getLocation(),
				Sound.BLOCK_NOTE_BLOCK_CHIME,
				SoundCategory.PLAYERS,
				0.5f,
				pitch
			);
		}
	}
}

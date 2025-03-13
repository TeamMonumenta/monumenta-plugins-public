package com.playmonumenta.plugins.cosmetics.skills.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.MetadataUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;


public class FrenzyCS implements CosmeticSkill {
	private static final String SOUND_METADATA = "FrenzyPlayedThisTick";
	private static final String ENHANCED_METADATA = "FrenzyEnhancePlayedThisTick";

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.FRENZY;
	}

	@Override
	public Material getDisplayItem() {
		return Material.STONE_AXE;
	}

	public void frenzyLevelOne(Player player) {
		if (MetadataUtils.checkOnceThisTick(Plugin.getInstance(), player, SOUND_METADATA)) {
			Location mLoc = player.getLocation();
			new PartialParticle(Particle.CLOUD, mLoc, 6)
				.delta(0.4)
				.extra(0.03)
				.spawnAsPlayerActive(player);
			player.playSound(mLoc, Sound.ENTITY_BLAZE_AMBIENT, 0.2f, 1.25f);
			player.playSound(mLoc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 0.6f, 1.5f);
			player.playSound(mLoc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.4f, 1.7f);
		}
	}

	public void frenzyLevelTwo(Player player) {
		if (MetadataUtils.checkOnceThisTick(Plugin.getInstance(), player, SOUND_METADATA)) {
			Location mLoc = player.getLocation();
			new PartialParticle(Particle.CLOUD, mLoc, 10)
				.delta(0.4)
				.extra(0.1)
				.spawnAsPlayerActive(player);
			player.playSound(mLoc, Sound.ENTITY_BLAZE_AMBIENT, 0.2f, 2f);
			player.playSound(mLoc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 0.7f, 1.65f);
			player.playSound(mLoc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.7f, 2f);
			player.playSound(mLoc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 0.9f, 0.1f);
		}
	}

	public void frenzyEnhancement(Player player) {
		if (MetadataUtils.checkOnceThisTick(Plugin.getInstance(), player, ENHANCED_METADATA)) {
			new PartialParticle(Particle.REDSTONE, player.getLocation().add(new Vector(0, 0.4, 0)), 15)
				.data(new Particle.DustOptions(Color.RED, 1f))
				.delta(0.4)
				.spawnAsPlayerActive(player);
		}
	}
}

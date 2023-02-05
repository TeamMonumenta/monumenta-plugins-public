package com.playmonumenta.plugins.cosmetics.skills.scout.ranger;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.skills.PrestigeCS;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class PrestigiousManeuverCS extends TacticalManeuverCS implements PrestigeCS {

	public static final String NAME = "Prestigious Maneuver";

	private static final Particle.DustOptions GOLD_COLOR = new Particle.DustOptions(Color.fromRGB(255, 208, 40), 1.0f);
	private static final Particle.DustOptions LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(255, 247, 207), 1.25f);
	private static double START_EFFECT_RADIUS = 2.8;

	@Override
	public @Nullable Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName(),
			"MANEUVER_DESC"
		);
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.TACTICAL_MANEUVER;
	}

	@Override
	public Material getDisplayItem() {
		return Material.GOLDEN_BOOTS;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public boolean isUnlocked(Player player) {
		return player != null;
	}

	@Override
	public String[] getLockDesc() {
		return List.of("LOCKED").toArray(new String[0]);
	}

	@Override
	public int getPrice() {
		return 1;
	}

	@Override
	public void maneuverStartEffect(World world, Player mPlayer, Vector dir) {
		Location loc = mPlayer.getLocation();
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.8f, 2f);
		world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.6f, 1.8f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 1f, 2f);
		new PartialParticle(Particle.REDSTONE, loc, 60, 2, 0.5, 2, 0.2, GOLD_COLOR).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CLOUD, loc, 10, 0.25, 0.1, 0.25, 0.125).spawnAsPlayerActive(mPlayer);
		Location mCenter = loc.clone().add(0, 0.4 * START_EFFECT_RADIUS, 0).add(dir.clone().multiply(0.8 * START_EFFECT_RADIUS));
		Vector mFront = VectorUtils.rotateTargetDirection(dir.clone().multiply(START_EFFECT_RADIUS), 0, -90);
		// Draw 風
		// I don't know how to describe these parameters. Quite complex in English.
		final int para1 = (int) Math.ceil(START_EFFECT_RADIUS * 4); //撇捺
		final double para2 = 0.7; //侧边
		final double para3 = 0.25; //倾角
		final int para4 = (int) Math.ceil(START_EFFECT_RADIUS * 2.4); //顶密
		final int para5 = (int) Math.ceil(START_EFFECT_RADIUS * 1.5); //上密
		final double para6 = 0.6; //上高
		final int para7 = (int) Math.ceil(START_EFFECT_RADIUS * 1.5); //下密
		final double para8 = -0.7; //下高
		final int para9 = (int) Math.ceil(START_EFFECT_RADIUS * 9.6); //口密
		final double para10 = 0.3; //口高
		final double para11 = 0.4; //口宽
		final double para12 = 0.075; //凹凸

		ParticleUtils.drawCurve(mCenter, -para1, para1, mFront,
			t -> FastUtils.sin(t * 3.1416 / para1 / 2),
				t -> 0, t -> para3 * t / para1 - para2,
				(l, t) -> new PartialParticle(Particle.REDSTONE, l, 2, 0.05, 0, 0.05, 0, LIGHT_COLOR).spawnAsPlayerActive(mPlayer)
		); // 左撇
		ParticleUtils.drawCurve(mCenter, -para1, para1, mFront,
			t -> FastUtils.sin(t * 3.1416 / para1 / 2),
				t -> 0, t -> -para3 * t / para1 + para2,
				(l, t) -> new PartialParticle(Particle.REDSTONE, l, 2, 0.05, 0, 0.05, 0, LIGHT_COLOR).spawnAsPlayerActive(mPlayer)
		); // 右钩
		ParticleUtils.drawCurve(mCenter, -para4, para4, mFront,
			t -> 1 + para12 * FastUtils.cos(t * 3.1416 / para4 / 2),
				t -> 0, t -> (para2 - para3) * t / para4,
				(l, t) -> new PartialParticle(Particle.REDSTONE, l, 2, 0.05, 0, 0.05, 0, LIGHT_COLOR).spawnAsPlayerActive(mPlayer)
		); // 上折
		ParticleUtils.drawCurve(mCenter, -para5, para5, mFront,
			t -> para6 + 0.8 * para12 * FastUtils.cos(t * 3.1416 / para5 / 2),
				t -> 0, t -> para2 * para6 * t / para5,
				(l, t) -> new PartialParticle(Particle.REDSTONE, l, 2, 0.05, 0, 0.05, 0, LIGHT_COLOR).spawnAsPlayerActive(mPlayer)
		); // 上横
		ParticleUtils.drawCurve(mCenter, -para7, para7, mFront,
			t -> para8 - 0.6 * para12 * FastUtils.cos(t * 3.1416 / para7 / 2),
				t -> 0, t -> para2 * para8 * t / para7,
				(l, t) -> new PartialParticle(Particle.REDSTONE, l, 2, 0.05, 0, 0.05, 0, LIGHT_COLOR).spawnAsPlayerActive(mPlayer)
		); // 下横
		ParticleUtils.drawCurve(mCenter, -para7, para5, mFront,
			t -> (para6 - para8) * (t + para7) / (para5 + para7) + para8,
				t -> 0, t -> 0,
				(l, t) -> new PartialParticle(Particle.REDSTONE, l, 2, 0.05, 0, 0.05, 0, LIGHT_COLOR).spawnAsPlayerActive(mPlayer)
		); // 中竖
		ParticleUtils.drawCurve(mCenter.clone().add(mFront.clone().multiply(0.5 * (para6 + para8))), 1, para9, mFront,
			t -> FastUtils.cos(2 * 3.1416 * t / para9) * para10,
				t -> 0, t -> FastUtils.sin(2 * 3.1416 * t / para9) * para11,
				(l, t) -> new PartialParticle(Particle.REDSTONE, l, 2, 0.05, 0, 0.05, 0, LIGHT_COLOR).spawnAsPlayerActive(mPlayer)
		); // 中口
	}

	@Override
	public void maneuverBackEffect(World world, Player mPlayer) {
		Location loc = mPlayer.getLocation();
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.45f, 2f);
		world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.35f, 1.8f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.6f, 2f);
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_SHOOT, SoundCategory.PLAYERS, 1.25f, 1.5f);
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_SHOOT, SoundCategory.PLAYERS, 1.5f, 1.75f);
		new PartialParticle(Particle.SPIT, loc, 25, 0.1, 0, 0.1, 0.125).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CLOUD, loc, 20, 0.5, 0.25, 0.5, 0.15f).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, loc, 30, 1, 0.5, 1, 0.15, GOLD_COLOR).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public void maneuverTickEffect(Player mPlayer) {
		Location loc = mPlayer.getLocation();
		new PartialParticle(Particle.CLOUD, loc, 2, 0.25, 0.1, 0.25, 0.1).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, loc, 4, 0.15, 0.05, 0.15, 0, LIGHT_COLOR).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, loc, 6, 0.25, 0.1, 0.25, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public void maneuverHitEffect(World world, Player mPlayer) {
		Location loc = mPlayer.getLocation();
		new PartialParticle(Particle.SPELL_INSTANT, loc, 100, 0.65, 0.35, 0.65, 0.3).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CLOUD, loc, 15, 0.25, 0.1, 0.25, 0.125).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, loc, 100, 2.5, 1.5, 2.5, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer);
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1.6f, 0.8f);
		world.playSound(mPlayer.getLocation(), Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 2f, 1.75f);
	}
}

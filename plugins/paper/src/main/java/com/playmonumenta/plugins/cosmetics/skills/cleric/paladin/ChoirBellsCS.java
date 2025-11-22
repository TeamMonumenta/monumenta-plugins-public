package com.playmonumenta.plugins.cosmetics.skills.cleric.paladin;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ChoirBellsCS implements CosmeticSkill {

	private static final SoundsList.CSound[][] SOUNDS = {
		new SoundsList.CSound[]{
			new SoundsList.CSound(Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 0.6f),
			new SoundsList.CSound(Sound.ENTITY_ALLAY_AMBIENT_WITH_ITEM, 0.7f, 0.4f),
			new SoundsList.CSound(Sound.BLOCK_BELL_USE, 0.1f, 2.0f)
		},
		new SoundsList.CSound[]{
			new SoundsList.CSound(Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 0.8f),
			new SoundsList.CSound(Sound.ITEM_TRIDENT_RETURN, 0.6f, 0.8f)
		},
		new SoundsList.CSound[]{
			new SoundsList.CSound(Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f),
			new SoundsList.CSound(Sound.ITEM_TRIDENT_RETURN, 0.6f, 1.0f)
		}
	};

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.CHOIR_BELLS;
	}

	@Override
	public Material getDisplayItem() {
		return Material.BELL;
	}

	public void bellsCastEffect(Player player, double range, Location loc) {
		new BukkitRunnable() {
			final double mDelta = 0.8;
			int mTicks = 0;

			@Override
			public void run() {
				double radius = (mTicks + 1.5) * mDelta;
				double height = (FastUtils.cos(mTicks * Math.PI / 4) + 1) * Math.exp(-0.06 * mTicks) * 1.25;
				int units = (mTicks + 3) * 3;
				ParticleUtils.drawCurve(loc, 1, units,
					new Vector(0, 0, 1), new Vector(1, 0, 0), new Vector(0, 1, 0),
					t -> radius * FastUtils.cos(Math.PI * 2 * t / units),
					t -> radius * FastUtils.sin(Math.PI * 2 * t / units),
					t -> height,
					(loc, t) -> {
						new PartialParticle(Particle.REDSTONE, loc, 1)
							.data(new Particle.DustOptions(ParticleUtils.getTransition(Color.fromRGB(255, 255, 50), Color.ORANGE,
								mTicks / range * mDelta), (float) (1.6f - mTicks / range * mDelta)))
							.spawnAsPlayerActive(player);
						new PartialParticle(Particle.SPELL_INSTANT, loc.clone().add(0, 0.25, 0))
							.extra(0.1)
							.directionalMode(true)
							.spawnAsPlayerActive(player);
					});

				if (++mTicks > range / mDelta) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				for (SoundsList.CSound sound : SOUNDS[mTicks]) {
					sound.play(loc, SoundCategory.PLAYERS);
				}
				if (++mTicks >= SOUNDS.length) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);

	}

	public void bellsApplyEffect(Player player, LivingEntity mob) {
		//Nope!
	}
}

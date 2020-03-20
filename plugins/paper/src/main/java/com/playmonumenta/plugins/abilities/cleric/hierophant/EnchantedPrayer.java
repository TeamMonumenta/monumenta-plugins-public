package com.playmonumenta.plugins.abilities.cleric.hierophant;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

/*
 * Enchanted Prayer: Jump and shift right-click to enchant the
 * weapons of all players in a 15-block radius (including
 * yourself) with holy magic, making their next melee attack
 * release light energy. The amplified attack deals additional
 * 7 / 12 damage in a 3.5 block radius around the target,
 * while healing the player for 2 / 4 hearts. (Cooldown: 18 s)
 *
 * TODO: The enchanting portion of this ability is not currently very
 * organized/efficient with our current systems setup. A workaround
 * will be used for now but I recommend we get some sort of Custom Effect
 * system implemented for current and future effects like this. - Fire
 */
public class EnchantedPrayer extends Ability {

	private static final int ENCHANTED_PRAYER_COOLDOWN = 20 * 18;
	private static final int ENCHANTED_PRAYER_1_DAMAGE = 7;
	private static final int ENCHANTED_PRAYER_2_DAMAGE = 12;
	private static final int ENCHANTED_PRAYER_1_HEAL = 2;
	private static final int ENCHANTED_PRAYER_2_HEAL = 4;

	public EnchantedPrayer(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Enchanted Prayer");
		mInfo.scoreboardId = "EPrayer";
		mInfo.mShorthandName = "EP";
		mInfo.mDescriptions.add("Right-clicking in the air while shifted enchants the weapons of all players in a 15 block radius with holy magic. Their next melee attack deals an additional 7 damage in a 3-block radius while healing the player for 2 hp. Cooldown: 18s.");
		mInfo.mDescriptions.add("Damage is increased to 12. Healing is increased to 4 hp.");
		mInfo.linkedSpell = Spells.ENCHANTED_PRAYER;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
		mInfo.cooldown = ENCHANTED_PRAYER_COOLDOWN;
	}

	public static final String ENCHANTED_PRAYER_METAKEY = "EnchantedPrayerMetakey";

	@Override
	public void cast(Action action) {
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.5f, 1);
		putOnCooldown();
		new BukkitRunnable() {
			double rotation = 0;
			Location loc = mPlayer.getLocation();
			double radius = 0;

			@Override
			public void run() {

				radius += 0.25;
				for (int i = 0; i < 36; i += 1) {
					rotation += 10;
					double radian1 = Math.toRadians(rotation);
					loc.add(Math.cos(radian1) * radius, 0.15, Math.sin(radian1) * radius);
					mWorld.spawnParticle(Particle.SPELL_INSTANT, loc, 2, 0.15, 0.15, 0.15, 0);
					loc.subtract(Math.cos(radian1) * radius, 0.15, Math.sin(radian1) * radius);

				}
				if (radius >= 5) {
					this.cancel();
				}

			}

		}.runTaskTimer(mPlugin, 0, 1);
		int enchantedPrayer = getAbilityScore();
		for (Player p : PlayerUtils.playersInRange(mPlayer, 15, true)) {
			p.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.2f, 1.0f);
			mWorld.spawnParticle(Particle.SPELL_INSTANT, mPlayer.getLocation(), 50, 0.25, 0, 0.25, 0.01);
			p.setMetadata(ENCHANTED_PRAYER_METAKEY, new FixedMetadataValue(mPlugin, enchantedPrayer));
			new BukkitRunnable() {
				int t = 0;
				@Override
				public void run() {
					t++;
					Location rightHand = PlayerUtils.getRightSide(p.getEyeLocation(), 0.45).subtract(0, .8, 0);
					Location leftHand = PlayerUtils.getRightSide(p.getEyeLocation(), -0.45).subtract(0, .8, 0);
					mWorld.spawnParticle(Particle.SPELL_INSTANT, leftHand, 1, 0.05f, 0.05f, 0.05f, 0);
					mWorld.spawnParticle(Particle.SPELL_INSTANT, rightHand, 1, 0.05f, 0.05f, 0.05f, 0);
					if (!p.hasMetadata(ENCHANTED_PRAYER_METAKEY)) {
						this.cancel();
					}

					if (t >= ENCHANTED_PRAYER_COOLDOWN || p == null || p.isDead() || !p.isOnline()) {
						this.cancel();
						p.removeMetadata(ENCHANTED_PRAYER_METAKEY, mPlugin);
					}
				}

			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	@Override
	public boolean runCheck() {
		return mPlayer.isSneaking() && !mPlayer.isOnGround();
	}

	/*
	 * Must be called for all players in the appropriate place in EntityDamageByEntityEvent!
	 */
	public static void onEntityAttack(Plugin plugin, Player player, LivingEntity damagee) {
		if (player.hasMetadata(ENCHANTED_PRAYER_METAKEY)) {
			World world = player.getWorld();
			int enchantedPrayer = player.getMetadata(ENCHANTED_PRAYER_METAKEY).get(0).asInt();
			player.removeMetadata(ENCHANTED_PRAYER_METAKEY, plugin);
			world.playSound(damagee.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 0.9f);
			world.playSound(damagee.getLocation(), Sound.ENTITY_BLAZE_DEATH, 1, 1.75f);
			world.spawnParticle(Particle.SPELL_INSTANT, damagee.getLocation().add(0, damagee.getHeight() / 2, 0), 100, 0.25f, 0.3f, 0.25f, 1);
			world.spawnParticle(Particle.FIREWORKS_SPARK, damagee.getLocation().add(0, damagee.getHeight() / 2, 0), 75, 0, 0, 0, 0.3);
			double damage = enchantedPrayer == 1 ? ENCHANTED_PRAYER_1_DAMAGE : ENCHANTED_PRAYER_2_DAMAGE;
			double heal = enchantedPrayer == 1 ? ENCHANTED_PRAYER_1_HEAL : ENCHANTED_PRAYER_2_HEAL;
			for (LivingEntity le : EntityUtils.getNearbyMobs(damagee.getLocation(), 3.5)) {
				EntityUtils.damageEntity(plugin, le, damage, player, MagicType.HOLY);
			}
			PlayerUtils.healPlayer(player, heal);
		}
	}
}

package com.playmonumenta.plugins.abilities.cleric.paladin;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

/*
* All attacks against undead deal +2/5 damage. Sneak and right-click while
* looking at the ground to charge your weapon with holy light for 15 seconds.
* Your next swing stuns non undead for 3s (slowness V weakness V) or deals +10
* damage to undead, and if it kills the undead, it explodes, dealing 10 damage
* to all mobs within 4 blocks. Cooldown 25/18s
*/

public class LuminousInfusion extends Ability {

	private static final String LUMINOUS_INFUSION_EXPIRATION_MESSAGE = "The light from your hands fades";
	private static final String LUMINOUS_INFUSION_TAG = "PlayerLuminousInfusion";
	private static final int LUMINOUS_INFUSION_ACTIVATION_ANGLE = 75;
	private static final double LUMINOUS_INFUSION_RADIUS = 4;
	private static final int LUMINOUS_INFUSION_EXPLOSION_DAMAGE = 10;
	private static final int LUMINOUS_INFUSION_UNDEAD_DAMAGE = 10;
	private static final int LUMINOUS_INFUSION_1_PASSIVE_DAMAGE = 2;
	private static final int LUMINOUS_INFUSION_2_PASSIVE_DAMAGE = 5;
	private static final int LUMINOUS_INFUSION_SLOWNESS_DURATION = 3 * 20;
	private static final int LUMINOUS_INFUSION_WEAKNESS_DURATION = 3 * 20;
	private static final int LUMINOUS_INFUSION_SLOWNESS_LEVEL = 4;
	private static final int LUMINOUS_INFUSION_WEAKNESS_LEVEL = 4;
	private static final int LUMINOUS_INFUSION_MAX_DURATION = 15 * 20;
	private static final int LUMINOUS_INFUSION_1_COOLDOWN = 25 * 20;
	private static final int LUMINOUS_INFUSION_2_COOLDOWN = 18 * 20;

	public LuminousInfusion(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.LUMINOUS_INFUSION;
		mInfo.scoreboardId = "LuminousInfusion";
		mInfo.cooldown = getAbilityScore() == 1 ? LUMINOUS_INFUSION_1_COOLDOWN : LUMINOUS_INFUSION_2_COOLDOWN;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public boolean runCheck() {
		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		return mPlayer.isSneaking() && !InventoryUtils.isBowItem(inMainHand) && mPlayer.getLocation().getPitch() > 75;
	}

	@Override
	public boolean cast() {
		mPlayer.setMetadata(LUMINOUS_INFUSION_TAG, new FixedMetadataValue(mPlugin, null));
		mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1, 1);
		mWorld.spawnParticle(Particle.SPELL_INSTANT, mPlayer.getLocation(), 50, 0.75f, 0.25f, 0.75f, 1);
		new BukkitRunnable() {
			int t = 0;

			@Override
			public void run() {
				t++;
				Location rightHand = PlayerUtils.getRightSide(mPlayer.getEyeLocation(), 0.45).subtract(0, .8, 0);
				Location leftHand = PlayerUtils.getRightSide(mPlayer.getEyeLocation(), -0.45).subtract(0, .8, 0);
				mWorld.spawnParticle(Particle.SPELL_INSTANT, leftHand, 2, 0.05f, 0.05f, 0.05f, 0);
				mWorld.spawnParticle(Particle.SPELL_INSTANT, rightHand, 2, 0.05f, 0.05f, 0.05f, 0);
				if (t > LUMINOUS_INFUSION_MAX_DURATION) {
					mPlayer.removeMetadata(LUMINOUS_INFUSION_TAG, mPlugin);
					MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, LUMINOUS_INFUSION_EXPIRATION_MESSAGE);
					this.cancel();
				}
				if (!mPlayer.hasMetadata(LUMINOUS_INFUSION_TAG)) {
					this.cancel();
				}
			}
		};

		return true;
	}

	public boolean LivingEntityDamagedByPlayerEvent(Player player, EntityDamageByEntityEvent event) {
		LivingEntity le = (LivingEntity) event.getEntity();
		if (EntityUtils.isUndead(le)) {
			int damage = getAbilityScore() == 1 ? LUMINOUS_INFUSION_1_PASSIVE_DAMAGE : LUMINOUS_INFUSION_2_PASSIVE_DAMAGE;
			event.setDamage(event.getFinalDamage() + damage);
			ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
			if (mPlayer.hasMetadata(LUMINOUS_INFUSION_TAG) && InventoryUtils.isBowItem(inMainHand)) {
				mPlayer.removeMetadata(LUMINOUS_INFUSION_TAG, mPlugin);
				event.setDamage(event.getFinalDamage() + LUMINOUS_INFUSION_UNDEAD_DAMAGE);

				new BukkitRunnable() {

					@Override
					public void run() {
						if (le.isDead()) {
							for (LivingEntity e : EntityUtils.getNearbyMobs(le.getLocation(), LUMINOUS_INFUSION_RADIUS)) {
								if (EntityUtils.isHostileMob(e)) {
									EntityUtils.damageEntity(mPlugin, e, LUMINOUS_INFUSION_EXPLOSION_DAMAGE, mPlayer);
									mWorld.spawnParticle(Particle.FIREWORKS_SPARK, e.getLocation(), 100, 0.05f, 0.05f, 0.05f, 0.3);
									mWorld.spawnParticle(Particle.FLAME, e.getLocation(), 75, 0.05f, 0.05f, 0.05f, 0.3);
									mWorld.playSound(e.getLocation(), Sound.ITEM_TOTEM_USE, 1, 1.1f);
								}
							}
						}
					}

				}.runTaskLater(mPlugin, 1);
			}
		} else if (EntityUtils.isHostileMob(le) && mPlayer.hasMetadata(LUMINOUS_INFUSION_TAG)) {
			mPlayer.removeMetadata(LUMINOUS_INFUSION_TAG, mPlugin);
			le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, LUMINOUS_INFUSION_WEAKNESS_DURATION, LUMINOUS_INFUSION_WEAKNESS_LEVEL, true, false));
			le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, LUMINOUS_INFUSION_SLOWNESS_DURATION, LUMINOUS_INFUSION_SLOWNESS_LEVEL, true, false));
		}

		return true;
	}
}
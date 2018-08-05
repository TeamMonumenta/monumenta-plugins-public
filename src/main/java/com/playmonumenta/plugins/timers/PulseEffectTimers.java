package com.playmonumenta.plugins.timers;

import java.util.List;
import java.util.Vector;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.metadata.FixedMetadataValue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.BaseClass;

public class PulseEffectTimers {
	class EffectInfo {
		public EffectInfo(Plugin plugin, Player player, BaseClass playerClass, int abilityID, String tagName, int duration, int cooldown, Location loc, int radius, boolean targetPlayers) {
			mPlugin = plugin;
			mOwner = player;
			mClass = playerClass;
			mAbilityID = abilityID;
			mTagName = tagName;
			mDuration = duration;
			mCooldown = cooldown;
			mLocation = loc;
			mRadius = radius;
			mTargetPlayers = targetPlayers;

			while(mLocation.getBlock().getType() != Material.AIR){
				mLocation.add(0, 0.25, 0);
			}

			World world = Bukkit.getWorld(player.getWorld().getName());
			mMarkerEntity = world.spawnEntity(loc, EntityType.ARMOR_STAND);
			ArmorStand armorStand = (ArmorStand)mMarkerEntity;
			armorStand.setInvulnerable(true);
			armorStand.setMarker(true);
			armorStand.setVisible(false);
			armorStand.setGravity(false);
		}

		public boolean Update(int tickPerUpdate) {
			mDuration -= tickPerUpdate;

			if (mDuration > 0) {
				if (mDuration % mCooldown == 0) {
					if (mRadius > 0) {
						List<Entity> entities = mMarkerEntity.getNearbyEntities(mRadius, mRadius, mRadius);

						for (Entity e : entities) {
							if (mTargetPlayers) {
								if (e instanceof Player) {
									Player player = (Player)e;
									mClass.PulseEffectApplyEffect(mOwner, mLocation, player, mAbilityID);
									player.setMetadata(mTagName, new FixedMetadataValue(mPlugin, 0));
									mPreviouslyEffected.add(player);
								}
							} else {
								if (e instanceof Entity) {
									if (!(e instanceof Player) && !(e instanceof Villager)) {
										mClass.PulseEffectApplyEffect(mOwner, mLocation, e, mAbilityID);
										mPreviouslyEffected.add(e);
									}
								}
							}
						}
					} else {
						mClass.PulseEffectApplyEffect(mOwner, mLocation, mMarkerEntity, mAbilityID);
					}
				}
			} else {
				return true;
			}

			return false;
		}

		public void Cleanup() {
			for (int i = 0; i < mPreviouslyEffected.size(); i++) {
				Entity e = mPreviouslyEffected.get(i);
				if (mTargetPlayers) {
					if (e instanceof Player) {
						Player p = (Player)e;
						p.removeMetadata(mTagName, mPlugin);
						mClass.PulseEffectRemoveEffect(mOwner, mLocation, p, mAbilityID);
					}
				} else {
					mClass.PulseEffectRemoveEffect(mOwner, mLocation, e, mAbilityID);
				}
			}

			mPreviouslyEffected.clear();
		}

		public void Complete() {
			mClass.PulseEffectComplete(mOwner, mLocation, mMarkerEntity, mAbilityID);
		}

		public void Remove() {
			mMarkerEntity.remove();
			mLocation.getBlock().setType(Material.AIR);
		}

		public Entity getMarkerEntity() {
			return mMarkerEntity;
		}

		Plugin mPlugin;
		Player mOwner;
		Entity mMarkerEntity;
		BaseClass mClass;
		int mAbilityID;
		String mTagName;
		int mDuration;
		int mCooldown;
		Location mLocation;
		int mRadius;
		boolean mTargetPlayers;

		Vector<Entity> mPreviouslyEffected = new Vector<Entity>();
	}

	private Vector<EffectInfo> mPulseEffects = null;
	private Plugin mPlugin = null;

	public PulseEffectTimers(Plugin plugin) {
		mPulseEffects = new Vector<>();
		mPlugin = plugin;
	}

	public void AddPulseEffect(Player player, BaseClass playerClass, int abilityID, String tagName, int duration, int cooldown, Location loc, int radius, boolean targetPlayers) {
		EffectInfo info = new EffectInfo(mPlugin, player, playerClass, abilityID, tagName, duration, cooldown, loc, radius, targetPlayers);
		mPulseEffects.add(info);
	}

	public void Update(int tickPerUpdate) {
		int size = mPulseEffects.size();
		for (int i = 0; i < size; i++) {
			EffectInfo info = mPulseEffects.get(i);

			//	Loop through and remove the effects from previously effected players.
			info.Cleanup();

			//	Loop through and apply the effects to the new people within range.
			boolean remove = info.Update(tickPerUpdate);
			if (remove) {
				info.Cleanup();
				info.Complete();
				info.Remove();
				mPulseEffects.remove(i);

				i--;
				size--;
			}
		}
	}
}

package com.playmonumenta.plugins.itemindex;

public class ItemIndexFilter {
	public enum Type {
		MATERIAL,
		MATERIAL_EXCLUDE,
	}

	private final Type mType;
	private final Object mValue;

	ItemIndexFilter(Type type, Object value) {
		this.mType = type;
		this.mValue = value;
	}

	public boolean match(MonumentaItem item) {
		switch (this.mType) {
		case MATERIAL:
			return item.getMaterial() == this.mValue;
		case MATERIAL_EXCLUDE:
			return item.getMaterial() != this.mValue;
		default:
			return true;
		}
	}
}

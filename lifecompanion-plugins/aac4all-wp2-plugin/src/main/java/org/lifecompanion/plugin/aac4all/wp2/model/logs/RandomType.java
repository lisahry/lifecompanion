package org.lifecompanion.plugin.aac4all.wp2.model.logs;

import java.util.List;

public enum RandomType {
    RANDOM_1(List.of(KeyboardType.STATIC, KeyboardType.REOLOC_G)),
    RANDOM_2(List.of(KeyboardType.REOLOC_G, KeyboardType.STATIC));

    private final List<KeyboardType> keyboards;

    RandomType(List<KeyboardType> keyboards) {
        this.keyboards = keyboards;
    }

    public List<KeyboardType> getKeyboards() {
        return keyboards;
    }
}

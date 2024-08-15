package org.lifecompanion.plugin.aac4all.wp2.model.logs;

import java.util.List;

public enum RandomType {
   RANDOM_REOLOC_1(List.of(KeyboardType.STATIC, KeyboardType.REOLOC_G,KeyboardType.REOLOC_L)),
   RANDOM_REOLOC_2(List.of(KeyboardType.STATIC, KeyboardType.REOLOC_L,KeyboardType.REOLOC_G)),
    RANDOM_REOLOC_3(List.of(KeyboardType.REOLOC_G, KeyboardType.STATIC,KeyboardType.REOLOC_L)),
    RANDOM_REOLOC_4(List.of(KeyboardType.REOLOC_G, KeyboardType.REOLOC_L,KeyboardType.STATIC)),
    RANDOM_REOLOC_5(List.of(KeyboardType.REOLOC_L, KeyboardType.REOLOC_G,KeyboardType.STATIC)),
    RANDOM_REOLOC_6(List.of(KeyboardType.REOLOC_L, KeyboardType.STATIC,KeyboardType.REOLOC_G)),
    RANDOM_CURSTA_1(List.of(KeyboardType.CUR_STA, KeyboardType.DY_LIN)),
    RANDOM_CURSTA_2(List.of(KeyboardType.DY_LIN, KeyboardType.CUR_STA));


    private final List<KeyboardType> keyboards;

    RandomType(List<KeyboardType> keyboards) {
        this.keyboards = keyboards;
    }

    public List<KeyboardType> getKeyboards() {
        return keyboards;
    }

    public void set(RandomType value) {
    }


}

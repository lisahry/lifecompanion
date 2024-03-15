package org.lifecompanion.model.impl.textprediction.aac4all;

import org.lifecompanion.controller.textcomponent.WritingStateController;
import org.lifecompanion.model.api.configurationcomponent.GridComponentI;
import org.lifecompanion.model.api.configurationcomponent.GridPartComponentI;
import org.lifecompanion.model.api.configurationcomponent.GridPartKeyComponentI;
import org.lifecompanion.model.api.configurationcomponent.LCConfigurationI;
import org.lifecompanion.model.api.lifecycle.ModeListenerI;
import org.lifecompanion.model.api.selectionmode.ComponentToScanI;
import org.lifecompanion.model.impl.configurationcomponent.keyoption.CustomCharKeyOption;
import org.lifecompanion.model.impl.textprediction.charprediction.LCCharPredictor;
import org.lifecompanion.util.javafx.FXThreadUtils;

import java.util.*;
import java.util.stream.Collectors;

public enum Aac4AllController implements ModeListenerI {
    INSTANCE;

    @Override
    public void modeStart(LCConfigurationI configuration) {

    }

    @Override
    public void modeStop(LCConfigurationI configuration) {

    }

    private Map<CustomCharKeyOption, String> previousLine;

    public void partScanComponentChanged(GridComponentI gridComponent, ComponentToScanI selectedComponentToScan) {
        FXThreadUtils.runOnFXThread(() -> {
            if (selectedComponentToScan == null) {
                // Should reset previous line to default configuration
                if (previousLine != null) {
                    previousLine.forEach((customCharKeyOption, previousValue) -> {
                        customCharKeyOption.predictionProperty().set(previousValue);
                    });
                }
            } else {
                // Should organize the keys with char prediction
                previousLine = new HashMap<>();
                for (int i = 0; i < selectedComponentToScan.getComponents().size(); i++) {
                    GridPartComponentI gridPartComponent = selectedComponentToScan.getPartIn(gridComponent, i);
                    if (gridPartComponent instanceof GridPartKeyComponentI) {
                        GridPartKeyComponentI key = (GridPartKeyComponentI) gridPartComponent;
                        if (key.keyOptionProperty().get() instanceof CustomCharKeyOption) {
                            CustomCharKeyOption customCharKeyOption = (CustomCharKeyOption) key.keyOptionProperty().get();
                            previousLine.put(customCharKeyOption, customCharKeyOption.predictionProperty().get());
                            customCharKeyOption.predictionProperty().set("Case " + (i + 1));
                        }
                    }
                }
                HashSet<Character> collect = new HashSet<>("abcdefgj".chars().mapToObj(c -> (char) c).collect(Collectors.toSet()));
                String textBeforeCaret = WritingStateController.INSTANCE.textBeforeCaretProperty().get();
                List<Character> predict = LCCharPredictor.INSTANCE.predict("bon", 10, collect);
                System.out.println(predict);
            }
        });
    }
}

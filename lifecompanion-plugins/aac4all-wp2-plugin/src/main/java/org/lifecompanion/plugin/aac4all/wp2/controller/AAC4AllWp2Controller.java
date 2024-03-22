package org.lifecompanion.plugin.aac4all.wp2.controller;

import org.lifecompanion.controller.selectionmode.SelectionModeController;
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
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public enum AAC4AllWp2Controller implements ModeListenerI {
    INSTANCE;

    private final BiConsumer<GridComponentI, ComponentToScanI> scannedPartChangedListener;

    AAC4AllWp2Controller() {
        scannedPartChangedListener = this::partScanComponentChanged;
    }

    @Override
    public void modeStart(LCConfigurationI configuration) {
        SelectionModeController.INSTANCE.addScannedPartChangedListeners(this.scannedPartChangedListener);
    }

    @Override
    public void modeStop(LCConfigurationI configuration) {
        SelectionModeController.INSTANCE.removeScannedPartChangedListeners(this.scannedPartChangedListener);
    }

    private Map<CustomCharKeyOption, String> previousLine;

    public void partScanComponentChanged(GridComponentI gridComponent, ComponentToScanI selectedComponentToScan) {
        System.out.println("Scanned part changed " + gridComponent + " : " + selectedComponentToScan);
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
                String charsPreviousLine = "";
                int indexOfFirtCustomCharKey = 0;
                boolean stop = false;

                //saving the configuration of the line.
                for (int i = 0; i < selectedComponentToScan.getComponents().size(); i++) {
                    GridPartComponentI gridPartComponent = selectedComponentToScan.getPartIn(gridComponent, i);
                    if (!stop) {
                        indexOfFirtCustomCharKey = indexOfFirtCustomCharKey + 1;
                    }// to keep the index of the first custom pred key.

                    if (gridPartComponent instanceof GridPartKeyComponentI) {
                        GridPartKeyComponentI key = (GridPartKeyComponentI) gridPartComponent;
                        if (key.keyOptionProperty().get() instanceof CustomCharKeyOption) {
                            stop = true;
                            CustomCharKeyOption customCharKeyOption = (CustomCharKeyOption) key.keyOptionProperty().get();
                            previousLine.put(customCharKeyOption, customCharKeyOption.predictionProperty().get());
                            charsPreviousLine = charsPreviousLine + customCharKeyOption.predictionProperty().get();
                        }
                    }
                }

                //previousLine.forEach((customCharKeyOption, previousValue)-> {
                //charsPreviousLine = charsPreviousLine +previousValue;
                //});
                HashSet<Character> acceptedCharact = new HashSet<>(charsPreviousLine.chars().mapToObj(c -> (char) c).collect(Collectors.toSet()));
                List<Character> predict = LCCharPredictor.INSTANCE.predict(WritingStateController.INSTANCE.textBeforeCaretProperty().get(), acceptedCharact.size(), acceptedCharact);

                //modifing the line with character predictionwha
                for (int i = 0; i < selectedComponentToScan.getComponents().size(); i++) {
                    GridPartComponentI gridPartComponent = selectedComponentToScan.getPartIn(gridComponent, i);
                    if (gridPartComponent instanceof GridPartKeyComponentI) {
                        GridPartKeyComponentI key = (GridPartKeyComponentI) gridPartComponent;
                        if (key.keyOptionProperty().get() instanceof CustomCharKeyOption) {
                            CustomCharKeyOption customCharKeyOption = (CustomCharKeyOption) key.keyOptionProperty().get();
                            customCharKeyOption.predictionProperty().set(String.valueOf(predict.get(i - (indexOfFirtCustomCharKey - 1))));
                        }
                    }
                }

            }
        });
    }
}

package org.lifecompanion.plugin.aac4all.wp2.controller;

import org.lifecompanion.controller.selectionmode.SelectionModeController;
import org.lifecompanion.controller.textcomponent.WritingStateController;
import org.lifecompanion.model.api.configurationcomponent.GridComponentI;
import org.lifecompanion.model.api.configurationcomponent.GridPartComponentI;
import org.lifecompanion.model.api.configurationcomponent.GridPartKeyComponentI;
import org.lifecompanion.model.api.configurationcomponent.LCConfigurationI;
import org.lifecompanion.model.api.lifecycle.ModeListenerI;
import org.lifecompanion.model.api.selectionmode.ComponentToScanI;
import org.lifecompanion.model.api.selectionmode.SelectionModeI;
import org.lifecompanion.model.impl.selectionmode.AbstractPartScanSelectionMode;
import org.lifecompanion.model.impl.textprediction.charprediction.LCCharPredictor;
import org.lifecompanion.plugin.aac4all.wp2.model.keyoption.AAC4AllKeyOptionReolocL;
import org.lifecompanion.util.javafx.FXThreadUtils;
import org.lifecompanion.util.model.SelectionModeUtils;

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
        initRelocG(configuration);
    }

    private void initRelocG(LCConfigurationI configuration) {
        WritingStateController.INSTANCE.textBeforeCaretProperty().addListener((obs, ov, nv) -> {
            SelectionModeI selectionMode = configuration.selectionModeProperty().get();
            if (selectionMode != null && selectionMode.currentGridProperty().get() != null) {
                List<ComponentToScanI> rows = SelectionModeUtils.getRowColumnScanningComponents(selectionMode.currentGridProperty().get(), false);
                System.out.println("Lignes dans la grille : " + rows.size());
            }
        });
    }

    @Override
    public void modeStop(LCConfigurationI configuration) {
        SelectionModeController.INSTANCE.removeScannedPartChangedListeners(this.scannedPartChangedListener);
    }

    private Map<AAC4AllKeyOptionReolocL, String> previousLine;

    public void partScanComponentChanged(GridComponentI gridComponent, ComponentToScanI selectedComponentToScan) {
        System.out.println("Scanned part changed " + gridComponent + " : " + selectedComponentToScan);
        FXThreadUtils.runOnFXThread(() -> {
            if (selectedComponentToScan == null) {
                // Should reset previous line to default configuration
                if (previousLine != null) {
                    previousLine.forEach((aac4AllKeyOptionReolocL, previousValue) -> aac4AllKeyOptionReolocL.predictionProperty().set(previousValue));
                }
            } else {
                // Should organize the keys with char prediction
                previousLine = new HashMap<>();
                String charsPreviousLine = "";

                //saving the configuration of the line.
                for (int i = 0; i < selectedComponentToScan.getComponents().size(); i++) {
                    GridPartComponentI gridPartComponent = selectedComponentToScan.getPartIn(gridComponent, i);
                    if (gridPartComponent instanceof GridPartKeyComponentI key) {
                        if (key.keyOptionProperty().get() instanceof AAC4AllKeyOptionReolocL aac4AllKeyOptionReolocL) {
                            previousLine.put(aac4AllKeyOptionReolocL, aac4AllKeyOptionReolocL.predictionProperty().get());
                            charsPreviousLine = charsPreviousLine + aac4AllKeyOptionReolocL.predictionProperty().get();
                        }
                    }
                }

                HashSet<Character> acceptedCharact = new HashSet<>(charsPreviousLine.chars().mapToObj(c -> (char) c).collect(Collectors.toSet()));
                List<Character> predict = LCCharPredictor.INSTANCE.predict(WritingStateController.INSTANCE.textBeforeCaretProperty().get(), acceptedCharact.size(), acceptedCharact);

                //modifing the line with character predictionwha
                int indexPosition = 0; // for save index of prediction for RéoLoc keys
                for (int i = 0; i < selectedComponentToScan.getComponents().size(); i++) {
                    GridPartComponentI gridPartComponent = selectedComponentToScan.getPartIn(gridComponent, i);
                    if (gridPartComponent instanceof GridPartKeyComponentI key) {
                        if (key.keyOptionProperty().get() instanceof AAC4AllKeyOptionReolocL aac4AllKeyOptionReolocL) {
                            aac4AllKeyOptionReolocL.predictionProperty().set(String.valueOf(predict.get(i - indexPosition)));
                        } else indexPosition++;
                    }
                }

            }
        });
    }
}

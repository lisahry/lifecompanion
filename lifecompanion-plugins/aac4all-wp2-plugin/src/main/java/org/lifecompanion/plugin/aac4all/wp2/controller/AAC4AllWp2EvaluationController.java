package org.lifecompanion.plugin.aac4all.wp2.controller;

import javafx.beans.property.BooleanProperty;
import org.lifecompanion.model.api.configurationcomponent.LCConfigurationI;
import org.lifecompanion.model.api.lifecycle.ModeListenerI;
import org.lifecompanion.controller.selectionmode.SelectionModeController;
import org.lifecompanion.model.api.configurationcomponent.GridComponentI;
import org.lifecompanion.model.api.configurationcomponent.GridPartComponentI;
import java.util.Comparator;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.List;

public enum AAC4AllWp2EvaluationController implements ModeListenerI {
    INSTANCE;

    private BooleanProperty evaluationRunning;

    AAC4AllWp2EvaluationController() {
    }

    private LCConfigurationI configuration;

    @Override
    public void modeStart(LCConfigurationI configuration) {
        this.configuration = configuration;
    }

    @Override
    public void modeStop(LCConfigurationI configuration) {
        this.configuration = null;
    }

    public void startDailyEvaluation() {
        // TODO : démarrer chrono etc.

        List<GridPartComponentI> grids = this.configuration.getAllComponent().values().stream()
                .filter(d -> d instanceof GridPartComponentI)
                .filter(c -> c.nameProperty().get().startsWith("Clavier"))
                .map(c -> (GridPartComponentI)c)
                .collect(Collectors.toList());
        GridPartComponentI evalKeyboard = grids.get(new Random().nextInt(grids.size()));
        SelectionModeController.INSTANCE.goToGridPart(evalKeyboard);

    }
}

package org.lifecompanion.plugin.aac4all.wp2.controller;

import javafx.beans.property.BooleanProperty;
import org.lifecompanion.controller.textcomponent.WritingStateController;
import org.lifecompanion.model.api.configurationcomponent.LCConfigurationI;
import org.lifecompanion.model.api.configurationcomponent.SoundResourceHolderI;
import org.lifecompanion.model.api.lifecycle.ModeListenerI;
import org.lifecompanion.controller.selectionmode.SelectionModeController;
import org.lifecompanion.model.api.configurationcomponent.GridComponentI;
import org.lifecompanion.model.api.configurationcomponent.GridPartComponentI;
import org.lifecompanion.model.api.textcomponent.WritingEventSource;

import java.util.*;
import java.util.stream.Collectors;

public enum AAC4AllWp2EvaluationController implements ModeListenerI {
    INSTANCE;

    private BooleanProperty evaluationRunning;
    private List<GridPartComponentI> keyboards;
    private GridPartComponentI firstEvalKeyboard;


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
        keyboards = this.configuration.getAllComponent().values().stream()
                .filter(d -> d instanceof GridPartComponentI)
                .filter(c -> c.nameProperty().get().startsWith("Consignes"))
                .map(c -> (GridPartComponentI)c)
                .collect(Collectors.toList());

        if (!keyboards.isEmpty()){
        int indexFirstTrainingKeyboard = new Random().nextInt(keyboards.size());
        GridPartComponentI evalKeyboard = keyboards.get(indexFirstTrainingKeyboard);
        SelectionModeController.INSTANCE.goToGridPart(evalKeyboard);
        keyboards.remove(indexFirstTrainingKeyboard);
        }
    }

    public void nextDailyEvaluation() {

        if (!keyboards.isEmpty()){
            int indexTrainingKeyboard = new Random().nextInt(keyboards.size());
            GridPartComponentI training = keyboards.get(indexTrainingKeyboard);
            SelectionModeController.INSTANCE.goToGridPart(training);
            keyboards.remove(indexTrainingKeyboard);
        }
        else {
            System.out.println("c'est fini ");
        }
    }

    public void startEvaluation(){
        // TODO : lancer les claviers selon l'ordre de l'ID
        //récupérer l'id de l'utilisateur


    }
    public void startTraining(){

        // TODO: clean l'éditeur
        WritingStateController.INSTANCE.removeAll(WritingEventSource.USER_ACTIONS);


        List<GridPartComponentI> EVA =this.configuration.getAllComponent().values().stream()
                .filter(d -> d instanceof GridPartComponentI)
                .filter(c -> c.nameProperty().get().startsWith("EVA"))
                .map(c -> (GridPartComponentI)c)
                .collect(Collectors.toList()); // trouver un moyen d'aller à l'EVA plus facielement

        // TODO : chrono 10 mins
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                // aller aux EVA
                SelectionModeController.INSTANCE.goToGridPart(EVA.get(0));
                //clean l'éditeur
                WritingStateController.INSTANCE.removeAll(WritingEventSource.USER_ACTIONS);
                timer.cancel();
            }
        };
        long delay = 10 * 60 * 1000;
        timer.schedule(timerTask, delay);


        // TODO : démarer le listener log


    }
}

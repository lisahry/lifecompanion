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
import org.lifecompanion.plugin.aac4all.wp2.model.logs.*;
import org.lifecompanion.controller.io.*;
import org.lifecompanion.controller.usevariable.*;

import java.util.*;
import java.util.stream.Collectors;

public enum AAC4AllWp2EvaluationController implements ModeListenerI {
    INSTANCE;

    private final long EVALUATION_DURATION_MS = (long) 20 * 1000;

    private BooleanProperty evaluationRunning;
    private List<GridPartComponentI> keyboards;
    private GridPartComponentI firstEvalKeyboard;

    private WP2Evaluation currentEvaluation;
    private WP2KeyboardEvaluation currentKeyboardEvaluation;

    private String currentSentence = "";


    AAC4AllWp2EvaluationController() {
    }

    public String getCurrentSentence() {
        return currentSentence;
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
                .map(c -> (GridPartComponentI) c)
                .collect(Collectors.toList());

        currentEvaluation = new WP2Evaluation(new Date());

        goToNextKeyboardToEvaluate();
    }

    private boolean goToNextKeyboardToEvaluate() {
        if (!keyboards.isEmpty()) {
            int indexFirstTrainingKeyboard = new Random().nextInt(keyboards.size());
            GridPartComponentI evalKeyboard = keyboards.get(indexFirstTrainingKeyboard);
            currentKeyboardEvaluation = new WP2KeyboardEvaluation(KeyboardType.REOLOC_G);
            SelectionModeController.INSTANCE.goToGridPart(evalKeyboard);
            keyboards.remove(indexFirstTrainingKeyboard);

            currentSentence = "ceci est la première phrase";

            return true;
        }
        return false;
    }

    public void nextDailyEvaluation() {
        if (!goToNextKeyboardToEvaluate()) {
            System.out.println("c'est fini ");
        }
    }

    public void startEvaluation() {
        // TODO : lancer les claviers selon l'ordre de l'ID
        //récupérer l'id de l'utilisateur


    }

    public void startTraining() {

        // TODO: clean l'éditeur
        WritingStateController.INSTANCE.removeAll(WritingEventSource.USER_ACTIONS);


        List<GridPartComponentI> EVA = this.configuration.getAllComponent().values().stream()
                .filter(d -> d instanceof GridPartComponentI)
                .filter(c -> c.nameProperty().get().startsWith("EVA"))
                .map(c -> (GridPartComponentI) c)
                .collect(Collectors.toList()); // trouver un moyen d'aller à l'EVA plus facielement

        WritingStateController.INSTANCE.currentWordProperty().addListener((obs, ov, nv) -> {
            currentKeyboardEvaluation.getLogs().add(new WP2Logs(new Date(), LogType.EYETRACKING_POSITION, new EyetrackingPosition(455, 555)));
        });

        // TODO : chrono 10 mins
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                // aller aux EVA
                SelectionModeController.INSTANCE.goToGridPart(EVA.get(0));
                //clean l'éditeur
                WritingStateController.INSTANCE.removeAll(WritingEventSource.USER_ACTIONS);
                currentSentence = "";
                UseVariableController.INSTANCE.requestVariablesUpdate();
                timer.cancel();

                currentKeyboardEvaluation.setFatigueScore(50);
                currentKeyboardEvaluation.setSatisfactionScore(10);
                currentEvaluation.getEvaluations().add(currentKeyboardEvaluation);
                currentKeyboardEvaluation = null;

                System.out.println(JsonHelper.GSON.toJson(currentEvaluation));
            }
        };
        timer.schedule(timerTask, EVALUATION_DURATION_MS);


        // TODO : démarer le listener log


    }
}

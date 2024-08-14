package org.lifecompanion.plugin.aac4all.wp2.controller;

import javafx.beans.property.BooleanProperty;
import org.lifecompanion.controller.io.JsonHelper;
import org.lifecompanion.controller.resource.ResourceHelper;
import org.lifecompanion.controller.selectionmode.SelectionModeController;
import org.lifecompanion.controller.textcomponent.WritingStateController;
import org.lifecompanion.controller.usevariable.UseVariableController;
import org.lifecompanion.framework.commons.translation.Translation;
import org.lifecompanion.framework.commons.utils.lang.StringUtils;
import org.lifecompanion.model.api.configurationcomponent.GridPartComponentI;
import org.lifecompanion.model.api.configurationcomponent.LCConfigurationI;
import org.lifecompanion.model.api.lifecycle.ModeListenerI;
import org.lifecompanion.model.api.textcomponent.WritingEventSource;
import org.lifecompanion.plugin.aac4all.wp2.AAC4AllWp2PluginProperties;
import org.lifecompanion.plugin.aac4all.wp2.model.logs.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public enum AAC4AllWp2EvaluationController implements ModeListenerI {
    INSTANCE;

    private final long EVALUATION_DURATION_MS = (long) 20 * 1000;
    private AAC4AllWp2PluginProperties currentAAC4AllWp2PluginProperties;
    private BooleanProperty evaluationRunning;

    private WP2Evaluation currentEvaluation;
    private WP2KeyboardEvaluation currentKeyboardEvaluation;
    private final List<String> phraseSetFR;

    private String currentSentence = "";

    private String functionalCurrentKeyboard = "";
    private GridPartComponentI keyboardConsigne;
    private GridPartComponentI keyboardEVA;

    private Map<KeyboardType, GridPartComponentI> keyboardsMap;
    private RandomType randomType;
    private int currentRandomIndex;
    private KeyboardType currentKeyboardType;

    public String getFunctionalCurrentKeyboard() {
        return functionalCurrentKeyboard;
    }

    private String instructionCurrentKeyboard = "";

    public String getInstructionCurrentKeyboard() {
        return instructionCurrentKeyboard;
    }

    private GridPartComponentI currentKeyboard;

    public GridPartComponentI getCurrentKeyboard() {
        return currentKeyboard;
    }


    AAC4AllWp2EvaluationController() {
        phraseSetFR = new ArrayList<>();
        try (Scanner scan = new Scanner(ResourceHelper.getInputStreamForPath("/text/PhraseSetFR.txt"), StandardCharsets.UTF_8)) {
            while (scan.hasNextLine()) {
                phraseSetFR.add(StringUtils.trimToEmpty(scan.nextLine()));
            }
        }
    }

    public String getCurrentSentence() {
        return currentSentence;
    }

    private LCConfigurationI configuration;


    @Override
    public void modeStart(LCConfigurationI configuration) {
        this.configuration = configuration;
        this.keyboardConsigne = this.configuration.getAllComponent().values().stream()
                .filter(d -> d instanceof GridPartComponentI)
                .filter(c -> c.nameProperty().get().startsWith("Consigne"))
                .map(c -> (GridPartComponentI) c)
                .findAny().orElse(null);

        this.keyboardEVA =this.configuration.getAllComponent().values().stream()
                .filter(d -> d instanceof GridPartComponentI)
                .filter(c -> c.nameProperty().get().startsWith("EVA"))
                .map(c -> (GridPartComponentI) c)
                .findAny().orElse(null);


        KeyboardType[] values = KeyboardType.values();
        keyboardsMap = new HashMap<>();
        for (KeyboardType keyboardType : values) {
            GridPartComponentI keyboard = this.configuration.getAllComponent().values().stream()
                    .filter(d -> d instanceof GridPartComponentI)
                    .filter(c -> c.nameProperty().get().startsWith(keyboardType.getGridName()))
                    .map(c -> (GridPartComponentI) c)
                    .findAny().orElse(null);
            if (keyboard != null) {
                keyboardsMap.put(keyboardType, keyboard);
            }
        }

    }

    @Override
    public void modeStop(LCConfigurationI configuration) {
        this.configuration = null;
    }

    public void startDailyEvaluation() {
        randomType = RandomType.RANDOM_1_1; // TODO stocker dans properties
        currentRandomIndex = 0;
        currentEvaluation = new WP2Evaluation(new Date());
        System.out.println(phraseSetFR);

        goToNextKeyboardToEvaluate();

    }

    private boolean goToNextKeyboardToEvaluate() {
        if (currentRandomIndex < randomType.getKeyboards().size()) {
            this.currentKeyboardType = randomType.getKeyboards().get(currentRandomIndex++);
            currentKeyboard = keyboardsMap.get(currentKeyboardType);
            updatevariables();
            return true;
        }
        return false;
    }

    public void updatevariables() {
        currentKeyboardEvaluation = new WP2KeyboardEvaluation(KeyboardType.REOLOC_G); // ??? pour les logs ? je ne sais plus
        functionalCurrentKeyboard = Translation.getText("aac4all.wp2.plugin.functional.description." + currentKeyboardType.getTranslationId());
        instructionCurrentKeyboard = Translation.getText("aac4all.wp2.plugin.instruction.description." + currentKeyboardType.getTranslationId());
        UseVariableController.INSTANCE.requestVariablesUpdate();
    }

    public void nextDailyEvaluation() {
        currentEvaluation.getEvaluations().add(currentKeyboardEvaluation);

        currentKeyboardEvaluation = null;

        if (!goToNextKeyboardToEvaluate()) {
            System.out.println("c'est fini ");
        } else {
            SelectionModeController.INSTANCE.goToGridPart(keyboardConsigne);
        }
    }

    public void startEvaluation() {
        // TODO : lancer les claviers selon l'ordre de l'ID
        //récupérer l'id de l'utilisateur
        String patientID = String.valueOf(currentAAC4AllWp2PluginProperties.patientIdProperty());

    }

    public void setEvaFatigueScore(int score){
        currentKeyboardEvaluation.setFatigueScore(score);
        System.out.println("Fatigue avec score de "+ currentKeyboardEvaluation.getFatigueScore());
    }
    public void setEvaSatisfactionScore(Integer score){
        currentKeyboardEvaluation.setSatisfactionScore(score);
        System.out.println("Satisfaction avec score de "+  currentKeyboardEvaluation.getSatisfactionScore());

    }

    public void startTraining() {
        // TODO: go to currentKeyboardEvaluation
        SelectionModeController.INSTANCE.goToGridPart(currentKeyboard);

        // TODO: clean l'éditeur
        WritingStateController.INSTANCE.removeAll(WritingEventSource.USER_ACTIONS);

        // TODO : démarer le listener log
        WritingStateController.INSTANCE.currentWordProperty().addListener((obs, ov, nv) -> {
            currentKeyboardEvaluation.getLogs().add(new WP2Logs(new Date(), LogType.EYETRACKING_POSITION, new EyetrackingPosition(455, 555)));
        }); // Pour les logs ex: à chaque nouveau mot saisi on récupère la position de l'eye tracking

        SelectionModeController.INSTANCE.addScannedPartChangedListeners((gridComponentI, componentToScanI) -> {
            System.out.println("Changement de ligne :" + componentToScanI);
        });

        SelectionModeController.INSTANCE.currentOverPartProperty().addListener((obs, ov, nv) -> {
            System.out.println("Changement de case :" + nv);
        });

        //TODO : affiche les phrases à saisir
        StartDislaySentence();

        // TODO : chrono 10 mins
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                // go to EVA interface
                SelectionModeController.INSTANCE.goToGridPart(keyboardEVA);
                //stop sentence display and clean editor
                StopDislaySentence();

                timer.cancel();


                System.out.println(JsonHelper.GSON.toJson(currentEvaluation));
            }
        };
        timer.schedule(timerTask, EVALUATION_DURATION_MS);
    }


    public void StartDislaySentence() {
        currentSentence = phraseSetFR.get(new Random().nextInt(phraseSetFR.size()));
        UseVariableController.INSTANCE.requestVariablesUpdate();
        WritingStateController.INSTANCE.removeAll(WritingEventSource.USER_ACTIONS);
    }

    public void StopDislaySentence() {
        currentSentence = "";
        UseVariableController.INSTANCE.requestVariablesUpdate();
        WritingStateController.INSTANCE.removeAll(WritingEventSource.USER_ACTIONS);
    }


    //
}

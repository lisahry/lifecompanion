package org.lifecompanion.plugin.aac4all.wp2.controller;

import javafx.beans.property.BooleanProperty;
import org.lifecompanion.controller.textcomponent.WritingStateController;
import org.lifecompanion.framework.commons.translation.Translation;
import org.lifecompanion.model.api.configurationcomponent.*;
import org.lifecompanion.model.api.lifecycle.ModeListenerI;
import org.lifecompanion.controller.selectionmode.SelectionModeController;
import org.lifecompanion.model.api.textcomponent.WritingEventSource;
import org.lifecompanion.plugin.aac4all.wp2.AAC4AllWp2PluginProperties;
import org.lifecompanion.plugin.aac4all.wp2.model.logs.*;
import org.lifecompanion.controller.io.*;
import org.lifecompanion.controller.usevariable.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public enum AAC4AllWp2EvaluationController implements ModeListenerI {
    INSTANCE;

    private final long EVALUATION_DURATION_MS = (long) 20 * 1000;
    private AAC4AllWp2PluginProperties currentAAC4AllWp2PluginProperties;
    private BooleanProperty evaluationRunning;
    private List<GridPartComponentI> keyboards;
    private GridPartComponentI firstEvalKeyboard;

    private WP2Evaluation currentEvaluation;
    private WP2KeyboardEvaluation currentKeyboardEvaluation;
    private List<String> phraseSetFR = fileToList("C:\\Users\\lhoiry\\Documents\\lifecompanion-main\\lifecompanion-plugins\\aac4all-wp2-plugin\\src\\main\\resources\\text\\PhraseSetFR.txt");//Arrays.asList("elle est directement allée te voir","du moment que tu le vis bien","ils aiment ni les pommes ni les ananas"); // il faudra l'alimenter par un fichier plus tard
    public static List<String> fileToList(String filePath) {
        List<String> lines = null;
        Path path = Paths.get(filePath);
        try {
            // Lire toutes les lignes du fichier et les stocker dans la liste
            lines = Files.readAllLines(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    private String currentSentence = "";

    private String functionalCurrentKeyboard = "";
    public String getFunctionalCurrentKeyboard() { return functionalCurrentKeyboard;    }

    private String instructionCurrentKeyboard ="";
    public String getInstructionCurrentKeyboard() {return instructionCurrentKeyboard;   }

    private GridPartComponentI currentKeyboard;
    public GridPartComponentI getCurrentKeyboard() {
        return currentKeyboard;
    }



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
                .filter(c -> c.nameProperty().get().startsWith("Clavier"))
                .map(c -> (GridPartComponentI) c)
                .collect(Collectors.toList());
        System.out.println(keyboards);
        currentEvaluation = new WP2Evaluation(new Date());
        System.out.println(phraseSetFR);

        goToNextKeyboardToEvaluate();

    }

    private boolean goToNextKeyboardToEvaluate() {
        if (!keyboards.isEmpty()) {
            //random define of currentkeyboard
            int indexTrainingKeyboard = new Random().nextInt(keyboards.size());
            currentKeyboard = keyboards.get(indexTrainingKeyboard);
            keyboards.remove(indexTrainingKeyboard);//update list of keyboards still to be tested
            updatevariables();
            return true;
        }
        return false;
    }
    public void updatevariables(){
        currentKeyboardEvaluation = new WP2KeyboardEvaluation(KeyboardType.REOLOC_G); // ??? pour les logs ? je ne sais plus

        //update description variables according to the current keyboard
        if(currentKeyboard.nameProperty().get().startsWith("Clavier RéoLocG")){
            functionalCurrentKeyboard=Translation.getText("aac4all.wp2.plugin.functional.description.RéoLocG");
            instructionCurrentKeyboard= Translation.getText("aac4all.wp2.plugin.instruction.description.RéoLocG");
        }
        if(currentKeyboard.nameProperty().get().startsWith("Clavier Statique")){
            functionalCurrentKeyboard=Translation.getText("aac4all.wp2.plugin.functional.description.Statique");
            instructionCurrentKeyboard= Translation.getText("aac4all.wp2.plugin.instruction.description.Statique");
        }
        if(currentKeyboard.nameProperty().get().startsWith("Clavier RéoLocL")){
            functionalCurrentKeyboard=Translation.getText("aac4all.wp2.plugin.functional.description.RéoLocL");
            instructionCurrentKeyboard= Translation.getText("aac4all.wp2.plugin.instruction.description.RéoLocL");
        }
        /*
         if(currentKeyboard.nameProperty().get().startsWith("Clavier CurSta")){
            functionalCurrentKeyboard=Translation.getText("aac4all.wp2.plugin.functional.description.CurSta");
            instructionCurrentKeyboard= Translation.getText("aac4all.wp2.plugin.instruction.description.CurSta");
        }
        if(currentKeyboard.nameProperty().get().startsWith("Clavier DyLin")){
            functionalCurrentKeyboard=Translation.getText("aac4all.wp2.plugin.functional.description.DyLin");
            instructionCurrentKeyboard= Translation.getText("aac4all.wp2.plugin.instruction.description.DyLin");
        }

        */
        UseVariableController.INSTANCE.requestVariablesUpdate();
    }

    public void nextDailyEvaluation() {
        if (!goToNextKeyboardToEvaluate()) {
            System.out.println("c'est fini ");
        }
        else {
            List<GridPartComponentI> consigne =this.configuration.getAllComponent().values().stream()
                    .filter(d -> d instanceof GridPartComponentI)
                    .filter(c -> c.nameProperty().get().startsWith("Consigne"))
                    .map(c -> (GridPartComponentI) c)
                    .collect(Collectors.toList());// trouver un moyen d'aller à Consigne plus facilement
            SelectionModeController.INSTANCE.goToGridPart(consigne.get(0));

        }
    }

    public void startEvaluation() {
        // TODO : lancer les claviers selon l'ordre de l'ID
        //récupérer l'id de l'utilisateur
        String patientID = String.valueOf(currentAAC4AllWp2PluginProperties.patientIdProperty());

    }


    public void startTraining() {
        // TODO: go to currentKeyboardEvaluation
        SelectionModeController.INSTANCE.goToGridPart(currentKeyboard);

        // TODO: clean l'éditeur
        WritingStateController.INSTANCE.removeAll(WritingEventSource.USER_ACTIONS);

        List<GridPartComponentI> EVA = this.configuration.getAllComponent().values().stream()
                .filter(d -> d instanceof GridPartComponentI)
                .filter(c -> c.nameProperty().get().startsWith("EVA"))
                .map(c -> (GridPartComponentI) c)
                .collect(Collectors.toList()); // trouver un moyen d'aller à l'EVA plus facilement

        // TODO : démarer le listener log
        WritingStateController.INSTANCE.currentWordProperty().addListener((obs, ov, nv) -> {
            currentKeyboardEvaluation.getLogs().add(new WP2Logs(new Date(), LogType.EYETRACKING_POSITION, new EyetrackingPosition(455, 555)));
        }); // Pour les logs ex: à chaque nouveau mot saisi on récupère la position de l'eye tracking


        //TODO : affiche les phrases à saisir
        StartDislaySentence();

        // TODO : chrono 10 mins
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                // aller aux EVA
                SelectionModeController.INSTANCE.goToGridPart(EVA.get(0));
                //stop sentence display and clean editor
                StopDislaySentence();

                timer.cancel();

                currentKeyboardEvaluation.setFatigueScore(50);
                currentKeyboardEvaluation.setSatisfactionScore(10);
                currentEvaluation.getEvaluations().add(currentKeyboardEvaluation);
                currentKeyboardEvaluation = null;

                System.out.println(JsonHelper.GSON.toJson(currentEvaluation));
            }
        };
        timer.schedule(timerTask,EVALUATION_DURATION_MS);
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

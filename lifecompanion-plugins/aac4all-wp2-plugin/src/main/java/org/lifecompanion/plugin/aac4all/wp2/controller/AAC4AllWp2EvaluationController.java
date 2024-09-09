package org.lifecompanion.plugin.aac4all.wp2.controller;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
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
import org.lifecompanion.plugin.aac4all.wp2.AAC4AllWp2Plugin;
import org.lifecompanion.plugin.aac4all.wp2.AAC4AllWp2PluginProperties;
import org.lifecompanion.plugin.aac4all.wp2.model.logs.*;
import tobii.Tobii;


import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public enum AAC4AllWp2EvaluationController implements ModeListenerI {
    INSTANCE;

    private final long TRAINING_DURATION_MS = (long) 2 * 60 * 1000; //20 sec à passer en 10 min
    private final long EVALUATION_DURATION_MS = (long) 15 * 60 * 1000;//15 min

    private AAC4AllWp2PluginProperties currentAAC4AllWp2PluginProperties;
    private BooleanProperty evaluationRunning;

    private WP2Evaluation currentEvaluation;
    private WP2KeyboardEvaluation currentKeyboardEvaluation;
    private final List<String> phraseSetFR;

    private String currentSentence = "";

    private String functionalCurrentKeyboard = "";
    private GridPartComponentI keyboardConsigne;
    private GridPartComponentI keyboardEVA;
    private GridPartComponentI endGrid;

    private Map<KeyboardType, GridPartComponentI> keyboardsMap;
    private RandomType randomType;
    private int currentRandomIndex;
    private KeyboardType currentKeyboardType;

    private StringProperty patientID;

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

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledEyetrackingTask;


    @Override
    public void modeStart(LCConfigurationI configuration) {
        this.configuration = configuration;
        currentAAC4AllWp2PluginProperties = configuration.getPluginConfigProperties(AAC4AllWp2Plugin.ID, AAC4AllWp2PluginProperties.class);
        patientID = currentAAC4AllWp2PluginProperties.patientIdProperty();


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

        this.endGrid =this.configuration.getAllComponent().values().stream()
                .filter(d -> d instanceof GridPartComponentI)
                .filter(c -> c.nameProperty().get().startsWith("Fin"))
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
        this.currentAAC4AllWp2PluginProperties = null;
       // this.randomType = null;
    }

    public void startDailyTraining() {
        List<RandomType> randonTypePossible = FXCollections.observableList(Arrays.stream(RandomType.values()).toList());
        int indexTrainingKeyboard = new Random().nextInt(randonTypePossible.size());
        randomType = randonTypePossible.get(indexTrainingKeyboard); // TODO stocker dans properties

        while (randomType.getKeyboards().size() != keyboardsMap.size()){
            indexTrainingKeyboard = new Random().nextInt(randonTypePossible.size());
            randomType = randonTypePossible.get(indexTrainingKeyboard);
        }
        currentRandomIndex = 0;
        currentEvaluation = new WP2Evaluation(new Date(),patientID.toString());

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
        currentKeyboardEvaluation = new WP2KeyboardEvaluation(currentKeyboardType);
        functionalCurrentKeyboard = Translation.getText("aac4all.wp2.plugin.functional.description." + currentKeyboardType.getTranslationId());
        instructionCurrentKeyboard = Translation.getText("aac4all.wp2.plugin.instruction.description." + currentKeyboardType.getTranslationId());
        UseVariableController.INSTANCE.requestVariablesUpdate();
    }

    public void nextDailyTraining() {
        currentEvaluation.getEvaluations().add(currentKeyboardEvaluation);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File ("C:\\Users\\lhoiry\\Documents\\lifecompanion-main\\lifecompanion-plugins\\aac4all-wp2-plugin\\src\\main\\resources\\text\\monfichierlogtest.json")))){
            writer.write(JsonHelper.GSON.toJson(currentEvaluation));
        }catch (IOException e){
            e.printStackTrace();
        }

        currentKeyboardEvaluation = null;
            //TODO le fichier à enregistrer ici
        if (!goToNextKeyboardToEvaluate()) {
            SelectionModeController.INSTANCE.goToGridPart(endGrid);
        } else {
            SelectionModeController.INSTANCE.goToGridPart(keyboardConsigne);
        }
    }

    public void startEvaluation() {
        // TODO : lancer les claviers en fonction de RandomType donnée dans les réglages.
        randomType= RandomType.fromName(currentAAC4AllWp2PluginProperties.getRandomTypeEval().getValue());

        currentRandomIndex = 0;
        currentEvaluation = new WP2Evaluation(new Date(),patientID.toString());
        goToNextKeyboardToEvaluate();


    }

    public void setEvaFatigueScore(int score){
        currentKeyboardEvaluation.setFatigueScore(score);
        System.out.println("Fatigue avec score de "+ currentKeyboardEvaluation.getFatigueScore());
    }
    public void setEvaSatisfactionScore(Integer score){
        currentKeyboardEvaluation.setSatisfactionScore(score);
        System.out.println("Satisfaction avec score de "+  currentKeyboardEvaluation.getSatisfactionScore());

    }

    public void startLogListener(){
        currentKeyboardEvaluation = new WP2KeyboardEvaluation(currentKeyboardType);

        if (currentKeyboardEvaluation != null) {

            SelectionModeController.INSTANCE.addScannedPartChangedListeners((gridComponentI, componentToScanI) -> {
                if (componentToScanI !=null){
                    ValidationLog log = new ValidationLog(componentToScanI.toString(), componentToScanI.getIndex());
                    currentKeyboardEvaluation.getLogs().add(new WP2Logs(new Date(), LogType.VALIDATION, log));

                    System.out.println("Sélection de la ligne :" + componentToScanI.getComponents());
                }
            });

            SelectionModeController.INSTANCE.currentOverPartProperty().addListener((obs, ov, nv) -> {
                if(nv !=null){
                    float[] position = Tobii.gazePosition();
                    float rawGazeX = position[0];
                    float rawGazeY = position[1];
                    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                    int gazeX = (int) (rawGazeX * screenSize.getWidth());
                    int gazeY = (int) (rawGazeY * screenSize.getHeight());
                    System.out.println(gazeX);
                    System.out.println(gazeY);
                    System.out.println("parcours de la case :" + nv.toString());
                }
            });

            WritingStateController.INSTANCE.currentWordProperty().addListener((obs, ov, nv) -> {
                //TODO : enregistrer le fichier à chaque nouveau mot saisie
                System.out.println("un mot est écrit "+nv);
            });

                //TODO enregistrer la position du regards toutes les
            scheduler = Executors.newScheduledThreadPool(1);
            scheduledEyetrackingTask = scheduler.scheduleAtFixedRate(() -> {
                float[] position = Tobii.gazePosition();
                float rawGazeX = position[0];
                float rawGazeY = position[1];
                //Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                //int gazeX = (int) (rawGazeX * screenSize.getWidth());
                //int gazeY = (int) (rawGazeY * screenSize.getHeight());
                System.out.println(rawGazeX);
                System.out.println(rawGazeY);
                //currentKeyboardEvaluation.getLogs().add(new WP2Logs(new Date(), LogType.EYETRACKING_POSITION, new EyetrackingPosition(gazeX, gazeY)));
            }, 0, 2, TimeUnit.SECONDS); //0, 20, TimeUnit.MILLISECONDS);



        }
    }

    public void stopLogListener(){
        //TODO stopper les


        //TODO stop eyetracking logs
        if (scheduledEyetrackingTask != null && !scheduledEyetrackingTask.isCancelled()) {
            scheduler.shutdown();
            System.out.println("Tâche arrêtée");
        }
    }

    public void startTraining() {

        // TODO: go to currentKeyboardEvaluation
        SelectionModeController.INSTANCE.goToGridPart(currentKeyboard);

        // TODO: clean l'éditeur
        WritingStateController.INSTANCE.removeAll(WritingEventSource.USER_ACTIONS);

        // TODO : démarer le listener log
        //startLogListener();
        scheduler = Executors.newScheduledThreadPool(1);
        scheduledEyetrackingTask = scheduler.scheduleAtFixedRate(() -> {
            float[] position = Tobii.gazePosition();
            float rawGazeX = position[0];
            float rawGazeY = position[1];
            //Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            //int gazeX = (int) (rawGazeX * screenSize.getWidth());
            //int gazeY = (int) (rawGazeY * screenSize.getHeight());
            System.out.println(rawGazeX);
            System.out.println(rawGazeY);
            //currentKeyboardEvaluation.getLogs().add(new WP2Logs(new Date(), LogType.EYETRACKING_POSITION, new EyetrackingPosition(gazeX, gazeY)));
        }, 0, 2, TimeUnit.SECONDS);




        //TODO : affiche les phrases à saisir
        StartDislaySentence();

        // chrono 10 mins
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {

                //TODO stopper le listener validation, hightligh etc
                stopLogListener();


               
                // go to EVA interface
                SelectionModeController.INSTANCE.goToGridPart(keyboardEVA);
                //stop sentence display and clean editor
                StopDislaySentence();
                timer.cancel();

            }
        };
        timer.schedule(timerTask, TRAINING_DURATION_MS);
    }


    public void StartDislaySentence() {
        currentKeyboardEvaluation.getLogs().add(new WP2Logs(new Date(), LogType.CURRENT_ENTRY,WritingStateController.INSTANCE.getLastSentence()));

        currentSentence = phraseSetFR.get(new Random().nextInt(phraseSetFR.size()));
        UseVariableController.INSTANCE.requestVariablesUpdate();
        WritingStateController.INSTANCE.removeAll(WritingEventSource.USER_ACTIONS);

        currentKeyboardEvaluation.getLogs().add(new WP2Logs(new Date(), LogType.SENTENCE,currentSentence));
    }

    public void StopDislaySentence() {
        currentSentence = "";
        UseVariableController.INSTANCE.requestVariablesUpdate();
        WritingStateController.INSTANCE.removeAll(WritingEventSource.USER_ACTIONS);
    }


}

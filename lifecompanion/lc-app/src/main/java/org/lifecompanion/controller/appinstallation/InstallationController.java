/*
 * LifeCompanion AAC and its sub projects
 *
 * Copyright (C) 2014 to 2019 Mathieu THEBAUD
 * Copyright (C) 2020 to 2021 CMRRF KERPAPE (Lorient, France)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.lifecompanion.controller.appinstallation;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Task;
import org.lifecompanion.controller.appinstallation.task.*;
import org.lifecompanion.controller.editaction.GlobalActions;
import org.lifecompanion.controller.editmode.ConfigActionController;
import org.lifecompanion.controller.lifecycle.AppModeController;
import org.lifecompanion.controller.plugin.PluginController;
import org.lifecompanion.controller.resource.ResourceHelper;
import org.lifecompanion.framework.client.http.AppServerClient;
import org.lifecompanion.framework.client.props.ApplicationBuildProperties;
import org.lifecompanion.framework.client.service.AppServerService;
import org.lifecompanion.framework.commons.SystemType;
import org.lifecompanion.framework.commons.translation.Translation;
import org.lifecompanion.framework.commons.utils.app.VersionUtils;
import org.lifecompanion.framework.commons.utils.io.IOUtils;
import org.lifecompanion.framework.commons.utils.lang.StringUtils;
import org.lifecompanion.framework.model.server.update.ApplicationPluginUpdate;
import org.lifecompanion.framework.utils.FluentHashMap;
import org.lifecompanion.framework.utils.LCNamedThreadFactory;
import org.lifecompanion.framework.utils.Pair;
import org.lifecompanion.model.api.lifecycle.LCStateListener;
import org.lifecompanion.model.impl.constant.LCConstant;
import org.lifecompanion.model.impl.notification.LCNotification;
import org.lifecompanion.model.impl.plugin.PluginInfo;
import org.lifecompanion.model.impl.plugin.PluginInfoState;
import org.lifecompanion.ui.notification.LCNotificationController;
import org.lifecompanion.util.DesktopUtils;
import org.lifecompanion.util.LangUtils;
import org.lifecompanion.util.javafx.FXThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.lifecompanion.framework.commons.ApplicationConstant.*;
import static org.lifecompanion.model.impl.constant.LCConstant.URL_PATH_CHANGELOG;

/**
 * Manages updates mechanisms and installation during execution.<br>
 * Rely on launcher that should inject launch args to know the update state.<br>
 * Update are checked/downloaded in backgrounds tasks while application is running : using low priority thread to avoid getting the app stuck.<br>
 * <br>
 * Each LC installation is identified uniquely by a installation token generated by the general server. This installation token is useful to identify this installation, but could also be used later to check premium items.
 *
 * @author Mathieu THEBAUD
 */
public enum InstallationController implements LCStateListener {
    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger(InstallationController.class);

    private final File FILE_LAST_UPDATE_DATE = new File(LCConstant.EXT_PATH_LAST_UPDATE_CHECK);
    private final File FILE_LAST_PLUGIN_UPDATE_DATE = new File(LCConstant.EXT_PATH_LAST_PLUGIN_UPDATE_CHECK);

    private final ExecutorService executorService;
    private final AppServerClient appServerClient;

    private final ApplicationBuildProperties buildProperties;

    private boolean updateDownloadFinished;
    private boolean updateFinished;
    private boolean enablePreviewUpdates;
    private boolean skipUpdates;

    private final StringProperty updateTaskMessage;
    private final BooleanProperty updateTaskRunning;
    private final DoubleProperty updateTaskProgress;

    private final ObjectProperty<Date> lastUpdateCheckDate;

    private Runnable installationRegistrationInformationSetCallback;

    private InstallationRegistrationInformation installationRegistrationInformation;

    InstallationController() {
        this.buildProperties = ApplicationBuildProperties.load(ResourceHelper.getInputStreamForPath("/app.properties"));
        this.appServerClient = new AppServerClient(buildProperties.getUpdateServerUrl());
        this.updateTaskMessage = new SimpleStringProperty();
        this.updateTaskRunning = new SimpleBooleanProperty();
        this.updateTaskProgress = new SimpleDoubleProperty(-1.0);
        this.lastUpdateCheckDate = new SimpleObjectProperty<>();
        // WARNING : implementation relies on the fact that this is a SINGLE thread executor
        // See lcStart() to understand
        this.executorService = Executors.newSingleThreadExecutor(LCNamedThreadFactory.daemonThreadFactoryWithPriority("InstallationController", Thread.MIN_PRIORITY));
    }

    public StringProperty updateTaskMessageProperty() {
        return updateTaskMessage;
    }

    public BooleanProperty updateTaskRunningProperty() {
        return updateTaskRunning;
    }

    public DoubleProperty updateTaskProgressProperty() {
        return updateTaskProgress;
    }

    public ObjectProperty<Date> lastUpdateCheckDateProperty() {
        return lastUpdateCheckDate;
    }

    private <T> void submitTask(Task<T> task, Consumer<T> successCallback) {
        submitTask(task, successCallback, null);
    }

    private <T> void submitTask(Task<T> task, Consumer<T> successCallback, Consumer<Throwable> errorCallback) {
        task.setOnSucceeded(e -> successCallback.accept(task.getValue()));
        task.setOnFailed(e -> {
            Throwable t = e.getSource().getException();
            LOGGER.error("Error in task {}", task.getClass().getSimpleName(), t);
            if (errorCallback != null) {
                errorCallback.accept(t);
            }
        });
        // Work because single thread executor
        task.runningProperty().addListener((obs, ov, nv) -> {
            if (nv) {
                updateTaskMessage.bind(task.messageProperty());
                updateTaskProgress.bind(task.progressProperty());
                updateTaskRunning.bind(task.runningProperty());
            }
        });
        this.executorService.submit(task);
    }

    // Updates from args (launched before lcStart > should block UI if updateDownloadFinished)
    //========================================================================
    public void handleLaunchArgs(Collection<String> args) {
        this.updateDownloadFinished = args.removeIf(ARG_UPDATE_DOWNLOAD_FINISHED::equals);
        this.updateFinished = args.removeIf(ARG_UPDATE_FINISHED::equals);
        this.enablePreviewUpdates = args.removeIf(ARG_ENABLE_PREVIEW_UPDATES::equals);
        this.skipUpdates = LangUtils.safeParseBoolean(System.getProperty("org.lifecompanion.debug.skip.update.check"));
    }

    public boolean isUpdateDownloadFinished() {
        return updateDownloadFinished;
    }

    public boolean isSkipUpdates() {
        return skipUpdates;
    }

    public ApplicationBuildProperties getBuildProperties() {
        return buildProperties;
    }

    public void writeLastUpdateCheckDate(Date date) {
        FXThreadUtils.runOnFXThread(() -> this.lastUpdateCheckDate.set(date));
        writeLastUpdateDate(FILE_LAST_UPDATE_DATE, date);
    }

    public Date readLastUpdateCheckDate() {
        Date lastUpdateDate = readLastUpdateCheckDate(FILE_LAST_UPDATE_DATE);
        FXThreadUtils.runOnFXThread(() -> lastUpdateCheckDate.set(lastUpdateDate));
        return lastUpdateDate;
    }

    private Date readLastUpdateCheckDate(File file) {
        try {
            if (file.exists()) {
                long lastCheck = Long.parseLong(StringUtils.stripToEmpty(IOUtils.readFileLines(file, StandardCharsets.UTF_8.name())));
                return new Date(lastCheck);
            }
        } catch (Exception e) {
            LOGGER.warn("Couldn't get last update check date from {}", file, e);
        }
        return null;
    }

    private void writeLastUpdateDate(File file, Date date) {
        try {
            IOUtils.writeToFile(file, String.valueOf(date.getTime()), StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            LOGGER.warn("Couldn't write last update check date to {}", file, e);
        }
    }

    private void clearLastUpdateCheckDate() {
        try {
            final boolean deleted = FILE_LAST_UPDATE_DATE.delete();
            FXThreadUtils.runOnFXThread(() -> this.lastUpdateCheckDate.set(null));
            LOGGER.info("Last update check file {} deleted {}", FILE_LAST_UPDATE_DATE, deleted);
            final boolean deletedPlugin = FILE_LAST_PLUGIN_UPDATE_DATE.delete();
            LOGGER.info("Last plugin update check file {} deleted {}", FILE_LAST_UPDATE_DATE, deletedPlugin);
        } catch (Exception e) {
            LOGGER.warn("Couldn't write last update check date", e);
        }
    }

    public InstallationRegistrationInformation getInstallationRegistrationInformation() {
        return installationRegistrationInformation;
    }

    private void setInstallationRegistrationInformation(InstallationRegistrationInformation installationRegistrationInformation) {
        this.installationRegistrationInformation = installationRegistrationInformation;
        if (this.installationRegistrationInformation != null) {
            this.appServerClient.setInstallationIdForHeader(this.installationRegistrationInformation.getInstallationId());
            if (this.installationRegistrationInformationSetCallback != null) {
                this.installationRegistrationInformationSetCallback.run();
            }
        }
    }

    public void setInstallationRegistrationInformationSetCallback(Runnable installationRegistrationInformationSetCallback) {
        this.installationRegistrationInformationSetCallback = installationRegistrationInformationSetCallback;
    }

    public void tryToSendUpdateStats() {
        if (this.installationRegistrationInformation != null) {
            final File updateStatCacheDir = new File(LCConstant.PATH_UPDATE_STAT_CACHE);
            final File[] updateCacheFiles = updateStatCacheDir.listFiles();
            if (updateCacheFiles != null) {
                LOGGER.info("Will try to send update cache information");
                AppServerService appServerService = new AppServerService(appServerClient);
                for (File updateCacheFile : updateCacheFiles) {
                    try {
                        VersionUtils.VersionInfo.parse(updateCacheFile.getName());// to check if the filename seems valid
                        String version = updateCacheFile.getName();
                        Date recordedDate = new Date(Long.parseLong(StringUtils.stripToEmpty(IOUtils.readFileLines(updateCacheFile, StandardCharsets.UTF_8.name()))));
                        appServerService.pushStat(version, recordedDate);
                        updateCacheFile.delete();
                        LOGGER.info("Update stat pushed to server : version {} at {}", version, recordedDate);
                    } catch (Exception e) {
                        LOGGER.warn("Could not handle update cache file {}", updateCacheFile, e);
                    }
                }
            }
        }
    }
    //========================================================================

    // PLUGIN
    //========================================================================
    public void tryToAddPluginsAfterDownload(java.util.List<Pair<ApplicationPluginUpdate, File>> updatedPlugins) {
        updatedPlugins.forEach(this::tryToAddPluginAfterDownload);
    }

    public CheckAndDownloadPluginUpdateTask createCheckAndDowloadPluginTask(boolean pauseOnStart) {
        return new CheckAndDownloadPluginUpdateTask(appServerClient, buildProperties.getAppId(), enablePreviewUpdates, pauseOnStart);
    }

    private void tryToAddPluginAfterDownload(Pair<ApplicationPluginUpdate, File> updatedPlugin) {
        try {
//            Pair<String, PluginInfo> added = PluginController.INSTANCE.tryToAddPluginFrom(updatedPlugin.getRight());
//            showPluginUpdateNotification(added.getRight());
        } catch (Exception e) {
            LOGGER.error("Couldn't add the plugin for {}", updatedPlugin.getRight());
        }
    }

    public Date readLastPluginUpdateCheckDate() {
        return readLastUpdateCheckDate(FILE_LAST_PLUGIN_UPDATE_DATE);
    }

    public void writeLastPluginUpdateCheckDate(Date date) {
        writeLastUpdateDate(FILE_LAST_PLUGIN_UPDATE_DATE, date);
    }

    public void launchPluginUpdateCheckTask(boolean manualRequest) {
        if (!skipUpdates) {
            this.submitTask(createDownloadAllPlugin(!manualRequest, buildProperties.getVersionLabel()), pluginFiles -> {
                for (File pluginFile : pluginFiles) {
                    try {
                        Pair<PluginController.PluginAddResult, PluginInfo> added = PluginController.INSTANCE.tryToAddPluginFrom(pluginFile);
                        if (added.getLeft() != PluginController.PluginAddResult.NOT_ADDED_ALREADY_SAME_OR_NEWER) {
                            showPluginUpdateNotification(added.getRight());
                        }
                    } catch (Exception e) {
                        LOGGER.error("Couldn't add the plugin for {}", pluginFile);
                    }
                }
            });
        }
    }

    public DownloadAllPluginUpdateTask createDownloadAllPlugin(boolean pauseOnStart, String appVersion) {
        List<String> pluginIds = PluginController.INSTANCE.getPluginInfoList().stream().filter(p -> p.stateProperty().get() != PluginInfoState.REMOVED).map(PluginInfo::getPluginId).collect(Collectors.toList());
        return new DownloadAllPluginUpdateTask(appServerClient, buildProperties.getAppId(), enablePreviewUpdates, pauseOnStart, pluginIds, appVersion);
    }

    public DownloadPluginTask createPluginDownloadTask(String pluginId) {
        return new DownloadPluginTask(appServerClient, buildProperties.getAppId(), enablePreviewUpdates, false, pluginId);
    }
    //========================================================================

    // TASK
    //========================================================================
    public void launchInstallAppUpdate() {
        this.submitTask(new InstallAppUpdateTask(appServerClient, buildProperties.getAppId(), enablePreviewUpdates,
                        InstallationConfigurationController.INSTANCE.getInstallationConfiguration()),
                filesCopied -> restart(filesCopied ? ARG_UPDATE_FINISHED : ""));
    }

    public void restart(String arg) {
        Platform.exit();
        try {
            new ProcessBuilder()//
                    .command(getLauncherPath().getAbsolutePath(), arg)// TO-TEST
                    .redirectError(ProcessBuilder.Redirect.DISCARD).redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
        } catch (Exception e) {
            LOGGER.error("Couldn't restart LifeCompanion", e);
        }
    }

    private static final Map<SystemType, String> LAUNCHER_PATH = FluentHashMap
            .map(SystemType.WINDOWS, "LifeCompanion.exe")
            .with(SystemType.MAC, "MacOS/lifecompanion.sh")
            .with(SystemType.UNIX, "launcher/lifecompanion.sh");

    public File getLauncherPath() {
        return new File(LAUNCHER_PATH.get(SystemType.current()));
    }
    //========================================================================

    // LC start/stop
    //========================================================================
    @Override
    public void lcStart() {
        // First : get the installation ID for LC (single thread executor, so the next tasks will be executed once it's done)
        this.submitTask(new GetInstallationIdTask(buildProperties), this::setInstallationRegistrationInformation);

        // Classic update checking mechanism : note a check happens before this (to check if an update was downloaded and is ready to be installed)
        this.launchUpdateCheckProcess(false);
    }

    @Override
    public void lcExit() {
        this.appServerClient.close();
        this.executorService.shutdownNow();
    }
    //========================================================================

    // UPDATE CHECK
    //========================================================================
    public void launchUpdateCheckProcess(boolean manualRequest) {
        if (manualRequest) {
            clearLastUpdateCheckDate();
        }
        if (!skipUpdates) {
            if (updateFinished) {
                this.submitTask(new DeleteUpdateTempFileTask(appServerClient, buildProperties.getAppId(), enablePreviewUpdates, !manualRequest), updateDirDeleted -> {
                    if (updateDirDeleted) {
                        updateFinished = false;
                        showUpdateInstallationDoneNotification();
                    }
                });
            } else {
                if (!updateDownloadFinished) {
                    this.submitTask(new CheckUpdateTask(appServerClient, buildProperties.getAppId(), enablePreviewUpdates, !manualRequest), updateProgress -> {
                        if (updateProgress != null) {
                            LOGGER.info("Update progress detected, LifeCompanion will be updating... (background task)");
                            this.submitTask(new DownloadUpdateTask(appServerClient, buildProperties.getAppId(), enablePreviewUpdates, !manualRequest, updateProgress), downloadFinished -> {
                                if (downloadFinished) {
                                    showUpdateDownloadFinishedNotification();
                                }
                            });
                        } else {
                            LOGGER.info("No update found");
                        }
                    });
                    if (manualRequest) {
                        launchPluginUpdateCheckTask(true);
                    }
                }
            }
        }
    }
    //========================================================================

    // UI
    //========================================================================
    private void showUpdateDownloadFinishedNotification() {
        FXThreadUtils.runOnFXThread(() -> {
            if (AppModeController.INSTANCE.isEditMode()) {
                LCNotificationController.INSTANCE.showNotification(LCNotification.createInfo("notification.info.update.download.finished.title", false,
                        "notification.info.update.download.finish.restart.button", () -> ConfigActionController.INSTANCE.executeAction(new GlobalActions.RestartAction(AppModeController.INSTANCE.getEditModeContext().getStage().getScene().getRoot())))
                );
            }
        });
    }

    private void showUpdateInstallationDoneNotification() {
        FXThreadUtils.runOnFXThread(() -> {
            if (AppModeController.INSTANCE.isEditMode()) {
                LCNotificationController.INSTANCE.showNotification(LCNotification.createInfo(Translation.getText("notification.info.update.done.title", InstallationController.INSTANCE.getBuildProperties().getVersionLabel()), false));
                DesktopUtils.openUrlInDefaultBrowser(InstallationController.INSTANCE.getBuildProperties().getAppServerUrl() + URL_PATH_CHANGELOG);
            }
        });
    }

    private void showPluginUpdateNotification(PluginInfo pluginInfo) {
        FXThreadUtils.runOnFXThread(() -> {
            if (AppModeController.INSTANCE.isEditMode()) {
                LCNotificationController.INSTANCE.showNotification(LCNotification.createInfo(Translation.getText("notification.info.plugin.update.done.title", pluginInfo.getPluginName(), pluginInfo.getPluginVersion()), true));
            }
        });
    }
    //========================================================================


    // UPDATE CHECK STATUS
    //========================================================================
    public static class InstallationRegistrationInformation {
        private final String deviceId;
        private final String installationId;

        public InstallationRegistrationInformation(String deviceId, String installationId) {
            this.deviceId = deviceId;
            this.installationId = installationId;
        }

        public String getDeviceId() {
            return deviceId;
        }

        public String getInstallationId() {
            return installationId;
        }

        @Override
        public String toString() {
            return "InstallationRegistrationInformation{" +
                    "deviceId='" + deviceId + '\'' +
                    ", installationId='" + installationId + '\'' +
                    '}';
        }
    }
    //========================================================================
}


package org.lifecompanion.plugin.aac4all.wp2;

import javafx.beans.property.ObjectProperty;
import org.lifecompanion.model.api.configurationcomponent.LCConfigurationI;
import org.lifecompanion.model.api.plugin.PluginConfigPropertiesI;
import org.lifecompanion.model.api.plugin.PluginI;
import org.lifecompanion.model.api.usevariable.UseVariableDefinitionI;
import org.lifecompanion.model.api.usevariable.UseVariableI;
import org.lifecompanion.model.impl.usevariable.IntegerUseVariable;
import org.lifecompanion.model.impl.usevariable.StringUseVariable;
import org.lifecompanion.model.impl.usevariable.UseVariableDefinition;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class AAC4AllWp2Plugin implements PluginI {
    public static final String ID = "aac4all-wp2-plugin";

    // RES
    //========================================================================
    @Override
    public String[] getLanguageFiles(final String languageCode) {
        return new String[]{"/text/" + languageCode + "_aac4all-wp2-plugin.xml"};
    }

    @Override
    public String[] getJavaFXStylesheets() {
        return new String[]{"/style/aac4all-wp2-plugin.css"};
    }

    @Override
    public String[] getDefaultConfigurations(String languageCode) {
        return null;
    }
    //========================================================================


    // PLUGIN START/STOP
    //========================================================================
    @Override
    public void start(File dataDirectory) {
        // Plugin global init here
    }

    @Override
    public void stop(File dataDirectory) {
        // Plugin global stop here
    }
    //========================================================================

    // MODE START/STOP
    //========================================================================
    @Override
    public void modeStart(LCConfigurationI configuration) {
        // TODO
    }

    @Override
    public void modeStop(LCConfigurationI configuration) {
        // TODO
    }
    //========================================================================


    // VARIABLES
    //========================================================================
    @Override
    public List<UseVariableDefinitionI> getDefinedVariables() {
        return null;
    }

    @Override
    public Function<UseVariableDefinitionI, UseVariableI<?>> getSupplierForUseVariable(String id) {
        return null;
    }
    //========================================================================

    // PLUGIN PROPERTIES
    //========================================================================
    @Override
    public PluginConfigPropertiesI newPluginConfigProperties(ObjectProperty<LCConfigurationI> parentConfiguration) {
        return new AAC4AllWp2PluginProperties(parentConfiguration);
    }
    //========================================================================
}

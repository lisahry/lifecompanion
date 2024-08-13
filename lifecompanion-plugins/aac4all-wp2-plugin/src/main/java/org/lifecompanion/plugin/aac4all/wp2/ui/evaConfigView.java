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

package org.lifecompanion.plugin.aac4all.wp2.ui;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.lifecompanion.framework.commons.translation.Translation;
import org.lifecompanion.model.api.categorizedelement.useaction.UseActionConfigurationViewI;
import org.lifecompanion.model.api.usevariable.UseVariableDefinitionI;
import org.lifecompanion.plugin.aac4all.wp2.model.useaction.evaAction;
import org.lifecompanion.ui.common.control.specific.usevariable.UseVariableTextArea;

public abstract class evaConfigView extends VBox implements UseActionConfigurationViewI<evaAction> {

    private UseVariableTextArea useVariableTextArea;

    public evaConfigView() {
    }


    public void initUI() {
        this.setSpacing(4.0);
        this.setPadding(new Insets(10.0));
        this.useVariableTextArea = new UseVariableTextArea();
        this.getChildren().addAll(new Label(Translation.getText("use.action.eva.categorie")), this.useVariableTextArea);

    }


    public Region getConfigurationView() {
        return this;
    }

    public void editStarts(final evaAction action, final ObservableList<UseVariableDefinitionI> possibleVariables) {
        this.useVariableTextArea.getTextArea().clear();
        this.useVariableTextArea.setAvailableUseVariable(possibleVariables);
        this.useVariableTextArea.getTextArea().setText(action.getEvaCategorieProperty().get());
    }

    public void editEnds(final evaAction action) {
        action.getEvaCategorieProperty().set(this.useVariableTextArea.getTextArea().getText());
    }

    @Override
    public Class<evaAction> getConfiguredActionType() {
        return evaAction.class;
    }

}

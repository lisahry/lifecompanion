/*
 * LifeCompanion AAC and its sub projects
 *
 * Copyright (C) 2014 to 2019 Mathieu THEBAUD
 * Copyright (C) 2020 to 2022 CMRRF KERPAPE (Lorient, France) and CoWork'HIT (Lorient, France)
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

package org.lifecompanion.plugin.aac4all.wp2.model.useaction;

import javafx.beans.property.*;
import org.jdom2.Element;
import org.lifecompanion.framework.commons.fx.io.XMLGenericProperty;
import org.lifecompanion.framework.commons.fx.io.XMLObjectSerializer;
import org.lifecompanion.model.api.categorizedelement.useaction.UseActionEvent;
import org.lifecompanion.model.api.categorizedelement.useaction.UseActionTriggerComponentI;
import org.lifecompanion.model.api.io.IOContextI;
import org.lifecompanion.model.api.usevariable.UseVariableI;
import org.lifecompanion.model.impl.categorizedelement.useaction.SimpleUseActionImpl;
import org.lifecompanion.model.impl.exception.LCException;

import java.util.Map;


public class SetEvaValueUseAction extends SimpleUseActionImpl<UseActionTriggerComponentI> {

    private final StringProperty evaCategorie;
    private final IntegerProperty evaScore;

    @XMLGenericProperty(EvaType.class)
    private final ObjectProperty<EvaType> evaType;

    public SetEvaValueUseAction() {
        super(UseActionTriggerComponentI.class);
        this.category = AAC4AllWp2SubCategories.TODO;
        this.order = 1;
        this.evaCategorie = new SimpleStringProperty("");
        this.evaScore = new SimpleIntegerProperty();
        this.evaType = new SimpleObjectProperty<>();
        this.nameID = "todo-eva";
        this.staticDescriptionID = "todo";
        this.configIconPath = "filler_icon_32px.png";
        this.parameterizableAction = true;
        this.variableDescriptionProperty().set(this.getStaticDescription());
    }

    public ObjectProperty<EvaType> evaTypeProperty() {
        return evaType;
    }

    public StringProperty getEvaCategorieProperty() {
        return this.evaCategorie;
    }

    public IntegerProperty getEvaScoreProperty() {
        return this.evaScore;
    }

    public void setEvaCategorie(String evaCategorie) {
        this.evaCategorie.set(evaCategorie);
    }

    public void setEvaScore(int evaScore) {
        this.evaScore.set(evaScore);
    }

    @Override
    public void execute(final UseActionEvent event, final Map<String, UseVariableI<?>> variables) {
        // AAC4AllWp2EvaluationController.INSTANCE.nextDailyEvaluation();
        if (this.evaCategorie.get() != null) {
            if (evaType.get() == EvaType.FATIGUE) {
                // TODO
            } else if (evaType.get() == EvaType.SATISFACTION) {
                // TODO
            }
        }
    }


    // Class part : "XML"
    //========================================================================
    @Override
    public Element serialize(final IOContextI contextP) {
        Element node = super.serialize(contextP);
        XMLObjectSerializer.serializeInto(SetEvaValueUseAction.class, this, node);
        return node;
    }

    @Override
    public void deserialize(final Element nodeP, final IOContextI contextP) throws LCException {
        super.deserialize(nodeP, contextP);
        XMLObjectSerializer.deserializeInto(SetEvaValueUseAction.class, this, nodeP);
    }

}

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

import org.lifecompanion.controller.textcomponent.WritingStateController;
import org.lifecompanion.framework.commons.utils.lang.StringUtils;
import org.lifecompanion.model.api.categorizedelement.useaction.UseActionEvent;
import org.lifecompanion.model.api.categorizedelement.useaction.UseActionTriggerComponentI;
import org.lifecompanion.model.api.configurationcomponent.GridPartKeyComponentI;
import org.lifecompanion.model.api.textcomponent.WritingEventSource;
import org.lifecompanion.model.api.usevariable.UseVariableI;
import org.lifecompanion.model.impl.categorizedelement.useaction.SimpleUseActionImpl;
import org.lifecompanion.model.impl.configurationcomponent.keyoption.AutoCharKeyOption;
import org.lifecompanion.model.impl.configurationcomponent.keyoption.CustomCharKeyOption;
import org.lifecompanion.plugin.aac4all.wp2.model.keyoption.AAC4AllKeyOption;

import java.util.Map;

public class WriteAAC4AllPredictionAction extends SimpleUseActionImpl<GridPartKeyComponentI> {

    public WriteAAC4AllPredictionAction() {
        super(GridPartKeyComponentI.class);
        this.category = AAC4AllWp2SubCategories.TODO;
        this.nameID = "aac4aal.wp2.plugin.action.write.prediction.name";
        this.staticDescriptionID = "aac4aal.wp2.plugin.action.write.prediction.description";
        this.configIconPath = "filler_icon_32px.png";
        this.parameterizableAction = false;
        this.variableDescriptionProperty().set(this.getStaticDescription());
    }

    @Override
    public void execute(final UseActionEvent event, final Map<String, UseVariableI<?>> variables) {
        // TODO
        GridPartKeyComponentI parentKey = (GridPartKeyComponentI) this.parentComponentProperty().get();
        if (parentKey != null) {
            String prediction = null;
            if (parentKey.keyOptionProperty().get() instanceof AAC4AllKeyOption) {
                AAC4AllKeyOption predOption = (AAC4AllKeyOption) parentKey.keyOptionProperty().get();
                prediction = predOption.predictionProperty().get();
            }
            if (prediction != null) {
                if (!prediction.isEmpty() && StringUtils.isEquals(prediction,
                        parentKey.configurationParentProperty().get().getPredictionParameters().charPredictionSpaceCharProperty().get())) {
                    WritingStateController.INSTANCE.space(WritingEventSource.USER_ACTIONS);
                } else {
                    WritingStateController.INSTANCE.insertCharPrediction(WritingEventSource.USER_ACTIONS, prediction);
                }
            }
        }

    }

}

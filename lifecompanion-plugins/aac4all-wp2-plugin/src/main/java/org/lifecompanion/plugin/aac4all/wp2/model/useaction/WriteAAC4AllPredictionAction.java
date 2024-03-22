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

import org.lifecompanion.model.api.categorizedelement.useaction.UseActionEvent;
import org.lifecompanion.model.api.categorizedelement.useaction.UseActionTriggerComponentI;
import org.lifecompanion.model.api.usevariable.UseVariableI;
import org.lifecompanion.model.impl.categorizedelement.useaction.SimpleUseActionImpl;

import java.util.Map;

public class WriteAAC4AllPredictionAction extends SimpleUseActionImpl<UseActionTriggerComponentI> {

    public WriteAAC4AllPredictionAction() {
        super(UseActionTriggerComponentI.class);
        this.order = 10;
        this.category = AAC4AllWp2SubCategories.TODO;
        this.nameID = "spellgame.plugin.action.skip.current.word.name";
        this.staticDescriptionID = "spellgame.plugin.action.skip.current.word.description";
        this.configIconPath = "filler_icon_32px.png";
        this.parameterizableAction = false;
        this.variableDescriptionProperty().set(getStaticDescription());
    }

    @Override
    public void execute(UseActionEvent event, Map<String, UseVariableI<?>> variables) {
        // TODO
    }

}

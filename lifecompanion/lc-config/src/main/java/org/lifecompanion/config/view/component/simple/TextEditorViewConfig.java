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

package org.lifecompanion.config.view.component.simple;

import org.lifecompanion.api.ui.ViewProviderI;
import org.lifecompanion.base.data.common.LCUtils;
import org.lifecompanion.base.data.component.simple.GridPartTextEditorComponent;
import org.lifecompanion.base.data.component.simple.TextEditorComponent;
import org.lifecompanion.base.data.control.WritingStateController;
import org.lifecompanion.base.view.component.simple.TextEditorViewBase;
import org.lifecompanion.config.view.component.option.ButtonComponentOption;
import org.lifecompanion.config.view.component.option.MoveButtonOption;
import org.lifecompanion.config.view.component.option.RootComponentOption;

public class TextEditorViewConfig extends TextEditorViewBase {
    private RootComponentOption rootComponentOption;

    public TextEditorViewConfig() {
    }

    @Override
    public void initUI() {
        super.initUI();
        //Button option
        ButtonComponentOption selectOption = new ButtonComponentOption(this.model);
        MoveButtonOption<TextEditorComponent> moveOption = new MoveButtonOption<>(this.model);
        selectOption.addOption(moveOption);
        //Root component UI
        this.rootComponentOption = new RootComponentOption(this.model);
        this.rootComponentOption.bindSize(this);
        this.getChildren().add(this.rootComponentOption);
        this.rootComponentOption.getChildren().add(selectOption);
        //UIUtils.applyPerformanceConfiguration(this);
    }

    @Override
    public void showToFront() {
        super.showToFront();
        this.rootComponentOption.toFront();
    }

    @Override
    public void initialize(ViewProviderI viewProvider, boolean useCache, final TextEditorComponent componentP) {
        super.initialize(viewProvider, useCache, componentP);
        WritingStateController.INSTANCE.initExampleEntriesIfNeeded();
    }

    @Override
    public void updateCaretScroll(final double yPercent) {
        //In config mode, we don't any automatic scroll on canvas update
    }
}

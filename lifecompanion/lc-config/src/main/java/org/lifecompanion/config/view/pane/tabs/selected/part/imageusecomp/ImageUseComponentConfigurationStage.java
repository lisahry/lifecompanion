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

package org.lifecompanion.config.view.pane.tabs.selected.part.imageusecomp;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.lifecompanion.api.component.definition.ImageUseComponentI;
import org.lifecompanion.base.data.common.LCUtils;
import org.lifecompanion.base.data.common.UIUtils;
import org.lifecompanion.base.data.config.IconManager;
import org.lifecompanion.base.data.config.LCConstant;
import org.lifecompanion.base.data.config.LCGraphicStyle;
import org.lifecompanion.config.view.reusable.image.ImageSelectorDialog;

public class ImageUseComponentConfigurationStage extends Stage {
    private static ImageUseComponentConfigurationStage instance;

    private final ImageUseComponentConfigurationView imageUseComponentConfigurationView;

    public ImageUseComponentConfigurationStage() {
        UIUtils.applyDefaultStageConfiguration(this);
        this.initModality(Modality.APPLICATION_MODAL);
        this.initStyle(StageStyle.UTILITY);
        this.setWidth(ImageSelectorDialog.IMAGE_DIALOGS_WIDTH);
        this.setHeight(ImageSelectorDialog.IMAGE_DIALOGS_HEIGHT);
        imageUseComponentConfigurationView = new ImageUseComponentConfigurationView();
        imageUseComponentConfigurationView.getStylesheets().addAll(LCConstant.CSS_STYLE_PATH);
        this.setScene(new Scene(imageUseComponentConfigurationView));
        this.addEventFilter(KeyEvent.KEY_RELEASED, k -> {
            if (k.getCode() == KeyCode.ESCAPE) {
                k.consume();
                this.hide();
            }
        });
        this.setOnHidden(e -> imageUseComponentConfigurationView.modelProperty().set(null));
    }

    public static ImageUseComponentConfigurationStage getInstance() {
        if (instance == null) {
            instance = new ImageUseComponentConfigurationStage();
        }
        return instance;
    }

    public void prepareAndShow(ImageUseComponentI imageUseComponent) {
        imageUseComponentConfigurationView.modelProperty().set(imageUseComponent);
        this.show();
    }
}

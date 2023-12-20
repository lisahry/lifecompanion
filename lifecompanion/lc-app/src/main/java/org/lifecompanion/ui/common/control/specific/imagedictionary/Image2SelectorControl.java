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
package org.lifecompanion.ui.common.control.specific.imagedictionary;

import javafx.beans.property.*;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.lifecompanion.model.api.configurationcomponent.ImageUseComponentI;
import org.lifecompanion.ui.controlsfx.glyphfont.FontAwesome;
import org.fxmisc.easybind.EasyBind;
import org.lifecompanion.model.api.imagedictionary.ImageElementI;
import org.lifecompanion.model.impl.constant.LCGraphicStyle;
import org.lifecompanion.controller.resource.GlyphFontHelper;
import org.lifecompanion.framework.commons.translation.Translation;
import org.lifecompanion.framework.commons.ui.LCViewInitHelper;
import org.lifecompanion.util.javafx.FXControlUtils;
import org.lifecompanion.util.javafx.StageUtils;
import org.lifecompanion.util.model.ConfigurationComponentUtils;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Control to select a image from the gallery.
 *
 * @author Mathieu THEBAUD <math.thebaud@gmail.com>
 */
public class Image2SelectorControl extends BorderPane implements LCViewInitHelper {
    private static final double IMAGE_WIDTH = 180, IMAGE_HEIGHT = 100;

    /**
     * Image use in key
     */
    private ImageView imageViewSelected;

    /**
     * Button to select or remove image
     */
    private Button buttonRemoveImage, buttonSelectImage;

    private Tooltip tooltipImageKeywords;

    /**
     * Property that contains the selected image
     */
    private final ObjectProperty<ImageElementI> selectedImage;

    /**
     * Property to disable image selection
     */
    private final BooleanProperty disableImageSelection;

    /**
     * Default text supplier
     */
    private Supplier<String> defaultSearchTextSupplier;

    private final String nodeIdForImageLoading;

    private final ObjectProperty<ImageUseComponentI> imageUseComponent;

    public Image2SelectorControl() {
        this.nodeIdForImageLoading = "Image2SelectorControl" + this.hashCode();
        this.disableImageSelection = new SimpleBooleanProperty(false);
        this.selectedImage = new SimpleObjectProperty<>();
        this.imageUseComponent = new SimpleObjectProperty<>();
        this.initAll();
    }

    // Class part : "Init"
    //========================================================================
    @Override
    public void initUI() {
        //Create buttons
        this.buttonRemoveImage = FXControlUtils.createTextButtonWithGraphics(Translation.getText("remove.image.component"),
                GlyphFontHelper.FONT_AWESOME.create(FontAwesome.Glyph.TRASH).size(20.0).color(LCGraphicStyle.SECOND_DARK), "tooltip.remove.image.button");
        this.buttonSelectImage = FXControlUtils.createTextButtonWithGraphics(Translation.getText("select.image.component"),
                GlyphFontHelper.FONT_AWESOME.create(FontAwesome.Glyph.PICTURE_ALT).size(20.0).color(LCGraphicStyle.MAIN_PRIMARY),
                "tooltip.select.image.button");

        //Image
        this.imageViewSelected = new ImageView();
        this.imageViewSelected.setFitHeight(Image2SelectorControl.IMAGE_HEIGHT);
        this.imageViewSelected.setFitWidth(Image2SelectorControl.IMAGE_WIDTH);
        this.imageViewSelected.setPreserveRatio(true);

        tooltipImageKeywords = FXControlUtils.createTooltip("");
        Tooltip.install(imageViewSelected, tooltipImageKeywords);

        //Add
        VBox boxButton = new VBox(this.buttonSelectImage, this.buttonRemoveImage);
        boxButton.setAlignment(Pos.CENTER);
        this.setCenter(this.imageViewSelected);
        this.setRight(boxButton);
    }

    @Override
    public void initBinding() {
        bindImageViewToCurrentSelection();
        //Disable remove when there is no image
        this.buttonRemoveImage.disableProperty().bind(this.disableImageSelection.or(this.imageViewSelected.imageProperty().isNull()));
        this.buttonSelectImage.disableProperty().bind(this.disableImageSelection);
    }

    @Override
    public void initListener() {
        //Remove image
        this.buttonRemoveImage.setOnAction((ae) -> {
            this.selectedImage.set(null);
        });
        //Select image
        this.buttonSelectImage.setOnAction((ea) -> {
            ImageSelectorDialog imageSelectorDialog = ImageSelectorDialog.getInstance();
            if (defaultSearchTextSupplier != null) {
                imageSelectorDialog.getImageSelectorSearchView().setSearchTextAndFireSearch(defaultSearchTextSupplier.get());
            }
            StageUtils.centerOnOwnerOrOnCurrentStage(imageSelectorDialog);
            Optional<ImageElementI> img = imageSelectorDialog.showAndWait();
            if (img.isPresent()) {
                selectedImage.set(img.get());
                imageSelectorDialog.getImageSelectorSearchView().clearResult();
            }
        });
        this.selectedImage.addListener((obs, ov, nv) -> {
            if (ov != null) {
                ov.requestImageUnload(this.nodeIdForImageLoading);
            }
            if (nv != null) {
                nv.requestImageLoad(this.nodeIdForImageLoading, IMAGE_WIDTH, IMAGE_HEIGHT, true, true);
                tooltipImageKeywords.setText(nv.getDescription());
            } else {
                tooltipImageKeywords.setText(null);
            }
        });
        this.imageUseComponent.addListener((obs, ov, nv) -> {
            if (ov != null) {
                ConfigurationComponentUtils.unbindImageViewFromImageUseComponent(this.imageViewSelected);
                bindImageViewToCurrentSelection();
            }
            if (nv != null) {
                ConfigurationComponentUtils.bindImageViewWithImageUseComponent(this.imageViewSelected, nv);
            }
        });
    }

    private void bindImageViewToCurrentSelection() {
        this.imageViewSelected.imageProperty().bind(EasyBind.select(this.selectedImage).selectObject(ImageElementI::loadedImageProperty));
    }

    //========================================================================l
    //========================================================================
    public ObjectProperty<ImageElementI> selectedImageProperty() {
        return this.selectedImage;
    }

    public ObjectProperty<ImageUseComponentI> imageUseComponentProperty() {
        return this.imageUseComponent;
    }

    public BooleanProperty disableImageSelectionProperty() {
        return this.disableImageSelection;
    }

    public void setDefaultSearchTextSupplier(Supplier<String> defaultSearchTextSupplier) {
        this.defaultSearchTextSupplier = defaultSearchTextSupplier;
    }
    //========================================================================
}

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

package org.lifecompanion.model.impl.categorizedelement.useaction.available;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.input.KeyCode;
import org.jdom2.Element;
import org.lifecompanion.model.api.categorizedelement.useaction.UseActionEvent;
import org.lifecompanion.model.api.categorizedelement.useaction.UseActionTriggerComponentI;
import org.lifecompanion.model.api.usevariable.UseVariableI;
import org.lifecompanion.model.impl.exception.LCException;
import org.lifecompanion.model.api.io.IOContextI;
import org.lifecompanion.model.api.categorizedelement.useaction.DefaultUseActionSubCategories;
import org.lifecompanion.controller.virtualkeyboard.VirtualKeyboardController;
import org.lifecompanion.model.impl.categorizedelement.useaction.SimpleUseActionImpl;
import org.lifecompanion.framework.commons.fx.io.XMLGenericProperty;
import org.lifecompanion.framework.commons.fx.io.XMLObjectSerializer;
import org.lifecompanion.framework.commons.fx.translation.TranslationFX;
import org.lifecompanion.framework.commons.translation.Translation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SimulateKeyboardKeyPressedAction extends SimpleUseActionImpl<UseActionTriggerComponentI> {

    @XMLGenericProperty(KeyCode.class)
    private ObjectProperty<KeyCode> keyPressed1, keyPressed2, keyPressed3;

    public SimulateKeyboardKeyPressedAction() {
        super(UseActionTriggerComponentI.class);
        this.order = 1;
        this.category = DefaultUseActionSubCategories.KEYBOARD;
        this.keyPressed1 = new SimpleObjectProperty<>();
        this.keyPressed2 = new SimpleObjectProperty<>();
        this.keyPressed3 = new SimpleObjectProperty<>();
        this.nameID = "action.simulate.keyboard.key.press.action.name";
        this.staticDescriptionID = "action.simulate.keyboard.key.press.action.description";
        this.configIconPath = "computeraccess/icon_keyboard_key.png";
        this.variableDescriptionProperty()
                .bind(TranslationFX.getTextBinding("action.simulate.keyboard.key.press.action.description.variable", Bindings.createStringBinding(() -> {
                    return this.getKeyText(this.keyPressed1, false, false) + this.getKeyText(this.keyPressed2, true, true)
                            + this.getKeyText(this.keyPressed3, true, true);
                }, this.keyPressed1, this.keyPressed2, this.keyPressed3)));
    }

    /**
     * Gets the translated name of the KeyCode. The key for the translation is the
     * String "keyboard.key." to which the default english name with spaces replaced
     * by dots and put to lowercase is added. For example the keyCode with the name
     * "Scroll Lock" has a translation searched for with the key
     * "keyboard.key.scroll.lock".
     * 
     * @param keyCode the keyCode whose name should be translated
     * @return the String of the translated name or the default name if the key
     *         doesn't exist in the translation
     */
    public static String getTranslatedKeyCodeName(final KeyCode keyCode) {
        String keyCodeKeyString = "keyboard.key." + keyCode.getName().toLowerCase().replace(' ', '.');
        String translatedText = Translation.getText(keyCodeKeyString);
        return translatedText.equals(keyCodeKeyString) ? keyCode.getName() : translatedText;
    }

    private String getKeyText(final ObjectProperty<KeyCode> keyProp, final boolean empty, final boolean comma) {
        return keyProp.get() != null ? (comma ? ", " : "") + getTranslatedKeyCodeName(keyProp.get()) : empty ? "" : Translation.getText("no.keyboard.key.selected");
    }

    public ObjectProperty<KeyCode> keyPressed1Property() {
        return this.keyPressed1;
    }

    public ObjectProperty<KeyCode> keyPressed2Property() {
        return this.keyPressed2;
    }

    public ObjectProperty<KeyCode> keyPressed3Property() {
        return this.keyPressed3;
    }

    @Override
    public void execute(final UseActionEvent eventP, final Map<String, UseVariableI<?>> variables) {
        KeyCode[] keyCodes = this.createKeyCodeList();
        //Execute
        if (keyCodes.length > 0) {
            VirtualKeyboardController.INSTANCE.keyPressThenRelease(keyCodes);
        }
    }

    private KeyCode[] createKeyCodeList() {
        List<KeyCode> keyCodes = new ArrayList<>(3);
        if (this.keyPressed1.get() != null) {
            keyCodes.add(this.keyPressed1.get());
        }
        if (this.keyPressed2.get() != null) {
            keyCodes.add(this.keyPressed2.get());
        }
        if (this.keyPressed3.get() != null) {
            keyCodes.add(this.keyPressed3.get());
        }
        return keyCodes.toArray(new KeyCode[keyCodes.size()]);
    }

    // Class part : "XML"
    //========================================================================
    @Override
    public Element serialize(final IOContextI contextP) {
        Element elem = super.serialize(contextP);
        XMLObjectSerializer.serializeInto(SimulateKeyboardKeyPressedAction.class, this, elem);
        return elem;
    }

    @Override
    public void deserialize(final Element nodeP, final IOContextI contextP) throws LCException {
        super.deserialize(nodeP, contextP);
        XMLObjectSerializer.deserializeInto(SimulateKeyboardKeyPressedAction.class, this, nodeP);
    }
    //========================================================================

}

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
package org.lifecompanion.base.data.component.simple;

import javafx.beans.property.*;
import javafx.collections.ObservableList;
import org.jdom2.Element;
import org.lifecompanion.api.component.definition.*;
import org.lifecompanion.api.component.definition.grid.ComponentGridI;
import org.lifecompanion.api.definition.selection.SelectionModeParameterI;
import org.lifecompanion.api.exception.LCException;
import org.lifecompanion.api.io.IOContextI;
import org.lifecompanion.api.ui.ViewProviderI;
import org.lifecompanion.base.data.component.baseimpl.GridPartComponentBaseImpl;
import org.lifecompanion.base.data.definition.selection.SelectionModeParameter;
import org.lifecompanion.framework.commons.fx.io.XMLObjectSerializer;

import java.util.function.Consumer;

/**
 * This component is a grid part that store all its component into a grid.<br>
 *
 * @author Mathieu THEBAUD <math.thebaud@gmail.com>
 */
public class GridPartGridComponent extends GridPartComponentBaseImpl implements GridComponentI {
    /**
     * The grid that contains every element placed in this grid
     */
    private final ComponentGridI grid;

    /**
     * Vertical and horizontal gap properties
     */
    private DoubleProperty vGap, hGap;

    /**
     * Number of row count and column count
     */
    private transient IntegerProperty rowCount, columnCount;

    /**
     * The size of each individual case in this grid
     */
    private transient DoubleProperty caseWidth, caseHeight;

    /**
     * The selection mode parameters.
     */
    private final SelectionModeParameter selectionModeParameter;

    private final BooleanProperty useParentSelectionMode;

    private final transient BooleanProperty canUseParentSelectionModeConfiguration;

    /**
     * Create a empty grid part component, without component inside
     */
    public GridPartGridComponent() {
        super();
        this.useParentSelectionMode = new SimpleBooleanProperty(this, "useParentSelectionMode", true);
        this.canUseParentSelectionModeConfiguration = new SimpleBooleanProperty(this, "parentSelectionModeEnabled", true);
        this.selectionModeParameter = new SelectionModeParameter();
        this.grid = new ComponentGrid(this);
        this.initProperties();
        this.initBinding();
    }

    // Class part : "Properties"
    //========================================================================

    /**
     * Create all this component properties
     */
    private void initProperties() {
        this.vGap = new SimpleDoubleProperty(this, "vGap", 8);
        this.hGap = new SimpleDoubleProperty(this, "hGap", 8);
        this.rowCount = new SimpleIntegerProperty(this, "rowCount", 0);
        this.columnCount = new SimpleIntegerProperty(this, "columnCount", 0);
        this.caseWidth = new SimpleDoubleProperty(this, "caseWidth");
        this.caseHeight = new SimpleDoubleProperty(this, "caseHeight");
    }

    /**
     * Init all the properties that must be binded
     */
    private void initBinding() {
        this.caseWidth.bind(this.layoutWidthProperty().subtract(this.columnCount.add(1).multiply(this.hGap)).divide(this.columnCount));
        this.caseHeight.bind(this.layoutHeightProperty().subtract(this.rowCount.add(1).multiply(this.vGap)).divide(this.rowCount));
        this.rowCount.bind(this.grid.rowProperty());
        this.columnCount.bind(this.grid.columnProperty());
    }

    //========================================================================

    // Class part : "Grid properties"
    //========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLeaf() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DoubleProperty vGapProperty() {
        return this.vGap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DoubleProperty hGapProperty() {
        return this.hGap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReadOnlyIntegerProperty rowCountProperty() {
        return this.rowCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReadOnlyIntegerProperty columnCountProperty() {
        return this.columnCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReadOnlyDoubleProperty caseWidthProperty() {
        return this.caseWidth;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReadOnlyDoubleProperty caseHeightProperty() {
        return this.caseHeight;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ComponentGridI getGrid() {
        return this.grid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BooleanProperty useParentSelectionModeProperty() {
        return this.useParentSelectionMode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReadOnlyBooleanProperty canUseParentSelectionModeConfigurationProperty() {
        return this.canUseParentSelectionModeConfiguration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SelectionModeParameterI getSelectionModeParameter() {
        return this.selectionModeParameter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showToFront(ViewProviderI viewProvider, boolean useCache) {
        //Display this grid in the stack parent when needed
        if (this.stackParent.get() != null) {
            if (this.stackParent.get().isDirectStackChild(this)) {
                this.stackParent.get().displayedComponentProperty().set(this);
            }
        }
        super.showToFront(viewProvider, useCache);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispatchRemovedPropertyValue(final boolean value) {
        super.dispatchRemovedPropertyValue(value);
        ObservableList<GridPartComponentI> children = this.grid.getGridContent();
        for (GridPartComponentI child : children) {
            child.dispatchRemovedPropertyValue(value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispatchDisplayedProperty(final boolean displayedP) {
        super.dispatchDisplayedProperty(displayedP);
        ObservableList<GridPartComponentI> children = this.grid.getGridContent();
        for (GridPartComponentI child : children) {
            child.dispatchDisplayedProperty(displayedP);
        }
    }

    @Override
    public void forEachKeys(final Consumer<GridPartKeyComponentI> action) {
        ObservableList<GridPartComponentI> children = this.grid.getGridContent();
        for (GridPartComponentI child : children) {
            child.forEachKeys(action);
        }
    }

    //========================================================================

    // Class part : "XML"
    //========================================================================

    @Override
    public Element serialize(final IOContextI contextP) {
        Element element = super.serialize(contextP);
        XMLObjectSerializer.serializeInto(GridPartGridComponent.class, this, element);

        //Selection mode parameter (only if different from parent mode)
        if (!this.useParentSelectionMode.get()) {
            element.addContent(this.selectionModeParameter.serialize(contextP));
        }

        //Grid
        Element gridElement = this.grid.serialize(contextP);
        element.addContent(gridElement);
        return element;
    }

    @Override
    public void deserialize(final Element nodeP, final IOContextI contextP) throws LCException {
        super.deserialize(nodeP, contextP);
        XMLObjectSerializer.deserializeInto(GridPartGridComponent.class, this, nodeP);

        //Selection mode parameter (only if different from parent)
        Element selectionModeParameter = nodeP.getChild(SelectionModeParameter.NODE_SELECTION_MODE);
        if (selectionModeParameter != null && !useParentSelectionMode.get()) {
            this.selectionModeParameter.deserialize(selectionModeParameter, contextP);
        }

        //Grid
        Element gridElement = nodeP.getChild(ComponentGrid.NODE_GRID);
        this.grid.deserialize(gridElement, contextP);
    }

    //========================================================================

    // Class part : "Tree part"
    //========================================================================
    @SuppressWarnings("unchecked")
    @Override
    public ObservableList<? extends TreeDisplayableComponentI> getChildrenNode() {
        return this.grid.getGridContent();
    }

    @Override
    public boolean isNodeLeaf() {
        return false;
    }

    @Override
    public TreeDisplayableType getNodeType() {
        return TreeDisplayableType.GRID;
    }
    //========================================================================

}

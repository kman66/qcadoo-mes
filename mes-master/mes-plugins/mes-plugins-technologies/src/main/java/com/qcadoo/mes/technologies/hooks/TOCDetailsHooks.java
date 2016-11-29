/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 1.4
 *
 * This file is part of Qcadoo.
 *
 * Qcadoo is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * ***************************************************************************
 */
package com.qcadoo.mes.technologies.hooks;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.qcadoo.mes.technologies.constants.AssignedToOperation;
import com.qcadoo.mes.technologies.constants.OperationFields;
import com.qcadoo.mes.technologies.constants.TechnologyOperationComponentFields;
import com.qcadoo.model.api.Entity;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FieldComponent;
import com.qcadoo.view.api.components.FormComponent;
import com.qcadoo.view.api.components.GridComponent;
import com.qcadoo.view.api.components.LookupComponent;
import com.qcadoo.view.api.components.WindowComponent;
import com.qcadoo.view.api.components.lookup.FilterValueHolder;
import com.qcadoo.view.api.ribbon.RibbonActionItem;

@Service
public class TOCDetailsHooks {

    public static final String L_FORM = "form";

    private static final String L_WORKSTATION_LOOKUP = "workstationLookup";

    private static final List<String> L_WORKSTATIONS_TAB_FIELDS = Arrays
            .asList(TechnologyOperationComponentFields.ASSIGNED_TO_OPERATION,
                    TechnologyOperationComponentFields.QUANTITY_OF_WORKSTATIONS);

    private static final List<String> L_WORKSTATIONS_TAB_LOOKUPS = Arrays.asList(
            TechnologyOperationComponentFields.PRODUCTION_LINE, TechnologyOperationComponentFields.DIVISION,
            TechnologyOperationComponentFields.WORKSTATION_TYPE);

    public final void onBeforeRender(final ViewDefinitionState view) {
        disableWorkstationsTabFieldsIfOperationIsNotSaved(view);
        setWorkstationsCriteriaModifiers(view);
    }

    public void setProductionLineLookup(final ViewDefinitionState view) {

        clearLookupField(view, OperationFields.PRODUCTION_LINE);
        clearWorkstationsField(view);
        setProductionLineCriteriaModifiers(view);
    }

    private void setWorkstationsCriteriaModifiers(final ViewDefinitionState view) {
        LookupComponent productionLineLookup = (LookupComponent) view.getComponentByReference(OperationFields.PRODUCTION_LINE);
        LookupComponent workstationLookup = (LookupComponent) view.getComponentByReference(L_WORKSTATION_LOOKUP);
        Entity productionLine = productionLineLookup.getEntity();
        FilterValueHolder filter = workstationLookup.getFilterValue();
        if (productionLine != null) {
            filter.put(OperationFields.PRODUCTION_LINE, productionLine.getId());
        } else {
            filter.remove(OperationFields.PRODUCTION_LINE);
        }
        workstationLookup.setFilterValue(filter);

    }

    public void setWorkstationsLookup(final ViewDefinitionState view) {
        clearWorkstationsField(view);
        setWorkstationsCriteriaModifiers(view);
    }

    private void setProductionLineCriteriaModifiers(final ViewDefinitionState view) {

        LookupComponent productionLineLookup = (LookupComponent) view
                .getComponentByReference(TechnologyOperationComponentFields.PRODUCTION_LINE);
        LookupComponent divisionLookup = (LookupComponent) view
                .getComponentByReference(TechnologyOperationComponentFields.DIVISION);
        Entity division = divisionLookup.getEntity();
        FilterValueHolder filter = productionLineLookup.getFilterValue();
        if (division != null) {
            filter.put(TechnologyOperationComponentFields.DIVISION, division.getId());
        } else {
            filter.remove(TechnologyOperationComponentFields.DIVISION);
        }
        productionLineLookup.setFilterValue(filter);
    }

    private void disableWorkstationsTabFieldsIfOperationIsNotSaved(ViewDefinitionState view) {
        FormComponent operationForm = (FormComponent) view.getComponentByReference(L_FORM);
        GridComponent workstations = (GridComponent) view
                .getComponentByReference(TechnologyOperationComponentFields.WORKSTATIONS);

        if (operationForm.getEntityId() == null) {
            changedEnabledFields(view, L_WORKSTATIONS_TAB_FIELDS, false);
            changeEnabledLookups(view, L_WORKSTATIONS_TAB_LOOKUPS, Lists.newArrayList(""));
            workstations.setEnabled(false);

        } else {
            changedEnabledFields(view, L_WORKSTATIONS_TAB_FIELDS, true);
            changeEnabledLookups(view, L_WORKSTATIONS_TAB_LOOKUPS, L_WORKSTATIONS_TAB_LOOKUPS);
            workstations.setEnabled(true);
            setWorkstationsTabFields(view);
        }
    }

    private void changedEnabledFields(final ViewDefinitionState view, final List<String> references, final boolean enabled) {
        for (String reference : references) {
            FieldComponent field = (FieldComponent) view.getComponentByReference(reference);
            field.setEnabled(enabled);
        }
    }

    private void changeEnabledLookups(final ViewDefinitionState view, final List<String> fields, final List<String> enabledFields) {
        for (String field : fields) {
            LookupComponent lookup = (LookupComponent) view.getComponentByReference(field);
            lookup.setEnabled(enabledFields.contains(field));
        }
    }

    public void setWorkstationsTabFields(final ViewDefinitionState view) {
        FieldComponent assignedToOperation = (FieldComponent) view
                .getComponentByReference(TechnologyOperationComponentFields.ASSIGNED_TO_OPERATION);
        String assignedToOperationValue = (String) assignedToOperation.getFieldValue();
        GridComponent workstations = (GridComponent) view
                .getComponentByReference(TechnologyOperationComponentFields.WORKSTATIONS);

        if (AssignedToOperation.WORKSTATIONS.getStringValue().equals(assignedToOperationValue)) {
            changeEnabledLookups(view, L_WORKSTATIONS_TAB_LOOKUPS,
                    Lists.newArrayList(OperationFields.DIVISION, OperationFields.PRODUCTION_LINE));
            workstations.setEnabled(true);
            enableRibbonItem(view, !workstations.getEntities().isEmpty());
        } else if (AssignedToOperation.WORKSTATIONS_TYPE.getStringValue().equals(assignedToOperationValue)) {
            changeEnabledLookups(view, L_WORKSTATIONS_TAB_LOOKUPS,
                    Lists.newArrayList(TechnologyOperationComponentFields.WORKSTATION_TYPE));
            workstations.setEnabled(false);
            enableRibbonItem(view, false);
        }
    }

    public void clearWorkstationsField(final ViewDefinitionState view) {
        GridComponent workstations = (GridComponent) view
                .getComponentByReference(TechnologyOperationComponentFields.WORKSTATIONS);
        FormComponent operationForm = (FormComponent) view.getComponentByReference(L_FORM);
        Entity operation = operationForm.getEntity();
        List<Entity> entities = Lists.newArrayList();
        workstations.setEntities(entities);
        workstations.setFieldValue(null);
        operation.setField(OperationFields.WORKSTATIONS, null);
        Entity savedOperation = operation.getDataDefinition().save(operation);
        operationForm.setEntity(savedOperation);
    }

    public void clearLookupField(final ViewDefinitionState view, String fieldName) {
        LookupComponent lookup = (LookupComponent) view.getComponentByReference(fieldName);
        lookup.setFieldValue(null);
        lookup.requestComponentUpdateState();
    }

    private void enableRibbonItem(final ViewDefinitionState view, final boolean enable) {
        WindowComponent window = (WindowComponent) view.getComponentByReference("window");
        RibbonActionItem addUp = window.getRibbon().getGroupByName("workstations").getItemByName("addUpTheNumberOfWorktations");
        addUp.setEnabled(enable);
        addUp.requestUpdate(true);
    }

}

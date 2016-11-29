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
package com.qcadoo.mes.orders.listeners;

import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;
import com.qcadoo.mes.orders.OrderService;
import com.qcadoo.mes.orders.TechnologyServiceO;
import com.qcadoo.mes.orders.constants.OrderFields;
import com.qcadoo.mes.orders.constants.OrderType;
import com.qcadoo.mes.orders.hooks.OrderDetailsHooks;
import com.qcadoo.mes.orders.states.constants.OrderState;
import com.qcadoo.model.api.Entity;
import com.qcadoo.view.api.ComponentState;
import com.qcadoo.view.api.ComponentState.MessageType;
import com.qcadoo.view.api.ViewDefinitionState;
import com.qcadoo.view.api.components.FieldComponent;
import com.qcadoo.view.api.components.FormComponent;
import com.qcadoo.view.api.components.LookupComponent;

@Service
public class OrderDetailsListeners {

    private static final String L_FORM = "form";

    private static final String L_PLANNED_DATE_FROM = "plannedDateFrom";

    private static final String L_PLANNED_DATE_TO = "plannedDateTo";

    private static final String L_EFFECTIVE_DATE_FROM = "effectiveDateFrom";

    private static final String L_EFFECTIVE_DATE_TO = "effectiveDateTo";

    @Autowired
    private TechnologyServiceO technologyServiceO;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailsHooks orderDetailsHooks;

    public void clearAddress(final ViewDefinitionState view, final ComponentState state, final String[] args) {
        LookupComponent address = (LookupComponent) view.getComponentByReference(OrderFields.ADDRESS);
        address.setFieldValue(null);
    }

    public void setDefaultNameUsingTechnology(final ViewDefinitionState view, final ComponentState state, final String[] args) {
        if (!(state instanceof FieldComponent)) {
            return;
        }

        LookupComponent productLookup = (LookupComponent) view.getComponentByReference(OrderFields.PRODUCT);
        LookupComponent technologyLookup = (LookupComponent) view.getComponentByReference(OrderFields.TECHNOLOGY_PROTOTYPE);
        FieldComponent nameField = (FieldComponent) view.getComponentByReference(OrderFields.NAME);

        Entity product = productLookup.getEntity();
        Entity technology = technologyLookup.getEntity();

        if ((product == null) || (nameField.getFieldValue() != null && !nameField.getFieldValue().equals(""))) {
            return;
        }

        Locale locale = state.getLocale();
        nameField.setFieldValue(orderService.makeDefaultName(product, technology, locale));
    }

    public final void fillProductionLine(final ViewDefinitionState view, final ComponentState state, final String[] args) {
        orderDetailsHooks.fillProductionLine(view);
    }

    public void showCopyOfTechnology(final ViewDefinitionState view, final ComponentState state, final String[] args) {
        Long orderId = (Long) state.getFieldValue();

        if (orderId != null) {
            Entity order = orderService.getOrder(orderId);

            String orderType = order.getStringField(OrderFields.ORDER_TYPE);

            if (OrderType.WITH_PATTERN_TECHNOLOGY.getStringValue().equals(orderType)) {
                LookupComponent patternTechnologyLookup = (LookupComponent) view
                        .getComponentByReference(OrderFields.TECHNOLOGY_PROTOTYPE);

                if (patternTechnologyLookup.getEntity() == null) {
                    state.addMessage("order.technology.patternTechnology.not.set", MessageType.INFO);

                    return;
                }
            }

            Long technologyId = order.getBelongsToField(OrderFields.TECHNOLOGY).getId();
            Map<String, Object> parameters = Maps.newHashMap();
            parameters.put("form.id", technologyId);

            String url = "../page/orders/copyOfTechnologyDetails.html";
            view.redirectTo(url, false, true, parameters);
        }
    }

    private void copyDate(final ViewDefinitionState view, final String fromNameField, final String toNameField) {
        FormComponent orderForm = (FormComponent) view.getComponentByReference(L_FORM);
        FieldComponent fromField = (FieldComponent) view.getComponentByReference(fromNameField);
        FieldComponent toField = (FieldComponent) view.getComponentByReference(toNameField);

        Long orderId = orderForm.getEntityId();

        if (orderId == null) {
            toField.setFieldValue(fromField.getFieldValue());

            return;
        }

        Entity order = orderService.getOrder(orderId);

        if (!fromField.getFieldValue().equals(order.getField(fromNameField))) {
            toField.setFieldValue(fromField.getFieldValue());
        }

        toField.requestComponentUpdateState();
    }

    public void copyStartDate(final ViewDefinitionState view, final ComponentState state, final String[] args) {
        if (state.getName().equals(L_PLANNED_DATE_FROM)) {
            copyDate(view, L_PLANNED_DATE_FROM, OrderFields.DATE_FROM);
        } else if (state.getName().equals(L_EFFECTIVE_DATE_FROM)) {
            copyDate(view, OrderFields.EFFECTIVE_DATE_FROM, OrderFields.DATE_FROM);
        } else {
            copyDate(view, OrderFields.CORRECTED_DATE_FROM, OrderFields.DATE_FROM);
        }
    }

    public void copyEndDate(final ViewDefinitionState view, final ComponentState state, final String[] args) {
        if (state.getName().equals(L_PLANNED_DATE_TO)) {
            copyDate(view, L_PLANNED_DATE_TO, OrderFields.DATE_TO);
        } else if (state.getName().equals(L_EFFECTIVE_DATE_TO)) {
            copyDate(view, OrderFields.EFFECTIVE_DATE_TO, OrderFields.DATE_TO);
        } else {
            copyDate(view, OrderFields.CORRECTED_DATE_TO, OrderFields.DATE_TO);
        }
    }

    public void copyStartDateToDetails(final ViewDefinitionState view, final ComponentState state, final String[] args) {
        FormComponent orderForm = (FormComponent) view.getComponentByReference(L_FORM);

        Long orderId = orderForm.getEntityId();

        if (orderId == null) {
            copyDate(view, OrderFields.DATE_FROM, L_PLANNED_DATE_FROM);
            return;
        }

        Entity order = orderService.getOrder(orderId);

        String orderState = order.getStringField(OrderFields.STATE);

        if (OrderState.PENDING.getStringValue().equals(orderState)) {
            copyDate(view, OrderFields.DATE_FROM, L_PLANNED_DATE_FROM);
        }
        if (OrderState.IN_PROGRESS.getStringValue().equals(orderState) || OrderState.ABANDONED.getStringValue().equals(orderState)
                || OrderState.COMPLETED.getStringValue().equals(orderState)) {
            copyDate(view, OrderFields.DATE_FROM, L_EFFECTIVE_DATE_FROM);
        }
        if ((OrderState.ACCEPTED.getStringValue().equals(orderState))) {
            copyDate(view, OrderFields.DATE_FROM, OrderFields.CORRECTED_DATE_FROM);
        }
    }

    public void copyFinishDateToDetails(final ViewDefinitionState view, final ComponentState state, final String[] args) {
        FormComponent orderForm = (FormComponent) view.getComponentByReference(L_FORM);

        Long orderId = orderForm.getEntityId();

        if (orderId == null) {
            copyDate(view, OrderFields.DATE_TO, L_PLANNED_DATE_TO);
            return;
        }

        Entity order = orderService.getOrder(orderId);

        String orderState = order.getStringField(OrderFields.STATE);

        if (OrderState.PENDING.getStringValue().equals(orderState)) {
            copyDate(view, OrderFields.DATE_TO, L_PLANNED_DATE_TO);
        }
        if (OrderState.COMPLETED.getStringValue().equals(orderState)
                || OrderState.ABANDONED.getStringValue().equals(orderState)) {
            copyDate(view, OrderFields.DATE_TO, L_EFFECTIVE_DATE_TO);
        }
        if (OrderState.ACCEPTED.getStringValue().equals(orderState)
                || OrderState.IN_PROGRESS.getStringValue().equals(orderState)) {
            copyDate(view, OrderFields.DATE_TO, OrderFields.CORRECTED_DATE_TO);
        }
    }

    public void changeOrderProduct(final ViewDefinitionState view, final ComponentState state, final String[] args) {
        FieldComponent orderTypeField = (FieldComponent) view.getComponentByReference(OrderFields.ORDER_TYPE);

        if (OrderType.WITH_PATTERN_TECHNOLOGY.getStringValue().equals(orderTypeField.getFieldValue())) {
            LookupComponent productLookup = (LookupComponent) view.getComponentByReference(OrderFields.PRODUCT);
            LookupComponent technologyLookup = (LookupComponent) view.getComponentByReference(OrderFields.TECHNOLOGY_PROTOTYPE);
            FieldComponent defaultTechnologyField = (FieldComponent) view.getComponentByReference(OrderFields.DEFAULT_TECHNOLOGY);

            Entity product = productLookup.getEntity();

            defaultTechnologyField.setFieldValue(null);
            technologyLookup.setFieldValue(null);

            if (product != null) {
                Entity defaultTechnologyEntity = technologyServiceO.getDefaultTechnology(product);

                if (defaultTechnologyEntity != null) {
                    technologyLookup.setFieldValue(defaultTechnologyEntity.getId());
                }
            }
        }

        LookupComponent recipeLookup = (LookupComponent) view.getComponentByReference(OrderFields.RECIPE);
        recipeLookup.setFieldValue(null);

    }

    public void onOrderTypeChange(final ViewDefinitionState view, final ComponentState state, final String[] args) {
        orderDetailsHooks.setFieldsVisibilityAndFill(view);

        final FormComponent orderForm = (FormComponent) view.getComponentByReference(L_FORM);

        Long orderId = orderForm.getEntityId();

        if (orderId != null) {
            FieldComponent orderTypeField = (FieldComponent) view.getComponentByReference(OrderFields.ORDER_TYPE);

            boolean selectForPatternTechnology = OrderType.WITH_PATTERN_TECHNOLOGY.getStringValue()
                    .equals(orderTypeField.getFieldValue());

            if (selectForPatternTechnology) {
                orderForm.addMessage("order.orderType.changeOrderType", MessageType.INFO, false);
            }
        }
    }

    public void printOrderReport(final ViewDefinitionState view, final ComponentState state, final String[] args) {
        state.performEvent(view, "save", args);

        if (!state.isHasError()) {
            view.redirectTo("/orders/ordersOrderReport." + args[0] + "?id=" + state.getFieldValue(), true, false);
        }
    }
}

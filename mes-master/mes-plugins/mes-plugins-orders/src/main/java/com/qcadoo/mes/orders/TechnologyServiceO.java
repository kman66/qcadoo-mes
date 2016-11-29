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
package com.qcadoo.mes.orders;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;
import com.qcadoo.mes.basic.ShiftsService;
import com.qcadoo.mes.basic.constants.ProductFields;
import com.qcadoo.mes.orders.constants.OrderFields;
import com.qcadoo.mes.orders.constants.OrderType;
import com.qcadoo.mes.states.StateChangeContext;
import com.qcadoo.mes.states.constants.StateChangeStatus;
import com.qcadoo.mes.states.service.StateChangeContextBuilder;
import com.qcadoo.mes.technologies.constants.TechnologiesConstants;
import com.qcadoo.mes.technologies.constants.TechnologyFields;
import com.qcadoo.mes.technologies.constants.TechnologyType;
import com.qcadoo.mes.technologies.states.aop.TechnologyStateChangeAspect;
import com.qcadoo.mes.technologies.states.constants.TechnologyStateChangeFields;
import com.qcadoo.mes.technologies.states.constants.TechnologyStateStringValues;
import com.qcadoo.model.api.DataDefinition;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.EntityTree;
import com.qcadoo.model.api.EntityTreeNode;
import com.qcadoo.model.api.search.SearchRestrictions;
import com.qcadoo.model.api.search.SearchResult;
import com.qcadoo.security.api.SecurityService;
import com.qcadoo.view.api.utils.NumberGeneratorService;

@Service
public class TechnologyServiceO {

    @Autowired
    private DataDefinitionService dataDefinitionService;

    @Autowired
    private NumberGeneratorService numberGeneratorService;

    @Autowired
    private TechnologyStateChangeAspect technologyStateChangeAspect;

    @Autowired
    private StateChangeContextBuilder stateChangeContextBuilder;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private ShiftsService shiftsService;

    @Autowired
    private SecurityService securityService;

    @Transactional
    public void createOrUpdateTechnology(final DataDefinition orderDD, final Entity order) {
        OrderType orderType = OrderType.of(order);
        Entity technologyPrototype = order.getBelongsToField(OrderFields.TECHNOLOGY_PROTOTYPE);

        if (orderType == OrderType.WITH_PATTERN_TECHNOLOGY) {
            if (technologyPrototype == null) {
                removeTechnologyFromOrder(order);
            } else {
                createOrUpdateTechnologyForWithPatternTechnology(order, technologyPrototype);
            }
        } else if (orderType == OrderType.WITH_OWN_TECHNOLOGY) {
            createOrUpdateForOwnTechnology(order, technologyPrototype);
        }
    }

    private void removeTechnologyFromOrder(final Entity order) {
        Entity orderTechnology = order.getBelongsToField(OrderFields.TECHNOLOGY);
        deleteTechnology(orderTechnology);

        order.setField(OrderFields.TECHNOLOGY, null);
    }

    private void createOrUpdateTechnologyForWithPatternTechnology(final Entity order, final Entity technologyPrototype) {
        Entity existingOrder = getExistingOrder(order);

        if (isTechnologyCopied(order)) {
            if (isOrderTypeChangedToWithPatternTechnology(order)) {
                Entity technology = order.getBelongsToField(OrderFields.TECHNOLOGY);

                deleteTechnology(technology);

                order.setField(OrderFields.TECHNOLOGY, copyTechnology(order, technologyPrototype));
            } else if (technologyWasChanged(order)) {
                Entity technology = order.getBelongsToField(OrderFields.TECHNOLOGY);

                deleteTechnology(technology);

                order.setField(OrderFields.TECHNOLOGY, copyTechnology(order, technologyPrototype));
            }
        } else {
            if (existingOrder == null) {
                order.setField(OrderFields.TECHNOLOGY, copyTechnology(order, technologyPrototype));
            } else if (!isTechnologySet(order)) {
                order.setField(OrderFields.TECHNOLOGY, copyTechnology(order, technologyPrototype));
            }
        }
    }

    private void createOrUpdateForOwnTechnology(final Entity order, final Entity technologyPrototype) {
        Entity existingOrder = getExistingOrder(order);

        if (isTechnologyCopied(order)) {
            if (technologyPrototype != null) {
                Entity technology = order.getBelongsToField(OrderFields.TECHNOLOGY);

                updateTechnology(technology);

                order.setField(OrderFields.TECHNOLOGY_PROTOTYPE, null);
            } else {
                Entity technology = order.getBelongsToField(OrderFields.TECHNOLOGY);
                if (technology != null) {
                    technology.setField(TechnologyFields.PRODUCT, order.getBelongsToField(OrderFields.PRODUCT));
                    technology.getDataDefinition().save(technology);
                }
                order.getGlobalErrors();
            }
        } else {
            if (existingOrder == null) {
                order.setField(OrderFields.TECHNOLOGY, createTechnology(order));

                if (technologyPrototype != null) {
                    order.setField(OrderFields.TECHNOLOGY_PROTOTYPE, null);
                }
            } else if (existingOrder.getBelongsToField(OrderFields.TECHNOLOGY) == null) {
                order.setField(OrderFields.TECHNOLOGY, createTechnology(order));

                if (technologyPrototype != null) {
                    order.setField(OrderFields.TECHNOLOGY_PROTOTYPE, null);
                }
            }
        }
    }

    private Entity getExistingOrder(final Entity order) {
        if (order.getId() == null) {
            return null;
        }

        return order.getDataDefinition().get(order.getId());
    }

    private boolean isTechnologyCopied(final Entity order) {
        Entity technology = order.getBelongsToField(OrderFields.TECHNOLOGY);

        if (technology == null) {
            return false;
        }

        return true;
    }

    private boolean isTechnologySet(final Entity order) {
        Entity existingOrder = getExistingOrder(order);

        if (existingOrder == null) {
            return false;
        }

        Entity technology = existingOrder.getBelongsToField(OrderFields.TECHNOLOGY);

        if (technology == null) {
            return false;
        }

        return true;
    }

    private boolean isOrderTypeChangedToWithPatternTechnology(final Entity order) {
        Entity existingOrder = getExistingOrder(order);

        if (existingOrder == null) {
            return false;
        }

        String orderType = existingOrder.getStringField(OrderFields.ORDER_TYPE);

        if (OrderType.WITH_PATTERN_TECHNOLOGY.getStringValue().equals(orderType)) {
            return false;
        }

        return true;
    }

    private boolean technologyWasChanged(final Entity order) {
        Entity existingOrder = getExistingOrder(order);

        if (existingOrder == null) {
            return false;
        }

        Entity technology = order.getBelongsToField(OrderFields.TECHNOLOGY_PROTOTYPE);
        Entity existingOrderTechnology = existingOrder.getBelongsToField(OrderFields.TECHNOLOGY_PROTOTYPE);

        if (existingOrderTechnology == null) {
            return true;
        }

        if (!existingOrderTechnology.equals(technology)) {
            if (order.getBelongsToField(OrderFields.TECHNOLOGY) != null
                    && existingOrder.getBelongsToField(OrderFields.TECHNOLOGY) == null) {
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    private Entity createTechnology(final Entity order) {
        Entity newTechnology = getTechnologyDD().create();

        String number = generateNumberForTechnologyInOrder(order, null);

        Entity product = order.getBelongsToField(TechnologyFields.PRODUCT);
        Entity technologyPrototype = order.getBelongsToField(OrderFields.TECHNOLOGY_PROTOTYPE);

        newTechnology.setField(TechnologyFields.NUMBER, number);
        newTechnology.setField(TechnologyFields.NAME, makeTechnologyName(number, product));
        newTechnology.setField(TechnologyFields.PRODUCT, product);
        newTechnology.setField(TechnologyFields.TECHNOLOGY_PROTOTYPE, technologyPrototype);
        newTechnology.setField(TechnologyFields.TECHNOLOGY_TYPE, TechnologyType.WITH_OWN_TECHNOLOGY.getStringValue());

        newTechnology = newTechnology.getDataDefinition().save(newTechnology);

        return newTechnology;
    }

    private Entity copyTechnology(final Entity order, final Entity technologyPrototype) {
        Entity copyOfTechnology = getTechnologyDD().create();

        String number = generateNumberForTechnologyInOrder(order, technologyPrototype);

        copyOfTechnology = copyOfTechnology.getDataDefinition().copy(technologyPrototype.getId()).get(0);

        copyOfTechnology.setField(TechnologyFields.NUMBER, number);
        copyOfTechnology.setField(TechnologyFields.TECHNOLOGY_PROTOTYPE, technologyPrototype);
        copyOfTechnology.setField(TechnologyFields.TECHNOLOGY_TYPE, TechnologyType.WITH_PATTERN_TECHNOLOGY.getStringValue());

        copyOfTechnology = copyOfTechnology.getDataDefinition().save(copyOfTechnology);
        changeTechnologyStateToChecked(copyOfTechnology);

        return copyOfTechnology;
    }

    private void updateTechnology(final Entity technology) {
        String number = technology.getStringField(TechnologyFields.NUMBER);
        Entity product = technology.getBelongsToField(TechnologyFields.PRODUCT);

        technology.setField(TechnologyFields.NAME, makeTechnologyName(number, product));
        technology.setField(TechnologyFields.TECHNOLOGY_PROTOTYPE, null);
        technology.setField(TechnologyFields.TECHNOLOGY_TYPE, TechnologyType.WITH_OWN_TECHNOLOGY.getStringValue());

        EntityTree operationComponents = technology.getTreeField(TechnologyFields.OPERATION_COMPONENTS);

        if ((operationComponents != null) && !operationComponents.isEmpty()) {
            EntityTreeNode root = operationComponents.getRoot();

            root.getDataDefinition().delete(root.getId());
        }

        technology.setField(TechnologyFields.OPERATION_COMPONENTS, Lists.newArrayList());

        technology.getDataDefinition().save(technology);

        if (TechnologyStateStringValues.CHECKED.equals(technology.getStringField(TechnologyFields.STATE))) {
            changeTechnologyState(technology, TechnologyStateStringValues.DRAFT);
        }
    }

    private void deleteTechnology(final Entity technology) {
        if (technology == null || technology.getId() == null) {
            return;
        }
        technology.getDataDefinition().delete(technology.getId());
    }

    public DataDefinition getTechnologyDD() {
        return dataDefinitionService.get(TechnologiesConstants.PLUGIN_IDENTIFIER, TechnologiesConstants.MODEL_TECHNOLOGY);
    }

    public String makeTechnologyName(final String technologyNumber, final Entity product) {
        return technologyNumber + " - " + product.getStringField(ProductFields.NUMBER);
    }

    public Entity getDefaultTechnology(final Entity product) {
        SearchResult searchResult = getTechnologyDD().find().setMaxResults(1)
                .add(SearchRestrictions.eq(TechnologyFields.MASTER, true)).add(SearchRestrictions.eq("active", true))
                .add(SearchRestrictions.belongsTo(TechnologyFields.PRODUCT, product)).list();

        if (searchResult.getTotalNumberOfEntities() == 1) {
            return searchResult.getEntities().get(0);
        } else {
            return null;
        }
    }

    public String generateNumberForTechnologyInOrder(final Entity order, final Entity technology) {
        StringBuilder number = new StringBuilder();
        if (technology == null) {
            number.append(numberGeneratorService.generateNumber(TechnologiesConstants.PLUGIN_IDENTIFIER,
                    TechnologiesConstants.MODEL_TECHNOLOGY));
        } else {
            number.append(technology.getStringField(TechnologyFields.NUMBER));
        }
        number.append(" - ");
        number.append(order.getStringField(OrderFields.NUMBER));
        number.append(" - ");
        return numberGeneratorService.generateNumberWithPrefix(TechnologiesConstants.PLUGIN_IDENTIFIER,
                TechnologiesConstants.MODEL_TECHNOLOGY, 3, number.toString());
    }

    public void changeTechnologyState(final Entity technology, final String targetState) {
        final StateChangeContext stateChangeContextT = stateChangeContextBuilder
                .build(technologyStateChangeAspect.getChangeEntityDescriber(), technology, targetState);

        stateChangeContextT.setStatus(StateChangeStatus.IN_PROGRESS);
        technologyStateChangeAspect.changeState(stateChangeContextT);
    }

    public void changeTechnologyStateToChecked(Entity technology) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("state", TechnologyStateStringValues.CHECKED);
        paramMap.put("id", technology.getId());
        jdbcTemplate.update("UPDATE technologies_technology SET " + TechnologyFields.STATE + " = :state WHERE id = :id",
                paramMap);

        DataDefinition technologyStateChangeDD = dataDefinitionService.get(TechnologiesConstants.PLUGIN_IDENTIFIER,
                TechnologiesConstants.MODEL_TECHNOLOGY_STATE_CHANGE);
        Entity technologyStateChange = technologyStateChangeDD.create();
        technologyStateChange.setField(TechnologyStateChangeFields.SOURCE_STATE, TechnologyStateStringValues.DRAFT);
        technologyStateChange.setField(TechnologyStateChangeFields.TARGET_STATE, TechnologyStateStringValues.CHECKED);
        technologyStateChange.setField(TechnologyStateChangeFields.TECHNOLOGY, technology);
        technologyStateChange.setField(TechnologyStateChangeFields.STATUS, StateChangeStatus.SUCCESSFUL);
        technologyStateChange.setField(technologyStateChangeAspect.getChangeEntityDescriber().getDateTimeFieldName(), new Date());
        technologyStateChange.setField(technologyStateChangeAspect.getChangeEntityDescriber().getShiftFieldName(),
                shiftsService.getShiftFromDateWithTime(new Date()));
        technologyStateChange.setField(technologyStateChangeAspect.getChangeEntityDescriber().getWorkerFieldName(),
                securityService.getCurrentUserName());

        technologyStateChangeDD.save(technologyStateChange);
    }

    public void setTechnologyNumber(final DataDefinition orderDD, final Entity order) {
        String orderType = order.getStringField(OrderFields.ORDER_TYPE);
        Entity technology = order.getBelongsToField(OrderFields.TECHNOLOGY);
        if (technology == null) {
            return;
        }
        String number = "";
        if (OrderType.WITH_PATTERN_TECHNOLOGY.getStringValue().equals(orderType)) {
            number = generateNumberForTechnologyInOrder(order, order.getBelongsToField(OrderFields.TECHNOLOGY_PROTOTYPE));
        } else if (OrderType.WITH_OWN_TECHNOLOGY.getStringValue().equals(orderType)) {
            number = generateNumberForTechnologyInOrder(order, null);
        }
        technology.setField(TechnologyFields.NUMBER, number);
        technology = technology.getDataDefinition().save(technology);
        order.setField(OrderFields.TECHNOLOGY, technology);

    }

}

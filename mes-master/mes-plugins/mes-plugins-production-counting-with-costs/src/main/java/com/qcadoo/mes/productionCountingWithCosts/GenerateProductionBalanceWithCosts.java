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
package com.qcadoo.mes.productionCountingWithCosts;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.lowagie.text.DocumentException;
import com.qcadoo.mes.costCalculation.CostCalculationService;
import com.qcadoo.mes.costCalculation.constants.CostCalculationFields;
import com.qcadoo.mes.costCalculation.constants.SourceOfMaterialCosts;
import com.qcadoo.mes.costNormsForMaterials.ProductsCostCalculationService;
import com.qcadoo.mes.costNormsForOperation.constants.CalculationOperationComponentFields;
import com.qcadoo.mes.costNormsForOperation.constants.CostNormsForOperationConstants;
import com.qcadoo.mes.orders.constants.OrderFields;
import com.qcadoo.mes.productionCounting.ProductionBalanceService;
import com.qcadoo.mes.productionCounting.ProductionCountingService;
import com.qcadoo.mes.productionCounting.constants.BalanceOperationProductInComponentFields;
import com.qcadoo.mes.productionCounting.constants.OrderFieldsPC;
import com.qcadoo.mes.productionCounting.constants.ProductionBalanceFields;
import com.qcadoo.mes.productionCounting.constants.ProductionCountingConstants;
import com.qcadoo.mes.productionCounting.constants.ProductionTrackingFields;
import com.qcadoo.mes.productionCountingWithCosts.constants.CalculationOperationComponentFieldsPCWC;
import com.qcadoo.mes.productionCountingWithCosts.constants.OperationCostComponentFields;
import com.qcadoo.mes.productionCountingWithCosts.constants.OperationPieceworkCostComponentFields;
import com.qcadoo.mes.productionCountingWithCosts.constants.ProductionBalanceFieldsPCWC;
import com.qcadoo.mes.productionCountingWithCosts.constants.ProductionCountingWithCostsConstants;
import com.qcadoo.mes.productionCountingWithCosts.constants.TechnologyOperationProductInCompFields;
import com.qcadoo.mes.productionCountingWithCosts.materials.RegisteredMaterialCostHelper;
import com.qcadoo.mes.productionCountingWithCosts.operations.RegisteredProductionCostHelper;
import com.qcadoo.mes.productionCountingWithCosts.pdf.ProductionBalanceWithCostsPdfService;
import com.qcadoo.model.api.BigDecimalUtils;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.model.api.NumberService;
import com.qcadoo.model.api.file.FileService;
import com.qcadoo.model.api.search.SearchRestrictions;
import com.qcadoo.plugin.api.PluginUtils;

@Service
public class GenerateProductionBalanceWithCosts implements Observer {

    private static final String L_LABOR_COSTS = "laborCosts";

    private static final String L_MACHINE_COSTS = "machineCosts";

    private static final String L_PLANNED_MACHINE_TIME = "plannedMachineTime";

    private static final String L_PLANNED_LABOR_TIME = "plannedLaborTime";

    @Autowired
    private DataDefinitionService dataDefinitionService;

    @Autowired
    private NumberService numberService;

    @Autowired
    private FileService fileService;

    @Autowired
    private ProductionCountingService productionCountingService;

    @Autowired
    private ProductionBalanceService productionBalanceService;

    @Autowired
    private ProductionBalanceWithCostsPdfService productionBalanceWithCostsPdfService;

    @Autowired
    private RegisteredMaterialCostHelper registeredMaterialCostHelper;

    @Autowired
    private RegisteredProductionCostHelper registeredProductionCostHelper;

    @Autowired
    private CostCalculationService costCalculationService;

    @Autowired
    private ProductsCostCalculationService productsCostCalculationService;

    @Override
    public void update(final Observable observable, final Object object) {
        if (PluginUtils.isEnabled(ProductionCountingWithCostsConstants.PLUGIN_IDENTIFIER)) {
            Entity productionBalance = (Entity) object;

            doTheCostsPart(productionBalance);
            fillFieldsAndGrids(productionBalance);

            generateBalanceWithCostsReport(productionBalance);
        }
    }

    void generateBalanceWithCostsReport(final Entity productionBalance) {
        Locale locale = LocaleContextHolder.getLocale();

        String localePrefix = "productionCounting.productionBalanceWithCosts.report.fileName";

        Entity productionBalanceWithFileName = fileService.updateReportFileName(productionBalance, ProductionBalanceFields.DATE,
                localePrefix);

        String localePrefixToMatch = localePrefix;

        try {
            productionBalanceWithCostsPdfService.generateDocument(productionBalanceWithFileName, locale, localePrefixToMatch);

            productionBalanceWithFileName.setField(ProductionBalanceFieldsPCWC.GENERATED_WITH_COSTS, Boolean.TRUE);

            productionBalanceWithFileName.getDataDefinition().save(productionBalanceWithFileName);
        } catch (IOException e) {
            throw new IllegalStateException("Problem with saving productionBalanceWithCosts report", e);
        } catch (DocumentException e) {
            throw new IllegalStateException("Problem with generating productionBalanceWithCosts report", e);
        }
    }

    public void doTheCostsPart(final Entity productionBalance) {
        Entity order = productionBalance.getBelongsToField(ProductionBalanceFields.ORDER);
        Entity technology = order.getBelongsToField(OrderFields.TECHNOLOGY);
        Entity productionLine = order.getBelongsToField(OrderFields.PRODUCTION_LINE);

        BigDecimal quantity = order.getDecimalField(OrderFields.PLANNED_QUANTITY);

        productionBalance.setField(ProductionBalanceFieldsPCWC.QUANTITY, quantity);
        productionBalance.setField(ProductionBalanceFieldsPCWC.TECHNOLOGY, technology);
        productionBalance.setField(ProductionBalanceFieldsPCWC.PRODUCTION_LINE, productionLine);

        // FIXME MAKU beware of side effects - order of below computations matter!
        costCalculationService.calculateOperationsAndProductsCosts(productionBalance);
        final BigDecimal productionCosts = costCalculationService.calculateProductionCost(productionBalance);
        final BigDecimal doneQuantity = order.getDecimalField(OrderFields.DONE_QUANTITY);
        costCalculationService.calculateTotalCosts(productionBalance, productionCosts, doneQuantity);

        BigDecimal perUnit = BigDecimal.ZERO;
        if (!BigDecimalUtils.valueEquals(BigDecimal.ZERO, doneQuantity)) {
            BigDecimal totalTechnicalProductionCosts = productionBalance
                    .getDecimalField(ProductionBalanceFieldsPCWC.TOTAL_TECHNICAL_PRODUCTION_COSTS);
            perUnit = totalTechnicalProductionCosts.divide(doneQuantity, numberService.getMathContext());
        }

        productionBalance.setField(ProductionBalanceFieldsPCWC.TOTAL_TECHNICAL_PRODUCTION_COST_PER_UNIT,
                numberService.setScale(perUnit));
    }

    public void fillFieldsAndGrids(final Entity productionBalance) {
        Entity order = productionBalance.getBelongsToField(ProductionBalanceFields.ORDER);

        if ((order == null)
                || productionCountingService.isTypeOfProductionRecordingBasic(order
                        .getStringField(OrderFieldsPC.TYPE_OF_PRODUCTION_RECORDING))) {
            return;
        }

        List<Entity> productionTrackings = productionCountingService.getProductionTrackingsForOrder(order);

        Map<Long, Entity> productionTrackingsWithRegisteredTimes = productionBalanceService
                .groupProductionTrackingsRegisteredTimes(productionBalance, productionTrackings);

        Map<Entity, BigDecimal> productWithCosts = getPlannedProductsWithCosts(productionBalance, order);

        fillMaterialValues(productionBalance, productWithCosts);
        fillTechnologyOperationProductInComponents(productionBalance, productWithCosts);

        if (productionCountingService.isCalculateOperationCostModeHourly(productionBalance
                .getStringField(ProductionBalanceFields.CALCULATE_OPERATION_COST_MODE))
                && order.getBooleanField(OrderFieldsPC.REGISTER_PRODUCTION_TIME)) {
            Map<Long, Map<String, Integer>> productionTrackingsWithPlannedTimes = productionBalanceService
                    .fillProductionTrackingsWithPlannedTimes(productionBalance, productionTrackings);

            String typeOfProductionRecording = order.getStringField(OrderFieldsPC.TYPE_OF_PRODUCTION_RECORDING);

            if (productionCountingService.isTypeOfProductionRecordingForEach(typeOfProductionRecording)) {
                fillCostValues(productionBalance, productionTrackingsWithRegisteredTimes, productionTrackingsWithPlannedTimes);
                fillOperationCostComponents(productionBalance, productionTrackingsWithRegisteredTimes,
                        productionTrackingsWithPlannedTimes);
            } else if (productionCountingService.isTypeOfProductionRecordingCumulated(typeOfProductionRecording)) {
                fillCostValues(productionBalance, productionTrackingsWithRegisteredTimes, productionTrackingsWithPlannedTimes);
            }
        } else if (productionCountingService.isCalculateOperationCostModePiecework(productionBalance
                .getStringField(ProductionBalanceFields.CALCULATE_OPERATION_COST_MODE))
                && order.getBooleanField(OrderFieldsPC.REGISTER_PIECEWORK)) {
            fillPieceworkCostValues(productionBalance, productionTrackingsWithRegisteredTimes);
            fillOperationPieceworkCostComponents(productionBalance, productionTrackingsWithRegisteredTimes);
        }

        sumarizeCostValues(productionBalance, order);
    }

    private void fillMaterialValues(final Entity productionBalance, final Map<Entity, BigDecimal> productWithCosts) {
        if (productionBalance == null) {
            return;
        }

        BigDecimal componentsCosts = BigDecimal.ZERO;

        for (Entry<Entity, BigDecimal> productWithCost : productWithCosts.entrySet()) {
            Entity product = productWithCost.getKey();
            Entity balanceOperationProductInComponent = getBalanceOperationProductInComponentFromDB(productionBalance, product);

            if (balanceOperationProductInComponent != null) {
                BigDecimal registeredQuantity = balanceOperationProductInComponent
                        .getDecimalField(BalanceOperationProductInComponentFields.USED_QUANTITY);

                BigDecimal productRegisteredCost = BigDecimal.ZERO;

                if (registeredQuantity != null) {
                    productRegisteredCost = getRegisteredProductWithCost(
                            productionBalance,
                            productsCostCalculationService.getAppropriateCostNormForProduct(product,
                                    productionBalance.getBelongsToField(ProductionBalanceFields.ORDER),
                                    productionBalance.getStringField(ProductionBalanceFieldsPCWC.SOURCE_OF_MATERIAL_COSTS)),
                            registeredQuantity);
                }

                componentsCosts = componentsCosts.add(productRegisteredCost, numberService.getMathContext());
            }
        }

        final BigDecimal plannedComponentsCosts = BigDecimalUtils.convertNullToZero(productionBalance
                .getDecimalField(CostCalculationFields.TOTAL_MATERIAL_COSTS));
        BigDecimal componentsCostsBalance = componentsCosts.subtract(plannedComponentsCosts, numberService.getMathContext());

        productionBalance.setField(ProductionBalanceFieldsPCWC.PLANNED_COMPONENTS_COSTS,
                numberService.setScale(plannedComponentsCosts));
        productionBalance.setField(ProductionBalanceFieldsPCWC.COMPONENTS_COSTS, numberService.setScale(componentsCosts));
        productionBalance.setField(ProductionBalanceFieldsPCWC.COMPONENTS_COSTS_BALANCE,
                numberService.setScale(componentsCostsBalance));
    }

    private void fillTechnologyOperationProductInComponents(final Entity productionBalance,
            final Map<Entity, BigDecimal> productWithCosts) {
        if (productionBalance == null) {
            return;
        }

        List<Entity> technologyOperationProductInComponents = Lists.newArrayList();

        for (Entry<Entity, BigDecimal> productWithCost : productWithCosts.entrySet()) {
            Entity product = productWithCost.getKey();
            BigDecimal productCost = productWithCost.getValue();

            Entity balanceOperationProductInComponent = getBalanceOperationProductInComponentFromDB(productionBalance, product);

            if (balanceOperationProductInComponent != null) {
                BigDecimal registeredQuantity = balanceOperationProductInComponent
                        .getDecimalField(BalanceOperationProductInComponentFields.USED_QUANTITY);

                BigDecimal productRegisteredCost = BigDecimal.ZERO;

                if (registeredQuantity != null) {
                    productRegisteredCost = getRegisteredProductWithCost(
                            productionBalance,
                            productsCostCalculationService.getAppropriateCostNormForProduct(product,
                                    productionBalance.getBelongsToField(ProductionBalanceFields.ORDER),
                                    productionBalance.getStringField(ProductionBalanceFieldsPCWC.SOURCE_OF_MATERIAL_COSTS)),
                            registeredQuantity);
                }

                BigDecimal balance = productRegisteredCost.subtract(productCost, numberService.getMathContext());

                Entity technologyOperationProductInComponent = dataDefinitionService.get(
                        ProductionCountingWithCostsConstants.PLUGIN_IDENTIFIER,
                        ProductionCountingWithCostsConstants.MODEL_TECHNOLOGY_OPERATION_PRODUCT_IN_COMPONENT).create();

                technologyOperationProductInComponent.setField(TechnologyOperationProductInCompFields.PRODUCT, product);

                technologyOperationProductInComponent.setField(TechnologyOperationProductInCompFields.PLANNED_COST,
                        numberService.setScale(productCost));
                technologyOperationProductInComponent.setField(TechnologyOperationProductInCompFields.REGISTERED_COST,
                        numberService.setScale(productRegisteredCost));
                technologyOperationProductInComponent.setField(TechnologyOperationProductInCompFields.BALANCE,
                        numberService.setScale(balance));

                technologyOperationProductInComponents.add(technologyOperationProductInComponent);
            }
        }

        productionBalance.setField(ProductionBalanceFieldsPCWC.TECHNOLOGY_OPERATION_PRODUCT_IN_COMPONENTS,
                technologyOperationProductInComponents);
    }

    private void fillCostValues(final Entity productionBalance, final Map<Long, Entity> productionTrackingsWithRegisteredTimes,
            final Map<Long, Map<String, Integer>> productionTrackingsWithPlannedTimes) {
        if (productionBalance == null) {
            return;
        }

        Map<String, BigDecimal> costs = Maps.newHashMap();

        Entity order = productionBalance.getBelongsToField(ProductionBalanceFields.ORDER);

        String typeOfProductionRecording = order.getStringField(OrderFieldsPC.TYPE_OF_PRODUCTION_RECORDING);

        if (!productionTrackingsWithPlannedTimes.isEmpty()) {
            if (productionCountingService.isTypeOfProductionRecordingForEach(typeOfProductionRecording)) {
                costs = costValueForTypeOfProductionRecordingForEach(productionBalance, productionTrackingsWithRegisteredTimes);
            } else if (productionCountingService.isTypeOfProductionRecordingCumulated(typeOfProductionRecording)) {
                costs = costValueForTypeOfProductionRecordingCumulated(productionBalance, productionTrackingsWithRegisteredTimes);
            }
        }

        final BigDecimal plannedMachineCosts = BigDecimalUtils.convertNullToZero(productionBalance
                .getDecimalField(CostCalculationFields.TOTAL_MACHINE_HOURLY_COSTS));
        final BigDecimal plannedLaborCosts = BigDecimalUtils.convertNullToZero(productionBalance
                .getDecimalField(CostCalculationFields.TOTAL_LABOR_HOURLY_COSTS));
        final BigDecimal machineCosts = costs.get(L_MACHINE_COSTS);
        final BigDecimal laborCosts = costs.get(L_LABOR_COSTS);

        final BigDecimal machineCostsBalance = machineCosts.subtract(plannedMachineCosts, numberService.getMathContext());
        final BigDecimal laborCostsBalance = laborCosts.subtract(plannedLaborCosts, numberService.getMathContext());

        productionBalance
                .setField(ProductionBalanceFieldsPCWC.PLANNED_MACHINE_COSTS, numberService.setScale(plannedMachineCosts));
        productionBalance.setField(ProductionBalanceFieldsPCWC.MACHINE_COSTS, numberService.setScale(machineCosts));
        productionBalance
                .setField(ProductionBalanceFieldsPCWC.MACHINE_COSTS_BALANCE, numberService.setScale(machineCostsBalance));

        productionBalance.setField(ProductionBalanceFieldsPCWC.PLANNED_LABOR_COSTS, numberService.setScale(plannedLaborCosts));
        productionBalance.setField(ProductionBalanceFieldsPCWC.LABOR_COSTS, numberService.setScale(laborCosts));
        productionBalance.setField(ProductionBalanceFieldsPCWC.LABOR_COSTS_BALANCE, numberService.setScale(laborCostsBalance));
    }

    private Map<String, BigDecimal> costValueForTypeOfProductionRecordingForEach(final Entity productionBalance,
            final Map<Long, Entity> productionTrackingsWithRegisteredTimes) {
        Map<String, BigDecimal> costsValues = Maps.newHashMap();

        BigDecimal machineCosts = BigDecimal.ZERO;
        BigDecimal laborCosts = BigDecimal.ZERO;

        for (Map.Entry<Long, Entity> productionTrackingsWithRegisteredTimesEntry : productionTrackingsWithRegisteredTimes
                .entrySet()) {
            Entity productionTracking = productionTrackingsWithRegisteredTimesEntry.getValue();

            Entity calculationOperationComponent = getCalculationOperationComponent(productionBalance, productionTracking);

            if (calculationOperationComponent != null) {
                BigDecimal milisecondsInHour = BigDecimal.valueOf(3600);

                BigDecimal machineHourlyCost = BigDecimalUtils.convertNullToZero(calculationOperationComponent
                        .getDecimalField(CalculationOperationComponentFields.MACHINE_HOURLY_COST));

                Integer machineTime = productionTracking.getIntegerField(ProductionTrackingFields.MACHINE_TIME);

                BigDecimal machineTimeHours = BigDecimal.valueOf(machineTime).divide(milisecondsInHour,
                        numberService.getMathContext());

                machineCosts = machineCosts.add(machineHourlyCost.multiply(machineTimeHours, numberService.getMathContext()),
                        numberService.getMathContext());

                BigDecimal laborHourlyCost = BigDecimalUtils.convertNullToZero(calculationOperationComponent
                        .getDecimalField(CalculationOperationComponentFields.LABOR_HOURLY_COST));

                Integer laborTime = productionTracking.getIntegerField(ProductionTrackingFields.LABOR_TIME);

                BigDecimal laborTimeHours = BigDecimal.valueOf(laborTime).divide(milisecondsInHour,
                        numberService.getMathContext());

                laborCosts = laborCosts.add(laborHourlyCost.multiply(laborTimeHours, numberService.getMathContext()),
                        numberService.getMathContext());
            }
        }

        costsValues.put(L_MACHINE_COSTS, machineCosts);
        costsValues.put(L_LABOR_COSTS, laborCosts);

        return costsValues;
    }

    private Map<String, BigDecimal> costValueForTypeOfProductionRecordingCumulated(final Entity productionBalance,
            final Map<Long, Entity> productionTrackingsWithRegisteredTimes) {
        Map<String, BigDecimal> costsValues = Maps.newHashMap();

        BigDecimal machineCosts = BigDecimal.ZERO;
        BigDecimal laborCosts = BigDecimal.ZERO;

        for (Map.Entry<Long, Entity> productionTrackingWithRegisteredTimes : productionTrackingsWithRegisteredTimes.entrySet()) {
            Entity productionTracking = productionTrackingWithRegisteredTimes.getValue();

            BigDecimal milisecondsInHour = BigDecimal.valueOf(3600);

            BigDecimal averageMachineHourlyCost = BigDecimalUtils.convertNullToZero(productionBalance
                    .getDecimalField(ProductionBalanceFieldsPCWC.AVERAGE_MACHINE_HOURLY_COST));

            Integer machineTime = productionTracking.getIntegerField(ProductionTrackingFields.MACHINE_TIME);

            BigDecimal machineTimeHours = BigDecimal.valueOf(machineTime).divide(milisecondsInHour,
                    numberService.getMathContext());

            machineCosts = machineCosts.add(averageMachineHourlyCost.multiply(machineTimeHours, numberService.getMathContext()),
                    numberService.getMathContext());

            BigDecimal averageLaborHourlyCost = BigDecimalUtils.convertNullToZero(productionBalance
                    .getDecimalField(ProductionBalanceFieldsPCWC.AVERAGE_LABOR_HOURLY_COST));

            Integer laborTime = productionTracking.getIntegerField(ProductionTrackingFields.LABOR_TIME);

            BigDecimal laborTimeHours = BigDecimal.valueOf(laborTime).divide(milisecondsInHour, numberService.getMathContext());

            laborCosts = laborCosts.add(averageLaborHourlyCost.multiply(laborTimeHours, numberService.getMathContext()),
                    numberService.getMathContext());
        }

        costsValues.put(L_MACHINE_COSTS, machineCosts);
        costsValues.put(L_LABOR_COSTS, laborCosts);

        return costsValues;
    }

    private void fillOperationCostComponents(final Entity productionBalance,
            final Map<Long, Entity> productionTrackingsWithRegisteredTimes,
            final Map<Long, Map<String, Integer>> productionTrackingsWithPlannedTimes) {
        if (productionBalance == null) {
            return;
        }

        List<Entity> operationCostComponents = Lists.newArrayList();

        if (!productionTrackingsWithPlannedTimes.isEmpty()) {
            for (Map.Entry<Long, Entity> productionTrackingWithRegisteredTimes : productionTrackingsWithRegisteredTimes
                    .entrySet()) {
                Long technologyOperationComponentId = productionTrackingWithRegisteredTimes.getKey();
                Entity productionTracking = productionTrackingWithRegisteredTimes.getValue();

                Entity calculationOperationComponent = getCalculationOperationComponent(productionBalance, productionTracking);

                if (calculationOperationComponent != null) {
                    BigDecimal milisecondsInHour = BigDecimal.valueOf(3600);

                    BigDecimal machineHourlyCost = BigDecimalUtils.convertNullToZero(calculationOperationComponent
                            .getDecimalField(CalculationOperationComponentFields.MACHINE_HOURLY_COST));

                    Integer plannedMachineTime = productionTrackingsWithPlannedTimes.get(technologyOperationComponentId).get(
                            L_PLANNED_MACHINE_TIME);
                    BigDecimal plannedMachineTimeHours = BigDecimal.valueOf(plannedMachineTime).divide(milisecondsInHour,
                            numberService.getMathContext());

                    BigDecimal plannedMachineCosts = machineHourlyCost.multiply(plannedMachineTimeHours,
                            numberService.getMathContext());

                    Integer machineTime = productionTracking.getIntegerField(ProductionTrackingFields.MACHINE_TIME);

                    BigDecimal machineTimeHours = BigDecimal.valueOf(machineTime).divide(milisecondsInHour,
                            numberService.getMathContext());

                    BigDecimal machineCosts = machineHourlyCost.multiply(machineTimeHours, numberService.getMathContext());

                    BigDecimal machineCostsBalance = machineCosts.subtract(plannedMachineCosts, numberService.getMathContext());

                    BigDecimal laborHourlyCost = BigDecimalUtils.convertNullToZero(calculationOperationComponent
                            .getDecimalField(CalculationOperationComponentFields.LABOR_HOURLY_COST));

                    Integer plannedLaborTime = productionTrackingsWithPlannedTimes.get(technologyOperationComponentId).get(
                            L_PLANNED_LABOR_TIME);
                    BigDecimal plannedLaborTimeHours = BigDecimal.valueOf(plannedLaborTime).divide(milisecondsInHour,
                            numberService.getMathContext());

                    BigDecimal plannedLaborCosts = laborHourlyCost
                            .multiply(plannedLaborTimeHours, numberService.getMathContext());

                    Integer laborTime = productionTracking.getIntegerField(ProductionTrackingFields.LABOR_TIME);

                    BigDecimal laborTimeHours = BigDecimal.valueOf(laborTime).divide(milisecondsInHour,
                            numberService.getMathContext());

                    BigDecimal laborCosts = laborHourlyCost.multiply(laborTimeHours, numberService.getMathContext());

                    BigDecimal laborCostsBalance = laborCosts.subtract(plannedLaborCosts, numberService.getMathContext());

                    Entity operationCostComponent = dataDefinitionService.get(
                            ProductionCountingWithCostsConstants.PLUGIN_IDENTIFIER,
                            ProductionCountingWithCostsConstants.MODEL_OPERATION_COST_COMPONENT).create();

                    operationCostComponent.setField(OperationCostComponentFields.TECHNOLOGY_OPERATION_COMPONENT,
                            productionTracking.getBelongsToField(ProductionTrackingFields.TECHNOLOGY_OPERATION_COMPONENT));

                    operationCostComponent.setField(OperationCostComponentFields.PLANNED_MACHINE_COSTS,
                            numberService.setScale(plannedMachineCosts));
                    operationCostComponent.setField(OperationCostComponentFields.MACHINE_COSTS,
                            numberService.setScale(machineCosts));
                    operationCostComponent.setField(OperationCostComponentFields.MACHINE_COSTS_BALANCE,
                            numberService.setScale(machineCostsBalance));

                    operationCostComponent.setField(OperationCostComponentFields.PLANNED_LABOR_COSTS,
                            numberService.setScale(plannedLaborCosts));
                    operationCostComponent.setField(OperationCostComponentFields.LABOR_COSTS, numberService.setScale(laborCosts));
                    operationCostComponent.setField(OperationCostComponentFields.LABOR_COSTS_BALANCE,
                            numberService.setScale(laborCostsBalance));

                    operationCostComponents.add(operationCostComponent);
                }
            }

        }

        productionBalance.setField(ProductionBalanceFieldsPCWC.OPERATION_COST_COMPONENTS, operationCostComponents);
    }

    private void fillPieceworkCostValues(final Entity productionBalance,
            final Map<Long, Entity> productionTrackingsWithRegisteredTimes) {
        if (productionBalance == null) {
            return;
        }

        BigDecimal cyclesCosts = BigDecimal.ZERO;

        for (Map.Entry<Long, Entity> productionTrackingWithRegisteredTimes : productionTrackingsWithRegisteredTimes.entrySet()) {
            Entity productionTracking = productionTrackingWithRegisteredTimes.getValue();

            Entity calculationOperationComponent = getCalculationOperationComponent(productionBalance, productionTracking);

            if (calculationOperationComponent != null) {
                final BigDecimal pieces = BigDecimalUtils.convertNullToOne(calculationOperationComponent
                        .getDecimalField(CalculationOperationComponentFields.PIECES));

                final BigDecimal cost = BigDecimalUtils.convertNullToZero(
                        calculationOperationComponent.getDecimalField(CalculationOperationComponentFields.OPERATION_COST))
                        .divide(pieces, numberService.getMathContext());

                if (productionTracking.getField(ProductionTrackingFields.EXECUTED_OPERATION_CYCLES) != null) {
                    cyclesCosts = cyclesCosts.add(cost.multiply(
                            productionTracking.getDecimalField(ProductionTrackingFields.EXECUTED_OPERATION_CYCLES),
                            numberService.getMathContext()), numberService.getMathContext());
                }
            }
        }

        final BigDecimal plannedCyclesCosts = BigDecimalUtils.convertNullToZero(productionBalance
                .getDecimalField(CostCalculationFields.TOTAL_PIECEWORK_COSTS));

        final BigDecimal cyclesCostsBalance = cyclesCosts.subtract(plannedCyclesCosts, numberService.getMathContext());

        productionBalance.setField(ProductionBalanceFieldsPCWC.PLANNED_CYCLES_COSTS, numberService.setScale(plannedCyclesCosts));
        productionBalance.setField(ProductionBalanceFieldsPCWC.CYCLES_COSTS, numberService.setScale(cyclesCosts));
        productionBalance.setField(ProductionBalanceFieldsPCWC.CYCLES_COSTS_BALANCE, numberService.setScale(cyclesCostsBalance));
    }

    private void fillOperationPieceworkCostComponents(final Entity productionBalance,
            final Map<Long, Entity> productionTrackingsWithRegisteredTimes) {
        if (productionBalance == null) {
            return;
        }

        List<Entity> operationPieceworkCostComponents = Lists.newArrayList();

        for (Map.Entry<Long, Entity> productionTrackingWithRegisteredTimes : productionTrackingsWithRegisteredTimes.entrySet()) {
            Entity productionTracking = productionTrackingWithRegisteredTimes.getValue();

            Entity calculationOperationComponent = getCalculationOperationComponent(productionBalance, productionTracking);

            if (calculationOperationComponent != null) {
                final BigDecimal plannedCyclesCosts = BigDecimalUtils.convertNullToZero(calculationOperationComponent
                        .getDecimalField(CalculationOperationComponentFields.OPERATION_COST));
                final BigDecimal pieces = BigDecimalUtils.convertNullToOne(calculationOperationComponent
                        .getDecimalField(CalculationOperationComponentFields.PIECES));

                final BigDecimal cost = BigDecimalUtils.convertNullToZero(
                        calculationOperationComponent.getDecimalField(CalculationOperationComponentFields.OPERATION_COST))
                        .divide(pieces, numberService.getMathContext());

                BigDecimal cyclesCosts = BigDecimal.ZERO;

                if (productionTracking.getField(ProductionTrackingFields.EXECUTED_OPERATION_CYCLES) != null) {
                    cyclesCosts = cost.multiply(
                            productionTracking.getDecimalField(ProductionTrackingFields.EXECUTED_OPERATION_CYCLES),
                            numberService.getMathContext());
                }

                BigDecimal cyclesCostsBalance = cyclesCosts.subtract(plannedCyclesCosts, numberService.getMathContext());

                Entity operationPieceworkCostComponent = dataDefinitionService.get(
                        ProductionCountingWithCostsConstants.PLUGIN_IDENTIFIER,
                        ProductionCountingWithCostsConstants.MODEL_OPERATION_PIECEWORK_COST_COMPONENT).create();

                operationPieceworkCostComponent.setField(OperationPieceworkCostComponentFields.TECHNOLOGY_OPERATION_COMPONENT,
                        productionTracking.getBelongsToField(ProductionTrackingFields.TECHNOLOGY_OPERATION_COMPONENT));

                operationPieceworkCostComponent.setField(OperationPieceworkCostComponentFields.PLANNED_CYCLES_COSTS,
                        numberService.setScale(plannedCyclesCosts));
                operationPieceworkCostComponent.setField(OperationPieceworkCostComponentFields.CYCLES_COSTS,
                        numberService.setScale(cyclesCosts));
                operationPieceworkCostComponent.setField(OperationPieceworkCostComponentFields.CYCLES_COSTS_BALANCE,
                        numberService.setScale(cyclesCostsBalance));

                operationPieceworkCostComponents.add(operationPieceworkCostComponent);
            }
        }

        productionBalance.setField(ProductionBalanceFieldsPCWC.OPERATION_PIECEWORK_COST_COMPONENTS,
                operationPieceworkCostComponents);
    }

    private void sumarizeCostValues(final Entity productionBalance, final Entity order) {
        if ((productionBalance == null) || (order == null)) {
            return;
        }

        BigDecimal registeredTotalTechnicalProductionCosts = BigDecimal.ZERO;

        registeredTotalTechnicalProductionCosts = registeredTotalTechnicalProductionCosts.add(
                productionBalance.getDecimalField(ProductionBalanceFieldsPCWC.COMPONENTS_COSTS), numberService.getMathContext());

        if (productionCountingService.isCalculateOperationCostModeHourly(productionBalance
                .getStringField(ProductionBalanceFields.CALCULATE_OPERATION_COST_MODE))
                && order.getBooleanField(OrderFieldsPC.REGISTER_PRODUCTION_TIME)) {
            registeredTotalTechnicalProductionCosts = registeredTotalTechnicalProductionCosts.add(
                    productionBalance.getDecimalField(ProductionBalanceFieldsPCWC.MACHINE_COSTS), numberService.getMathContext());
            registeredTotalTechnicalProductionCosts = registeredTotalTechnicalProductionCosts.add(
                    productionBalance.getDecimalField(ProductionBalanceFieldsPCWC.LABOR_COSTS), numberService.getMathContext());
        } else if (productionCountingService.isCalculateOperationCostModePiecework(productionBalance
                .getStringField(ProductionBalanceFields.CALCULATE_OPERATION_COST_MODE))
                && order.getBooleanField(OrderFieldsPC.REGISTER_PIECEWORK)) {
            registeredTotalTechnicalProductionCosts = registeredTotalTechnicalProductionCosts.add(
                    productionBalance.getDecimalField(ProductionBalanceFieldsPCWC.CYCLES_COSTS), numberService.getMathContext());
        }

        BigDecimal balanceTechnicalProductionCosts = registeredTotalTechnicalProductionCosts.subtract(
                productionBalance.getDecimalField(ProductionBalanceFieldsPCWC.TOTAL_TECHNICAL_PRODUCTION_COSTS),
                numberService.getMathContext());

        productionBalance.setField(ProductionBalanceFieldsPCWC.REGISTERED_TOTAL_TECHNICAL_PRODUCTION_COSTS,
                numberService.setScale(registeredTotalTechnicalProductionCosts));

        productionBalance.setField(ProductionBalanceFieldsPCWC.BALANCE_TECHNICAL_PRODUCTION_COSTS,
                numberService.setScale(balanceTechnicalProductionCosts));

        registeredMaterialCostHelper.countRegisteredMaterialMarginValue(productionBalance);
        registeredProductionCostHelper.countRegisteredProductionMarginValue(productionBalance);
        costCalculationService.calculateTotalOverhead(productionBalance);

        BigDecimal totalCosts = registeredTotalTechnicalProductionCosts.add(
                BigDecimalUtils.convertNullToZero(productionBalance.getDecimalField(ProductionBalanceFieldsPCWC.TOTAL_OVERHEAD)),
                numberService.getMathContext());
        productionBalance.setField(ProductionBalanceFieldsPCWC.TOTAL_COSTS, numberService.setScale(totalCosts));

        final BigDecimal doneQuantity = order.getDecimalField(OrderFields.DONE_QUANTITY);

        if (doneQuantity != null && BigDecimal.ZERO.compareTo(doneQuantity) != 0) {
            final BigDecimal totalCostPerUnit = totalCosts.divide(doneQuantity, numberService.getMathContext());
            final BigDecimal registeredTotalTechnicalProductionCostPerUnit = registeredTotalTechnicalProductionCosts.divide(
                    doneQuantity, numberService.getMathContext());

            productionBalance.setField(ProductionBalanceFieldsPCWC.TOTAL_COST_PER_UNIT, numberService.setScale(totalCostPerUnit));
            productionBalance.setField(ProductionBalanceFieldsPCWC.REGISTERED_TOTAL_TECHNICAL_PRODUCTION_COST_PER_UNIT,
                    numberService.setScale(registeredTotalTechnicalProductionCostPerUnit));

            final BigDecimal balanceTechnicalProductionCostPerUnit = registeredTotalTechnicalProductionCostPerUnit.subtract(
                    productionBalance.getDecimalField(ProductionBalanceFieldsPCWC.TOTAL_TECHNICAL_PRODUCTION_COST_PER_UNIT),
                    numberService.getMathContext());
            productionBalance.setField(ProductionBalanceFieldsPCWC.BALANCE_TECHNICAL_PRODUCTION_COST_PER_UNIT,
                    numberService.setScale(balanceTechnicalProductionCostPerUnit));
        } else {
            productionBalance.setField(ProductionBalanceFieldsPCWC.TOTAL_COST_PER_UNIT, null);
            productionBalance.setField(ProductionBalanceFieldsPCWC.REGISTERED_TOTAL_TECHNICAL_PRODUCTION_COST_PER_UNIT, null);
            productionBalance.setField(ProductionBalanceFieldsPCWC.BALANCE_TECHNICAL_PRODUCTION_COST_PER_UNIT, null);
        }
    }

    private Map<Entity, BigDecimal> getPlannedProductsWithCosts(final Entity productionBalance, final Entity order) {
        BigDecimal givenQty = productionBalance.getDecimalField(ProductionBalanceFieldsPCWC.QUANTITY);

        String sourceOfMaterialCosts = productionBalance.getStringField(ProductionBalanceFieldsPCWC.SOURCE_OF_MATERIAL_COSTS);

        String calculateMaterialCostsMode = productionBalance
                .getStringField(ProductionBalanceFieldsPCWC.CALCULATE_MATERIAL_COSTS_MODE);

        if (SourceOfMaterialCosts.FROM_ORDERS_MATERIAL_COSTS.getStringValue().equals(sourceOfMaterialCosts)) {
            return productsCostCalculationService.getProductWithCostForPlannedQuantities(
                    productionBalance.getBelongsToField(ProductionBalanceFieldsPCWC.TECHNOLOGY), givenQty,
                    calculateMaterialCostsMode, order);
        } else if (SourceOfMaterialCosts.CURRENT_GLOBAL_DEFINITIONS_IN_PRODUCT.getStringValue().equals(sourceOfMaterialCosts)) {
            return productsCostCalculationService.getProductWithCostForPlannedQuantities(
                    productionBalance.getBelongsToField(ProductionBalanceFieldsPCWC.TECHNOLOGY), givenQty,
                    calculateMaterialCostsMode);
        }

        return Maps.newHashMap();
    }

    private BigDecimal getRegisteredProductWithCost(final Entity productionBalance, final Entity product,
            final BigDecimal registeredQuantity) {
        String calculateMaterialCostsMode = productionBalance
                .getStringField(ProductionBalanceFieldsPCWC.CALCULATE_MATERIAL_COSTS_MODE);

        return productsCostCalculationService.calculateProductCostForGivenQuantity(product, registeredQuantity,
                calculateMaterialCostsMode);
    }

    private Entity getBalanceOperationProductInComponentFromDB(final Entity productionBalance, final Entity product) {
        return dataDefinitionService
                .get(ProductionCountingConstants.PLUGIN_IDENTIFIER,
                        ProductionCountingConstants.MODEL_BALANCE_OPERATION_PRODUCT_IN_COMPONENT)
                .find()
                .add(SearchRestrictions.belongsTo(BalanceOperationProductInComponentFields.PRODUCTION_BALANCE, productionBalance))
                .add(SearchRestrictions.belongsTo(BalanceOperationProductInComponentFields.PRODUCT, product)).setMaxResults(1)
                .uniqueResult();
    }

    private Entity getCalculationOperationComponent(final Entity productionBalance, final Entity productionTracking) {
        Entity technologyOperationComponent = productionTracking
                .getBelongsToField(ProductionTrackingFields.TECHNOLOGY_OPERATION_COMPONENT);

        if (technologyOperationComponent == null) {
            return null;
        } else {
            return dataDefinitionService
                    .get(CostNormsForOperationConstants.PLUGIN_IDENTIFIER,
                            CostNormsForOperationConstants.MODEL_CALCULATION_OPERATION_COMPONENT)
                    .find()
                    .add(SearchRestrictions.belongsTo(CalculationOperationComponentFieldsPCWC.PRODUCTION_BALANCE,
                            productionBalance))
                    .add(SearchRestrictions.belongsTo(CalculationOperationComponentFields.TECHNOLOGY_OPERATION_COMPONENT,
                            technologyOperationComponent)).setMaxResults(1).uniqueResult();
        }
    }

}

-- Table: orders_order
-- changed: 20.05.2013

ALTER TABLE orders_order ADD COLUMN ordertype character varying(255) DEFAULT '01withPatternTechnology'::character varying;

-- end


-- Table: technologies_technology
-- changed: 05.06.2013

ALTER TABLE technologies_technology ADD COLUMN  technologytype character varying(255);

-- end


-- Table: technologies_technology
-- changed: 05.06.2013

ALTER TABLE technologies_technology ADD COLUMN  technologyprototype_id bigint;

ALTER TABLE technologies_technology
  ADD CONSTRAINT technology_technology_fkey FOREIGN KEY (technologyprototype_id)
      REFERENCES technologies_technology (id) DEFERRABLE;
      
-- end

      
-- Table: orders_order
-- changed: 05.06.2013

ALTER TABLE orders_order ADD COLUMN  technologyprototype_id bigint;

ALTER TABLE orders_order
  ADD CONSTRAINT order_technology_fkey FOREIGN KEY (technologyprototype_id)
      REFERENCES technologies_technology (id) DEFERRABLE;
    
-- end


-- Table: basicproductioncounting_productioncountingquantity
-- changed: 04.06.2013

ALTER TABLE basicproductioncounting_productioncountingquantity ADD COLUMN technologyoperationcomponent_id bigint;

ALTER TABLE basicproductioncounting_productioncountingquantity
  ADD CONSTRAINT productioncountingquantity_technologyoperationcomponent_fkey FOREIGN KEY (technologyoperationcomponent_id)
      REFERENCES technologies_technologyoperationcomponent (id) DEFERRABLE;


ALTER TABLE basicproductioncounting_productioncountingquantity ADD COLUMN basicproductioncounting_id bigint;

ALTER TABLE basicproductioncounting_productioncountingquantity
  ADD CONSTRAINT productioncountingquantity_basicproductioncounting_fkey FOREIGN KEY (basicproductioncounting_id)
      REFERENCES basicproductioncounting_basicproductioncounting (id) DEFERRABLE;
      
ALTER TABLE basicproductioncounting_productioncountingquantity ADD COLUMN typeofmaterial character varying(255);
ALTER TABLE basicproductioncounting_productioncountingquantity ALTER COLUMN typeofmaterial SET DEFAULT '01component'::character varying;

ALTER TABLE basicproductioncounting_productioncountingquantity ADD COLUMN role character varying(255);
ALTER TABLE basicproductioncounting_productioncountingquantity ALTER COLUMN role SET DEFAULT '01used'::character varying;
     
-- end


-- Table: technologies_technologyoperationcomponent
-- changed: 22.06.2013

ALTER TABLE technologies_technologyoperationcomponent ADD COLUMN createdate timestamp without time zone;
ALTER TABLE technologies_technologyoperationcomponent ADD COLUMN updatedate timestamp without time zone;
ALTER TABLE technologies_technologyoperationcomponent ADD COLUMN createuser character varying(255);
ALTER TABLE technologies_technologyoperationcomponent ADD COLUMN updateuser character varying(255);

-- end


-- Table: productioncounting_productionbalance
-- changed: 26.06.2013

ALTER TABLE productioncounting_productionbalance RENAME COLUMN recordsnumber TO trackingsnumber;

-- end


-- Table: timenormsforoperations_techopercomptimecalculation
-- changed: 22.06.2013

CREATE TABLE timenormsforoperations_techopercomptimecalculation
(
  id bigint NOT NULL,
  operationoffset integer,
  effectiveoperationrealizationtime integer,
  effectivedatefrom timestamp without time zone,
  effectivedateto timestamp without time zone,
  duration integer DEFAULT 0,
  machineworktime integer DEFAULT 0,
  laborworktime integer DEFAULT 0,
  CONSTRAINT timenormsforoperations_techopercomptimecalculation_pkey PRIMARY KEY (id)
);

-- end


-- Table: productioncounting_productionrecord
-- changed: 26.06.2013

ALTER TABLE productioncounting_productionrecord RENAME TO productioncounting_productiontracking;

ALTER TABLE productioncounting_productiontracking RENAME COLUMN lastrecord TO lasttracking;

ALTER TABLE productioncounting_productiontracking ADD COLUMN technologyoperationcomponent_id bigint;

ALTER TABLE productioncounting_productiontracking
  ADD CONSTRAINT productiontracking_technologyoperationcomponent_fkey FOREIGN KEY (technologyoperationcomponent_id)
      REFERENCES technologies_technologyoperationcomponent (id) DEFERRABLE;
      
-- end


-- Table: productioncounting_productionrecordstatechange
-- changed: 26.06.2013

ALTER TABLE productioncounting_productionrecordstatechange RENAME TO productioncounting_productiontrackingstatechange;

ALTER TABLE productioncounting_productiontrackingstatechange RENAME COLUMN productionrecord_id TO productiontracking_id;

-- end


-- Table: operationaltasksfororders_techopercompoperationaltask
-- changed: 22.06.2013

CREATE TABLE operationaltasksfororders_techopercompoperationaltask
(
  id bigint NOT NULL,
  technologyoperationcomponent_id bigint,
  CONSTRAINT operationaltasksfororders_techopercompoperationaltask_pkey PRIMARY KEY (id),
  CONSTRAINT techopercompoperationaltask_technologyoperationcomponent_fkey FOREIGN KEY (technologyoperationcomponent_id)
      REFERENCES technologies_technologyoperationcomponent (id) DEFERRABLE
);

ALTER TABLE operationaltasks_operationaltask ADD COLUMN techopercompoperationaltask_id bigint;      

ALTER TABLE operationaltasks_operationaltask
  ADD CONSTRAINT operationaltask_techopercompoperationaltask_fkey FOREIGN KEY (techopercompoperationaltask_id)
      REFERENCES operationaltasksfororders_techopercompoperationaltask (id) DEFERRABLE;

-- end

      
-- Table: productioncounting_productioncounting
-- changed: 26.06.2013

ALTER TABLE productioncounting_productioncounting RENAME TO productioncounting_productiontrackingreport;

-- end


-- Table: technologies_technologyoperationcomponent
-- changed: 22.06.2013

ALTER TABLE technologies_technologyoperationcomponent ADD COLUMN techopercomptimecalculation_id bigint;      

ALTER TABLE technologies_technologyoperationcomponent
  ADD CONSTRAINT technologyoperationcomponent_techopercomptimecalculation_fkey FOREIGN KEY (techopercomptimecalculation_id)
      REFERENCES timenormsforoperations_techopercomptimecalculation (id) DEFERRABLE;

-- end

      
-- Table: productioncounting_recordoperationproductincomponent
-- changed: 26.06.2013

ALTER TABLE productioncounting_recordoperationproductincomponent RENAME TO productioncounting_trackingoperationproductincomponent;

ALTER TABLE productioncounting_trackingoperationproductincomponent RENAME COLUMN productionrecord_id TO productiontracking_id;

-- end


-- Table: productioncounting_recordoperationproductoutcomponent
-- changed: 26.06.2013

ALTER TABLE productioncounting_recordoperationproductoutcomponent RENAME TO productioncounting_trackingoperationproductoutcomponent;

ALTER TABLE productioncounting_trackingoperationproductoutcomponent RENAME COLUMN productionrecord_id TO productiontracking_id;

-- end


-- Table: productioncounting_operationtimecomponent
-- changed: 02.07.2013

ALTER TABLE productioncounting_operationtimecomponent ADD COLUMN technologyoperationcomponent_id bigint;

ALTER TABLE productioncounting_operationtimecomponent
  ADD CONSTRAINT operationtimecomponent_technologyoperationcomponent_fkey FOREIGN KEY (technologyoperationcomponent_id)
      REFERENCES technologies_technologyoperationcomponent (id) DEFERRABLE;
      
-- end


-- Table: productioncounting_operationpieceworkcomponent
-- changed: 02.07.2013

ALTER TABLE productioncounting_operationpieceworkcomponent ADD COLUMN technologyoperationcomponent_id bigint;

ALTER TABLE productioncounting_operationpieceworkcomponent
  ADD CONSTRAINT operationpieceworkcomponent_technologyoperationcomponent_fkey FOREIGN KEY (technologyoperationcomponent_id)
      REFERENCES technologies_technologyoperationcomponent (id) DEFERRABLE;
      
-- end


-- Table: productioncountingwithcosts_operationcostcomponent
-- changed: 02.07.2013

ALTER TABLE productioncountingwithcosts_operationcostcomponent ADD COLUMN technologyoperationcomponent_id bigint;

ALTER TABLE productioncountingwithcosts_operationcostcomponent
  ADD CONSTRAINT operationcostcomponent_toc_fkey FOREIGN KEY (technologyoperationcomponent_id)
      REFERENCES technologies_technologyoperationcomponent (id) DEFERRABLE;
      
-- end


-- Table: productioncountingwithcosts_operationpieceworkcostcomponent
-- changed: 02.07.2013

ALTER TABLE productioncountingwithcosts_operationpieceworkcostcomponent ADD COLUMN technologyoperationcomponent_id bigint;

ALTER TABLE productioncountingwithcosts_operationpieceworkcostcomponent
  ADD CONSTRAINT operationpieceworkcostcomponent_toc_fkey FOREIGN KEY (technologyoperationcomponent_id)
      REFERENCES technologies_technologyoperationcomponent (id) DEFERRABLE;
      
-- end


-- Table: productioncountingwithcosts_technologyinstoperproductincomp
-- changed: 02.07.2013

ALTER TABLE productioncountingwithcosts_technologyinstoperproductincomp RENAME TO productioncountingwithcosts_technologyoperationproductincomp;

-- end


-- Table: productionpershift_progressforday
-- changed: 15.07.2013

ALTER TABLE productionpershift_progressforday ADD COLUMN technologyoperationcomponent_id bigint;

ALTER TABLE productionpershift_progressforday
  ADD CONSTRAINT progressforday_technologyoperationcomponent_fkey FOREIGN KEY (technologyoperationcomponent_id)
      REFERENCES technologies_technologyoperationcomponent (id) DEFERRABLE;
      
-- end


-- Table: qualitycontrols_qualitycontrol
-- changed: 19.07.2013

ALTER TABLE qualitycontrols_qualitycontrol ADD COLUMN technologyoperationcomponent_id bigint;

ALTER TABLE qualitycontrols_qualitycontrol
  ADD CONSTRAINT qualitycontrol_technologyoperationcomponent_fkey FOREIGN KEY (technologyoperationcomponent_id)
      REFERENCES technologies_technologyoperationcomponent (id) DEFERRABLE;
      
-- end


-- Table: workplans_orderoperationinputcolumn
-- changed: 14.08.2013

DROP TABLE workplans_orderoperationinputcolumn;

-- end


-- Table: workplans_orderoperationoutputcolumn
-- changed: 14.08.2013

DROP TABLE workplans_orderoperationoutputcolumn;

-- end


-- Table: basic_parameter
-- changed: 16.09.2013 by maku
-- SC#QCADOOMES-1599 -> PKT-6

ALTER TABLE basic_parameter ADD COLUMN locktechnologytree boolean DEFAULT false;
ALTER TABLE basic_parameter ADD COLUMN lockProductionProgress boolean DEFAULT false;

-- end


-- Table: productioncounting_productiontracking
-- changed: 08.05.2014

ALTER TABLE productioncounting_productiontracking ALTER COLUMN timerangefrom TYPE timestamp without time zone;

ALTER TABLE productioncounting_productiontracking ALTER COLUMN timerangeto TYPE timestamp without time zone;

-- end


-- Table: states_message
-- changed: 08.05.2014

ALTER TABLE states_message RENAME COLUMN productionrecordstatechange_id TO productiontrackingstatechange_id;

-- end


-- Table: technologies_technologyoperationcomponent
-- changed: 08.05.2014

ALTER TABLE technologies_technologyoperationcomponent ADD COLUMN hascorrections boolean;

-- end


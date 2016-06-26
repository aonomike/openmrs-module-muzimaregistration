/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.muzimaregistration.handler;

import net.minidev.json.JSONArray;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.PersonName;
import org.openmrs.User;
import org.openmrs.annotation.Handler;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.openmrs.module.muzima.exception.QueueProcessorException;
import org.openmrs.module.muzima.model.QueueData;
import org.openmrs.module.muzima.model.handler.QueueDataHandler;
import org.openmrs.module.muzimaregistration.utils.JsonUtils;
import org.openmrs.module.muzimaregistration.utils.PatientSearchUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 */
@Component
@Handler(supports = QueueData.class, order = 6)
public class ObsUpdateQueueDataHandler implements QueueDataHandler {

    private static final String DISCRIMINATOR_VALUE = "json-obs-update";

    private final Log log = LogFactory.getLog(DemographicsUpdateQueueDataHandler.class);

    private Obs unsavedObs;
    private Obs savedObs;
    private String payload;
    private QueueProcessorException queueProcessorException;

    @Override
    public void process(final QueueData queueData) throws QueueProcessorException {
        log.info("Processing obs update form data: " + queueData.getUuid());
        try {
            if (validate(queueData)) {
                updateSavedObs();
               // Context.getPatientService().savePatient(savedPatient);
                Context.getObsService().saveObs(unsavedObs);
            }
        } catch (Exception e) {
            if (!e.getClass().equals(QueueProcessorException.class)) {
                queueProcessorException.addException(e);
            }
        } finally {
            if (queueProcessorException.anyExceptions()) {
                throw queueProcessorException;
            }
        }
    }

    private void updateSavedObs(){
        if(unsavedObs.getConceptId() != null){
            savedPatient.addIdentifiers(unsavedPatient.getIdentifiers());
        }
        if(unsavedObs.getObsDateTime() != null) {
            savedPatient.setObsDateTime(unsavedObs.getObsDateTime());
        }
        if(StringUtils.isNotBlank(unsavedObs.getObsId())) {
            savedObs.setObsId(unsavedObs.getObsId());
        }
        if(unsavedObs..getBirthdate() != null) {
            savedPatient.setBirthdate(unsavedPatient.getBirthdate());
            savedPatient.setBirthdateEstimated(unsavedPatient.getBirthdateEstimated());
        }
        if(unsavedPatient.getPersonAddress() != null) {
            savedPatient.addAddress(unsavedPatient.getPersonAddress());
        }
        if(unsavedPatient.getAttributes() != null) {
            Set<PersonAttribute> attributes = unsavedPatient.getAttributes();
            Iterator<PersonAttribute> iterator = attributes.iterator();
            while(iterator.hasNext()) {
                savedPatient.addAttribute(iterator.next());
            }
        }
        if(unsavedPatient.getChangedBy() != null) {
            savedPatient.setChangedBy(unsavedPatient.getChangedBy());
        }
    }

    @Override
    public boolean validate(QueueData queueData) {
        log.info("Processing obs Update form data: " + queueData.getUuid());
        queueProcessorException = new QueueProcessorException();
        try {
            payload = queueData.getPayload();
            Obs obs = getObsFromPayload();
            savedObs = ObsSearchUtils.findSavedObs(obs,true);
            if(savedObs == null){
                queueProcessorException.addException(new Exception("Unable to uniquely identify obs for this " +
                        "observation update form data. "));
            } else {
                unsavedObs = new Obs();
                populateUnsavedObsFromPayload();
            }
            return true;
        } catch (Exception e) {
            queueProcessorException.addException(e);
            return false;
        } finally {
            if (queueProcessorException.anyExceptions()) {
                throw queueProcessorException;
            }
        }
    }

    @Override
    public String getDiscriminator() {
        return DISCRIMINATOR_VALUE;
    }

    private Obs getObsFromPayload(){
        Obs obs = new Obs();

        String uuid = getObsUuidFromPayload();
        obs.setUuid(uuid);

        Date obsDateTime = getObsDateTimeFromPayload();
        obs.addObsDateTime(obsDateTime);

        String obsValue = getObsValueFromPayload();
        obs.obsValue(obsValue);

        int conceptId = getConceptIdFromPayload();
        concept.setConceptId(conceptId);

        return obs;
    }

    private String getObsUuidFromPayload(){
        return JsonUtils.readAsString(payload, "$['obs']['obs.uuid']");
    }

    private Date getObsDateTimeFromPayload(){
    	Date obsDateTime = JsonUtils.readAsDate(payload, "$['obs']['obs.obs_date_time']")
    	return obsDatetime;
    }

    private String getObsValueFromPayload(){
        String obsValue = JsonUtils.readAsString(payload, "$['obs']['obs.obs_value']");
        return personName;        
    }

    private int getConceptIdFromPayload(){
        return JsonUtils.readAsInteger(payload, "$['obs']['obs.concept_id']");
    }

    private void populateUnsavedObsFromPayload() {
        setUnsavedUuidFromPayload();
        setUnsavedObsDateTimeFromPayload();
        setUnsavedObsValueFromPayload();
    }

    private void setUnsavedObsDateTimeFromPayload(){
        Date obsDateTime = JsonUtils.readAsDate(payload, "$['obs']['obs.obs_date_time']");
        if(obsDateTime != null){
            if(isObsDateChangeValidated()){
                unsavedObs.setObsDateTime(ObsDateTime);
            }else{
                queueProcessorException.addException(
                        new Exception("Change of Obs Date requires manual review"));
            }
        }

    }

    private void setUnsavedObsValueFromPayload(){
        String obsValue = JsonUtils.readAsString(payload, "$['obs']['obs.obs_value']");
        if(StringUtils.isNotBlank(obsValue)){
            if(isObsValueChangeValidated()){
                unsavedObs.setObsValue(obsValue);
            }else{
                queueProcessorException.addException(
                        new Exception("Change of Obs Value requires manual review"));
            }
        }
    }

    
    private  void setUnsavedObsUuidFromPayload(){
        String uuid = JsonUtils.readAsString(payload, "$['obs']['uuid']");
        if(StringUtils.isNotBlank(uuid)){
        	unsavedObs.setConceptId(uuid);
        }
    }

    private boolean isObsDateChangeValidated(){
        return JsonUtils.readAsBoolean(payload, "$['obs']['obs.value_date_time_change_validated']");
    }

    private boolean isObsValueChangeValidated(){
        return JsonUtils.readAsBoolean(payload, "$['obs']['obs.obs_value_change_validated']");
    }
    
   @Override
    public boolean accept(final QueueData queueData) {
        return StringUtils.equals(DISCRIMINATOR_VALUE, queueData.getDiscriminator());
    }
}
package org.openmrs.module.appointments.extension.scheduler.tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.*;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentStatus;
import org.openmrs.module.appointments.service.AppointmentsService;
import org.openmrs.scheduler.TaskDefinition;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.when;

@PrepareForTest(Context.class)
@RunWith(PowerMockRunner.class)
public class MarkAppointmentAsCheckedInTaskTest {

    @Mock
    private AppointmentsService appointmentsService;

    @Mock
    private AdministrationService administrationService;

    @Mock
    private VisitService visitsService;

    private MarkAppointmentAsCheckedInTask markAppointmentAsCheckedInTask;
    private GlobalProperty globalProperty;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(Context.class);
        when(Context.getService(AppointmentsService.class)).thenReturn(appointmentsService);
        when(Context.getService(AdministrationService.class)).thenReturn(administrationService);
        when(Context.getService(VisitService.class)).thenReturn(visitsService);
        markAppointmentAsCheckedInTask = new MarkAppointmentAsCheckedInTask();
    }


    @Test
    public void executeShouldMarkScheduledAppointmentsAsCheckedInWhenSchedulerTurnedOn() throws Exception {
        String schedulerMarksAppointmentCheckedIn = "SchedulerMarksAppointmentCheckedIn";
        globalProperty = new GlobalProperty(schedulerMarksAppointmentCheckedIn, "true");
        when(administrationService.getGlobalPropertyObject(schedulerMarksAppointmentCheckedIn)).thenReturn(globalProperty);

        TaskDefinition definition = new TaskDefinition();
        definition.setProperty("PreviousDaysToConsiderVisitsFrom", "0");
        markAppointmentAsCheckedInTask.initialize(definition);
        Patient p1 = new Patient();
        List<Appointment> appointments = new ArrayList<>();
        Appointment appointment1 = new Appointment();
        appointment1.setPatient(p1);
        appointment1.setStatus(AppointmentStatus.Scheduled);
        appointments.add(appointment1);

        Patient p2 = new Patient();
        Appointment appointment2 = new Appointment();
        appointment2.setPatient(p2);
        appointment2.setStatus(AppointmentStatus.Scheduled);
        appointments.add(appointment2);

        when(appointmentsService.getAllAppointmentsInDateRange(any(Date.class), any(Date.class))).thenReturn(appointments);

        List<Visit> visits = new ArrayList<>();
        Visit visit = new Visit();
        visit.setPatient(p1);
        visits.add(visit);

        when(visitsService.getVisits(anyCollectionOf(VisitType.class), anyCollectionOf(Patient.class), anyCollectionOf(Location.class),
                anyCollectionOf(Concept.class), any(Date.class), any(Date.class), any(Date.class), any(Date.class),
                anyMapOf(VisitAttributeType.class, Object.class), anyBoolean(),
                anyBoolean())).thenReturn(visits);

        markAppointmentAsCheckedInTask.execute();

        String checkedInStatus = AppointmentStatus.CheckedIn.toString();
        Mockito.verify(appointmentsService, times(1)).changeStatus(eq(appointment1), eq(checkedInStatus), any(Date.class));
    }

    @Test
    public void shouldNotMarkAppointmentAsCheckedInWhenSchedulerIsTurnedOff() {
        String schedulerMarksCheckedIn = "SchedulerMarksAppointmentCheckedIn";
        globalProperty = new GlobalProperty(schedulerMarksCheckedIn, "false");
        when(administrationService.getGlobalPropertyObject(schedulerMarksCheckedIn)).thenReturn(globalProperty);
        markAppointmentAsCheckedInTask.execute();
        Mockito.verify(appointmentsService, times(0)).changeStatus(any(Appointment.class), any(String.class), any(Date.class));
    }
}
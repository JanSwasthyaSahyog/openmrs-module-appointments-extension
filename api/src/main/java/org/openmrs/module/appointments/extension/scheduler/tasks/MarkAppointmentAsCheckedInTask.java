package org.openmrs.module.appointments.extension.scheduler.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.GlobalProperty;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentStatus;
import org.openmrs.module.appointments.service.AppointmentsService;
import org.openmrs.scheduler.tasks.AbstractTask;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class MarkAppointmentAsCheckedInTask extends AbstractTask {

    private static final Log log = LogFactory.getLog(MarkAppointmentAsCheckedInTask.class);
    private static final String SCHEDULER_MARKS_APPOINTMENT_CHECKED_IN = "SchedulerMarksAppointmentCheckedIn";
    private static final String CHECKED_IN = AppointmentStatus.CheckedIn.toString();
    private static final String VISITS_FROM_PREVIOUS_DAYS = "PreviousDaysToConsiderVisitsFrom";

    @Override
    public void execute() {

        if (!shouldSchedulerMarkAppointmentCheckedIn()){
            return;
        }

        AppointmentsService appointmentsService = Context.getService(AppointmentsService.class);
        Date now = new Date();
        List<Appointment> appointmentsScheduledForToday = getAppointmentsScheduledFor(appointmentsService,
                startOfTheDay(now, 0), endOfTheDay(now));

        if(appointmentsScheduledForToday.isEmpty()){
            return;
        }
        List<Patient> patients = collectPatients(appointmentsScheduledForToday);
        List<Visit> visits = getVisits(now, patients);

        for (Appointment appointment : appointmentsScheduledForToday) {
            if (isCorrespondingVisitPresent(appointment, visits)) {
                appointmentsService.changeStatus(appointment, CHECKED_IN, now);
            }
        }
    }

    private List<Visit> getVisits(Date now, List<Patient> patients) {
        VisitService visitService = Context.getService(VisitService.class);
        String visitsFromPreviousDays = this.getTaskDefinition().getProperties().get(VISITS_FROM_PREVIOUS_DAYS);
        int shift = visitsFromPreviousDays!=null ? - Integer.valueOf(visitsFromPreviousDays) : 0;
        return visitService.getVisits(null, patients, null, null,
                startOfTheDay(now, shift), now, null, null, null, false, false);
    }

    private Boolean shouldSchedulerMarkAppointmentCheckedIn() {
        AdministrationService administrationService = Context.getService(AdministrationService.class);
        GlobalProperty schedulerMarksAppointmentCheckedInProperty = administrationService
                .getGlobalPropertyObject(SCHEDULER_MARKS_APPOINTMENT_CHECKED_IN);
        return Boolean.valueOf(schedulerMarksAppointmentCheckedInProperty.getPropertyValue());
    }

    private boolean isCorrespondingVisitPresent(Appointment appointment, List<Visit> visits) {
        Visit visit = visits.stream()
                .filter(x -> appointment.getPatient().equals(x.getPatient()))
                .findAny()
                .orElse(null);
        return (visit!=null);
    }

    private List<Appointment> getAppointmentsScheduledFor(AppointmentsService appointmentsService, Date startDate, Date endDate) {
        List<Appointment> appointments = appointmentsService.getAllAppointmentsInDateRange(startDate, endDate);
        return appointments.stream()
                .filter(this::isAppointmentScheduled)
                .collect(Collectors.toList());
    }

    private List<Patient> collectPatients(List<Appointment> appointments) {
        return appointments.stream()
                .map(Appointment::getPatient)
                .collect(Collectors.toList());
    }

    private boolean isAppointmentScheduled(Appointment appointment) {
        return appointment.getStatus().equals(AppointmentStatus.Scheduled);
    }


    private static Date endOfTheDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private static Date startOfTheDay(Date date, int shift) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DATE, shift);
        return cal.getTime();
    }

}

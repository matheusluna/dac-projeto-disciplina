package com.ifpb.dac.googleAgenda;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.ifpb.dac.entidades.Aluno;
import com.ifpb.dac.entidades.Atividade;
import com.ifpb.dac.interfaces.AtividadeDao;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

/**
 *
 * @author rodrigobento
 */
@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue")
    ,
    @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "java:global/jms/queueAgendaAdd")
})
public class GoogleAgendaAdicionar implements MessageListener {

    @EJB
    private GoogleAPI api;
    @EJB
    private AtividadeDao dao;

    @Override
    public void onMessage(Message message) {
        try {
            Atividade a = message.getBody(Atividade.class);
            Event eventUsu = evento(a);
            String id = api.adicionarEvento(eventUsu);
            a.setId(id);
            dao.adicionar(a);
        } catch (JMSException ex) {
            ex.printStackTrace();
        }
    }

    // Tratamento para criar evento
    private static Event evento(Atividade a) {
        Event event = new Event()
                .setSummary(a.getResumo())
                .setLocation("Cajazeiras")
                .setDescription(a.getDescricao());

        LocalDateTime ini = a.getInicio().plusHours(2);
        LocalDateTime fm = a.getFim().plusHours(2);

        String dataInicioFormatada = ini.format(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String dataFinalFormatada = fm.format(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String dataInicioConvertida = DateTime.parseRfc3339(dataInicioFormatada).toString();
        String dataFimConvertida = DateTime.parseRfc3339(dataFinalFormatada).toString();
        DateTime startDateTime = new DateTime(dataInicioConvertida);
        EventDateTime start = new EventDateTime()
                .setDateTime(startDateTime).setTimeZone("America/Fortaleza");
        event.setStart(start);
        DateTime endDateTime = new DateTime(dataFimConvertida);
        EventDateTime end = new EventDateTime()
                .setDateTime(endDateTime).setTimeZone("America/Fortaleza");
        event.setEnd(end);

        EventReminder[] reminderOverrides = new EventReminder[]{
            new EventReminder().setMethod("email").setMinutes(2)};
        Event.Reminders reminders = new Event.Reminders()
                .setUseDefault(false)
                .setOverrides(Arrays.asList(reminderOverrides));
        event.setReminders(reminders);

        EventAttendee[] attendees;

        List<Aluno> alunosTurma = a.getTurma().getAlunos();
        if (alunosTurma.isEmpty()) {
            attendees = new EventAttendee[2];
            EventAttendee setEmail = new EventAttendee().setEmail("rodrigobentor2014@gmail.com");
            EventAttendee setEmail2 = new EventAttendee().setEmail("projetodacq@gmail.com");
            attendees[0] = setEmail;
            attendees[1] = setEmail2;
        } else {
            int cont = 0;
            int emails = alunosTurma.size();
            attendees = new EventAttendee[emails];
            for (Aluno auxiliar : alunosTurma) {
                attendees[cont] = new EventAttendee().setEmail(auxiliar.getEmail());
                cont++;
            }
        }
        event.setColorId("10");
        event.setAttendees(Arrays.asList(attendees));
        return event;
    }

}

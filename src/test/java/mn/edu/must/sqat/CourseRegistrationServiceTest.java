package mn.edu.must.sqat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceTest {

    @Mock StudentRepository repository;
    @Mock EmailSender emailSender;

    CourseRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new CourseRegistrationService(repository, emailSender);
    }

    private static Student activeStudent(String id, List<String> taken) {
        return new Student(id, id + "@must.edu.mn", true, taken);
    }

    // A1
    @Test
    void register_activeStudentWithPrerequisitesMet_returnsOkSavesAndSendsEmail() {
        Student s = activeStudent("S1", List.of("MATH101"));
        when(repository.findStudent("S1")).thenReturn(s);
        when(repository.courseExists("CS201")).thenReturn(true);
        when(repository.prerequisites("CS201")).thenReturn(List.of("MATH101"));

        String result = service.register("S1", "CS201");

        assertEquals("OK", result);
        verify(repository).saveRegistration("S1", "CS201");
        verify(emailSender).send(eq("S1@must.edu.mn"), anyString(), anyString());
    }

    // A2
    @Test
    void register_unknownStudent_returnsNoStudentAndDoesNotSaveOrEmail() {
        when(repository.findStudent("X")).thenReturn(null);

        String result = service.register("X", "CS201");

        assertEquals("ERROR_NO_STUDENT", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    // A3
    @Test
    void register_inactiveStudent_returnsInactiveAndNeverTouchesEmailSender() {
        Student s = new Student("S2", "S2@must.edu.mn", false, List.of());
        when(repository.findStudent("S2")).thenReturn(s);

        String result = service.register("S2", "CS201");

        assertEquals("ERROR_INACTIVE_STUDENT", result);
        verifyNoInteractions(emailSender);
    }

    // A4
    @Test
    void register_missingPrerequisite_returnsPrerequisitesErrorAndDoesNotSave() {
        Student s = activeStudent("S3", List.of("MATH101"));
        when(repository.findStudent("S3")).thenReturn(s);
        when(repository.courseExists("CS301")).thenReturn(true);
        when(repository.prerequisites("CS301")).thenReturn(List.of("MATH101", "CS201"));

        String result = service.register("S3", "CS301");

        assertEquals("ERROR_PREREQUISITES", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
    }

    // A5
    @Test
    void register_emailSenderThrows_stillReturnsOkAndSavesRegistration() {
        Student s = activeStudent("S4", List.of());
        when(repository.findStudent("S4")).thenReturn(s);
        when(repository.courseExists("CS101")).thenReturn(true);
        when(repository.prerequisites("CS101")).thenReturn(List.of());
        doThrow(new RuntimeException("SMTP down"))
                .when(emailSender).send(anyString(), anyString(), anyString());

        String result = service.register("S4", "CS101");

        assertEquals("OK", result);
        verify(repository).saveRegistration("S4", "CS101");
    }

    // A6
    @Test
    void register_nullArguments_throwsIllegalArgumentAndTouchesNoDependency() {
        assertThrows(IllegalArgumentException.class, () -> service.register(null, "CS101"));
        assertThrows(IllegalArgumentException.class, () -> service.register("S1", null));
        assertThrows(IllegalArgumentException.class, () -> service.register(null, null));

        verifyNoInteractions(repository, emailSender);
    }

    // A7
    @Test
    void register_success_emailBodyContainsCourseCode() {
        Student s = activeStudent("S5", List.of());
        when(repository.findStudent("S5")).thenReturn(s);
        when(repository.courseExists("CS450")).thenReturn(true);
        when(repository.prerequisites("CS450")).thenReturn(List.of());

        service.register("S5", "CS450");

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(eq("S5@must.edu.mn"), anyString(), body.capture());
        assertTrue(body.getValue().contains("CS450"),
                "Имэйлийн бие хичээлийн кодыг агуулах ёстой: " + body.getValue());
        verify(emailSender).send(anyString(), anyString(), contains("CS450"));
    }

    // A8 — гурван алдааны анги нэг параметржүүлсэн тестээр
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "NO_STUDENT, ERROR_NO_STUDENT",
            "INACTIVE,   ERROR_INACTIVE_STUDENT",
            "PREREQ,     ERROR_PREREQUISITES"
    })
    void register_errorScenario_returnsExpectedCodeAndNeverSaves(String scenario, String expected) {
        // Мөр бүр өөр stub ашигладаг тул strict stubs-д "ашиглагдаагүй stub" гэж
        // тооцогдохгүйн тулд lenient() хэрэглэв.
        switch (scenario) {
            case "NO_STUDENT" ->
                    lenient().when(repository.findStudent("S9")).thenReturn(null);
            case "INACTIVE" ->
                    lenient().when(repository.findStudent("S9"))
                            .thenReturn(new Student("S9", "S9@must.edu.mn", false, List.of()));
            case "PREREQ" -> {
                lenient().when(repository.findStudent("S9"))
                        .thenReturn(activeStudent("S9", List.of()));
                lenient().when(repository.courseExists("CS301")).thenReturn(true);
                lenient().when(repository.prerequisites("CS301")).thenReturn(List.of("CS201"));
            }
            default -> throw new IllegalArgumentException(scenario);
        }

        assertEquals(expected, service.register("S9", "CS301"));
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }
}

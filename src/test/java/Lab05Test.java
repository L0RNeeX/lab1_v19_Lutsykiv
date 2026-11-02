import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/** Запускає ті самі тести ЛР5 через JUnit 5. */
class Lab05Test {
    @TestFactory
    Stream<DynamicTest> testScenarios() {
        List<Lab05Checks.Case> scenarios = Lab05Checks.cases();
        return scenarios.stream().map(s -> DynamicTest.dynamicTest(s.name(), () -> s.check().run()));
    }
}

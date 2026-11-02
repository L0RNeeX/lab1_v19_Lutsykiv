import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/** Запуск нових перевірок через Maven/JUnit 5. */
class Lab04Test {
    @TestFactory
    Stream<DynamicTest> checks() {
        return Lab04Checks.cases().stream()
                .map(test -> DynamicTest.dynamicTest(test.name(), test.check()::run));
    }
}

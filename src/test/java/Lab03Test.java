import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/** Виконує нові перевірки через Maven / JUnit 5. */
class Lab03Test {
    @TestFactory
    Stream<DynamicTest> checks() {
        return Lab03Checks.cases().stream()
                .map(test -> DynamicTest.dynamicTest(test.name(), test.check()::run));
    }
}

import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/** Запускає спільні перевірки як динамічні тести JUnit 5. */
class Lab02Test {
    @TestFactory
    Stream<DynamicTest> checks() {
        return Lab02Checks.cases().stream()
                .map(test -> DynamicTest.dynamicTest(
                        "%s: %s".formatted(test.group(), test.name()), test.check()::run));
    }
}

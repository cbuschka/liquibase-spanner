package liquibase.ext.spanner;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import org.junit.jupiter.api.Test;
import liquibase.database.core.PostgresDatabase;

public class VarcharTypeSpannerTest {
  private final VarcharTypeSpanner type = new VarcharTypeSpanner();
  
  @Test
  public void testSupportsDatabase() {
    assertThat(type.supports(mock(PostgresDatabase.class))).isFalse();
    assertThat(type.supports(mock(CloudSpanner.class))).isTrue();
  }
}

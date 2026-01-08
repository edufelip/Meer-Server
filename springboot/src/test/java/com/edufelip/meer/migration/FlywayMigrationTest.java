package com.edufelip.meer.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@Tag("slow")
class FlywayMigrationTest {

  @Container
  static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgis/postgis:15-3.4-alpine");

  @Test
  void migrationsApplyCleanly() {
    Flyway flyway =
        Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .load();

    var result = flyway.migrate();
    assertThat(result.migrationsExecuted).isGreaterThan(0);

    var validation = flyway.validateWithResult();
    assertThat(validation.validationSuccessful).isTrue();

    var second = flyway.migrate();
    assertThat(second.migrationsExecuted).isEqualTo(0);
  }

  @Test
  void storeFeedbackHasUniqueConstraintOnUserAndStore() throws Exception {
    Flyway flyway =
        Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .load();
    flyway.migrate();

    try (Connection conn =
            java.sql.DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        Statement stmt = conn.createStatement()) {
      ResultSet rs =
          stmt.executeQuery(
              """
              select count(*) as cnt
              from pg_constraint c
              join pg_class t on c.conrelid = t.oid
              join pg_namespace n on t.relnamespace = n.oid
              where n.nspname = 'public'
                and t.relname = 'store_feedback'
                and c.contype = 'u'
                and c.conname = 'store_feedback_auth_user_id_thrift_store_id_key'
              """);
      rs.next();
      assertThat(rs.getInt("cnt")).isEqualTo(1);
    }
  }
}

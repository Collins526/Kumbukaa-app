package com.kumbukaa.config;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.Statement;

@Configuration
public class TransactionConstraintRepair {

    private static final Logger log = LoggerFactory.getLogger(TransactionConstraintRepair.class);

    @Bean
    public ApplicationRunner repairTransactionStatusConstraint(DataSource dataSource) {
        return args -> {
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("ALTER TABLE IF EXISTS public.\"transaction\" DROP CONSTRAINT IF EXISTS transaction_status_check");
                statement.executeUpdate("ALTER TABLE IF EXISTS public.\"transaction\" ADD CONSTRAINT transaction_status_check CHECK (status IN ('PENDING','UNPAID','PAID'))");
                log.info("Ensured transaction_status_check constraint exists and matches TransactionStatus values");
            } catch (Exception ex) {
                log.warn("Unable to repair transaction_status_check constraint", ex);
            }
        };
    }
}

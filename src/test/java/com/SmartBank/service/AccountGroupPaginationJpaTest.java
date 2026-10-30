package com.SmartBank.service;

import com.SmartBank.dto.response.AccountResponse;
import com.SmartBank.dto.response.CustomerAccountResponse;
import com.SmartBank.dto.response.PageResponse;
import com.SmartBank.entity.Account;
import com.SmartBank.entity.Customer;
import com.SmartBank.entity.enums.AccountType;
import com.SmartBank.entity.enums.CustomerStatus;
import com.SmartBank.mapper.AccountMapper;
import com.SmartBank.service.impl.AccountServiceImpl;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.query.fail_on_pagination_over_collection_fetch=true",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.SQL=OFF",
        "logging.level.org.hibernate.stat=OFF",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({AccountServiceImpl.class, AccountGroupPaginationJpaTest.Config.class})
class AccountGroupPaginationJpaTest {
    @Autowired private EntityManager entityManager;
    @Autowired private AccountService service;

    @Test
    void pagesCustomerGroupsWithoutSplittingOrLoadingTheirCollections() {
        Customer first = persistCustomer("first", CustomerStatus.ACTIVE);
        Customer second = persistCustomer("second", CustomerStatus.ACTIVE);
        Customer third = persistCustomer("third", CustomerStatus.LOCKED);
        persistAccount(first, "1000000001");
        // More accounts than the page size: pagination must still count customers.
        persistAccount(third, "1000000002");
        persistAccount(third, "1000000003");
        persistAccount(third, "1000000004");
        entityManager.flush();
        entityManager.clear();
        var statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        PageResponse<CustomerAccountResponse> page = service.findAllGroupedByCustomer(0, 2);

        assertThat(page.getContent()).extracting(CustomerAccountResponse::getCustomerId)
                .containsExactly(third.getId(), second.getId());
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.isLast()).isFalse();
        assertThat(page.getContent().getFirst().getAccounts()).extracting(AccountResponse::getAccountNumber)
                .containsExactly("1000000002", "1000000003", "1000000004");
        assertThat(page.getContent().getFirst().getAccounts()).allSatisfy(account -> {
            assertThat(account.getCustomerId()).isEqualTo(third.getId());
            assertThat(account.getCustomerName()).isEqualTo("third");
        });
        assertThat(page.getContent().get(1).getAccounts()).isEmpty();
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
        assertThat(Hibernate.isInitialized(entityManager.find(Customer.class, third.getId()).getAccountList())).isFalse();

        PageResponse<CustomerAccountResponse> last = service.findAllGroupedByCustomer(1, 2);
        assertThat(last.getContent()).extracting(CustomerAccountResponse::getCustomerId).containsExactly(first.getId());
        assertThat(last.getContent().getFirst().getAccounts()).extracting(AccountResponse::getAccountNumber)
                .containsExactly("1000000001");
        assertThat(last.getPageSize()).isEqualTo(2);
        assertThat(last.isLast()).isTrue();
    }

    @Test
    void emptyDatabaseReturnsEmptyPage() {
        PageResponse<CustomerAccountResponse> page = service.findAllGroupedByCustomer(0, 10);
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getTotalPages()).isZero();
        assertThat(page.isLast()).isTrue();
    }

    @Test
    void subsequentRequestSeesNewAccount() {
        Customer customer = persistCustomer("customer", CustomerStatus.ACTIVE);
        entityManager.flush();
        entityManager.clear();
        assertThat(service.findAllGroupedByCustomer(0, 10).getContent().getFirst().getAccounts()).isEmpty();

        persistAccount(entityManager.getReference(Customer.class, customer.getId()), "1000000005");
        entityManager.flush();
        entityManager.clear();
        assertThat(service.findAllGroupedByCustomer(0, 10).getContent().getFirst().getAccounts())
                .extracting(AccountResponse::getAccountNumber).containsExactly("1000000005");
    }

    private Customer persistCustomer(String username, CustomerStatus status) {
        Customer customer = Customer.builder().username(username).fullName(username)
                .password("test-hash").status(status).build();
        entityManager.persist(customer);
        return customer;
    }

    private void persistAccount(Customer customer, String number) {
        entityManager.persist(Account.builder().customer(customer).accountNumber(number)
                .type(AccountType.CHECKING).build());
    }

    @TestConfiguration
    static class Config {
        @Bean AccountMapper accountMapper() { return Mappers.getMapper(AccountMapper.class); }
        @Bean CacheManager cacheManager() { return new ConcurrentMapCacheManager("accounts"); }
    }
}

package com.SmartBank.service;

import com.SmartBank.dto.response.CustomerResponse;
import com.SmartBank.dto.response.PageResponse;
import com.SmartBank.entity.Account;
import com.SmartBank.entity.Customer;
import com.SmartBank.entity.enums.AccountType;
import com.SmartBank.mapper.AccountMapper;
import com.SmartBank.mapper.CustomerMapper;
import com.SmartBank.service.impl.CustomerServiceImpl;
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
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.query.fail_on_pagination_over_collection_fetch=true",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({CustomerServiceImpl.class, CustomerPaginationJpaTest.Config.class})
class CustomerPaginationJpaTest {
    @Autowired private EntityManager entityManager;
    @Autowired private CustomerService service;

    @Test
    void paginatesCustomersAndCountsAccountsWithoutLoadingCollections() {
        Customer first = persistCustomer("first");
        Customer second = persistCustomer("second");
        Customer third = persistCustomer("third");
        persistAccount(first, "1000000001");
        persistAccount(third, "1000000002");
        persistAccount(third, "1000000003");
        entityManager.flush();
        entityManager.clear();
        var statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        PageResponse<CustomerResponse> page = service.getAllCustomers(0, 2);

        assertThat(page.getContent()).extracting(CustomerResponse::getId)
                .containsExactly(third.getId(), second.getId());
        assertThat(page.getContent()).extracting(CustomerResponse::getTotalAccounts).containsExactly(2, 0);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.isLast()).isFalse();
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
        assertThat(Hibernate.isInitialized(entityManager.find(Customer.class, third.getId()).getAccountList())).isFalse();

        PageResponse<CustomerResponse> last = service.getAllCustomers(1, 2);
        assertThat(last.getContent()).extracting(CustomerResponse::getId).containsExactly(first.getId());
        assertThat(last.getContent().getFirst().getTotalAccounts()).isEqualTo(1);
        assertThat(last.getPageSize()).isEqualTo(2);
        assertThat(last.isLast()).isTrue();
    }

    @Test
    void repeatedListRequestSeesNewCustomerWithCachingEnabled() {
        Customer first = persistCustomer("existing");
        entityManager.flush();
        entityManager.clear();
        assertThat(service.getAllCustomers(0, 10).getContent()).extracting(CustomerResponse::getId)
                .containsExactly(first.getId());

        Customer added = persistCustomer("added");
        entityManager.flush();
        entityManager.clear();

        PageResponse<CustomerResponse> fresh = service.getAllCustomers(0, 10);
        assertThat(fresh.getContent()).extracting(CustomerResponse::getId)
                .containsExactly(added.getId(), first.getId());
        assertThat(fresh.getTotalElements()).isEqualTo(2);
    }

    @Test
    void emptyDatabaseReturnsEmptyPage() {
        PageResponse<CustomerResponse> page = service.getAllCustomers(0, 10);
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getPageNo()).isZero();
        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getTotalPages()).isZero();
        assertThat(page.isLast()).isTrue();
    }

    private Customer persistCustomer(String username) {
        Customer customer = Customer.builder().username(username).password("test-hash").build();
        entityManager.persist(customer);
        return customer;
    }

    private void persistAccount(Customer customer, String number) {
        entityManager.persist(Account.builder().customer(customer).accountNumber(number)
                .type(AccountType.CHECKING).build());
    }

    @TestConfiguration
    @EnableCaching
    static class Config {
        @Bean CustomerMapper customerMapper() { return Mappers.getMapper(CustomerMapper.class); }
        @Bean AccountMapper accountMapper() { return Mappers.getMapper(AccountMapper.class); }
        @Bean PasswordEncoder passwordEncoder() { return mock(PasswordEncoder.class); }
        @Bean CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("customers", "accounts_customers");
        }
    }
}

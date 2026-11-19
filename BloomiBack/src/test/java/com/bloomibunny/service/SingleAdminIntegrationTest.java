package com.bloomibunny.service;

import com.bloomibunny.config.DataInitializer;
import com.bloomibunny.entity.*;
import com.bloomibunny.repository.AppUserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.annotation.DirtiesContext;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="BLOOMI_TEST_DB_URL",matches="jdbc:postgresql://[^?]+\\?currentSchema=bloomi_sales_test_[a-z0-9_]+")
@SpringBootTest(properties={"spring.datasource.url=${BLOOMI_TEST_DB_URL}","spring.datasource.username=${BLOOMI_TEST_DB_USERNAME}","spring.datasource.password=${BLOOMI_TEST_DB_PASSWORD}","spring.jpa.hibernate.ddl-auto=create-drop","app.bootstrap.admin-username=sales-integration-admin","app.bootstrap.admin-password=integration-fixture-password","jwt.secret=integration-only-signing-key-with-enough-bytes"})
@DirtiesContext
class SingleAdminIntegrationTest {
 @Autowired AppUserRepository users;
 @Autowired JdbcTemplate jdbc;
 @Autowired DataInitializer initializer;
 @BeforeEach void reset()throws Exception{
  assertTrue(jdbc.queryForObject("select current_schema()",String.class).startsWith("bloomi_sales_test_"));
  jdbc.execute("truncate table app_user restart identity cascade");
  try(var connection=jdbc.getDataSource().getConnection()){
   ScriptUtils.executeSqlScript(connection,new ClassPathResource("db/migrations/012_single_admin.sql"));
  }
 }
 AppUser account(String name,Role role){var user=new AppUser();user.setUsername(name);user.setRole(role);user.setPassword("unchanged-fixture-hash");return users.saveAndFlush(user);}
 @Test void databaseRejectsSecondAdminIncludingInactiveAccounts(){
  var owner=account("owner",Role.ADMIN);owner.setActive(false);users.saveAndFlush(owner);
  assertThrows(DataIntegrityViolationException.class,()->account("second",Role.ADMIN));
  assertEquals(1,users.countByRole(Role.ADMIN));assertEquals("unchanged-fixture-hash",users.findByUsername("owner").orElseThrow().getPassword());
 }
 @Test void databaseAlsoRejectsPromotingACustomerToSecondAdmin(){
  account("owner",Role.ADMIN);var customer=account("customer",Role.CUSTOMER);customer.setRole(Role.ADMIN);
  assertThrows(DataIntegrityViolationException.class,()->users.saveAndFlush(customer));
  assertEquals(Role.CUSTOMER,users.findByUsername("customer").orElseThrow().getRole());
 }
 @Test void changingBootstrapUsernameDoesNotCreateOrResetExistingAdmin(){
  var owner=account("different-owner-name",Role.ADMIN);initializer.run();
  assertEquals(1,users.count());assertEquals(owner.getId(),users.findByUsername("different-owner-name").orElseThrow().getId());
  assertEquals("unchanged-fixture-hash",users.findByUsername("different-owner-name").orElseThrow().getPassword());
 }
 @Test void inactiveAdminIsPreservedWithoutCreatingAnother(){
  var owner=account("disabled-owner",Role.ADMIN);owner.setActive(false);users.saveAndFlush(owner);initializer.run();
  assertEquals(1,users.count());assertFalse(users.findByUsername("disabled-owner").orElseThrow().isActive());
 }
 @Test void existingCustomersNeverGetPromotedOrCauseAdminRecreation(){
  account("sales-integration-admin",Role.CUSTOMER);initializer.run();
  assertEquals(1,users.count());assertEquals(0,users.countByRole(Role.ADMIN));assertEquals(Role.CUSTOMER,users.findAll().getFirst().getRole());
 }
 @Test void emptyInstallationCreatesOnlyOneAdminAcrossConcurrentStarts()throws Exception{
  var gate=new CountDownLatch(1);
  try(var pool=Executors.newFixedThreadPool(2)){
   var first=pool.submit(()->{gate.await();initializer.run();return true;});var second=pool.submit(()->{gate.await();initializer.run();return true;});gate.countDown();
   assertTrue(first.get(15,TimeUnit.SECONDS));assertTrue(second.get(15,TimeUnit.SECONDS));
  }
  assertEquals(1,users.count());assertEquals(1,users.countByRole(Role.ADMIN));
 }
 @Test void anyNumberOfCustomerAccountsRemainsAllowed(){
  account("owner",Role.ADMIN);account("one",Role.CUSTOMER);account("two",Role.CUSTOMER);assertEquals(3,users.count());
 }
}

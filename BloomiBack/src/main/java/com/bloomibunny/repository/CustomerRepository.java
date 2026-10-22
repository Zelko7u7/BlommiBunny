package com.bloomibunny.repository;
import com.bloomibunny.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CustomerRepository extends JpaRepository<Customer,Long>{}

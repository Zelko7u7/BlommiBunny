package com.bloomibunny.entity;
import jakarta.persistence.*;
@Entity @Table(indexes=@Index(name="idx_supplier_name",columnList="name"))
public class Supplier extends Contact {}

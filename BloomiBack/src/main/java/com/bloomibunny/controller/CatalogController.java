package com.bloomibunny.controller;
import com.bloomibunny.dto.CatalogDtos.*;
import com.bloomibunny.entity.*;
import com.bloomibunny.exception.ResourceNotFoundException;
import com.bloomibunny.repository.*;
import jakarta.validation.Valid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api")
public class CatalogController {
 private final CategoryRepository categories;private final BrandRepository brands;private final SupplierRepository suppliers;private final CustomerRepository customers;
 public CatalogController(CategoryRepository c,BrandRepository b,SupplierRepository s,CustomerRepository u){categories=c;brands=b;suppliers=s;customers=u;}
 @GetMapping("/categories") List<NamedResponse> categories(){return categories.findAll().stream().map(this::named).toList();}
 @PostMapping("/categories") @ResponseStatus(HttpStatus.CREATED) NamedResponse category(@Valid @RequestBody NamedRequest r){var e=new Category();apply(e,r);return named(categories.save(e));}
 @PutMapping("/categories/{id}") NamedResponse category(@PathVariable Long id,@Valid @RequestBody NamedRequest r){var e=find(categories,id,"Categoría");apply(e,r);return named(categories.save(e));}
 @DeleteMapping("/categories/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void dc(@PathVariable Long id){categories.deleteById(id);}
 @GetMapping("/brands") List<NamedResponse> brands(){return brands.findAll().stream().map(this::named).toList();}
 @PostMapping("/brands") @ResponseStatus(HttpStatus.CREATED) NamedResponse brand(@Valid @RequestBody NamedRequest r){var e=new Brand();apply(e,r);return named(brands.save(e));}
 @PutMapping("/brands/{id}") NamedResponse brand(@PathVariable Long id,@Valid @RequestBody NamedRequest r){var e=find(brands,id,"Marca");apply(e,r);return named(brands.save(e));}
 @DeleteMapping("/brands/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void db(@PathVariable Long id){brands.deleteById(id);}
 @GetMapping("/suppliers") List<ContactResponse> suppliers(){return suppliers.findAll().stream().map(this::contact).toList();}
 @PostMapping("/suppliers") @ResponseStatus(HttpStatus.CREATED) ContactResponse supplier(@Valid @RequestBody ContactRequest r){var e=new Supplier();apply(e,r);return contact(suppliers.save(e));}
 @PutMapping("/suppliers/{id}") ContactResponse supplier(@PathVariable Long id,@Valid @RequestBody ContactRequest r){var e=find(suppliers,id,"Proveedor");apply(e,r);return contact(suppliers.save(e));}
 @DeleteMapping("/suppliers/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void ds(@PathVariable Long id){suppliers.deleteById(id);}
 @GetMapping("/customers") List<ContactResponse> customers(){return customers.findAll().stream().map(this::contact).toList();}
 @PostMapping("/customers") @ResponseStatus(HttpStatus.CREATED) ContactResponse customer(@Valid @RequestBody ContactRequest r){var e=new Customer();apply(e,r);e.setBirthday(r.birthday());return contact(customers.save(e));}
 @PutMapping("/customers/{id}") ContactResponse customer(@PathVariable Long id,@Valid @RequestBody ContactRequest r){var e=find(customers,id,"Cliente");apply(e,r);e.setBirthday(r.birthday());return contact(customers.save(e));}
 @DeleteMapping("/customers/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void du(@PathVariable Long id){customers.deleteById(id);}
 private <T> T find(JpaRepository<T,Long> r,Long id,String n){return r.findById(id).orElseThrow(()->new ResourceNotFoundException(n+" no encontrado"));}private void apply(NamedEntity e,NamedRequest r){e.setName(r.name());e.setDescription(r.description());e.setActive(r.active()==null||r.active());}private NamedResponse named(NamedEntity e){return new NamedResponse(e.getId(),e.getName(),e.getDescription(),e.isActive());}private void apply(Contact e,ContactRequest r){e.setName(r.name());e.setPhone(r.phone());e.setEmail(r.email());e.setAddress(r.address());e.setNotes(r.notes());e.setActive(r.active()==null||r.active());}private ContactResponse contact(Contact e){return new ContactResponse(e.getId(),e.getName(),e.getPhone(),e.getEmail(),e.getAddress(),e.getNotes(),e instanceof Customer c?c.getBirthday():null,e.isActive());}
}

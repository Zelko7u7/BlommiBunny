package com.bloomibunny.controller;
import com.bloomibunny.dto.DashboardResponse;
import com.bloomibunny.service.DashboardService;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/dashboard")
public class DashboardController {private final DashboardService service;public DashboardController(DashboardService s){service=s;}@GetMapping DashboardResponse summary(){return service.summary();}}

package com.bloomibunny.exception;
public class BannerConflictException extends RuntimeException {
 public BannerConflictException(){super("Los banners cambiaron. Actualiza la lista antes de guardar de nuevo.");}
}

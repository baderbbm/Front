package com.microservices.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.http.HttpStatus;

@ControllerAdvice
public class CustomizedResponseEntityExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ModelAndView handleUnauthorized(ServerWebExchange exchange, ResponseStatusException ex) {
        if (ex.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) { 
            ModelAndView modelAndView = new ModelAndView("redirect:/login");
            modelAndView.addObject("error", ex.getReason());
            return modelAndView;
        }
        return null;
    }
}

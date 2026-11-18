package com.smartstock.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

/** Exposes a form's field errors to templates as {@code errors['field']}. */
final class Forms {

	private Forms() {
	}

	/** Adds the form and its errors to the model; returns true when the form is valid. */
	static boolean check(Model model, Object form, BindingResult result) {
		Map<String, String> errors = new LinkedHashMap<>();
		for (FieldError error : result.getFieldErrors()) {
			errors.putIfAbsent(error.getField(), error.isBindingFailure() ? "Invalid value" : error.getDefaultMessage());
		}
		model.addAttribute("form", form);
		model.addAttribute("errors", errors);
		return errors.isEmpty();
	}

	static void empty(Model model, Object form) {
		model.addAttribute("form", form);
		model.addAttribute("errors", Map.of());
	}

}

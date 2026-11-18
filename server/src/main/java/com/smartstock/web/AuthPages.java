package com.smartstock.web;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.smartstock.auth.Role;
import com.smartstock.auth.UserDtos.RegisterRequest;
import com.smartstock.auth.UserService;
import com.smartstock.common.ApiException;
import com.smartstock.security.CurrentUser;

@Controller
class AuthPages {

	private final UserService users;

	AuthPages(UserService users) {
		this.users = users;
	}

	/** Sends each role to its main page. */
	@GetMapping("/")
	String home(@AuthenticationPrincipal CurrentUser me) {
		if (me.role().isManagement()) {
			return "redirect:/dashboard";
		}
		return me.role() == Role.CASHIER ? "redirect:/pos" : "redirect:/orders";
	}

	@GetMapping("/login")
	String login() {
		return "login";
	}

	@GetMapping("/register")
	String registerForm(Model model) {
		Forms.empty(model, null);
		return "register";
	}

	@PostMapping("/register")
	String register(@Valid @ModelAttribute("form") RegisterRequest form, BindingResult result, Model model) {
		if (!Forms.check(model, form, result)) {
			return "register";
		}
		try {
			users.registerCustomer(form);
		}
		catch (ApiException ex) {
			model.addAttribute(WebSupport.ERROR, ex.getBody().getDetail());
			return "register";
		}
		return "redirect:/login?registered";
	}

}

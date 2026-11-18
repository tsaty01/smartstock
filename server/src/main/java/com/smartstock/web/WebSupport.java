package com.smartstock.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.support.RequestContextUtils;

import com.smartstock.auth.Role;
import com.smartstock.security.CurrentUser;

/** Shared model and error handling for the website pages. */
@ControllerAdvice(basePackages = "com.smartstock.web")
class WebSupport {

	static final String NOTICE = "notice";

	static final String ERROR = "error";

	/** Blank form fields mean "not given", as a missing JSON property does in the API. */
	@InitBinder
	void blankAsNull(WebDataBinder binder) {
		binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
	}

	@ModelAttribute
	void common(Model model, HttpServletRequest request, Authentication authentication) {
		model.addAttribute("path", request.getRequestURI());
		if (authentication != null && authentication.getPrincipal() instanceof CurrentUser me) {
			model.addAttribute("me", me);
			model.addAttribute("staff", me.role().isStaff());
			model.addAttribute("management", me.role().isManagement());
			model.addAttribute("admin", me.role() == Role.ADMIN);
		}
	}

	/**
	 * A rejected form goes back to the page it came from with the reason; a failed page load shows the
	 * error page with the right status.
	 */
	@ExceptionHandler(ErrorResponseException.class)
	String failed(ErrorResponseException ex, HttpServletRequest request, HttpServletResponse response, Model model) {
		String detail = ex.getBody().getDetail();
		if (!"GET".equals(request.getMethod())) {
			RequestContextUtils.getOutputFlashMap(request).put(ERROR, detail);
			String referer = request.getHeader(HttpHeaders.REFERER);
			return "redirect:" + (referer != null ? referer : "/");
		}
		response.setStatus(ex.getStatusCode().value());
		model.addAttribute("status", ex.getStatusCode().value());
		model.addAttribute("message", detail);
		return "error";
	}

}

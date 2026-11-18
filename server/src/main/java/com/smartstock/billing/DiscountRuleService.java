package com.smartstock.billing;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartstock.billing.BillingDtos.DiscountRuleRequest;
import com.smartstock.billing.BillingDtos.DiscountRuleView;
import com.smartstock.common.ApiException;

@Service
public class DiscountRuleService {

	private final DiscountRuleRepository rules;

	DiscountRuleService(DiscountRuleRepository rules) {
		this.rules = rules;
	}

	@Transactional(readOnly = true)
	public List<DiscountRuleView> list() {
		return rules.findAll(Sort.by("minAmount", "minQuantity")).stream().map(DiscountRuleView::of).toList();
	}

	@Transactional
	public DiscountRuleView create(DiscountRuleRequest request) {
		return DiscountRuleView.of(rules.save(apply(new DiscountRule(request.name()), request)));
	}

	@Transactional
	public DiscountRuleView update(long id, DiscountRuleRequest request) {
		DiscountRule rule = rules.findById(id).orElseThrow(() -> ApiException.notFound("Discount rule"));
		return DiscountRuleView.of(apply(rule, request));
	}

	private static DiscountRule apply(DiscountRule rule, DiscountRuleRequest r) {
		if (r.validFrom() != null && r.validTo() != null && r.validTo().isBefore(r.validFrom())) {
			throw ApiException.badRequest("validTo must not be before validFrom");
		}
		rule.update(r.name(), r.minQuantity(), r.minAmount(), r.percent(), r.active(), r.validFrom(), r.validTo());
		return rule;
	}

}

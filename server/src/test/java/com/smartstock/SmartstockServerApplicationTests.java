package com.smartstock;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class SmartstockServerApplicationTests extends ApiTestSupport {

	@Test
	void healthIsUpWithDatabase() throws Exception {
		get(null, "/actuator/health").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void unknownPathsOutsideTheApiAreDenied() throws Exception {
		get(null, "/anything").andExpect(status().isForbidden());
	}

}

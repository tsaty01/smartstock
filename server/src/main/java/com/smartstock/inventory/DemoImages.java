package com.smartstock.inventory;

import java.io.IOException;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Attaches the bundled photos in db/demo/images, named by barcode, to seeded demo products. */
@Component
@Profile("demo")
class DemoImages {

	private final ProductImageService images;

	DemoImages(ProductImageService images) {
		this.images = images;
	}

	@EventListener(ApplicationReadyEvent.class)
	void attach() throws IOException {
		for (Resource image : new PathMatchingResourcePatternResolver().getResources("classpath:db/demo/images/*.jpg")) {
			images.storeIfMissing(StringUtils.stripFilenameExtension(image.getFilename()), image);
		}
	}

}

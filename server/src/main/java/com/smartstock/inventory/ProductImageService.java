package com.smartstock.inventory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.smartstock.common.ApiException;
import com.smartstock.common.SmartstockProperties;

/** Product images on the persistent file volume. The type is taken from the file content, not the client. */
@Service
public class ProductImageService {

	private final ProductRepository products;

	private final Path directory;

	ProductImageService(ProductRepository products, SmartstockProperties properties) {
		this.products = products;
		this.directory = properties.storageDir().resolve("product-images");
	}

	@Transactional
	public void store(long productId, MultipartFile file) {
		Product product = products.findById(productId).orElseThrow(() -> ApiException.notFound("Product"));
		ImageType type = ImageType.detect(file);
		String name = "%d-%s.%s".formatted(productId, UUID.randomUUID(), type.extension);
		try {
			Files.createDirectories(directory);
			file.transferTo(directory.resolve(name));
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Could not store product image", ex);
		}
		String previous = product.getImageName();
		product.setImageName(name);
		if (previous != null) {
			delete(previous);
		}
	}

	@Transactional(readOnly = true)
	public StoredImage load(long productId) {
		Product product = products.findById(productId).orElseThrow(() -> ApiException.notFound("Product"));
		Path path = product.getImageName() == null ? null : directory.resolve(product.getImageName());
		if (path == null || !Files.isReadable(path)) {
			throw ApiException.notFound("Image");
		}
		return new StoredImage(new FileSystemResource(path), ImageType.fromName(product.getImageName()).mediaType);
	}

	private void delete(String name) {
		try {
			Files.deleteIfExists(directory.resolve(name));
		}
		catch (IOException ignored) {
			// An orphaned file only wastes space; the product already points at the new image.
		}
	}

	public record StoredImage(Resource resource, MediaType mediaType) {
	}

	private enum ImageType {

		PNG("png", MediaType.IMAGE_PNG, new byte[] { (byte) 0x89, 'P', 'N', 'G' }),
		JPEG("jpg", MediaType.IMAGE_JPEG, new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF }),
		WEBP("webp", MediaType.parseMediaType("image/webp"), "WEBP".getBytes());

		private final String extension;

		private final MediaType mediaType;

		private final byte[] magic;

		ImageType(String extension, MediaType mediaType, byte[] magic) {
			this.extension = extension;
			this.mediaType = mediaType;
			this.magic = magic;
		}

		static ImageType detect(MultipartFile file) {
			byte[] header = new byte[12];
			try (InputStream in = file.getInputStream()) {
				in.readNBytes(header, 0, header.length);
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
			for (ImageType type : values()) {
				// WebP: "RIFF" <size> "WEBP"
				int offset = type == WEBP ? 8 : 0;
				if (Arrays.equals(header, offset, offset + type.magic.length, type.magic, 0, type.magic.length)
						&& (type != WEBP || Arrays.equals(header, 0, 4, "RIFF".getBytes(), 0, 4))) {
					return type;
				}
			}
			throw ApiException.badRequest("Image must be PNG, JPEG or WebP");
		}

		static ImageType fromName(String name) {
			String extension = name.substring(name.lastIndexOf('.') + 1);
			return Arrays.stream(values()).filter(t -> t.extension.equals(extension)).findFirst().orElseThrow();
		}

	}

}

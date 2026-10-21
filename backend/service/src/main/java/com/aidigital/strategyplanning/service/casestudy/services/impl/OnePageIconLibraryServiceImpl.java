package com.aidigital.strategyplanning.service.casestudy.services.impl;

import com.aidigital.strategyplanning.service.casestudy.models.OnePageIconOption;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageIconSpec;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageIconLibraryService;
import com.aidigital.strategyplanning.service.casestudy.templates.OnePageIconLibrary;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Default implementation of {@link OnePageIconLibraryService}.
 */
@Service
public class OnePageIconLibraryServiceImpl implements OnePageIconLibraryService {

	private static final String TRAILING_SLASHES = "/+$";

	@Override
	public List<OnePageIconOption> options() {
		return OnePageIconLibrary.ICONS.stream()
				.map(icon -> new OnePageIconOption(icon.name(), icon.description(), imagePath(icon.name())))
				.toList();
	}

	@Override
	public boolean contains(String name) {
		return name != null && OnePageIconLibrary.ICONS.stream().map(OnePageIconSpec::name).anyMatch(name::equals);
	}

	@Override
	public String imagePath(String name) {
		return OnePageIconLibrary.IMAGE_PATH + name + OnePageIconLibrary.IMAGE_EXTENSION;
	}

	@Override
	public String imageUrl(String publicBaseUrl, String name) {
		return publicBaseUrl.replaceAll(TRAILING_SLASHES, "") + imagePath(name);
	}
}

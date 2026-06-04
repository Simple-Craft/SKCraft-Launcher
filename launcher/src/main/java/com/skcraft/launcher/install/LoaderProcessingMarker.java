package com.skcraft.launcher.install;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.hash.Hashing;
import com.skcraft.launcher.Launcher;
import com.skcraft.launcher.model.loader.InstallProcessor;
import com.skcraft.launcher.model.loader.LoaderManifest;
import com.skcraft.launcher.model.minecraft.Library;
import lombok.extern.java.Log;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Log
public final class LoaderProcessingMarker {
	private static final ObjectMapper MAPPER = new ObjectMapper()
			.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

	private LoaderProcessingMarker() {
	}

	public static String computeFingerprint(LoaderManifest loader, List<InstallProcessor> processors)
			throws JsonProcessingException {
		Map<String, Object> payload = new TreeMap<String, Object>();
		payload.put("processors", processors);
		payload.put("sidedData", new TreeMap<String, Object>(loader.getSidedData()));
		String json = MAPPER.writeValueAsString(payload);
		return Hashing.sha1().hashString(json, StandardCharsets.UTF_8).toString();
	}

	public static File markerFile(Launcher launcher, String loaderName) {
		return new File(new File(launcher.getCommonDataDir(), "loader-markers"),
				sanitize(loaderName) + ".sha1");
	}

	public static boolean matches(File markerFile, String fingerprint) {
		if (!markerFile.isFile()) return false;
		try {
			String stored = new String(Files.readAllBytes(markerFile.toPath()), StandardCharsets.UTF_8).trim();
			return stored.equalsIgnoreCase(fingerprint);
		} catch (IOException e) {
			log.warning("Failed to read loader marker " + markerFile + ": " + e);
			return false;
		}
	}

	public static void write(File markerFile, String fingerprint) {
		try {
			File parent = markerFile.getParentFile();
			if (parent != null) parent.mkdirs();
			Files.write(markerFile.toPath(), fingerprint.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			log.warning("Failed to write loader marker " + markerFile + ": " + e);
		}
	}

	public static boolean allGeneratedLibrariesPresent(Launcher launcher, LoaderManifest loader) {
		for (Library library : loader.getLibraries()) {
			if (!library.isGenerated()) continue;
			File file = launcher.getLibraryFile(library);
			if (file == null || !file.isFile()) {
				log.info("Loader marker invalidated: generated library missing " + library.getName());
				return false;
			}
		}
		return true;
	}

	private static String sanitize(String name) {
		return name.replaceAll("[^A-Za-z0-9._-]", "_");
	}
}

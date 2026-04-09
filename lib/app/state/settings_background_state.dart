import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

class AppBackgroundSettings {
  static const String _prefsBackgroundImagePath =
      'setting_background_image_path';
  static const String _prefsBackgroundMaskOpacity =
      'setting_background_mask_opacity';
  static const String _prefsPageGlowEnabled = 'setting_page_glow_enabled';

  static final ValueNotifier<String?> backgroundImagePath = ValueNotifier(null);
  static final ValueNotifier<double> backgroundMaskOpacity = ValueNotifier(
    0.35,
  );
  static final ValueNotifier<bool> pageGlowEnabled = ValueNotifier(false);

  static bool _loaded = false;

  static Future<void> ensureLoaded() async {
    if (_loaded) return;
    _loaded = true;
    final prefs = await SharedPreferences.getInstance();
    backgroundImagePath.value = prefs.getString(_prefsBackgroundImagePath);
    backgroundMaskOpacity.value =
        (prefs.getDouble(_prefsBackgroundMaskOpacity) ?? 0.5).clamp(0.0, 1.0);
    pageGlowEnabled.value = prefs.getBool(_prefsPageGlowEnabled) ?? false;
  }

  static Future<void> setBackgroundImagePath(String? path) async {
    final prefs = await SharedPreferences.getInstance();
    if (path == null || path.isEmpty) {
      await prefs.remove(_prefsBackgroundImagePath);
      backgroundImagePath.value = null;
      return;
    }
    await prefs.setString(_prefsBackgroundImagePath, path);
    backgroundImagePath.value = path;
  }

  static Future<void> setBackgroundMaskOpacity(double value) async {
    final prefs = await SharedPreferences.getInstance();
    final next = value.clamp(0.0, 1.0);
    await prefs.setDouble(_prefsBackgroundMaskOpacity, next);
    backgroundMaskOpacity.value = next;
  }

  static Future<void> setPageGlowEnabled(bool enabled) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_prefsPageGlowEnabled, enabled);
    pageGlowEnabled.value = enabled;
  }
}

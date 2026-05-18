import 'package:dio/dio.dart';

class AppUpdateInfo {
  final String latestVersion;
  final String? releaseName;
  final String? releaseUrl;
  final bool hasUpdate;

  const AppUpdateInfo({
    required this.latestVersion,
    required this.hasUpdate,
    this.releaseName,
    this.releaseUrl,
  });
}

class AppUpdateService {
  AppUpdateService._();

  static final AppUpdateService instance = AppUpdateService._();

  static const String releasePageUrl =
      'https://github.com/Happier-X/Muses/releases/latest';
  static const String latestReleaseApiUrl =
      'https://api.github.com/repos/Happier-X/Muses/releases/latest';

  final Dio _dio = Dio(
    BaseOptions(
      connectTimeout: const Duration(seconds: 8),
      receiveTimeout: const Duration(seconds: 8),
    ),
  );

  Future<AppUpdateInfo> checkLatest(String currentVersion) async {
    final response = await _dio.get<Map<String, dynamic>>(latestReleaseApiUrl);
    final data = response.data ?? <String, dynamic>{};
    final tag = (data['tag_name'] as String? ?? '').trim();
    final name = (data['name'] as String?)?.trim();
    final url = (data['html_url'] as String?)?.trim();
    final latest = tag.isEmpty ? currentVersion : _normalizeVersion(tag);
    return AppUpdateInfo(
      latestVersion: latest,
      releaseName: name == null || name.isEmpty ? null : name,
      releaseUrl: url == null || url.isEmpty ? releasePageUrl : url,
      hasUpdate: _compareVersions(latest, currentVersion) > 0,
    );
  }

  String _normalizeVersion(String version) {
    final value = version.trim();
    if (value.startsWith('v') || value.startsWith('V')) {
      return value.substring(1);
    }
    return value;
  }

  int _compareVersions(String a, String b) {
    final left = _normalizeVersion(a).split(RegExp(r'[.+-]'));
    final right = _normalizeVersion(b).split(RegExp(r'[.+-]'));
    final length = left.length > right.length ? left.length : right.length;
    for (var i = 0; i < length; i++) {
      final l = i < left.length ? int.tryParse(left[i]) ?? 0 : 0;
      final r = i < right.length ? int.tryParse(right[i]) ?? 0 : 0;
      if (l != r) return l.compareTo(r);
    }
    return 0;
  }
}

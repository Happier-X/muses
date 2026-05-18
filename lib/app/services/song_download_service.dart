import 'dart:convert';
import 'dart:io';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:path/path.dart' as p;
import 'package:permission_handler/permission_handler.dart';

import '../state/settings_state.dart';
import '../state/song_state.dart';
import 'cache/audio_cache_service.dart';

class SongDownloadResult {
  final bool success;
  final String message;
  final String? savedPath;

  const SongDownloadResult({
    required this.success,
    required this.message,
    this.savedPath,
  });
}

class SongDownloadService {
  SongDownloadService._();

  static final SongDownloadService instance = SongDownloadService._();

  static const MethodChannel _channel = MethodChannel(
    'com.happier.muses/downloads',
  );

  final AudioCacheService _audioCache = AudioCacheService.instance;

  Future<SongDownloadResult> saveToLocal(SongEntity song) async {
    await SongDownloadSettings.ensureLoaded();
    final useCustom = SongDownloadSettings.useCustomDirectory.value;
    final customPath = SongDownloadSettings.customDirectoryPath.value;
    if (useCustom && customPath != null && customPath.trim().isNotEmpty) {
      return saveToDirectory(song, customPath);
    }
    return downloadToSystemFolder(song);
  }

  Future<SongDownloadResult> downloadToSystemFolder(SongEntity song) async {
    final source = await _prepareSourceFile(song);
    if (source == null) {
      return const SongDownloadResult(success: false, message: '无法获取歌曲文件');
    }

    final fileName = _buildFileName(song, source.path);
    try {
      final savedPath = await _channel.invokeMethod<String>('saveToDownloads', {
        'sourcePath': source.path,
        'fileName': fileName,
        'mimeType': _mimeTypeFor(fileName),
        'subdirectory': 'Muses',
      });
      if (savedPath == null || savedPath.trim().isEmpty) {
        return const SongDownloadResult(success: false, message: '保存失败');
      }
      return SongDownloadResult(
        success: true,
        message: '已保存到下载目录',
        savedPath: savedPath,
      );
    } on PlatformException catch (e) {
      final message = (e.message ?? '').trim();
      return SongDownloadResult(
        success: false,
        message: message.isEmpty ? '保存到下载目录失败' : message,
      );
    } catch (e) {
      if (kDebugMode) {
        debugPrint('downloadToSystemFolder failed: $e');
      }
      return const SongDownloadResult(success: false, message: '保存到下载目录失败');
    }
  }

  Future<SongDownloadResult> chooseAndSaveToCustomFolder(
    SongEntity song,
  ) async {
    final directoryPath = await FilePicker.platform.getDirectoryPath();
    if (directoryPath == null || directoryPath.trim().isEmpty) {
      return const SongDownloadResult(success: false, message: '已取消选择目录');
    }
    return saveToDirectory(song, directoryPath);
  }

  Future<SongDownloadResult> saveToDirectory(
    SongEntity song,
    String directoryPath,
  ) async {
    final granted = await ensureStoragePermission();
    if (!granted) {
      return const SongDownloadResult(success: false, message: '未授予存储权限');
    }
    final source = await _prepareSourceFile(song);
    if (source == null) {
      return const SongDownloadResult(success: false, message: '无法获取歌曲文件');
    }

    try {
      final dir = Directory(directoryPath);
      if (!await dir.exists()) {
        await dir.create(recursive: true);
      }
      final target = await _nextAvailableFile(
        dir,
        _buildFileName(song, source.path),
      );
      await source.copy(target.path);
      return SongDownloadResult(
        success: true,
        message: '已保存到自定义目录',
        savedPath: target.path,
      );
    } catch (e) {
      if (kDebugMode) {
        debugPrint('saveToDirectory failed: $e');
      }
      return const SongDownloadResult(success: false, message: '保存到自定义目录失败');
    }
  }

  Future<File?> _prepareSourceFile(SongEntity song) async {
    final uri = (song.uri ?? '').trim();
    if (uri.isEmpty) return null;

    if (song.isLocal) {
      final file = File(uri);
      return await file.exists() ? file : null;
    }

    final headers = _headersFromSong(song);
    final cached = await _audioCache.getCompleteCachedFile(
      uri: uri,
      headers: headers,
    );
    if (cached != null && await cached.exists()) {
      return cached;
    }

    return await _audioCache.downloadToCacheSegmented(
      uri: uri,
      headers: headers,
    );
  }

  Map<String, String>? _headersFromSong(SongEntity song) {
    final raw = (song.headersJson ?? '').trim();
    if (raw.isEmpty) return null;
    try {
      final decoded = jsonDecode(raw);
      if (decoded is Map) {
        return decoded.map(
          (key, value) => MapEntry(key.toString(), value.toString()),
        );
      }
    } catch (_) {}
    return null;
  }

  String _buildFileName(SongEntity song, String sourcePath) {
    final artist = _sanitizeName(song.artist.trim());
    final title = _sanitizeName(
      song.title.trim().isEmpty ? '未知标题' : song.title,
    );
    final ext = _resolveExtension(song, sourcePath);
    final base = artist.isEmpty || artist == '未知艺术家'
        ? title
        : '$artist - $title';
    return '$base$ext';
  }

  String _resolveExtension(SongEntity song, String sourcePath) {
    final fromUri = p.extension(sourcePath).trim();
    if (fromUri.isNotEmpty && fromUri.length <= 8) {
      return fromUri.toLowerCase();
    }
    final format = (song.format ?? '').trim().toLowerCase();
    if (format.isNotEmpty) {
      final normalized = format.startsWith('.') ? format : '.$format';
      if (normalized.length <= 8) return normalized;
    }
    return '.mp3';
  }

  String _sanitizeName(String input) {
    final cleaned = input
        .replaceAll(RegExp(r'[\\/:*?"<>|]'), '_')
        .replaceAll(RegExp(r'\s+'), ' ')
        .trim();
    if (cleaned.isEmpty) return '未知标题';
    return cleaned;
  }

  String _mimeTypeFor(String fileName) {
    final ext = p.extension(fileName).toLowerCase();
    return switch (ext) {
      '.flac' => 'audio/flac',
      '.wav' => 'audio/wav',
      '.m4a' => 'audio/mp4',
      '.aac' => 'audio/aac',
      '.ogg' => 'audio/ogg',
      _ => 'audio/mpeg',
    };
  }

  Future<File> _nextAvailableFile(Directory dir, String fileName) async {
    final ext = p.extension(fileName);
    final base = ext.isEmpty
        ? fileName
        : fileName.substring(0, fileName.length - ext.length);
    var candidate = File(p.join(dir.path, fileName));
    var index = 1;
    while (await candidate.exists()) {
      candidate = File(p.join(dir.path, '$base ($index)$ext'));
      index += 1;
    }
    return candidate;
  }

  Future<bool> ensureStoragePermission() async {
    if (!Platform.isAndroid) return true;
    final sdk = await _androidSdkInt();
    if (sdk >= 29) return true;
    final status = await Permission.storage.request();
    return status.isGranted;
  }

  Future<int> _androidSdkInt() async {
    try {
      final value = await _channel.invokeMethod<int>('getAndroidSdkInt');
      return value ?? 0;
    } catch (_) {
      return 0;
    }
  }
}

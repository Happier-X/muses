import 'package:flutter/services.dart';

class NativeAudioThumbnailService {
  NativeAudioThumbnailService._();

  static final NativeAudioThumbnailService instance =
      NativeAudioThumbnailService._();

  static const MethodChannel _channel = MethodChannel(
    'com.happier.muses/native_artwork',
  );

  Future<Uint8List?> loadThumbnail(String? path, {int size = 320}) async {
    final trimmed = (path ?? '').trim();
    if (trimmed.isEmpty) return null;
    try {
      final bytes = await _channel.invokeMethod<Uint8List>(
        'loadAudioThumbnail',
        {'path': trimmed, 'size': size},
      );
      return bytes == null || bytes.isEmpty ? null : bytes;
    } catch (_) {
      return null;
    }
  }
}

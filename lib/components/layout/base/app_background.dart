import 'dart:io';
import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'dart:ui' as ui;

import '../../../app/state/settings_state.dart';

class AppBackground extends StatefulWidget {
  final Widget child;

  const AppBackground({super.key, required this.child});

  @override
  State<AppBackground> createState() => _AppBackgroundState();
}

class _AppBackgroundState extends State<AppBackground> {
  @override
  void initState() {
    super.initState();
    AppBackgroundSettings.ensureLoaded();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final isDark = theme.brightness == Brightness.dark;

    return AnimatedBuilder(
      animation: Listenable.merge([
        AppBackgroundSettings.backgroundImagePath,
        AppBackgroundSettings.backgroundMaskOpacity,
        AppBackgroundSettings.pageGlowEnabled,
      ]),
      builder: (context, _) {
        final path = AppBackgroundSettings.backgroundImagePath.value;
        final maskOpacity = AppBackgroundSettings.backgroundMaskOpacity.value;
        final glowEnabled = AppBackgroundSettings.pageGlowEnabled.value;
        final imagePath = path;
        final hasImage =
            imagePath != null &&
            imagePath.isNotEmpty &&
            File(imagePath).existsSync();
        final baseColor = colorScheme.surface;
        final maskColor = colorScheme.surface.withValues(alpha: maskOpacity);
        return Container(
          color: baseColor,
          child: Stack(
            children: [
              if (hasImage)
                Positioned.fill(
                  child: Image.file(File(imagePath), fit: BoxFit.cover),
                ),
              if (glowEnabled) ...[
                _glow(
                  alignment: Alignment.topRight,
                  size: 320,
                  colors: _glowColors(
                    colorScheme,
                    isDark: isDark,
                    primary: false,
                  ),
                ),
                _glow(
                  alignment: Alignment.bottomLeft,
                  size: 300,
                  colors: _glowColors(
                    colorScheme,
                    isDark: isDark,
                    primary: true,
                  ),
                ),
              ],
              if (hasImage && maskOpacity > 0)
                Positioned.fill(
                  child: BackdropFilter(
                    filter: ui.ImageFilter.blur(sigmaX: 16, sigmaY: 16),
                    child: Container(color: maskColor),
                  ),
                ),
              widget.child,
            ],
          ),
        );
      },
    );
  }

  List<Color> _glowColors(
    ColorScheme colorScheme, {
    required bool isDark,
    required bool primary,
  }) {
    if (isDark) {
      final seed = primary
          ? colorScheme.primary.withValues(alpha: 0.22)
          : colorScheme.tertiary.withValues(alpha: 0.16);
      return [seed, seed.withValues(alpha: 0.0)];
    }

    final lightPrimary = primary
        ? const Color(0x66CBE8FF)
        : const Color(0x66FDE2A7);
    return [lightPrimary, lightPrimary.withValues(alpha: 0.0)];
  }

  Widget _glow({
    required Alignment alignment,
    required double size,
    required List<Color> colors,
  }) {
    return Align(
      alignment: alignment,
      child: Container(
        width: size,
        height: size,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          gradient: RadialGradient(
            radius: 0.72,
            colors: colors,
            stops: const [0.0, 1.0],
            transform: const GradientRotation(math.pi / 8),
          ),
        ),
      ),
    );
  }
}

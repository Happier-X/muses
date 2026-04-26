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
        final baseColor = theme.scaffoldBackgroundColor;
        final maskColor = theme.scaffoldBackgroundColor.withValues(
          alpha: maskOpacity,
        );
        return Container(
          color: baseColor,
          child: Stack(
            children: [
              if (hasImage)
                Positioned.fill(
                  child: Image.file(File(imagePath), fit: BoxFit.cover),
                ),
              if (glowEnabled) ...[
                Positioned.fill(
                  child: IgnorePointer(
                    child: RepaintBoundary(
                      child: Stack(
                        children: [
                          _glow(
                            alignment: const Alignment(0.88, -0.92),
                            size: 320,
                            scaleX: 1.12,
                            scaleY: 0.94,
                            blurSigma: 28,
                            colors: _glowColors(
                              colorScheme,
                              isDark: isDark,
                              primary: false,
                              strength: 0.88,
                            ),
                          ),
                          _glow(
                            alignment: const Alignment(-0.86, 0.82),
                            size: 290,
                            scaleX: 0.98,
                            scaleY: 1.08,
                            blurSigma: 24,
                            colors: _glowColors(
                              colorScheme,
                              isDark: isDark,
                              primary: true,
                              strength: 0.82,
                            ),
                          ),
                          _glow(
                            alignment: const Alignment(0.12, -0.08),
                            size: 240,
                            scaleX: 1.22,
                            scaleY: 0.82,
                            blurSigma: 30,
                            colors: _glowColors(
                              colorScheme,
                              isDark: isDark,
                              primary: true,
                              strength: 0.42,
                            ),
                          ),
                        ],
                      ),
                    ),
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
    required double strength,
  }) {
    if (isDark) {
      final seed = primary
          ? colorScheme.primary.withValues(alpha: 0.22 * strength)
          : colorScheme.tertiary.withValues(alpha: 0.16 * strength);
      return [
        seed,
        seed.withValues(alpha: seed.a * 0.5),
        seed.withValues(alpha: 0.0),
      ];
    }

    final lightPrimary = primary
        ? const Color(0x66CBE8FF)
        : const Color(0x66FDE2A7);
    final seed = lightPrimary.withValues(alpha: lightPrimary.a * strength);
    return [
      seed,
      seed.withValues(alpha: seed.a * 0.45),
      seed.withValues(alpha: 0.0),
    ];
  }

  Widget _glow({
    required Alignment alignment,
    required double size,
    required List<Color> colors,
    required double scaleX,
    required double scaleY,
    required double blurSigma,
  }) {
    return Align(
      alignment: alignment,
      child: Transform.scale(
        scaleX: scaleX,
        scaleY: scaleY,
        child: ImageFiltered(
          imageFilter: ui.ImageFilter.blur(
            sigmaX: blurSigma,
            sigmaY: blurSigma,
          ),
          child: Container(
            width: size,
            height: size,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              gradient: RadialGradient(
                radius: 0.76,
                colors: colors,
                stops: const [0.0, 0.55, 1.0],
                transform: const GradientRotation(math.pi / 8),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

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

class _AppBackgroundState extends State<AppBackground>
    with SingleTickerProviderStateMixin {
  late final AnimationController _glowController;

  @override
  void initState() {
    super.initState();
    AppBackgroundSettings.ensureLoaded();
    _glowController = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 20),
    )..repeat();
  }

  @override
  void dispose() {
    _glowController.dispose();
    super.dispose();
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
                Positioned.fill(
                  child: IgnorePointer(
                    child: AnimatedBuilder(
                      animation: _glowController,
                      builder: (context, _) {
                        final t = _glowController.value;
                        return Stack(
                          children: [
                            _glow(
                              alignment: Alignment(
                                0.82 + 0.14 * math.sin(t * math.pi * 2),
                                -0.92 + 0.26 * math.cos(t * math.pi * 2),
                              ),
                              size: 380,
                              scaleX: 1.18,
                              scaleY: 0.92,
                              blurSigma: 46,
                              colors: _glowColors(
                                colorScheme,
                                isDark: isDark,
                                primary: false,
                                strength: 1.0,
                              ),
                            ),
                            _glow(
                              alignment: Alignment(
                                -0.88 + 0.20 * math.cos(t * math.pi * 2),
                                0.84 + 0.18 * math.sin(t * math.pi * 2),
                              ),
                              size: 340,
                              scaleX: 0.96,
                              scaleY: 1.12,
                              blurSigma: 40,
                              colors: _glowColors(
                                colorScheme,
                                isDark: isDark,
                                primary: true,
                                strength: 1.0,
                              ),
                            ),
                            _glow(
                              alignment: Alignment(
                                -0.10 + 0.36 * math.sin(t * math.pi * 2),
                                -0.18 + 0.24 * math.cos(t * math.pi * 2),
                              ),
                              size: 300,
                              scaleX: 1.35,
                              scaleY: 0.78,
                              blurSigma: 50,
                              colors: _glowColors(
                                colorScheme,
                                isDark: isDark,
                                primary: true,
                                strength: 0.58,
                              ),
                            ),
                            _sweepGlow(
                              color: isDark
                                  ? colorScheme.primary.withValues(alpha: 0.08)
                                  : colorScheme.primary.withValues(alpha: 0.06),
                              angle: -0.42 + 0.28 * math.sin(t * math.pi * 2),
                              dx: -18 + 36 * math.sin(t * math.pi * 2),
                              dy: -14 + 28 * math.cos(t * math.pi * 2),
                            ),
                          ],
                        );
                      },
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

  Widget _sweepGlow({
    required Color color,
    required double angle,
    required double dx,
    required double dy,
  }) {
    return Transform.translate(
      offset: Offset(dx, dy),
      child: Transform.rotate(
        angle: angle,
        child: Center(
          child: ImageFiltered(
            imageFilter: ui.ImageFilter.blur(sigmaX: 30, sigmaY: 30),
            child: Container(
              width: 220,
              height: 680,
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                  colors: [
                    color.withValues(alpha: 0.0),
                    color,
                    color.withValues(alpha: 0.0),
                  ],
                  stops: const [0.0, 0.48, 1.0],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

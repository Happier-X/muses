import 'dart:ui' as ui;

import 'package:flutter/material.dart';

import '../../app/state/settings_state.dart';
import '../../app/theme/app_styles.dart';

class GlassPanel extends StatelessWidget {
  final Widget child;
  final EdgeInsetsGeometry? padding;
  final BorderRadiusGeometry borderRadius;
  final Color? color;
  final Color? borderColor;
  final Color? shadowColor;
  final List<BoxShadow>? boxShadow;
  final double blurSigma;
  final VoidCallback? onTap;
  final double? height;

  const GlassPanel({
    super.key,
    required this.child,
    this.padding,
    this.borderRadius = const BorderRadius.all(Radius.circular(20)),
    this.color,
    this.borderColor,
    this.shadowColor,
    this.boxShadow,
    this.blurSigma = 1,
    this.onTap,
    this.height,
  });

  BorderRadius get _resolvedBorderRadius {
    final value = borderRadius;
    if (value is BorderRadius) return value;
    return BorderRadius.circular(20);
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final resolvedColor = color ?? theme.appPanelColor;
    final resolvedBorderColor = borderColor ?? theme.appPanelBorderColor;
    final resolvedShadowColor = shadowColor ?? theme.appPanelShadowColor;
    final resolvedShadow =
        boxShadow ??
        [
          BoxShadow(
            color: resolvedShadowColor,
            blurRadius: 16,
            offset: const Offset(0, 4),
          ),
        ];

    return ValueListenableBuilder<bool>(
      valueListenable: AppBackgroundSettings.glassEffectEnabled,
      builder: (context, glassEnabled, _) {
        return ValueListenableBuilder<double>(
          valueListenable: AppBackgroundSettings.panelBlurStrength,
          builder: (context, panelBlurStrength, _) {
            final sigma = glassEnabled ? panelBlurStrength * blurSigma : 0.0;
            final isInvisible = resolvedColor.a <= 0 && sigma <= 0;
            final panel = Container(
              height: height,
              decoration: BoxDecoration(
                color: resolvedColor,
                borderRadius: _resolvedBorderRadius,
                border: isInvisible
                    ? null
                    : Border.all(color: resolvedBorderColor),
                boxShadow: isInvisible ? null : resolvedShadow,
              ),
              child: padding == null
                  ? child
                  : Padding(padding: padding!, child: child),
            );

            final material = Material(
              color: Colors.transparent,
              child: onTap == null
                  ? panel
                  : InkWell(
                      borderRadius: _resolvedBorderRadius,
                      onTap: onTap,
                      child: panel,
                    ),
            );

            return ClipRRect(
              borderRadius: _resolvedBorderRadius,
              child: glassEnabled && sigma > 0
                  ? BackdropFilter(
                      filter: ui.ImageFilter.blur(sigmaX: sigma, sigmaY: sigma),
                      child: material,
                    )
                  : material,
            );
          },
        );
      },
    );
  }
}
